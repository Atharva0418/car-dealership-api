package com.atharva.dealership.auth;

import com.atharva.dealership.exception.AuthenticationError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String INVALID_ACCESS_TOKEN_MESSAGE = "Invalid access token.";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token.";

    private final Key key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;
    private final Clock clock;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds,
            Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.clock = clock;
    }

    public String generateAccessToken(String subject) {
        return generateToken(subject, accessTokenExpirationSeconds, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(String subject) {
        return generateToken(subject, refreshTokenExpirationSeconds, REFRESH_TOKEN_TYPE);
    }

    public String extractSubject(String token) {
        TokenClaims claims = parseAndValidate(token, ACCESS_TOKEN_TYPE);
        return claims.subject();
    }

    public String extractRefreshSubject(String token) {
        TokenClaims claims = parseAndValidate(token, REFRESH_TOKEN_TYPE);
        return claims.subject();
    }

    public Instant extractExpiration(String token) {
        TokenClaims claims = parseAndValidate(token, ACCESS_TOKEN_TYPE);
        return claims.expiration();
    }

    public Instant extractRefreshExpiration(String token) {
        TokenClaims claims = parseAndValidate(token, REFRESH_TOKEN_TYPE);
        return claims.expiration();
    }

    public boolean isValidAccessToken(String token) {
        try {
            parseAndValidate(token, ACCESS_TOKEN_TYPE);
            return true;
        } catch (AuthenticationError error) {
            return false;
        }
    }

    public boolean isValidRefreshToken(String token) {
        try {
            parseAndValidate(token, REFRESH_TOKEN_TYPE);
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

    private String generateToken(String subject, long expirationSeconds, String tokenType) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        log.debug("Generated {} token for subject {} expiring at {}", tokenType, subject, expiresAt);
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", tokenType)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private TokenClaims parseAndValidate(String token, String expectedType) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String subject = claims.getSubject();
            String tokenType = claims.get("type", String.class);
            Date expiration = claims.getExpiration();

            if (subject == null || subject.isBlank()) {
                log.debug("Rejected {} token: missing subject", expectedType);
                throw invalidTokenError(expectedType);
            }
            if (!expectedType.equals(tokenType)) {
                log.debug("Rejected {} token: token type claim was {}", expectedType, tokenType);
                throw invalidTokenError(expectedType);
            }
            if (expiration == null) {
                log.debug("Rejected {} token: missing expiration", expectedType);
                throw invalidTokenError(expectedType);
            }

            log.debug("Validated {} token for subject {}", expectedType, subject);
            return new TokenClaims(subject, expiration.toInstant());
        } catch (JwtException | IllegalArgumentException error) {
            log.debug("Rejected {} token: {}", expectedType, error.getClass().getSimpleName());
            throw invalidTokenError(expectedType);
        }
    }

    private AuthenticationError invalidTokenError(String expectedType) {
        if (REFRESH_TOKEN_TYPE.equals(expectedType)) {
            return new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        return new AuthenticationError(INVALID_ACCESS_TOKEN_MESSAGE);
    }

    private record TokenClaims(String subject, Instant expiration) {
    }
}
