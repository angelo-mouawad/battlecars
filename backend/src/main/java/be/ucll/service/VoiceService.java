package be.ucll.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String apiKey;
    private final String voiceId;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VoiceService(@Value("${elevenlabs.api.key:}") String apiKey,
                        @Value("${elevenlabs.voice.id:}") String voiceId) {
        this.apiKey = apiKey;
        this.voiceId = voiceId;
    }

    @Async
    public CompletableFuture<byte[]> generateSpeech(String text) {
        if (text == null || text.isBlank() || apiKey == null || apiKey.isBlank() || voiceId == null || voiceId.isBlank()) {
            log.debug("Speech skipped: ElevenLabs is not configured or there is no text");
            return CompletableFuture.completedFuture(null);
        }

        try {
            Map<String, Object> body = Map.of(
                    "text", text,
                    "model_id", "eleven_multilingual_v2",
                    "voice_settings", Map.of(
                            "stability", 0.25,
                            "similarity_boost", 0.80,
                            "style", 0.75,
                            "use_speaker_boost", true));

            URI url = URI.create("https://api.elevenlabs.io/v1/text-to-speech/"
                    + URLEncoder.encode(voiceId, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder(url)
                    .timeout(TIMEOUT)
                    .header("xi-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "audio/mpeg")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Speech request failed with HTTP {}", response.statusCode());
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.warn("Speech generation failed: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}
