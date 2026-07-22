package com.atharva.dealership.auth;

import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public AuthResponse refresh(String rawRefreshToken) {
        log.info("Starting refresh token exchange");
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.warn("Refresh token exchange failed: missing token");
            throw new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        if (!jwtService.isValidRefreshToken(rawRefreshToken)) {
            log.warn("Refresh token exchange failed: invalid token");
            throw new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String email = jwtService.extractRefreshSubject(rawRefreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Refresh token exchange failed: user no longer exists for email {}", email);
                    return new AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE);
                });

        AuthResponse response = issueTokens(user);
        log.info("Refresh token exchange completed for email {}", user.getEmail());
        return response;
    }

    private AuthResponse issueTokens(User user) {
        log.info("Starting token issuance");
        log.debug("Issuing auth tokens for email {}", user.getEmail());
        String role = user.getRole().name();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        log.debug("Issued stateless refresh token for email {}", user.getEmail());
        log.info("Token issuance completed successfully for email {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                role);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
