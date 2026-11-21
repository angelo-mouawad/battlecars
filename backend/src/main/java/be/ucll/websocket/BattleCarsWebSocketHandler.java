package be.ucll.websocket;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.ucll.security.WebSocketAuthInterceptor;
import be.ucll.service.CommentaryService;
import be.ucll.service.GameService;
import be.ucll.service.SensorService;
import be.ucll.service.VoiceService;
import jakarta.annotation.PreDestroy;

@Component
public class BattleCarsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleCarsWebSocketHandler.class);

    static final Pattern CAR_COMMAND = Pattern.compile("^[12]:(UP|DOWN|LEFT|RIGHT|STOP|HAMMER)$");
    private static final int HIT_DAMAGE = 10;
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

    private final SensorService sensorService;
    private final GameService gameService;
    private final CommentaryService commentaryService;
    private final VoiceService voiceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sensor-persistence");
        thread.setDaemon(true);
        return thread;
    });

    private final Set<WebSocketSession> browserSessions = new CopyOnWriteArraySet<>();
    private volatile WebSocketSession espSession;
    private volatile Long activeMatchId;
    private volatile String player1Name = "Player 1";
    private volatile String player2Name = "Player 2";

    public BattleCarsWebSocketHandler(SensorService sensorService, GameService gameService,
                                      CommentaryService commentaryService, VoiceService voiceService) {
        this.sensorService = sensorService;
        this.gameService = gameService;
        this.commentaryService = commentaryService;
        this.voiceService = voiceService;
    }

    @PreDestroy
    void shutdown() {
        persistenceExecutor.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);

        if (isEsp(rawSession)) {
            WebSocketSession previous = espSession;
            espSession = session;
            if (previous != null && previous.isOpen() && !previous.getId().equals(session.getId())) {
                closeQuietly(previous, CloseStatus.POLICY_VIOLATION.withReason("Replaced by a new hub connection"));
            }
            log.info("Hub connected: {}", rawSession.getId());
            broadcastHubStatus();
        } else {
            browserSessions.add(session);
            log.info("Browser connected: {}", rawSession.getId());
            send(session, hubStatusMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload() == null ? "" : message.getPayload().trim();
        WebSocketSession hub = espSession;

        if (hub != null && hub.getId().equals(session.getId())) {
            handleHubMessage(payload);
        } else {
            handleBrowserMessage(payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSession hub = espSession;
        if (hub != null && hub.getId().equals(session.getId())) {
            espSession = null;
            log.info("Hub disconnected: {}", status);
            broadcastHubStatus();
        } else {
            browserSessions.removeIf(s -> s.getId().equals(session.getId()) || !s.isOpen());
            log.info("Browser disconnected: {}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error on {}: {}", session.getId(), exception.getMessage());
    }

    void handleHubMessage(String payload) {
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Ignoring hub message that is not JSON: {}", abbreviate(payload));
            return;
        }
        if (node == null || !node.isObject()) {
            log.warn("Ignoring hub message that is not a JSON object");
            return;
        }

        Long matchId = activeMatchId;
        persistenceExecutor.execute(() -> {
            try {
                if (matchId != null) {
                    sensorService.saveFromJsonWithMatch(payload, matchId);
                } else {
                    sensorService.saveFromJson(payload);
                }
            } catch (Exception e) {
                log.warn("Could not save sensor reading: {}", e.getMessage());
            }
        });

        if (matchId != null && node.path("hit").asBoolean(false)) {
            int carId = node.path("carId").asInt(0);
            if (carId == 1 || carId == 2) {
                processHit(matchId, carId);
            } else {
                log.warn("Ignoring hit with invalid carId: {}", node.path("carId"));
            }
        }

        broadcastToBrowsers(payload);
    }

    private void processHit(Long matchId, int carId) {
        Map<String, Object> hitResult;
        try {
            hitResult = gameService.processHit(matchId, carId, HIT_DAMAGE);
        } catch (Exception e) {
            log.warn("Could not process hit for car {}: {}", carId, e.getMessage());
            return;
        }

        broadcastJson(hitResult);

        if (Boolean.TRUE.equals(hitResult.get("matchOver"))) {
            log.info("Match {} ended, winner: {}", matchId, hitResult.get("winnerName"));
            activeMatchId = null;
        }

        int car1Health = healthOf(hitResult.get("car1Health"));
        int car2Health = healthOf(hitResult.get("car2Health"));
        String p1 = player1Name;
        String p2 = player2Name;

        commentaryService.generateCommentary(hitResult, p1, car1Health, p2, car2Health)
                .thenCompose(commentary -> {
                    if (commentary == null || commentary.isBlank()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    broadcastJson(Map.of("type", "COMMENTARY", "text", commentary));
                    return voiceService.generateSpeech(commentary).thenAccept(audio -> {
                        if (audio != null && audio.length > 0) {
                            broadcastJson(Map.of("type", "COMMENTARY_AUDIO",
                                    "audio", Base64.getEncoder().encodeToString(audio)));
                        }
                    });
                })
                .exceptionally(error -> {
                    log.warn("Commentary pipeline failed: {}", error.getMessage());
                    return null;
                });
    }

    void handleBrowserMessage(String payload) {
        if (payload.startsWith("MATCH_START:")) {
            startMatchSession(payload.substring("MATCH_START:".length()).trim());
        } else if (payload.equals("MATCH_END")) {
            activeMatchId = null;
            player1Name = "Player 1";
            player2Name = "Player 2";
            log.info("Active match cleared by browser");
        } else if (CAR_COMMAND.matcher(payload).matches()) {
            WebSocketSession hub = espSession;
            if (hub != null && hub.isOpen()) {
                send(hub, payload);
            }
        } else {
            log.warn("Ignoring unknown browser message: {}", abbreviate(payload));
        }
    }

    private void startMatchSession(String rawId) {
        long matchId;
        try {
            matchId = Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            log.warn("Could not parse match id from: {}", abbreviate(rawId));
            return;
        }

        try {
            Map<String, Object> state = gameService.getMatchState(matchId);
            if (!"ACTIVE".equals(state.get("status"))) {
                log.warn("Ignoring MATCH_START for match {} with status {}", matchId, state.get("status"));
                return;
            }
            activeMatchId = matchId;
            if (state.get("players") instanceof List<?> players && players.size() >= 2) {
                player1Name = nameOf(players.get(0), "Player 1");
                player2Name = nameOf(players.get(1), "Player 2");
            }
            log.info("Active match set to {} ({} vs {})", matchId, player1Name, player2Name);
        } catch (Exception e) {
            log.warn("Could not start match {}: {}", matchId, e.getMessage());
        }
    }

    private static String nameOf(Object player, String fallback) {
        if (player instanceof Map<?, ?> map && map.get("playerName") instanceof String name && !name.isBlank()) {
            return name;
        }
        return fallback;
    }

    private static int healthOf(Object value) {
        return value instanceof Number number ? number.intValue() : 50;
    }

    /** Set by {@link WebSocketAuthInterceptor} only after the hub has presented HUB_TOKEN. */
    private static boolean isEsp(WebSocketSession session) {
        return session.getAttributes().get(WebSocketAuthInterceptor.CLIENT_ATTRIBUTE)
                == WebSocketAuthInterceptor.Client.HUB;
    }

    private String hubStatusMessage() {
        WebSocketSession hub = espSession;
        boolean connected = hub != null && hub.isOpen();
        return "{\"type\":\"HUB_STATUS\",\"connected\":" + connected + "}";
    }

    private void broadcastHubStatus() {
        broadcastToBrowsers(hubStatusMessage());
    }

    private void broadcastJson(Object value) {
        try {
            broadcastToBrowsers(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.warn("Could not serialize broadcast: {}", e.getMessage());
        }
    }

    private void broadcastToBrowsers(String message) {
        for (WebSocketSession browser : browserSessions) {
            if (browser.isOpen()) {
                send(browser, message);
            } else {
                browserSessions.remove(browser);
            }
        }
    }

    private void send(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            log.warn("Could not send to {}: {}", session.getId(), e.getMessage());
        }
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            log.debug("Session {} was already closed", session.getId());
        }
    }

    private static String abbreviate(String value) {
        return value.length() > 80 ? value.substring(0, 80) + "..." : value;
    }

    Long getActiveMatchId() {
        return activeMatchId;
    }
}
