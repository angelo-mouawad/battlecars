package be.ucll.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import be.ucll.security.WebSocketAuthInterceptor;
import be.ucll.security.WebSocketAuthInterceptor.Client;
import be.ucll.service.CommentaryService;
import be.ucll.service.GameService;
import be.ucll.service.SensorService;
import be.ucll.service.VoiceService;

@ExtendWith(MockitoExtension.class)
class BattleCarsWebSocketHandlerTest {

    @Mock private SensorService sensorService;
    @Mock private GameService gameService;
    @Mock private CommentaryService commentaryService;
    @Mock private VoiceService voiceService;

    private BattleCarsWebSocketHandler handler;
    private WebSocketSession hub;
    private WebSocketSession browser;
    private final List<String> hubInbox = new ArrayList<>();
    private final List<String> browserInbox = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        handler = new BattleCarsWebSocketHandler(sensorService, gameService, commentaryService, voiceService);
        hub = session("hub", Client.HUB, hubInbox);
        browser = session("browser", Client.BROWSER, browserInbox);
        handler.afterConnectionEstablished(browser);
        handler.afterConnectionEstablished(hub);
        hubInbox.clear();
        browserInbox.clear();
    }

    private static WebSocketSession session(String id, Client client, List<String> inbox) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        lenient().when(session.getId()).thenReturn(id);
        lenient().when(session.getAttributes())
                .thenReturn(new HashMap<>(Map.of(WebSocketAuthInterceptor.CLIENT_ATTRIBUTE, client)));
        lenient().when(session.isOpen()).thenReturn(true);
        lenient().doAnswer(call -> {
            WebSocketMessage<?> message = call.getArgument(0);
            inbox.add(message.getPayload().toString());
            return null;
        }).when(session).sendMessage(any());
        return session;
    }

    private void fromBrowser(String text) throws Exception {
        handler.handleTextMessage(browser, new TextMessage(text));
    }

    private void fromHub(String text) throws Exception {
        handler.handleTextMessage(hub, new TextMessage(text));
    }

    @Test
    void browserLearnsWhetherTheHubIsConnected() throws Exception {
        List<String> inbox = new ArrayList<>();
        handler.afterConnectionEstablished(session("late", Client.BROWSER, inbox));
        assertThat(inbox).containsExactly("{\"type\":\"HUB_STATUS\",\"connected\":true}");

        handler.afterConnectionClosed(hub, CloseStatus.NORMAL);
        assertThat(browserInbox).contains("{\"type\":\"HUB_STATUS\",\"connected\":false}");
    }

    @Test
    void validCarCommandsAreForwardedToTheHub() throws Exception {
        fromBrowser("1:UP");
        fromBrowser("2:STOP");
        fromBrowser("1:HAMMER");

        assertThat(hubInbox).containsExactly("1:UP", "2:STOP", "1:HAMMER");
    }

    @Test
    void unknownBrowserMessagesNeverReachTheHub() throws Exception {
        fromBrowser("3:UP");
        fromBrowser("1:FLY");
        fromBrowser("1:UP; 2:UP");
        fromBrowser("");

        assertThat(hubInbox).isEmpty();
    }

    @Test
    void matchStartIsIgnoredWhenTheMatchIsNotActive() throws Exception {
        when(gameService.getMatchState(5L)).thenReturn(Map.of("status", "WAITING", "players", List.of()));

        fromBrowser("MATCH_START:5");

        assertThat(handler.getActiveMatchId()).isNull();
    }

    @Test
    void matchStartWithGarbageIdIsIgnored() throws Exception {
        fromBrowser("MATCH_START:abc");

        assertThat(handler.getActiveMatchId()).isNull();
        verify(gameService, never()).getMatchState(any());
    }

    @Test
    void hitFromHubDuringAMatchAppliesDamageAndBroadcasts() throws Exception {
        when(gameService.getMatchState(7L)).thenReturn(Map.of(
                "status", "ACTIVE",
                "players", List.of(Map.of("playerName", "Angelo"), Map.of("playerName", "Lama"))));
        when(gameService.processHit(7L, 2, 10)).thenReturn(Map.of(
                "type", "HIT_RESULT", "hitCarId", 2, "damage", 10,
                "car1Health", 50, "car2Health", 40, "matchOver", false));
        when(commentaryService.generateCommentary(any(), eq("Angelo"), anyInt(), eq("Lama"), anyInt()))
                .thenReturn(CompletableFuture.completedFuture("What a smash!"));
        when(voiceService.generateSpeech("What a smash!")).thenReturn(CompletableFuture.completedFuture(null));

        fromBrowser("MATCH_START:7");
        String hit = "{\"carId\":2,\"hit\":true,\"orientation\":\"upright\"}";
        fromHub(hit);

        assertThat(browserInbox).anyMatch(m -> m.contains("\"HIT_RESULT\""));
        assertThat(browserInbox).anyMatch(m -> m.contains("\"type\":\"COMMENTARY\"") && m.contains("\"text\":\"What a smash!\""));
        assertThat(browserInbox).contains(hit);
        verify(sensorService, timeout(1000)).saveFromJsonWithMatch(hit, 7L);
    }

    @Test
    void matchOverClearsTheActiveMatch() throws Exception {
        when(gameService.getMatchState(7L)).thenReturn(Map.of("status", "ACTIVE", "players", List.of()));
        when(gameService.processHit(7L, 1, 10)).thenReturn(Map.of(
                "type", "HIT_RESULT", "hitCarId", 1, "damage", 10, "matchOver", true, "winnerName", "Lama"));
        when(commentaryService.generateCommentary(any(), anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(null));

        fromBrowser("MATCH_START:7");
        fromHub("{\"carId\":1,\"hit\":true}");

        assertThat(handler.getActiveMatchId()).isNull();
    }

    @Test
    void hitOutsideAMatchIsStoredButDoesNotTouchTheGame() throws Exception {
        String hit = "{\"carId\":1,\"hit\":true,\"orientation\":\"upright\"}";
        fromHub(hit);

        verify(sensorService, timeout(1000)).saveFromJson(hit);
        verify(gameService, never()).processHit(any(), any(), anyInt());
        assertThat(browserInbox).containsExactly(hit);
    }

    @Test
    void malformedHubMessagesAreDropped() throws Exception {
        fromHub("not json");
        fromHub("[1,2,3]");

        assertThat(browserInbox).isEmpty();
        verify(sensorService, never()).saveFromJson(anyString());
    }

    @Test
    void aFailingGameServiceDoesNotBreakTheConnection() throws Exception {
        when(gameService.getMatchState(7L)).thenReturn(Map.of("status", "ACTIVE", "players", List.of()));
        when(gameService.processHit(7L, 1, 10)).thenThrow(new IllegalStateException("Match is not active"));

        fromBrowser("MATCH_START:7");
        fromHub("{\"carId\":1,\"hit\":true}");

        assertThat(browserInbox).containsExactly("{\"carId\":1,\"hit\":true}");
    }
}
