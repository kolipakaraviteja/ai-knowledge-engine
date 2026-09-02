package com.enterprise.ai.knowledge.assistant.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

public class Collection {
    private UUID id;
    private UUID knowledgeBaseId;
    private String name;
    private String description;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;

    public Collection() {}

    public Collection(UUID id, UUID knowledgeBaseId, String name, String description, UUID ownerId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(UUID knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
