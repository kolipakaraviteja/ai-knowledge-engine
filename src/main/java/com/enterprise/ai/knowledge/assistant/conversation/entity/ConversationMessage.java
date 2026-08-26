package com.enterprise.ai.knowledge.assistant.conversation.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ConversationMessage {
    private UUID id;
    private UUID conversationId;
    private int messageOrder;
    private String role;
    private String message;
    private Instant createdAt;
    private Map<String, Object> metadata;

    public ConversationMessage() {}

    public ConversationMessage(UUID id, UUID conversationId, int messageOrder, String role,
                               String message, Instant createdAt, Map<String, Object> metadata) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageOrder = messageOrder;
        this.role = role;
        this.message = message;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public int getMessageOrder() { return messageOrder; }
    public void setMessageOrder(int messageOrder) { this.messageOrder = messageOrder; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

