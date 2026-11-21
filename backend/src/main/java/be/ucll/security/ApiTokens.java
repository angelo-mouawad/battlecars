package be.ucll.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the two kinds of credentials the backend accepts.
 *
 * Browser tokens are minted by the Next.js server for logged-in users. A token is
 * {@code <expiry in epoch seconds>.<hex HMAC-SHA256 of "battlecars-api:<expiry>">},
 * signed with API_SECRET, which only the backend and the Next.js server know.
 *
 * The hub token is a fixed shared secret, flashed into the hub's config.h.
 */
@Component
public class ApiTokens {

    /** Tokens claiming to live longer than this are rejected, even with a valid signature. */
    static final Duration MAX_LIFETIME = Duration.ofHours(1);
    static final int MIN_SECRET_LENGTH = 16;
    private static final String PAYLOAD_PREFIX = "battlecars-api:";
    private static final String HMAC = "HmacSHA256";

    private final SecretKeySpec apiKey;
    private final byte[] hubToken;
    private final Clock clock;

    @Autowired
    public ApiTokens(@Value("${battlecars.api-secret:}") String apiSecret,
                     @Value("${battlecars.hub-token:}") String hubToken) {
        this(apiSecret, hubToken, Clock.systemUTC());
    }

    ApiTokens(String apiSecret, String hubToken, Clock clock) {
        requireSecret("API_SECRET", apiSecret);
        requireSecret("HUB_TOKEN", hubToken);
        this.apiKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC);
        this.hubToken = hubToken.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    private static void requireSecret(String name, String value) {
        if (value == null || value.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(name + " must be set to a random value of at least "
                    + MIN_SECRET_LENGTH + " characters, for example with: openssl rand -hex 32");
        }
    }

    public boolean isValidBrowserToken(String token) {
        if (token == null) return false;
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return false;

        String expiryText = token.substring(0, dot);
        long expiry;
        try {
            expiry = Long.parseLong(expiryText);
        } catch (NumberFormatException e) {
            return false;
        }

        long now = clock.instant().getEpochSecond();
        if (expiry <= now || expiry > now + MAX_LIFETIME.toSeconds()) return false;

        byte[] expected = sign(expiryText).getBytes(StandardCharsets.UTF_8);
        byte[] given = token.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }

    public boolean isValidHubToken(String token) {
        return token != null && MessageDigest.isEqual(hubToken, token.getBytes(StandardCharsets.UTF_8));
    }

    /** Mints a browser token. The Next.js server does the same in TypeScript; this is used by tests. */
    public String issueBrowserToken(Duration lifetime) {
        String expiry = Long.toString(clock.instant().getEpochSecond() + lifetime.toSeconds());
        return expiry + "." + sign(expiry);
    }

    private String sign(String expiry) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(apiKey);
            return HexFormat.of().formatHex(mac.doFinal((PAYLOAD_PREFIX + expiry).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
