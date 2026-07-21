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
    void generateRefreshTokenCreatesTokenWithEmailSubject() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        String token = jwtService.generateRefreshToken("valid.user@example.com");

        assertEquals("valid.user@example.com", jwtService.extractRefreshSubject(token));
        assertTrue(jwtService.isValidRefreshToken(token));
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
    void generatedRefreshTokenUsesConfiguredRefreshTokenExpiry() {
        Instant now = Instant.parse("2026-07-21T12:00:00Z");
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(now, ZoneOffset.UTC));

        String token = jwtService.generateRefreshToken("valid.user@example.com");

        assertEquals(now.plusSeconds(604800L), jwtService.extractRefreshExpiration(token));
        assertEquals(604800L, jwtService.getRefreshTokenExpirationSeconds());
    }

    @Test
    void expiredAccessTokenIsRejected() {
        Instant issuedAt = Instant.parse("2026-07-21T12:00:00Z");
        JwtService issuer = new JwtService(SECRET, 1L, 604800L, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.generateAccessToken("valid.user@example.com");
        JwtService validator = new JwtService(SECRET, 1L, 604800L,
                Clock.fixed(issuedAt.plusSeconds(2), ZoneOffset.UTC));

        assertFalse(validator.isValidAccessToken(token));
        assertThrows(AuthenticationError.class, () -> validator.extractSubject(token));
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        Instant issuedAt = Instant.parse("2026-07-21T12:00:00Z");
        JwtService issuer = new JwtService(SECRET, 900L, 1L, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.generateRefreshToken("valid.user@example.com");
        JwtService validator = new JwtService(SECRET, 900L, 1L,
                Clock.fixed(issuedAt.plusSeconds(2), ZoneOffset.UTC));

        assertFalse(validator.isValidRefreshToken(token));
        assertThrows(AuthenticationError.class, () -> validator.extractRefreshSubject(token));
    }

    @Test
    void malformedAccessTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        assertFalse(jwtService.isValidAccessToken("not-a-jwt"));
        assertThrows(AuthenticationError.class, () -> jwtService.extractSubject("not-a-jwt"));
    }

    @Test
    void malformedRefreshTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        assertFalse(jwtService.isValidRefreshToken("not-a-jwt"));
        assertThrows(AuthenticationError.class, () -> jwtService.extractRefreshSubject("not-a-jwt"));
    }

    @Test
    void tamperedRefreshTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));
        String token = jwtService.generateRefreshToken("valid.user@example.com");
        String tamperedToken = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertFalse(jwtService.isValidRefreshToken(tamperedToken));
        assertThrows(AuthenticationError.class, () -> jwtService.extractRefreshSubject(tamperedToken));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));
        String refreshToken = jwtService.generateRefreshToken("valid.user@example.com");

        assertFalse(jwtService.isValidAccessToken(refreshToken));
        assertThrows(AuthenticationError.class, () -> jwtService.extractSubject(refreshToken));
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));
        String accessToken = jwtService.generateAccessToken("valid.user@example.com");

        assertFalse(jwtService.isValidRefreshToken(accessToken));
        assertThrows(AuthenticationError.class, () -> jwtService.extractRefreshSubject(accessToken));
    }
}
