package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Component-specific logger for RAG (Retrieval-Augmented Generation) pipeline operations.
 * Provides structured logging with correlation IDs and key-value pairs for observability.
 */
@Slf4j
@Component
public class RAGLogger {

    private static final String COMPONENT = "RAG";
    private static final String OPERATION = "operation";
    private static final String QUERY = "query";
    private static final String RESULTS_COUNT = "results_count";
    private static final String RETRIEVAL_TIME = "retrieval_time_ms";
    private static final String VECTOR_TOP_K = "vector_top_k";
    private static final String FINAL_TOP_N = "final_top_n";
    private static final String RETRIEVAL_TYPE = "retrieval_type";
    private static final String RERANKING_TIME = "reranking_time_ms";
    private static final String CONTEXT_LENGTH = "context_length";
    private static final String DOCUMENT_COUNT = "document_count";

    /**
     * Log retrieval operation start
     */
    public void logRetrievalStart(String query, int topK, String retrievalType) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "retrieval_start");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put(VECTOR_TOP_K, String.valueOf(topK));
        MDC.put(RETRIEVAL_TYPE, retrievalType);
        
        log.info("[{}] RAG retrieval started: query={}, topK={}, type={}", correlationId, maskSensitiveData(query), topK, retrievalType);
        
        clearMdc();
    }

    /**
     * Log retrieval operation completion
     */
    public void logRetrievalComplete(String query, int resultsCount, long retrievalTimeMs, String retrievalType) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "retrieval_complete");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put(RESULTS_COUNT, String.valueOf(resultsCount));
        MDC.put(RETRIEVAL_TIME, String.valueOf(retrievalTimeMs));
        MDC.put(RETRIEVAL_TYPE, retrievalType);
        
        log.info("[{}] RAG retrieval completed: query={}, results={}, time={}ms, type={}", 
                correlationId, maskSensitiveData(query), resultsCount, retrievalTimeMs, retrievalType);
        
        clearMdc();
    }

    /**
     * Log hybrid retrieval operation
     */
    public void logHybridRetrieval(String query, int vectorResults, int keywordResults, int fusedResults, long totalTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "hybrid_retrieval");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put("vector_results", String.valueOf(vectorResults));
        MDC.put("keyword_results", String.valueOf(keywordResults));
        MDC.put("fused_results", String.valueOf(fusedResults));
        MDC.put(RETRIEVAL_TIME, String.valueOf(totalTimeMs));
        
        log.info("[{}] Hybrid retrieval: query={}, vector={}, keyword={}, fused={}, time={}ms", 
                correlationId, maskSensitiveData(query), vectorResults, keywordResults, fusedResults, totalTimeMs);
        
        clearMdc();
    }

    /**
     * Log reranking operation
     */
    public void logReranking(String query, int inputCount, int outputCount, long rerankingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "reranking");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put("input_count", String.valueOf(inputCount));
        MDC.put("output_count", String.valueOf(outputCount));
        MDC.put(RERANKING_TIME, String.valueOf(rerankingTimeMs));
        
        log.debug("[{}] Reranking: query={}, input={}, output={}, time={}ms", 
                correlationId, maskSensitiveData(query), inputCount, outputCount, rerankingTimeMs);
        
        clearMdc();
    }

    /**
     * Log context building operation
     */
    public void logContextBuilding(String query, int chunkCount, int documentCount, int contextLength) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "context_building");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put("chunk_count", String.valueOf(chunkCount));
        MDC.put(DOCUMENT_COUNT, String.valueOf(documentCount));
        MDC.put(CONTEXT_LENGTH, String.valueOf(contextLength));
        
        log.debug("[{}] Context building: query={}, chunks={}, documents={}, context_length={}", 
                correlationId, maskSensitiveData(query), chunkCount, documentCount, contextLength);
        
        clearMdc();
    }

    /**
     * Log RAG pipeline error
     */
    public void logRagError(String operation, String query, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, operation + "_error");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put("error_type", e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] RAG error in {}: query={}, error={}", 
                correlationId, operation, maskSensitiveData(query), e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log fallback to simple vector search
     */
    public void logFallback(String query, String reason) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "fallback");
        MDC.put(QUERY, maskSensitiveData(query));
        MDC.put("fallback_reason", reason);
        
        log.warn("[{}] RAG fallback to simple search: query={}, reason={}", 
                correlationId, maskSensitiveData(query), reason);
        
        clearMdc();
    }

    /**
     * Log query rewriting operation
     */
    public void logQueryRewrite(String originalQuery, String rewrittenQuery, String conversationContext) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "query_rewrite");
        MDC.put("original_query", maskSensitiveData(originalQuery));
        MDC.put("rewritten_query", maskSensitiveData(rewrittenQuery));
        MDC.put("has_context", String.valueOf(conversationContext != null && !conversationContext.isEmpty()));
        
        log.debug("[{}] Query rewrite: original='{}', rewritten='{}', has_context={}", 
                correlationId, maskSensitiveData(originalQuery), maskSensitiveData(rewrittenQuery), 
                conversationContext != null && !conversationContext.isEmpty());
        
        clearMdc();
    }

    /**
     * Log metadata filter operation
     */
    public void logMetadataFilter(int inputCount, int outputCount, String filterCriteria) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "metadata_filter");
        MDC.put("input_count", String.valueOf(inputCount));
        MDC.put("output_count", String.valueOf(outputCount));
        MDC.put("filter_criteria", filterCriteria != null ? filterCriteria : "none");
        
        log.debug("[{}] Metadata filter: input={}, output={}, criteria={}", 
                correlationId, inputCount, outputCount, filterCriteria != null ? filterCriteria : "none");
        
        clearMdc();
    }

    /**
     * Log context compression operation
     */
    public void logContextCompression(int originalLength, int compressedLength, double compressionRatio) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "context_compression");
        MDC.put("original_length", String.valueOf(originalLength));
        MDC.put("compressed_length", String.valueOf(compressedLength));
        MDC.put("compression_ratio", String.format("%.2f", compressionRatio));
        
        log.debug("[{}] Context compression: original={}, compressed={}, ratio={}", 
                correlationId, originalLength, compressedLength, String.format("%.2f", compressionRatio));
        
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
        MDC.remove(QUERY);
        MDC.remove(RESULTS_COUNT);
        MDC.remove(RETRIEVAL_TIME);
        MDC.remove(VECTOR_TOP_K);
        MDC.remove(FINAL_TOP_N);
        MDC.remove(RETRIEVAL_TYPE);
        MDC.remove(RERANKING_TIME);
        MDC.remove(CONTEXT_LENGTH);
        MDC.remove(DOCUMENT_COUNT);
    }

    /**
     * Mask sensitive data in queries to prevent logging of PII or sensitive information
     */
    private String maskSensitiveData(String query) {
        if (query == null) {
            return null;
        }
        // Basic masking for common sensitive patterns
        // This is a simple implementation - enhance based on your specific requirements
        return query.replaceAll("(?i)(password|api[_-]?key|secret|token)\\s*[:=]\\s*\\S+", "$1=***")
                   .replaceAll("\\b\\d{16}\\b", "****-****-****-****") // Credit card numbers
                   .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "***@***.***"); // Email addresses
    }
}