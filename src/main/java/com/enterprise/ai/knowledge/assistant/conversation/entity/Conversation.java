package com.enterprise.ai.knowledge.assistant.conversation.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Conversation {
    private UUID id;
    private String title;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> metadata;

    public Conversation() {}

    public Conversation(UUID id, String title, UUID ownerId, Instant createdAt, Instant updatedAt, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

