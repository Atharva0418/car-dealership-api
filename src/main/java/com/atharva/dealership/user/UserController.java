package com.atharva.dealership.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atharva.dealership.dto.RegisterUserRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
}
