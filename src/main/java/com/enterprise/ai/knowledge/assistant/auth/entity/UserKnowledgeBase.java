package com.enterprise.ai.knowledge.assistant.auth.entity;

import java.time.Instant;
import java.util.UUID;

public class UserKnowledgeBase {
    private UUID id;
    private UUID userId;
    private UUID knowledgeBaseId;
    private Instant createdAt;

    public UserKnowledgeBase() {}

    public UserKnowledgeBase(UUID id, UUID userId, UUID knowledgeBaseId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(UUID knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
