package com.atharva.dealership.user;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atharva.dealership.dto.RegisterUserRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceHashingTest {

    @Test
    void registerPersistsPasswordHashThatMatchesRawPasswordButIsNotRawPassword() {
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserService userService = new UserService(userRepository, passwordEncoder);
        String rawPassword = "StrongPassword123!";
        RegisterUserRequest request = new RegisterUserRequest("hashing@example.com", rawPassword);

        when(userRepository.findByEmail("hashing@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        String savedPassword = userCaptor.getValue().getPassword();
        assertNotEquals(rawPassword, savedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, savedPassword));
    }
}
