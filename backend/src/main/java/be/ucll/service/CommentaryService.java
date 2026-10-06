package be.ucll.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CommentaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentaryService.class);
    private static final URI GROQ_URL = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final String groqApiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommentaryService(@Value("${groq.api.key:}") String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    @Async
    public CompletableFuture<String> generateCommentary(Map<String, Object> hitResult,
                                                        String player1Name, int car1Health,
                                                        String player2Name, int car2Health) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.debug("Commentary skipped: GROQ_API_KEY is not configured");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String prompt = buildPrompt(hitResult, player1Name, car1Health, player2Name, car2Health);
            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "max_tokens", 60,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpRequest request = HttpRequest.newBuilder(GROQ_URL)
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Commentary request failed with HTTP {}", response.statusCode());
                return CompletableFuture.completedFuture(null);
            }

            JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            String text = content.isTextual() ? content.asText().trim() : null;
            return CompletableFuture.completedFuture(text == null || text.isEmpty() ? null : text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.warn("Commentary generation failed: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    String buildPrompt(Map<String, Object> hitResult, String player1Name, int car1Health,
                       String player2Name, int car2Health) {
        boolean matchOver = Boolean.TRUE.equals(hitResult.get("matchOver"));
        boolean isDraw = Boolean.TRUE.equals(hitResult.get("draw"));
        int damage = hitResult.get("damage") instanceof Number n ? n.intValue() : 10;
        int hitCarId = hitResult.get("hitCarId") instanceof Number n ? n.intValue() : 1;
        String hitStrength = damage <= 5 ? "light" : damage <= 10 ? "medium" : "hard";

        String context = matchOver
                ? (isDraw
                        ? "MATCH OVER, it's a draw! Both cars were destroyed at the same time."
                        : "MATCH OVER! Winner: " + hitResult.get("winnerName"))
                : player1Name + " has " + car1Health + "/50 HP, " + player2Name + " has " + car2Health + "/50 HP.";

        return """
                You are a loud, funny live commentator for a robot battle car fight.
                Hype the crowd, tease the car that is losing and keep it playful and family friendly.

                Event: Car #%d (%s) took a %s hit for %d damage!
                %s

                Write ONE short commentary line. Max 18 words.
                No hashtags. No quotation marks around the line.
                """.formatted(hitCarId, hitCarId == 1 ? player1Name : player2Name, hitStrength, damage, context);
    }
}
