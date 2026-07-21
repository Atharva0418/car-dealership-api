package com.atharva.dealership.user;

import com.atharva.dealership.dto.RegisterUserRequest;
import com.atharva.dealership.exception.EmailAlreadyExistsError;
import com.atharva.dealership.exception.ValidationError;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
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
        log.debug("Encoding password for user registration");
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword);

        User savedUser = userRepository.save(user);
        log.info("User registration persisted for email: {}", savedUser.getEmail());
        return savedUser;
    }

    public User register(RegisterUserRequest request) {
        log.info("Starting user registration");
        validateBasicInput(request);
        String normalizedEmail = normalizeEmail(request.email());
        log.debug("Normalized registration email from input");
        RegisterUserRequest normalizedRequest = new RegisterUserRequest(normalizedEmail, request.password());

        validate(normalizedRequest);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Duplicate registration attempt for email: {}", normalizedEmail);
            throw new EmailAlreadyExistsError("Email already exists: " + normalizedEmail);
        }

        User registeredUser = registerUser(normalizedRequest);
        log.info("Completed user registration for email: {}", registeredUser.getEmail());
        return registeredUser;
    }

    private void validate(RegisterUserRequest request) {
        if (!request.email().matches(EMAIL_PATTERN)) {
            log.warn("User registration validation failed: invalid email format");
            throw new ValidationError("Email must be a valid email address.");
        }

        if (request.password().length() < MINIMUM_PASSWORD_LENGTH) {
            log.warn("User registration validation failed: password does not meet minimum length");
            throw new ValidationError("Password must be at least 8 characters long.");
        }
    }

    private void validateBasicInput(RegisterUserRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            log.warn("User registration validation failed: email is missing");
            throw new ValidationError("Email must be a valid email address.");
        }

        if (request.password() == null) {
            log.warn("User registration validation failed: password is missing");
            throw new ValidationError("Password must be at least 8 characters long.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
