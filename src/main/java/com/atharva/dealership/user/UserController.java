package com.atharva.dealership.user;

import com.atharva.dealership.auth.AuthService;
import com.atharva.dealership.dto.AuthResponse;
import com.atharva.dealership.dto.LoginRequest;
import com.atharva.dealership.dto.RefreshTokenRequest;
import com.atharva.dealership.dto.RegisterUserRequest;
import com.atharva.dealership.exception.AuthenticationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService) {
        this(userService, null);
    }

    @Autowired
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterUserRequest request) {
        log.info("Received user registration request");
        if (request.email() == null || request.password() == null) {
            log.warn("Rejecting user registration request with missing required fields");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        User registeredUser = userService.register(request);
        log.info("User registration request handled successfully for email: {}", registeredUser.getEmail());
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Received login request");
        if (request.email() == null || request.password() == null) {
            log.warn("Rejecting login request with missing required fields");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        AuthResponse response = authService.login(request);
        log.info("Login request handled successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("Received refresh token request");
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            log.warn("Rejecting refresh token request with missing token");
            throw new AuthenticationError("Invalid refresh token.");
        }

        AuthResponse response = authService.refresh(request.refreshToken());
        log.info("Refresh token request handled successfully");
        return ResponseEntity.ok(response);
    }
}
