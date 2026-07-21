package com.atharva.dealership.user;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.atharva.dealership.dto.RegisterUserRequest;
import com.atharva.dealership.exception.EmailAlreadyExistsError;
import com.atharva.dealership.exception.ValidationError;

@Service
public class UserService {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterUserRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword);

        return userRepository.save(user);
    }

    public User register(RegisterUserRequest request) {
        validateBasicInput(request);
        String normalizedEmail = normalizeEmail(request.email());
        RegisterUserRequest normalizedRequest = new RegisterUserRequest(normalizedEmail, request.password());

        validate(normalizedRequest);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsError("Email already exists: " + normalizedEmail);
        }

        return registerUser(normalizedRequest);
    }

    private void validate(RegisterUserRequest request) {
        if (!request.email().matches(EMAIL_PATTERN)) {
            throw new ValidationError("Email must be a valid email address.");
        }

        if (request.password().length() < MINIMUM_PASSWORD_LENGTH) {
            throw new ValidationError("Password must be at least 8 characters long.");
        }
    }

    private void validateBasicInput(RegisterUserRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationError("Email must be a valid email address.");
        }

        if (request.password() == null) {
            throw new ValidationError("Password must be at least 8 characters long.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
