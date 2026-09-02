package com.enterprise.ai.knowledge.assistant.auth.service;

import com.enterprise.ai.knowledge.assistant.auth.dto.CreateUserRequest;
import com.enterprise.ai.knowledge.assistant.auth.entity.User;
import com.enterprise.ai.knowledge.assistant.auth.entity.UserActivityLog;
import com.enterprise.ai.knowledge.assistant.auth.entity.UserKnowledgeBase;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserActivityLogRepository;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserKnowledgeBaseRepository;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserRepository;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private final UserConversationRepository userConversationRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    public User createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Validate role
        if (!request.getRole().equals("ADMIN") && !request.getRole().equals("USER")) {
            throw new RuntimeException("Invalid role. Must be ADMIN or USER");
        }

        // Create new user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        log.info("Admin created new user: {} with role: {}", user.getEmail(), user.getRole());

        return user;
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent deleting the default admin
        if (user.getEmail().equals("admin@enterprise.ai")) {
            throw new RuntimeException("Cannot delete default admin user");
        }

        userRepository.deleteById(userId);
        log.info("Admin deleted user: {}", user.getEmail());
    }

    public List<UserActivityLog> getUserActivity(UUID userId) {
        log.info("Fetching activity for user: {}", userId);
        return userActivityLogRepository.findByUserId(userId);
    }

    public List<UserKnowledgeBase> getUserKnowledgeBases(UUID userId) {
        log.info("Fetching knowledge bases for user: {}", userId);
        return userKnowledgeBaseRepository.findByUserId(userId);
    }

    public List<com.enterprise.ai.knowledge.assistant.auth.entity.UserConversation> getUserConversations(UUID userId) {
        log.info("Fetching conversations for user: {}", userId);
        return userConversationRepository.findByUserId(userId);
    }

    public void assignKnowledgeBaseToUser(UUID userId, UUID knowledgeBaseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if assignment already exists
        if (userKnowledgeBaseRepository.findByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId).isPresent()) {
            throw new RuntimeException("Knowledge base already assigned to user");
        }

        UserKnowledgeBase assignment = new UserKnowledgeBase();
        assignment.setId(UUID.randomUUID());
        assignment.setUserId(userId);
        assignment.setKnowledgeBaseId(knowledgeBaseId);
        assignment.setCreatedAt(Instant.now());

        userKnowledgeBaseRepository.save(assignment);

        log.info("Assigned knowledge base {} to user {}", knowledgeBaseId, user.getEmail());
    }

    public void assignConversationToUser(UUID userId, UUID conversationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if assignment already exists
        if (userConversationRepository.findByUserIdAndConversationId(userId, conversationId).isPresent()) {
            throw new RuntimeException("Conversation already assigned to user");
        }

        com.enterprise.ai.knowledge.assistant.auth.entity.UserConversation assignment = 
            new com.enterprise.ai.knowledge.assistant.auth.entity.UserConversation();
        assignment.setId(UUID.randomUUID());
        assignment.setUserId(userId);
        assignment.setConversationId(conversationId);
        assignment.setCreatedAt(Instant.now());

        userConversationRepository.save(assignment);

        log.info("Assigned conversation {} to user {}", conversationId, user.getEmail());
    }
}
