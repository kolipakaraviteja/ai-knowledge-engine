package com.enterprise.ai.knowledge.assistant.auth.controller;

import com.enterprise.ai.knowledge.assistant.auth.dto.CreateUserRequest;
import com.enterprise.ai.knowledge.assistant.auth.entity.User;
import com.enterprise.ai.knowledge.assistant.auth.entity.UserActivityLog;
import com.enterprise.ai.knowledge.assistant.auth.entity.UserConversation;
import com.enterprise.ai.knowledge.assistant.auth.entity.UserKnowledgeBase;
import com.enterprise.ai.knowledge.assistant.auth.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin API", description = "Admin-only endpoints for user management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", description = "Get list of all users (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of users retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Admin fetching all users");
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Admin creating user with email: {}", request.getEmail());
        User user = adminService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("Admin deleting user with id: {}", id);
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user activity", description = "Get activity logs for a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity logs retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<List<UserActivityLog>> getUserActivity(@PathVariable UUID id) {
        log.info("Admin fetching activity for user: {}", id);
        List<UserActivityLog> activity = adminService.getUserActivity(id);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/users/{id}/knowledge-bases")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user knowledge bases", description = "Get knowledge bases assigned to a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge bases retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<List<UserKnowledgeBase>> getUserKnowledgeBases(@PathVariable UUID id) {
        log.info("Admin fetching knowledge bases for user: {}", id);
        List<UserKnowledgeBase> knowledgeBases = adminService.getUserKnowledgeBases(id);
        return ResponseEntity.ok(knowledgeBases);
    }

    @GetMapping("/users/{id}/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user conversations", description = "Get conversations assigned to a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversations retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<List<UserConversation>> getUserConversations(@PathVariable UUID id) {
        log.info("Admin fetching conversations for user: {}", id);
        List<UserConversation> conversations = adminService.getUserConversations(id);
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/users/{userId}/knowledge-bases/{knowledgeBaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign knowledge base to user", description = "Assign a knowledge base to a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Knowledge base assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or knowledge base not found"),
            @ApiResponse(responseCode = "400", description = "Knowledge base already assigned"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<Void> assignKnowledgeBaseToUser(@PathVariable UUID userId, @PathVariable UUID knowledgeBaseId) {
        log.info("Admin assigning knowledge base {} to user {}", knowledgeBaseId, userId);
        adminService.assignKnowledgeBaseToUser(userId, knowledgeBaseId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/conversations/{conversationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign conversation to user", description = "Assign a conversation to a user (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or conversation not found"),
            @ApiResponse(responseCode = "400", description = "Conversation already assigned"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<Void> assignConversationToUser(@PathVariable UUID userId, @PathVariable UUID conversationId) {
        log.info("Admin assigning conversation {} to user {}", conversationId, userId);
        adminService.assignConversationToUser(userId, conversationId);
        return ResponseEntity.ok().build();
    }
}
