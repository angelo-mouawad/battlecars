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

import be.ucll.service.GameService;
import be.ucll.service.SensorService;

@Component
public class BattleCarsWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private GameService gameService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession espSession = null;
    private final List<WebSocketSession> browserSessions = new ArrayList<>();

    // Set by the browser when a match starts: "MATCH_START:{matchId}"
    // Cleared when the match ends or browser sends "MATCH_END"
    private Long activeMatchId = null;

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

            // Save to DB — tag with match_id if a match is running
            try {
                if (activeMatchId != null) {
                    sensorService.saveFromJsonWithMatch(payload, activeMatchId);
                } else {
                    sensorService.saveFromJson(payload);
                }
            } catch (Exception e) {
                System.out.println("Could not save sensor reading: " + e.getMessage());
            }

            // If a match is active, check for hit events and apply game logic
            if (activeMatchId != null) {
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    boolean isHit = node.has("hit") && node.get("hit").asBoolean();

                    if (isHit) {
                        Integer carId = node.has("carId") ? node.get("carId").asInt() : null;

                        // Determine damage from hitStrength — default 10
                        // light = 5, medium = 10, hard = 15
                        int damage = 10;
                        if (node.has("hitStrength")) {
                            damage = switch (node.get("hitStrength").asText()) {
                                case "light"  -> 5;
                                case "medium" -> 10;
                                case "hard"   -> 15;
                                default       -> 10;
                            };
                        }

                        if (carId != null) {
                            Map<String, Object> hitResult = gameService.processHit(activeMatchId, carId, damage);

                            // Broadcast HIT_RESULT to all browsers so they can update HP bars
                            broadcastToBrowsers(objectMapper.writeValueAsString(hitResult));

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
            // (browsers use this for movement, speed, orientation display)
            broadcastToBrowsers(payload);

        } else {
            // ----------------------------------------------------------------
            // Message from browser
            // ----------------------------------------------------------------
            System.out.println("Received from browser: " + payload);

            if (payload.startsWith("MATCH_START:")) {
                // Browser is telling us which match just started
                try {
                    activeMatchId = Long.parseLong(payload.split(":")[1].trim());
                    System.out.println("Active match set to: " + activeMatchId);
                } catch (Exception e) {
                    System.out.println("Could not parse match ID from: " + payload);
                }
            } else if (payload.equals("MATCH_END")) {
                // Browser is forcing the match to end (e.g. player quit)
                activeMatchId = null;
                System.out.println("Active match cleared by browser");
            } else {
                // Regular movement command — forward to ESP32
                // Format: "1:UP", "1:LEFT", "2:DOWN", etc.
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