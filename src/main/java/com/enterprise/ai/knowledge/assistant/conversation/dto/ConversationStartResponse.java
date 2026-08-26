package com.enterprise.ai.knowledge.assistant.conversation.dto;

import java.util.UUID;

public class ConversationStartResponse {
    private UUID conversationId;

    public ConversationStartResponse() {}

    public ConversationStartResponse(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
}

