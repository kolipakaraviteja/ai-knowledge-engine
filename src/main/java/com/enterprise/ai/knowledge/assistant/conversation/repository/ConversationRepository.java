package com.enterprise.ai.knowledge.assistant.conversation.repository;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.conversation.entity.Conversation;
import com.enterprise.ai.knowledge.assistant.conversation.entity.ConversationMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
    UUID createConversation(String title);
    Optional<Conversation> getConversation(UUID conversationId);
    List<ConversationMessage> getRecentMessages(UUID conversationId, int limit);
    void saveMessage(ConversationMessage message);
    int getMessageCount(UUID conversationId);
    void updateConversationTitle(UUID conversationId, String title);
    List<ChatResponse> getConversationHistory(UUID conversationId);
    List<Map<String, Object>> getAllConversations();
    void deleteConversation(UUID conversationId);
    Map<String, Object> getCitationDetails(String chunkHash);
    List<Map<String, Object>> searchConversations(String query);
}

