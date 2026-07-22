package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.dto.RegisterUserRequest;
import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAccessUnitTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Test
    void registrationAlwaysPersistsCustomerRoleEvenWhenEmailLooksLikeAdmin() {
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("$2a$10$encoded-password");
        when(userRepository.findByEmail("site.admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        com.atharva.dealership.user.UserService userService =
                new com.atharva.dealership.user.UserService(userRepository, passwordEncoder);

        userService.register(new RegisterUserRequest("site.admin@example.com", "StrongPassword123!"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());
        assertEquals("CUSTOMER", roleName(userCaptor.getValue()));
    }

    @Test
    void loginReturnsStoredRoleAndIssuesAccessTokenWithStoredRole() {
        User admin = userWithRole("evaluator.admin@example.com", "$2a$10$encoded-password", "ADMIN");
        when(userRepository.findByEmail("evaluator.admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Evaluator@12345", "$2a$10$encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("evaluator.admin@example.com", "ADMIN")).thenReturn("admin.access.jwt");
        when(jwtService.generateRefreshToken("evaluator.admin@example.com")).thenReturn("admin.refresh.jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

        AuthResponse response = authService.login(
                new LoginRequest("evaluator.admin@example.com", "Evaluator@12345"));

        assertEquals("admin.access.jwt", response.accessToken());
        assertEquals("ADMIN", roleFromAuthResponse(response));
    }

    @Test
    void accessTokenContainsRoleClaimAndDoesNotInferRoleFromEmailAddress() {
        JwtService jwtService = new JwtService(SECRET, 900L, 604800L,
                Clock.fixed(Instant.parse("2030-07-22T09:00:00Z"), ZoneOffset.UTC));

        String token = generateAccessTokenWithRole(jwtService, "admin.customer@example.com", "CUSTOMER");
        Claims claims = parseClaims(token);

        assertEquals("admin.customer@example.com", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));
        assertEquals("CUSTOMER", extractRole(jwtService, token));
    }

    private User userWithRole(String email, String password, String role) {
        User user = new User(email, password);
        assertDoesNotThrow(() -> {
            Method setRole = User.class.getDeclaredMethod("setRole", Class.forName("com.atharva.dealership.user.UserRole"));
            Object enumRole = Enum.valueOf(
                    (Class<Enum>) Class.forName("com.atharva.dealership.user.UserRole").asSubclass(Enum.class),
                    role);
            setRole.setAccessible(true);
            setRole.invoke(user, enumRole);
        });
        return user;
    }

    private String generateAccessTokenWithRole(JwtService jwtService, String subject, String role) {
        return assertDoesNotThrow(() -> {
            Method method = JwtService.class.getDeclaredMethod("generateAccessToken", String.class, String.class);
            return (String) method.invoke(jwtService, subject, role);
        });
    }

    private String extractRole(JwtService jwtService, String token) {
        return assertDoesNotThrow(() -> {
            Method method = JwtService.class.getDeclaredMethod("extractRole", String.class);
            return (String) method.invoke(jwtService, token);
        });
    }

    private String roleName(User user) {
        Object role = assertDoesNotThrow(() -> {
            Method method = User.class.getDeclaredMethod("getRole");
            return method.invoke(user);
        });
        assertNotNull(role);
        return role.toString();
    }

    private String roleFromAuthResponse(AuthResponse response) {
        return assertDoesNotThrow(() -> {
            Method method = AuthResponse.class.getDeclaredMethod("role");
            return (String) method.invoke(response);
        });
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
