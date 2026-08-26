package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Component-specific logger for chat operations and conversation management.
 * Provides structured logging with correlation IDs and key-value pairs for observability.
 */
@Slf4j
@Component
public class ChatLogger {

    private static final String COMPONENT = "CHAT";
    private static final String OPERATION = "operation";
    private static final String CONVERSATION_ID = "conversation_id";
    private static final String MESSAGE_LENGTH = "message_length";
    private static final String HISTORY_DEPTH = "history_depth";
    private static final String RESPONSE_LENGTH = "response_length";
    private static final String IS_FROM_CONTEXT = "is_from_context";
    private static final String RETRIEVAL_COUNT = "retrieval_count";
    private static final String PROCESSING_TIME = "processing_time_ms";
    private static final String LLM_TIME = "llm_time_ms";
    private static final String RETRIEVAL_TIME = "retrieval_time_ms";
    private static final String CHAT_TYPE = "chat_type";
    private static final String ERROR_TYPE = "error_type";

    /**
     * Log simple chat request (no RAG)
     */
    public void logSimpleChatRequest(String message) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "simple_chat_request");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(CHAT_TYPE, "simple");
        
        log.info("[{}] Simple chat request: message_length={}", 
                correlationId, message != null ? message.length() : 0);
        
        clearMdc();
    }

    /**
     * Log RAG chat request
     */
    public void logRagChatRequest(String message, int vectorTopK, int finalTopN) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "rag_chat_request");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put("vector_top_k", String.valueOf(vectorTopK));
        MDC.put("final_top_n", String.valueOf(finalTopN));
        MDC.put(CHAT_TYPE, "rag");
        
        log.info("[{}] RAG chat request: message_length={}, vector_top_k={}, final_top_n={}", 
                correlationId, message != null ? message.length() : 0, vectorTopK, finalTopN);
        
        clearMdc();
    }

    /**
     * Log chat response
     */
    public void logChatResponse(String message, String response, boolean isFromContext, 
                                int retrievalCount, long processingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "chat_response");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(RESPONSE_LENGTH, String.valueOf(response != null ? response.length() : 0));
        MDC.put(IS_FROM_CONTEXT, String.valueOf(isFromContext));
        MDC.put(RETRIEVAL_COUNT, String.valueOf(retrievalCount));
        MDC.put(PROCESSING_TIME, String.valueOf(processingTimeMs));
        
        log.info("[{}] Chat response: message_length={}, response_length={}, from_context={}, retrieval_count={}, time={}ms", 
                correlationId, message != null ? message.length() : 0, response != null ? response.length() : 0, 
                isFromContext, retrievalCount, processingTimeMs);
        
        clearMdc();
    }

    /**
     * Log conversation start
     */
    public void logConversationStart(UUID conversationId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_start");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "new");
        
        log.info("[{}] Conversation started: conversation_id={}", 
                correlationId, conversationId != null ? conversationId.toString() : "new");
        
        clearMdc();
    }

    /**
     * Log conversation message
     */
    public void logConversationMessage(UUID conversationId, String role, String message, int messageOrder) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_message");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        MDC.put("role", role);
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put("message_order", String.valueOf(messageOrder));
        
        log.debug("[{}] Conversation message: conversation_id={}, role={}, message_length={}, order={}", 
                correlationId, conversationId != null ? conversationId.toString() : "unknown", 
                role, message != null ? message.length() : 0, messageOrder);
        
        clearMdc();
    }

    /**
     * Log conversation with history
     */
    public void logConversationWithHistory(UUID conversationId, String message, int historyDepth) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_with_history");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(HISTORY_DEPTH, String.valueOf(historyDepth));
        
        log.info("[{}] Conversation with history: conversation_id={}, message_length={}, history_depth={}", 
                correlationId, conversationId != null ? conversationId.toString() : "unknown", 
                message != null ? message.length() : 0, historyDepth);
        
        clearMdc();
    }

    /**
     * Log conversation deletion
     */
    public void logConversationDeletion(UUID conversationId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_deletion");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        
        log.info("[{}] Conversation deleted: conversation_id={}", 
                correlationId, conversationId != null ? conversationId.toString() : "unknown");
        
        clearMdc();
    }

    /**
     * Log conversation search
     */
    public void logConversationSearch(String query, int resultCount) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_search");
        MDC.put("query_length", String.valueOf(query != null ? query.length() : 0));
        MDC.put("result_count", String.valueOf(resultCount));
        
        log.debug("[{}] Conversation search: query_length={}, result_count={}", 
                correlationId, query != null ? query.length() : 0, resultCount);
        
        clearMdc();
    }

    /**
     * Log conversation listing
     */
    public void logConversationListing(int conversationCount) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "conversation_listing");
        MDC.put("conversation_count", String.valueOf(conversationCount));
        
        log.debug("[{}] Conversation listing: count={}", correlationId, conversationCount);
        
        clearMdc();
    }

    /**
     * Log response regeneration
     */
    public void logResponseRegeneration(UUID conversationId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "response_regeneration");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        
        log.info("[{}] Response regeneration: conversation_id={}", 
                correlationId, conversationId != null ? conversationId.toString() : "unknown");
        
        clearMdc();
    }

    /**
     * Log follow-up question generation
     */
    public void logFollowUpGeneration(UUID conversationId, int questionCount) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "follow_up_generation");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        MDC.put("question_count", String.valueOf(questionCount));
        
        log.debug("[{}] Follow-up question generation: conversation_id={}, count={}", 
                correlationId, conversationId != null ? conversationId.toString() : "unknown", questionCount);
        
        clearMdc();
    }

    /**
     * Log streaming chat start
     */
    public void logStreamChatStart(String message) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "stream_chat_start");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(CHAT_TYPE, "stream");
        
        log.info("[{}] Stream chat started: message_length={}", 
                correlationId, message != null ? message.length() : 0);
        
        clearMdc();
    }

    /**
     * Log streaming chat completion
     */
    public void logStreamChatComplete(String message, long streamTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "stream_chat_complete");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(PROCESSING_TIME, String.valueOf(streamTimeMs));
        
        log.info("[{}] Stream chat completed: message_length={}, time={}ms", 
                correlationId, message != null ? message.length() : 0, streamTimeMs);
        
        clearMdc();
    }

    /**
     * Log streaming chat error
     */
    public void logStreamChatError(String message, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "stream_chat_error");
        MDC.put(MESSAGE_LENGTH, String.valueOf(message != null ? message.length() : 0));
        MDC.put(ERROR_TYPE, e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] Stream chat error: message_length={}, error={}", 
                correlationId, message != null ? message.length() : 0, e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log chat operation error
     */
    public void logChatError(String operation, UUID conversationId, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, operation + "_error");
        MDC.put(CONVERSATION_ID, conversationId != null ? conversationId.toString() : "unknown");
        MDC.put(ERROR_TYPE, e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] Chat error in {}: conversation_id={}, error={}", 
                correlationId, operation, conversationId != null ? conversationId.toString() : "unknown", e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log fallback to simple chat (when RAG fails)
     */
    public void logChatFallback(String operation, String reason) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "chat_fallback");
        MDC.put("original_operation", operation);
        MDC.put("fallback_reason", reason);
        
        log.warn("[{}] Chat fallback: operation={}, reason={}", correlationId, operation, reason);
        
        clearMdc();
    }

    /**
     * Add structured key-value pairs to MDC for custom logging
     */
    public void putStructuredData(Map<String, String> data) {
        if (data != null) {
            data.forEach(MDC::put);
        }
    }

    /**
     * Clear MDC after logging operations
     */
    private void clearMdc() {
        MDC.remove("component");
        MDC.remove(OPERATION);
        MDC.remove(CONVERSATION_ID);
        MDC.remove(MESSAGE_LENGTH);
        MDC.remove(HISTORY_DEPTH);
        MDC.remove(RESPONSE_LENGTH);
        MDC.remove(IS_FROM_CONTEXT);
        MDC.remove(RETRIEVAL_COUNT);
        MDC.remove(PROCESSING_TIME);
        MDC.remove(LLM_TIME);
        MDC.remove(RETRIEVAL_TIME);
        MDC.remove(CHAT_TYPE);
        MDC.remove(ERROR_TYPE);
    }
}