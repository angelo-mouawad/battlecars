package be.ucll.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import be.ucll.security.ApiTokens;

/** Runs the real security filters, unlike the other tests which switch them off. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ApiSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiTokens tokens;

    private String bearer() {
        return "Bearer " + tokens.issueBrowserToken(Duration.ofMinutes(15));
    }

    @Test
    void rejectsApiCallsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(delete("/api/players/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/players/Angelo/score")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"points\":100}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAForgedToken() throws Exception {
        long expiry = System.currentTimeMillis() / 1000 + 600;
        mockMvc.perform(get("/api/players").header("Authorization", "Bearer " + expiry + ".deadbeef"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTheHubTokenOnTheRestApi() throws Exception {
        mockMvc.perform(get("/api/players").header("Authorization", "Bearer test-hub-token-0123456789"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsApiCallsWithAValidToken() throws Exception {
        mockMvc.perform(get("/api/players").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void keepsTheStatusEndpointOpen() throws Exception {
        mockMvc.perform(get("/status")).andExpect(status().isOk());
    }

    @Test
    void answersCorsPreflightWithoutAToken() throws Exception {
        mockMvc.perform(options("/api/players")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
