package com.atharva.dealership.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atharva.dealership.dto.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class UserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registerPersistsUserAndCanFetchItBackByNormalizedEmail() {
        String rawPassword = "StrongPassword123!";
        RegisterUserRequest request = new RegisterUserRequest("  Integration.Test@Example.com  ", rawPassword);

        User registeredUser = userService.register(request);

        User fetchedUser = userRepository.findByEmail("integration.test@example.com").orElseThrow();
        assertEquals(registeredUser.getEmail(), fetchedUser.getEmail());
        assertEquals("integration.test@example.com", fetchedUser.getEmail());
        assertNotEquals(rawPassword, fetchedUser.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, fetchedUser.getPassword()));
    }
}
