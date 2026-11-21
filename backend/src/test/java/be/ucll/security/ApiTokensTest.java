package be.ucll.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ApiTokensTest {

    private static final String SECRET = "test-api-secret-0123456789";
    private static final String HUB = "test-hub-token-0123456789";
    private static final Instant NOW = Instant.parse("2026-05-11T10:00:00Z");

    private final ApiTokens tokens = at(NOW);

    private static ApiTokens at(Instant instant) {
        return new ApiTokens(SECRET, HUB, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void acceptsAFreshToken() {
        assertThat(tokens.isValidBrowserToken(tokens.issueBrowserToken(Duration.ofMinutes(15)))).isTrue();
    }

    @Test
    void matchesTheTokenTheFrontendMints() {
        // Same HMAC as frontend/lib/auth.ts: hex(HMAC-SHA256(secret, "battlecars-api:" + expiry)).
        long expiry = NOW.getEpochSecond() + 900;
        String token = tokens.issueBrowserToken(Duration.ofSeconds(900));
        assertThat(token).startsWith(expiry + ".").matches("\\d+\\.[0-9a-f]{64}");
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = tokens.issueBrowserToken(Duration.ofMinutes(15));
        assertThat(at(NOW.plus(Duration.ofMinutes(16))).isValidBrowserToken(token)).isFalse();
    }

    @Test
    void rejectsATokenThatClaimsToLiveTooLong() {
        assertThat(tokens.isValidBrowserToken(tokens.issueBrowserToken(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void rejectsATamperedExpiry() {
        String token = tokens.issueBrowserToken(Duration.ofMinutes(15));
        String signature = token.substring(token.indexOf('.') + 1);
        assertThat(tokens.isValidBrowserToken((NOW.getEpochSecond() + 1800) + "." + signature)).isFalse();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        ApiTokens other = new ApiTokens("another-secret-0123456789", HUB, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(tokens.isValidBrowserToken(other.issueBrowserToken(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void rejectsMalformedTokens() {
        assertThat(tokens.isValidBrowserToken(null)).isFalse();
        assertThat(tokens.isValidBrowserToken("")).isFalse();
        assertThat(tokens.isValidBrowserToken("abc")).isFalse();
        assertThat(tokens.isValidBrowserToken(".abc")).isFalse();
        assertThat(tokens.isValidBrowserToken("123.")).isFalse();
        assertThat(tokens.isValidBrowserToken("soon.abc")).isFalse();
    }

    @Test
    void checksTheHubToken() {
        assertThat(tokens.isValidHubToken(HUB)).isTrue();
        assertThat(tokens.isValidHubToken("test-hub-token-wrong")).isFalse();
        assertThat(tokens.isValidHubToken(null)).isFalse();
    }

    @Test
    void refusesToStartWithoutStrongSecrets() {
        assertThatThrownBy(() -> new ApiTokens("", HUB)).hasMessageContaining("API_SECRET");
        assertThatThrownBy(() -> new ApiTokens(SECRET, "short")).hasMessageContaining("HUB_TOKEN");
    }
}
