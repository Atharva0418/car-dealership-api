package com.atharva.dealership.auth;

import com.atharva.dealership.exception.AuthenticationError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    private final String secret;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;
    private final Clock clock;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds,
            Clock clock) {
        this.secret = secret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.clock = clock;
    }

    public String generateAccessToken(String subject) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"" + subject + "\",\"iat\":" + now.getEpochSecond()
                + ",\"exp\":" + expiresAt.getEpochSecond() + "}");
        String unsignedToken = header + "." + payload;
        log.debug("Generated access token for subject {} expiring at {}", subject, expiresAt);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractSubject(String token) {
        Claims claims = parseAndValidate(token);
        return claims.subject();
    }

    public Instant extractExpiration(String token) {
        Claims claims = parseAndValidate(token);
        return Instant.ofEpochSecond(claims.expirationEpochSecond());
    }

    public boolean isValidAccessToken(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (AuthenticationError error) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private Claims parseAndValidate(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            log.debug("Rejected access token: malformed token structure");
            throw new AuthenticationError("Invalid access token.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            log.debug("Rejected access token: invalid signature");
            throw new AuthenticationError("Invalid access token.");
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            log.debug("Rejected access token: payload is not valid Base64URL");
            throw new AuthenticationError("Invalid access token.");
        }
        String subject = extractStringClaim(payload, "sub");
        long expiration = extractLongClaim(payload, "exp");
        if (!Instant.ofEpochSecond(expiration).isAfter(clock.instant())) {
            log.debug("Rejected access token: token expired for subject {}", subject);
            throw new AuthenticationError("Invalid access token.");
        }

        log.debug("Validated access token for subject {}", subject);
        return new Claims(subject, expiration);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to sign JWT.", error);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String extractStringClaim(String json, String claimName) {
        String marker = "\"" + claimName + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AuthenticationError("Invalid access token.");
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            throw new AuthenticationError("Invalid access token.");
        }
        return json.substring(valueStart, valueEnd);
    }

    private long extractLongClaim(String json, String claimName) {
        String marker = "\"" + claimName + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AuthenticationError("Invalid access token.");
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf(",", valueStart);
        if (valueEnd < 0) {
            valueEnd = json.indexOf("}", valueStart);
        }
        try {
            return Long.parseLong(json.substring(valueStart, valueEnd));
        } catch (RuntimeException error) {
            throw new AuthenticationError("Invalid access token.");
        }
    }

    private record Claims(String subject, long expirationEpochSecond) {
    }
}
