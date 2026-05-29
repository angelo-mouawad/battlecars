package be.ucll.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class VoiceService {

    @Value("${elevenlabs.api.key:}")
    private String elevenLabsApiKey;

    @Value("${elevenlabs.voice.id:}")
    private String voiceId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<byte[]> generateSpeech(String text) {
        if (elevenLabsApiKey == null || elevenLabsApiKey.isBlank()) {
            System.out.println("TTS skipped: ELEVENLABS_API_KEY not configured.");
            return CompletableFuture.completedFuture(null);
        }
        if (voiceId == null || voiceId.isBlank()) {
            System.out.println("TTS skipped: ELEVENLABS_VOICE_ID not configured.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Voice settings tuned for maximum energy and expressiveness
            Map<String, Object> voiceSettings = Map.of(
                    "stability",        0.25,   // low = more emotional variation
                    "similarity_boost", 0.80,
                    "style",            0.75,   // high style exaggeration
                    "use_speaker_boost", true
            );

            Map<String, Object> body = Map.of(
                    "text",          text,
                    "model_id",      "eleven_multilingual_v2",
                    "voice_settings", voiceSettings
            );

            String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("xi-api-key",    elevenLabsApiKey)
                    .header("Content-Type",  "application/json")
                    .header("Accept",        "audio/mpeg")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                System.out.println("ElevenLabs TTS failed: HTTP " + response.statusCode()
                        + " — " + new String(response.body()));
                return CompletableFuture.completedFuture(null);
            }

            System.out.println("TTS generated: " + text.length() + " chars → "
                    + response.body().length + " bytes");
            return CompletableFuture.completedFuture(response.body());

        } catch (Exception e) {
            System.out.println("TTS generation failed: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}