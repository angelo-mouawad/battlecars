package be.ucll.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import be.ucll.service.CommentaryService;
import be.ucll.service.GameService;
import be.ucll.service.SensorService;

@Component
public class BattleCarsWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private GameService gameService;

    @Autowired
    private CommentaryService commentaryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession espSession = null;
    private final List<WebSocketSession> browserSessions = new ArrayList<>();

    // Set by the browser when a match starts: "MATCH_START:{matchId}"
    // Cleared when the match ends or browser sends "MATCH_END"
    private Long activeMatchId = null;

    // Player names loaded from DB when match starts — used for commentary
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        if (query != null && query.equals("client=esp")) {
            espSession = session;
            System.out.println("ESP32 connected: " + session.getId());
        } else {
            browserSessions.add(session);
            System.out.println("Browser connected: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (session == espSession) {
            // ----------------------------------------------------------------
            // Message from ESP32
            // ----------------------------------------------------------------
            System.out.println("Received from ESP32: " + payload);

            // Save to DB in background — do NOT block the WebSocket thread
            final Long matchIdForSave = activeMatchId;
            new Thread(() -> {
                try {
                    if (matchIdForSave != null) {
                        sensorService.saveFromJsonWithMatch(payload, matchIdForSave);
                    } else {
                        sensorService.saveFromJson(payload);
                    }
                } catch (Exception e) {
                    System.out.println("Could not save sensor reading: " + e.getMessage());
                }
            }).start();

            // If a match is active, check for hit events and apply game logic
            if (activeMatchId != null) {
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    boolean isHit = node.has("hit") && node.get("hit").asBoolean();

                    if (isHit) {
                        Integer carId = node.has("carId") ? node.get("carId").asInt() : null;

                        // Always 10 damage — simplified from light/medium/hard
                        int damage = 10;

                        if (carId != null) {
                            Map<String, Object> hitResult = gameService.processHit(activeMatchId, carId, damage);

                            // Broadcast HIT_RESULT to all browsers so they can update HP bars
                            broadcastToBrowsers(objectMapper.writeValueAsString(hitResult));

                            // Generate and broadcast AI commentary asynchronously
                            int car1Health = hitResult.get("car1Health") != null ? (int) hitResult.get("car1Health") : 50;
                            int car2Health = hitResult.get("car2Health") != null ? (int) hitResult.get("car2Health") : 50;
                            final String p1 = player1Name;
                            final String p2 = player2Name;

                            commentaryService.generateCommentary(hitResult, p1, car1Health, p2, car2Health)
                                    .thenAccept(commentary -> {
                                        if (commentary != null && !commentary.isBlank()) {
                                            try {
                                                String commentaryMsg = objectMapper.writeValueAsString(
                                                        Map.of("type", "COMMENTARY", "text", commentary)
                                                );
                                                broadcastToBrowsers(commentaryMsg);
                                            } catch (Exception e) {
                                                System.out.println("Could not broadcast commentary: " + e.getMessage());
                                            }
                                        }
                                    });

                            // If the match is over, clear the active match ID
                            if (Boolean.TRUE.equals(hitResult.get("matchOver"))) {
                                System.out.println("Match " + activeMatchId + " ended. Winner: " + hitResult.get("winnerName"));
                                activeMatchId = null;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not process hit logic: " + e.getMessage());
                }
            }

            // Always forward the raw ESP32 payload to all browsers
            broadcastToBrowsers(payload);

        } else {
            // ----------------------------------------------------------------
            // Message from browser
            // ----------------------------------------------------------------
            System.out.println("Received from browser: " + payload);

            if (payload.startsWith("MATCH_START:")) {
                try {
                    activeMatchId = Long.parseLong(payload.split(":")[1].trim());
                    System.out.println("Active match set to: " + activeMatchId);

                    // Load actual player names from DB for commentary
                    try {
                        Map<String, Object> matchState = gameService.getMatchState(activeMatchId);
                        List<?> players = (List<?>) matchState.get("players");
                        if (players != null && players.size() >= 2) {
                            player1Name = (String) ((Map<?, ?>) players.get(0)).get("playerName");
                            player2Name = (String) ((Map<?, ?>) players.get(1)).get("playerName");
                            System.out.println("Commentary players: " + player1Name + " vs " + player2Name);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load player names: " + e.getMessage());
                    }

                } catch (Exception e) {
                    System.out.println("Could not parse match ID from: " + payload);
                }
            } else if (payload.equals("MATCH_END")) {
                activeMatchId = null;
                player1Name = "Player 1";
                player2Name = "Player 2";
                System.out.println("Active match cleared by browser");
            } else {
                // Regular movement command — forward to ESP32
                if (espSession != null && espSession.isOpen()) {
                    espSession.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session == espSession) {
            espSession = null;
            System.out.println("ESP32 disconnected");
        } else {
            browserSessions.remove(session);
            browserSessions.removeIf(s -> s == null || !s.isOpen());
            System.out.println("Browser disconnected: " + session.getId());
        }
    }

    private void broadcastToBrowsers(String message) {
        for (WebSocketSession browser : new ArrayList<>(browserSessions)) {
            if (browser != null && browser.isOpen()) {
                try {
                    browser.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    System.out.println("Could not send to browser: " + e.getMessage());
                }
            }
        }
    }
}