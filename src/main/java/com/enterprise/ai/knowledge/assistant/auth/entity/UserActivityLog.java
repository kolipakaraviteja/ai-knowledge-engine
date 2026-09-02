package com.enterprise.ai.knowledge.assistant.auth.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class UserActivityLog {
    private UUID id;
    private UUID userId;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private Map<String, Object> details;
    private Instant createdAt;

    public UserActivityLog() {}

    public UserActivityLog(UUID id, UUID userId, String action, String resourceType, 
                          UUID resourceId, Map<String, Object> details, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
