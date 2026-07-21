package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.exception.AuthenticationError;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenRepository,
                FIXED_CLOCK);
    }

    @Test
    void loginWithValidCredentialsReturnsAccessAndRefreshTokensAndStoresOnlyRefreshTokenHash() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("valid.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "$2a$10$encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("valid.user@example.com")).thenReturn("access.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.login(
                new LoginRequest("valid.user@example.com", "StrongPassword123!"));

        assertEquals("access.jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresInSeconds());
        assertNotNull(response.refreshToken());
        assertFalse(response.refreshToken().isBlank());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotEquals(response.refreshToken(), savedToken.getTokenHash());
        assertEquals(FIXED_CLOCK.instant().plusSeconds(604800L), savedToken.getExpiresAt());
        assertFalse(savedToken.isRevoked());
    }

    @Test
    void loginNormalizesEmailBeforeLookingUpUser() {
        User user = new User("test@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "$2a$10$encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("test@example.com")).thenReturn("access.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        authService.login(new LoginRequest("  Test@Example.com  ", "StrongPassword123!"));

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void loginWithUnknownEmailThrowsAuthenticationErrorAndDoesNotIssueTokens() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationError.class,
                () -> authService.login(new LoginRequest("missing@example.com", "StrongPassword123!")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginWithWrongPasswordThrowsAuthenticationErrorAndDoesNotIssueTokens() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("valid.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$10$encoded-password")).thenReturn(false);

        assertThrows(AuthenticationError.class,
                () -> authService.login(new LoginRequest("valid.user@example.com", "wrong-password")));

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshWithValidTokenRotatesRefreshTokenAndReturnsNewTokens() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        String rawRefreshToken = "existing-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                authService.hashRefreshToken(rawRefreshToken),
                FIXED_CLOCK.instant().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(authService.hashRefreshToken(rawRefreshToken)))
                .thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken("valid.user@example.com")).thenReturn("new.access.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.refresh(rawRefreshToken);

        assertEquals("new.access.jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertNotEquals(rawRefreshToken, response.refreshToken());
        assertTrue(existingToken.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshWithReusedTokenThrowsAuthenticationError() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        String rawRefreshToken = "reused-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                authService.hashRefreshToken(rawRefreshToken),
                FIXED_CLOCK.instant().plusSeconds(60));
        existingToken.revoke(FIXED_CLOCK.instant());
        when(refreshTokenRepository.findByTokenHash(authService.hashRefreshToken(rawRefreshToken)))
                .thenReturn(Optional.of(existingToken));

        assertThrows(AuthenticationError.class, () -> authService.refresh(rawRefreshToken));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refreshWithExpiredTokenThrowsAuthenticationError() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        String rawRefreshToken = "expired-refresh-token";
        RefreshToken existingToken = new RefreshToken(
                user,
                authService.hashRefreshToken(rawRefreshToken),
                FIXED_CLOCK.instant().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(authService.hashRefreshToken(rawRefreshToken)))
                .thenReturn(Optional.of(existingToken));

        assertThrows(AuthenticationError.class, () -> authService.refresh(rawRefreshToken));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refreshWithUnknownTokenThrowsAuthenticationError() {
        when(refreshTokenRepository.findByTokenHash(authService.hashRefreshToken("unknown-refresh-token")))
                .thenReturn(Optional.empty());

        assertThrows(AuthenticationError.class, () -> authService.refresh("unknown-refresh-token"));

        verify(jwtService, never()).generateAccessToken(any());
    }
}
