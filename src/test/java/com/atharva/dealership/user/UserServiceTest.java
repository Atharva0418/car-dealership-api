package com.atharva.dealership.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * Assumed production API for the green step:
 *
 * Classes:
 * - UserService
 * - UserRepository, a Spring Data JPA repository for User
 * - User
 * - RegisterUserRequest
 *
 * Methods:
 * - UserService.registerUser(RegisterUserRequest request)
 * - UserRepository.save(User user)
 * - PasswordEncoder.encode(String rawPassword)
 * - RegisterUserRequest.getEmail()
 * - RegisterUserRequest.getPassword()
 * - User.getEmail()
 * - User.getPassword()
 *
 * Constructors:
 * - RegisterUserRequest(String email, String password)
 *
 * Fields:
 * - RegisterUserRequest.email
 * - RegisterUserRequest.password
 * - User.email
 * - User.password
 *
 * Choice:
 * - This test assumes registerUser returns the persisted User entity. If the
 *   application later prefers a response DTO, replace the result type with that
 *   DTO and keep the assertion that the returned email matches the request.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserWithUniqueEmailAndValidPasswordEncodesPasswordBeforeSaving() {
        String email = "valid.user@example.com";
        String rawPassword = "StrongPassword123!";
        String encodedPassword = "$2a$10$encoded-password";
        RegisterUserRequest request = new RegisterUserRequest(email, rawPassword);

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(request);

        verify(passwordEncoder, times(1)).encode(rawPassword);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(email, result.getEmail());
    }
}
