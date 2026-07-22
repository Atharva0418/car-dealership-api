package com.atharva.dealership.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "ADMIN_EMAIL=admin.seed@example.com",
        "ADMIN_PASSWORD=Admin@12345"
})
class AdminSeedingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void startupCreatesExactlyOneAdminWhenCredentialsAreConfigured() {
        User admin = userRepository.findByEmail("admin.seed@example.com").orElseThrow();

        assertEquals("ADMIN", roleName(admin));
        assertTrue(passwordEncoder.matches("Admin@12345", admin.getPassword()));
        assertEquals(1L, userRepository.findAll().stream()
                .filter(user -> "admin.seed@example.com".equals(user.getEmail()))
                .count());
    }

    private String roleName(User user) {
        try {
            Method method = User.class.getDeclaredMethod("getRole");
            Object role = method.invoke(user);
            return role.toString();
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("User must expose a persisted role through getRole().", error);
        }
    }
}
