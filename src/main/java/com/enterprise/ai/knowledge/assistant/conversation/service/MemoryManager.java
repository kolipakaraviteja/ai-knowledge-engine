package com.enterprise.ai.knowledge.assistant.conversation.service;

import com.enterprise.ai.knowledge.assistant.conversation.entity.ConversationMessage;
import com.enterprise.ai.knowledge.assistant.conversation.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MemoryManager {

    private final ConversationRepository conversationRepository;

    public MemoryManager(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public String getFormattedHistory(UUID conversationId, int exchangeCount) {
        if (conversationId == null) return "";

        int messageLimit = exchangeCount * 2;
        List<ConversationMessage> messages = conversationRepository.getRecentMessages(conversationId, messageLimit);

        if (messages == null || messages.isEmpty()) return "";

        Collections.reverse(messages);

        StringBuilder history = new StringBuilder();
        for (ConversationMessage msg : messages) {
            String roleDisplay = "user".equals(msg.getRole()) ? "User" : "Assistant";
            history.append(roleDisplay).append(": ").append(msg.getMessage()).append("\n");
        }

        return history.toString();
    }

    public void saveUserMessage(UUID conversationId, String message, int order) {
        ConversationMessage msg = new ConversationMessage();
        msg.setConversationId(conversationId);
        msg.setMessageOrder(order);
        msg.setRole("user");
        msg.setMessage(message);
        msg.setCreatedAt(Instant.now());

        conversationRepository.saveMessage(msg);
    }

    public void saveAssistantMessage(UUID conversationId, String message, int order) {
        ConversationMessage msg = new ConversationMessage();
        msg.setConversationId(conversationId);
        msg.setMessageOrder(order);
        msg.setRole("assistant");
        msg.setMessage(message);
        msg.setCreatedAt(Instant.now());

        conversationRepository.saveMessage(msg);
    }

    public int getMessageCount(UUID conversationId) {
        return conversationRepository.getMessageCount(conversationId);
    }
}

