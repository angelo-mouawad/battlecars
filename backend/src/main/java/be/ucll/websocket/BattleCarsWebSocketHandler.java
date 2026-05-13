package be.ucll.websocket;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import be.ucll.service.SensorService;

@Component
public class BattleCarsWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SensorService sensorService;

    private WebSocketSession espSession = null;
    private final List<WebSocketSession> browserSessions = new ArrayList<>();

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
            // Message from ESP32 → save sensor data to DB + forward to all browsers
            System.out.println("Received from ESP32: " + payload);

            // Save to database
            try {
                sensorService.saveFromJson(payload);
            } catch (Exception e) {
                System.out.println("Could not save sensor reading: " + e.getMessage());
            }

            // Forward live data to all connected browsers
            for (WebSocketSession browser : new ArrayList<>(browserSessions)) {
                if (browser.isOpen()) {
                    browser.sendMessage(new TextMessage(payload));
                }
            }
        } else {
            // Message from browser → forward control command to ESP32
            System.out.println("Received command from browser: " + payload);
            if (espSession != null && espSession.isOpen()) {
                espSession.sendMessage(new TextMessage(payload));
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
            System.out.println("Browser disconnected: " + session.getId());
        }
    }
}