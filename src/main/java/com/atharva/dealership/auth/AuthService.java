package com.atharva.dealership.auth;

import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Starting login attempt");
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("Looking up user for normalized login email");
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for email {}", normalizedEmail);
                    return new AuthenticationError(INVALID_CREDENTIALS_MESSAGE);
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed: invalid password for email {}", normalizedEmail);
            throw new AuthenticationError(INVALID_CREDENTIALS_MESSAGE);
        }

        AuthResponse response = issueTokens(user);
        log.info("Login completed for email {}", normalizedEmail);
        return response;
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        log.info("Starting refresh token exchange");
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.warn("Refresh token exchange failed: missing token");
            throw new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> {
                    log.warn("Refresh token exchange failed: token not found");
                    return new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
                });

        Instant now = clock.instant();
        if (refreshToken.isRevoked()) {
            log.warn("Refresh token exchange failed: token already revoked for email {}",
                    refreshToken.getUser().getEmail());
            throw new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        if (refreshToken.isExpired(now)) {
            log.warn("Refresh token exchange failed: token expired for email {}",
                    refreshToken.getUser().getEmail());
            throw new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        refreshToken.revoke(now);
        refreshTokenRepository.save(refreshToken);
        AuthResponse response = issueTokens(refreshToken.getUser());
        log.info("Refresh token exchange completed for email {}", refreshToken.getUser().getEmail());
        return response;
    }

    public String hashRefreshToken(String rawRefreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available.", error);
        }
    }

    private AuthResponse issueTokens(User user) {
        log.debug("Issuing auth tokens for email {}", user.getEmail());
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = generateRefreshToken();
        RefreshToken savedRefreshToken = new RefreshToken(
                user,
                hashRefreshToken(refreshToken),
                clock.instant().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()));
        refreshTokenRepository.save(savedRefreshToken);
        log.debug("Persisted hashed refresh token for email {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
