package com.enterprise.ai.knowledge.assistant.auth.service;

import com.enterprise.ai.knowledge.assistant.auth.dto.AuthResponse;
import com.enterprise.ai.knowledge.assistant.auth.dto.LoginRequest;
import com.enterprise.ai.knowledge.assistant.auth.dto.RegisterRequest;
import com.enterprise.ai.knowledge.assistant.auth.entity.User;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserRepository;
import com.enterprise.ai.knowledge.assistant.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ActivityLogService activityLogService;

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Create new user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setRole("USER");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        log.info("New user registered: {}", user.getEmail());

        // Log activity
        activityLogService.logActivity(user.getId(), "REGISTER", "USER", user.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found for email: {}", request.getEmail());
                    return new RuntimeException("Invalid email or password");
                });

        log.info("User found: {} with password hash: {}", user.getEmail(), user.getPasswordHash());
        log.info("Attempting to match password: {}", request.getPassword());

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Password match result: {}", passwordMatches);

        if (!passwordMatches) {
            log.error("Password mismatch for email: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        // Update last login
        userRepository.updateLastLogin(user.getId());

        log.info("User logged in: {}", user.getEmail());

        // Log activity
        activityLogService.logLogin(user.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    }

    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Temporary method to reset admin password - remove in production
    public String resetAdminPassword() {
        User adminUser = userRepository.findByEmail("admin@enterprise.ai")
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        String newPassword = "Admin@123";
        String newHash = passwordEncoder.encode(newPassword);
        
        adminUser.setPasswordHash(newHash);
        userRepository.save(adminUser);
        
        log.info("Admin password reset. New hash: {}", newHash);
        return newHash;
    }
}
