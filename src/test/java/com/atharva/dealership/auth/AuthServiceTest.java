package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService);
    }

    @Test
    void loginWithValidCredentialsReturnsAccessAndRefreshJwtTokens() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("valid.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "$2a$10$encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("valid.user@example.com")).thenReturn("access.jwt");
        when(jwtService.generateRefreshToken("valid.user@example.com")).thenReturn("refresh.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(
                new LoginRequest("valid.user@example.com", "StrongPassword123!"));

        assertEquals("access.jwt", response.accessToken());
        assertEquals("refresh.jwt", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresInSeconds());
    }

    @Test
    void loginNormalizesEmailBeforeLookingUpUser() {
        User user = new User("test@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "$2a$10$encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("test@example.com")).thenReturn("access.jwt");
        when(jwtService.generateRefreshToken("test@example.com")).thenReturn("refresh.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

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
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    void loginWithWrongPasswordThrowsAuthenticationErrorAndDoesNotIssueTokens() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        when(userRepository.findByEmail("valid.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$10$encoded-password")).thenReturn(false);

        assertThrows(AuthenticationError.class,
                () -> authService.login(new LoginRequest("valid.user@example.com", "wrong-password")));

        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    void refreshWithValidTokenReturnsNewTokensForTokenSubject() {
        User user = new User("valid.user@example.com", "$2a$10$encoded-password");
        when(jwtService.isValidRefreshToken("refresh.jwt")).thenReturn(true);
        when(jwtService.extractRefreshSubject("refresh.jwt")).thenReturn("valid.user@example.com");
        when(userRepository.findByEmail("valid.user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("valid.user@example.com")).thenReturn("new.access.jwt");
        when(jwtService.generateRefreshToken("valid.user@example.com")).thenReturn("new.refresh.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.refresh("refresh.jwt");

        assertEquals("new.access.jwt", response.accessToken());
        assertEquals("new.refresh.jwt", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresInSeconds());
    }

    @Test
    void refreshWithMissingTokenThrowsAuthenticationError() {
        assertThrows(AuthenticationError.class, () -> authService.refresh(" "));

        verify(jwtService, never()).isValidRefreshToken(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void refreshWithInvalidTokenThrowsAuthenticationError() {
        when(jwtService.isValidRefreshToken("invalid-refresh.jwt")).thenReturn(false);

        assertThrows(AuthenticationError.class, () -> authService.refresh("invalid-refresh.jwt"));

        verify(jwtService, never()).extractRefreshSubject(any());
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    void refreshForDeletedUserThrowsAuthenticationError() {
        when(jwtService.isValidRefreshToken("orphaned-refresh.jwt")).thenReturn(true);
        when(jwtService.extractRefreshSubject("orphaned-refresh.jwt")).thenReturn("deleted@example.com");
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationError.class, () -> authService.refresh("orphaned-refresh.jwt"));

        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
    }
}
