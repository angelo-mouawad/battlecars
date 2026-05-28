package be.ucll.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the match lifecycle API.
 *
 * These tests spin up the full Spring Boot context against a real PostgreSQL
 * database. Each test is @Transactional so all DB changes are rolled back
 * automatically after each test so no cleanup needed.
 *
 *
 * These also run automatically in CI via: mvn test :)
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class MatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Player IDs seeded by DbInitializer on startup
    // DbInitializer seeds 6 players — we use player 1 and 2
    private static final long PLAYER_1_ID = 1L;
    private static final long PLAYER_2_ID = 2L;

    // -------------------------------------------------------------------------
    // Helper — creates a match and returns its ID
    // -------------------------------------------------------------------------
    private long createMatch() throws Exception {
        Map<String, Object> body = Map.of(
                "player1Id", PLAYER_1_ID,
                "car1Id", 1,
                "player2Id", PLAYER_2_ID,
                "car2Id", 2
        );

        MvcResult result = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) response.get("matchId")).longValue();
    }

    // -------------------------------------------------------------------------
    // POST /api/matches — create match
    // -------------------------------------------------------------------------

    @Test
    void createMatch_validPlayers_returns200WithMatchId() throws Exception {
        Map<String, Object> body = Map.of(
                "player1Id", PLAYER_1_ID,
                "car1Id", 1,
                "player2Id", PLAYER_2_ID,
                "car2Id", 2
        );

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").isNumber())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.player1.health").value(50))
                .andExpect(jsonPath("$.player2.health").value(50));
    }

    @Test
    void createMatch_invalidPlayerId_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "player1Id", 99999L,   // does not exist
                "car1Id", 1,
                "player2Id", PLAYER_2_ID,
                "car2Id", 2
        );

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMatch_samePlayer_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "player1Id", PLAYER_1_ID,
                "car1Id", 1,
                "player2Id", PLAYER_1_ID,  // same player twice
                "car2Id", 2
        );

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMatch_sameCar_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "player1Id", PLAYER_1_ID,
                "car1Id", 1,
                "player2Id", PLAYER_2_ID,
                "car2Id", 1  // same car twice
        );

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/matches/{id}/start — start match
    // -------------------------------------------------------------------------

    @Test
    void startMatch_waitingMatch_returnsActive() throws Exception {
        long matchId = createMatch();

        mockMvc.perform(post("/api/matches/" + matchId + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.matchId").value(matchId));
    }

    @Test
    void startMatch_alreadyActive_returns400() throws Exception {
        long matchId = createMatch();

        // Start it once — OK
        mockMvc.perform(post("/api/matches/" + matchId + "/start"))
                .andExpect(status().isOk());

        // Try to start again — should fail
        mockMvc.perform(post("/api/matches/" + matchId + "/start"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startMatch_nonExistentMatch_returns400() throws Exception {
        mockMvc.perform(post("/api/matches/99999/start"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/matches/active — get active match
    // -------------------------------------------------------------------------

    @Test
    void getActiveMatch_whenActive_returnsMatch() throws Exception {
        long matchId = createMatch();
        mockMvc.perform(post("/api/matches/" + matchId + "/start"));

        mockMvc.perform(get("/api/matches/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getActiveMatch_whenNoneActive_returns404() throws Exception {
        // No match created — should return 404
        mockMvc.perform(get("/api/matches/active"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/matches/{id}/hit — process hit
    // -------------------------------------------------------------------------

    @Test
    void processHit_activeMatch_reducesHealth() throws Exception {
        long matchId = createMatch();
        mockMvc.perform(post("/api/matches/" + matchId + "/start"));

        Map<String, Object> hitBody = Map.of("carId", 1, "damage", 10);

        mockMvc.perform(post("/api/matches/" + matchId + "/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("HIT_RESULT"))
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.hitCarId").value(1))
                .andExpect(jsonPath("$.damage").value(10))
                .andExpect(jsonPath("$.car1Health").value(40))  // 50 - 10 = 40
                .andExpect(jsonPath("$.matchOver").value(false));
    }

    @Test
    void processHit_fiveHits_matchOver() throws Exception {
        long matchId = createMatch();
        mockMvc.perform(post("/api/matches/" + matchId + "/start"));

        Map<String, Object> hitBody = Map.of("carId", 1, "damage", 10);

        // 4 hits — still alive (health = 10)
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/matches/" + matchId + "/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(hitBody)))
                    .andExpect(jsonPath("$.matchOver").value(false));
        }

        // 5th hit — match over (health = 0)
        mockMvc.perform(post("/api/matches/" + matchId + "/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchOver").value(true))
                .andExpect(jsonPath("$.car1Health").value(0))
                .andExpect(jsonPath("$.winnerName").isString())
                .andExpect(jsonPath("$.winnerCarId").isNumber());
    }

    @Test
    void processHit_finishedMatch_returns400() throws Exception {
        long matchId = createMatch();
        mockMvc.perform(post("/api/matches/" + matchId + "/start"));

        // Force end the match
        Map<String, Object> endBody = Map.of("winnerPlayerId", PLAYER_1_ID);
        mockMvc.perform(post("/api/matches/" + matchId + "/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endBody)));

        // Try to hit a finished match — should fail
        Map<String, Object> hitBody = Map.of("carId", 1, "damage", 10);
        mockMvc.perform(post("/api/matches/" + matchId + "/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitBody)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/matches/{id}/end — force end match
    // -------------------------------------------------------------------------

    @Test
    void endMatch_activeMatch_returnsFinished() throws Exception {
        long matchId = createMatch();
        mockMvc.perform(post("/api/matches/" + matchId + "/start"));

        Map<String, Object> endBody = Map.of("winnerPlayerId", PLAYER_1_ID);

        mockMvc.perform(post("/api/matches/" + matchId + "/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.winnerId").value(PLAYER_1_ID));
    }

    @Test
    void endMatch_nonExistentMatch_returns400() throws Exception {
        Map<String, Object> endBody = Map.of("winnerPlayerId", PLAYER_1_ID);

        mockMvc.perform(post("/api/matches/99999/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endBody)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/matches/{id} — get match state
    // -------------------------------------------------------------------------

    @Test
    void getMatchState_existingMatch_returnsState() throws Exception {
        long matchId = createMatch();

        mockMvc.perform(get("/api/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.players").isArray())
                .andExpect(jsonPath("$.players.length()").value(2));
    }

    @Test
    void getMatchState_nonExistentMatch_returns400() throws Exception {
        mockMvc.perform(get("/api/matches/99999"))
                .andExpect(status().isBadRequest());
    }
}