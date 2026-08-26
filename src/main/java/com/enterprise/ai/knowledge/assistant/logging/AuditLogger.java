package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Audit logger for tracking sensitive operations and providing compliance audit trails.
 * Logs security-relevant events with user context, operation details, and timestamps.
 */
@Slf4j
@Component
public class AuditLogger {

    private static final String COMPONENT = "AUDIT";
    private static final String OPERATION = "operation";
    private static final String USER_ID = "user_id";
    private static final String RESOURCE_TYPE = "resource_type";
    private static final String RESOURCE_ID = "resource_id";
    private static final String ACTION = "action";
    private static final String RESULT = "result";
    private static final String REASON = "reason";
    private static final String IP_ADDRESS = "ip_address";
    private static final String USER_AGENT = "user_agent";
    private static final String TIMESTAMP = "timestamp";

    private final boolean auditEnabled;
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AuditLogger(@Value("${app.logging.audit.enabled:true}") boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    /**
     * Log document upload operation
     */
    public void logDocumentUpload(String documentId, String documentName, String userId, boolean success, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "document_upload");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "document");
        MDC.put(RESOURCE_ID, documentId);
        MDC.put("resource_name", documentName);
        MDC.put(ACTION, "upload");
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Document upload - user={}, document_id={}, document_name={}, result={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", documentId, documentName, 
                success ? "success" : "failure", reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log document deletion operation
     */
    public void logDocumentDeletion(String documentId, String documentName, String userId, boolean success, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "document_deletion");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "document");
        MDC.put(RESOURCE_ID, documentId);
        MDC.put("resource_name", documentName);
        MDC.put(ACTION, "delete");
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.warn("[{}] AUDIT: Document deletion - user={}, document_id={}, document_name={}, result={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", documentId, documentName, 
                success ? "success" : "failure", reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log conversation creation
     */
    public void logConversationCreation(String conversationId, String userId, boolean success) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_creation");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "conversation");
        MDC.put(RESOURCE_ID, conversationId);
        MDC.put(ACTION, "create");
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Conversation creation - user={}, conversation_id={}, result={}", 
                correlationId, userId != null ? userId : "anonymous", conversationId, success ? "success" : "failure");

        clearMdc();
    }

    /**
     * Log conversation deletion
     */
    public void logConversationDeletion(String conversationId, String userId, boolean success, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_deletion");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "conversation");
        MDC.put(RESOURCE_ID, conversationId);
        MDC.put(ACTION, "delete");
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.warn("[{}] AUDIT: Conversation deletion - user={}, conversation_id={}, result={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", conversationId, 
                success ? "success" : "failure", reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log knowledge base access
     */
    public void logKnowledgeBaseAccess(String knowledgeBaseId, String userId, String action, boolean success) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "knowledge_base_access");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "knowledge_base");
        MDC.put(RESOURCE_ID, knowledgeBaseId);
        MDC.put(ACTION, action);
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Knowledge base access - user={}, knowledge_base_id={}, action={}, result={}", 
                correlationId, userId != null ? userId : "anonymous", knowledgeBaseId, action, success ? "success" : "failure");

        clearMdc();
    }

    /**
     * Log collection management operation
     */
    public void logCollectionManagement(String collectionId, String userId, String action, boolean success, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "collection_management");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "collection");
        MDC.put(RESOURCE_ID, collectionId);
        MDC.put(ACTION, action);
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Collection management - user={}, collection_id={}, action={}, result={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", collectionId, action, 
                success ? "success" : "failure", reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log evaluation run operation
     */
    public void logEvaluationRun(String evaluationRunId, String userId, int testCount, boolean success) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "evaluation_run");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, "evaluation");
        MDC.put(RESOURCE_ID, evaluationRunId);
        MDC.put(ACTION, "run");
        MDC.put("test_count", String.valueOf(testCount));
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Evaluation run - user={}, evaluation_run_id={}, test_count={}, result={}", 
                correlationId, userId != null ? userId : "anonymous", evaluationRunId, testCount, success ? "success" : "failure");

        clearMdc();
    }

    /**
     * Log administrative operation
     */
    public void logAdminOperation(String operation, String userId, Map<String, String> details, boolean success, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "admin_operation");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put("admin_operation", operation);
        MDC.put(ACTION, "admin");
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        // Add details to MDC if provided
        if (details != null) {
            details.forEach((key, value) -> MDC.put("detail_" + key, value));
        }

        log.warn("[{}] AUDIT: Admin operation - user={}, operation={}, result={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", operation, 
                success ? "success" : "failure", reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log failed authentication attempt
     */
    public void logAuthenticationFailure(String userId, String ipAddress, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "authentication_failure");
        MDC.put(USER_ID, userId != null ? userId : "unknown");
        MDC.put(IP_ADDRESS, ipAddress != null ? ipAddress : "unknown");
        MDC.put(ACTION, "authenticate");
        MDC.put(RESULT, "failure");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.warn("[{}] AUDIT: Authentication failure - user={}, ip_address={}, reason={}", 
                correlationId, userId != null ? userId : "unknown", ipAddress != null ? ipAddress : "unknown", 
                reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Log data export operation
     */
    public void logDataExport(String resourceType, String resourceId, String userId, int recordCount, boolean success) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "data_export");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, resourceType);
        MDC.put(RESOURCE_ID, resourceId);
        MDC.put(ACTION, "export");
        MDC.put("record_count", String.valueOf(recordCount));
        MDC.put(RESULT, success ? "success" : "failure");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.info("[{}] AUDIT: Data export - user={}, resource_type={}, resource_id={}, record_count={}, result={}", 
                correlationId, userId != null ? userId : "anonymous", resourceType, resourceId, recordCount, 
                success ? "success" : "failure");

        clearMdc();
    }

    /**
     * Log configuration change
     */
    public void logConfigurationChange(String configKey, String oldValue, String newValue, String userId) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "configuration_change");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put("config_key", configKey);
        MDC.put("old_value", oldValue != null ? oldValue : "N/A");
        MDC.put("new_value", newValue != null ? newValue : "N/A");
        MDC.put(ACTION, "configure");
        MDC.put(RESULT, "success");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.warn("[{}] AUDIT: Configuration change - user={}, config_key={}, old_value={}, new_value={}", 
                correlationId, userId != null ? userId : "anonymous", configKey, 
                oldValue != null ? oldValue : "N/A", newValue != null ? newValue : "N/A");

        clearMdc();
    }

    /**
     * Log access denied event
     */
    public void logAccessDenied(String resourceType, String resourceId, String userId, String reason) {
        if (!auditEnabled) return;

        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "access_denied");
        MDC.put(USER_ID, userId != null ? userId : "anonymous");
        MDC.put(RESOURCE_TYPE, resourceType);
        MDC.put(RESOURCE_ID, resourceId);
        MDC.put(ACTION, "access");
        MDC.put(RESULT, "denied");
        MDC.put(REASON, reason != null ? reason : "N/A");
        MDC.put(TIMESTAMP, LocalDateTime.now().format(timestampFormatter));

        log.warn("[{}] AUDIT: Access denied - user={}, resource_type={}, resource_id={}, reason={}", 
                correlationId, userId != null ? userId : "anonymous", resourceType, resourceId, 
                reason != null ? reason : "N/A");

        clearMdc();
    }

    /**
     * Check if audit logging is enabled
     */
    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    /**
     * Clear MDC after logging operations
     */
    private void clearMdc() {
        MDC.remove("component");
        MDC.remove(OPERATION);
        MDC.remove(USER_ID);
        MDC.remove(RESOURCE_TYPE);
        MDC.remove(RESOURCE_ID);
        MDC.remove(ACTION);
        MDC.remove(RESULT);
        MDC.remove(REASON);
        MDC.remove(IP_ADDRESS);
        MDC.remove(USER_AGENT);
        MDC.remove(TIMESTAMP);
    }
}