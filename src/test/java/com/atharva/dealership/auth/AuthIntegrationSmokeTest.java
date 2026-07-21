package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.dto.RegisterUserRequest;
import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthIntegrationSmokeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void clearData() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registeredUserCanLoginRefreshAndCannotReuseOldRefreshToken() {
        userService.register(new RegisterUserRequest("auth.smoke@example.com", "StrongPassword123!"));

        AuthResponse loginResponse = authService.login(
                new LoginRequest("auth.smoke@example.com", "StrongPassword123!"));
        assertNotNull(loginResponse.accessToken());
        assertNotNull(loginResponse.refreshToken());
        assertFalse(loginResponse.refreshToken().isBlank());

        AuthResponse refreshResponse = authService.refresh(loginResponse.refreshToken());
        assertNotNull(refreshResponse.accessToken());
        assertNotEquals(loginResponse.refreshToken(), refreshResponse.refreshToken());

        assertThrows(AuthenticationError.class, () -> authService.refresh(loginResponse.refreshToken()));
        assertFalse(jwtService.extractSubject(refreshResponse.accessToken()).isBlank());
    }
}
