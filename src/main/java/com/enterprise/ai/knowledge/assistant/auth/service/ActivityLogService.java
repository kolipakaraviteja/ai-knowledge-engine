package com.enterprise.ai.knowledge.assistant.auth.service;

import com.enterprise.ai.knowledge.assistant.auth.entity.UserActivityLog;
import com.enterprise.ai.knowledge.assistant.auth.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    public void logActivity(UUID userId, String action, String resourceType, UUID resourceId) {
        logActivity(userId, action, resourceType, resourceId, null);
    }

    public void logActivity(UUID userId, String action, String resourceType, UUID resourceId, Map<String, Object> details) {
        UserActivityLog activityLog = new UserActivityLog();
        activityLog.setId(UUID.randomUUID());
        activityLog.setUserId(userId);
        activityLog.setAction(action);
        activityLog.setResourceType(resourceType);
        activityLog.setResourceId(resourceId);
        activityLog.setDetails(details);
        activityLog.setCreatedAt(Instant.now());

        userActivityLogRepository.save(activityLog);
        log.debug("Logged activity: {} for user: {} on resource: {}", action, userId, resourceType);
    }

    public void logLogin(UUID userId) {
        logActivity(userId, "LOGIN", "USER", userId);
    }

    public void logLogout(UUID userId) {
        logActivity(userId, "LOGOUT", "USER", userId);
    }

    public void logKnowledgeBaseCreate(UUID userId, UUID knowledgeBaseId) {
        logActivity(userId, "CREATE", "KNOWLEDGE_BASE", knowledgeBaseId);
    }

    public void logKnowledgeBaseDelete(UUID userId, UUID knowledgeBaseId) {
        logActivity(userId, "DELETE", "KNOWLEDGE_BASE", knowledgeBaseId);
    }

    public void logConversationCreate(UUID userId, UUID conversationId) {
        logActivity(userId, "CREATE", "CONVERSATION", conversationId);
    }

    public void logConversationDelete(UUID userId, UUID conversationId) {
        logActivity(userId, "DELETE", "CONVERSATION", conversationId);
    }

    public void logDocumentUpload(UUID userId, String documentId) {
        Map<String, Object> details = new HashMap<>();
        details.put("documentId", documentId);
        logActivity(userId, "UPLOAD", "DOCUMENT", null, details);
    }

    public void logDocumentDelete(UUID userId, String documentId) {
        Map<String, Object> details = new HashMap<>();
        details.put("documentId", documentId);
        logActivity(userId, "DELETE", "DOCUMENT", null, details);
    }

    public void logChatQuery(UUID userId, UUID conversationId) {
        logActivity(userId, "CHAT", "CONVERSATION", conversationId);
    }
}
