package com.enterprise.ai.knowledge.assistant.auth.entity;

import java.time.Instant;
import java.util.UUID;

public class UserConversation {
    private UUID id;
    private UUID userId;
    private UUID conversationId;
    private Instant createdAt;

    public UserConversation() {}

    public UserConversation(UUID id, UUID userId, UUID conversationId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
