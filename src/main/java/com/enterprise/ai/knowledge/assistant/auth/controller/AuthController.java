package com.enterprise.ai.knowledge.assistant.auth.controller;

import com.enterprise.ai.knowledge.assistant.auth.dto.AuthResponse;
import com.enterprise.ai.knowledge.assistant.auth.dto.LoginRequest;
import com.enterprise.ai.knowledge.assistant.auth.dto.RegisterRequest;
import com.enterprise.ai.knowledge.assistant.auth.entity.User;
import com.enterprise.ai.knowledge.assistant.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication API", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or email/username already taken")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UUID userId) {
        log.info("Fetching current user info for id: {}", userId);
        User user = authenticationService.getCurrentUser(userId);
        return ResponseEntity.ok(user);
    }

    // Temporary endpoint to reset admin password - remove in production
    @PostMapping("/reset-admin-password")
    @Operation(summary = "Reset admin password (TEMPORARY)", description = "Reset admin password to Admin@123 - remove in production")
    public ResponseEntity<String> resetAdminPassword() {
        log.warn("Admin password reset requested");
        String newHash = authenticationService.resetAdminPassword();
        return ResponseEntity.ok("Admin password reset. New hash: " + newHash);
    }
}
