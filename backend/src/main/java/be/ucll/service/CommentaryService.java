package be.ucll.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CommentaryService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<String> generateCommentary(Map<String, Object> hitResult,
                                                         String player1Name, int car1Health,
                                                         String player2Name, int car2Health) {
        try {
            boolean matchOver = Boolean.TRUE.equals(hitResult.get("matchOver"));
            boolean isDraw    = Boolean.TRUE.equals(hitResult.get("draw"));
            int damage        = (int) hitResult.get("damage");
            int hitCarId      = (int) hitResult.get("hitCarId");

            String hitStrength = damage <= 5 ? "light" : damage <= 10 ? "medium" : "hard";

            String context = matchOver
                ? (isDraw
                    ? "MATCH OVER — it's a draw! Both cars destroyed at the same time."
                    : "MATCH OVER! Winner: " + hitResult.get("winnerName"))
                : player1Name + " has " + car1Health + "/50 HP, " + player2Name + " has " + car2Health + "/50 HP.";

            String prompt = """
                You are a hyped-up live commentator for a battle cars robot match.
                Event: Car #%d took a %s hit for %d damage!
                %s
                Write ONE short, punchy, exciting commentary line. Max 15 words. No hashtags.
                """.formatted(hitCarId, hitStrength, damage, context);

            Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "max_tokens", 60,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<?, ?> parsed   = objectMapper.readValue(response.body(), Map.class);
            List<?> choices    = (List<?>) parsed.get("choices");
            Map<?, ?> message  = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return CompletableFuture.completedFuture(message.get("content").toString().trim());

        } catch (Exception e) {
            System.out.println("Commentary generation failed: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}
