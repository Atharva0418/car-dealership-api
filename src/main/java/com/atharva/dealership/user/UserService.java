package com.atharva.dealership.user;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.atharva.dealership.dto.RegisterUserRequest;

public class UserService {

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
}
