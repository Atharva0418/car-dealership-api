package com.atharva.dealership.config;

import com.atharva.dealership.user.User;
import com.atharva.dealership.user.UserRepository;
import com.atharva.dealership.user.UserRole;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        log.info("Starting admin user seeding");
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.debug("Skipping admin user seeding because admin credentials are not configured");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.info("Admin user seeding completed successfully; admin already exists");
            return;
        }

        User admin = new User(normalizedEmail, passwordEncoder.encode(adminPassword), UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Admin user seeding completed successfully for email {}", normalizedEmail);
    }
}
