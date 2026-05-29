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

    // Default to empty string so the app starts even without the key configured.
    // Commentary will silently skip if the key is missing.
    @Value("${groq.api.key:}")
    private String groqApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<String> generateCommentary(Map<String, Object> hitResult,
                                                        String player1Name, int car1Health,
                                                        String player2Name, int car2Health) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            System.out.println("Commentary skipped: GROQ_API_KEY not configured.");
            return CompletableFuture.completedFuture(null);
        }

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
                You are a chaotic, funny, unhinged live commentator for a battle cars robot fight.
                You swear occasionally (damn, hell, crap, what the hell ,  FUCK, bomboclat) and make jokes.
                You roast the losing car, hype the crowd, and sometimes say something completely absurd.
                Mix excitement with humor — think less ESPN, more drunk uncle at a robot demolition derby.

                Event: Car #%d (%s) took a %s hit for %d damage!
                %s

                Write ONE short commentary line. Max 18 words. Be funny, loud, and a little unhinged.
                No hashtags. No quotation marks around the line.
                """.formatted(hitCarId,
                    hitCarId == 1 ? player1Name : player2Name,
                    hitStrength, damage, context);

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