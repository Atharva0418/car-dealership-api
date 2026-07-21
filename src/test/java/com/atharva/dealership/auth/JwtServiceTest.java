package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atharva.dealership.exception.AuthenticationError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void generateAccessTokenCreatesTokenWithEmailSubject() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        String token = jwtService.generateAccessToken("valid.user@example.com");

        assertEquals("valid.user@example.com", jwtService.extractSubject(token));
        assertTrue(jwtService.isValidAccessToken(token));
    }

    @Test
    void generatedTokenUsesConfiguredAccessTokenExpiry() {
        Instant now = Instant.parse("2026-07-21T12:00:00Z");
        JwtService jwtService = new JwtService(SECRET, 123L, 604800L,
                Clock.fixed(now, ZoneOffset.UTC));

        String token = jwtService.generateAccessToken("valid.user@example.com");

        assertEquals(now.plusSeconds(123L), jwtService.extractExpiration(token));
        assertEquals(123L, jwtService.getAccessTokenExpirationSeconds());
    }

    @Test
    void expiredTokenIsRejected() {
        Instant issuedAt = Instant.parse("2026-07-21T12:00:00Z");
        JwtService issuer = new JwtService(SECRET, 1L, 604800L, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.generateAccessToken("valid.user@example.com");
        JwtService validator = new JwtService(SECRET, 1L, 604800L,
                Clock.fixed(issuedAt.plusSeconds(2), ZoneOffset.UTC));

        assertFalse(validator.isValidAccessToken(token));
        assertThrows(AuthenticationError.class, () -> validator.extractSubject(token));
    }

    @Test
    void malformedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        assertFalse(jwtService.isValidAccessToken("not-a-jwt"));
        assertThrows(AuthenticationError.class, () -> jwtService.extractSubject("not-a-jwt"));
    }
}
