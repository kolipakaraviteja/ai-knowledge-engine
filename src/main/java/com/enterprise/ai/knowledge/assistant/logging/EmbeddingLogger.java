package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Component-specific logger for embedding generation and vector operations.
 * Provides structured logging with correlation IDs and key-value pairs for observability.
 */
@Slf4j
@Component
public class EmbeddingLogger {

    private static final String COMPONENT = "EMBEDDING";
    private static final String OPERATION = "operation";
    private static final String MODEL_NAME = "model_name";
    private static final String TEXT_LENGTH = "text_length";
    private static final String VECTOR_DIMENSION = "vector_dimension";
    private static final String EMBEDDING_TIME = "embedding_time_ms";
    private static final String BATCH_SIZE = "batch_size";
    private static final String SUCCESS_COUNT = "success_count";
    private static final String FAILURE_COUNT = "failure_count";
    private static final String CHUNK_HASH = "chunk_hash";
    private static final String DOCUMENT_ID = "document_id";

    /**
     * Log embedding generation start
     */
    public void logEmbeddingGenerationStart(String text, String modelName) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "embedding_generation_start");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(TEXT_LENGTH, String.valueOf(text != null ? text.length() : 0));
        
        log.debug("[{}] Embedding generation started: model={}, text_length={}", 
                correlationId, modelName, text != null ? text.length() : 0);
        
        clearMdc();
    }

    /**
     * Log embedding generation completion
     */
    public void logEmbeddingGenerationComplete(String text, String modelName, int vectorDimension, long embeddingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "embedding_generation_complete");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(TEXT_LENGTH, String.valueOf(text != null ? text.length() : 0));
        MDC.put(VECTOR_DIMENSION, String.valueOf(vectorDimension));
        MDC.put(EMBEDDING_TIME, String.valueOf(embeddingTimeMs));
        
        log.debug("[{}] Embedding generation completed: model={}, text_length={}, dimension={}, time={}ms", 
                correlationId, modelName, text != null ? text.length() : 0, vectorDimension, embeddingTimeMs);
        
        clearMdc();
    }

    /**
     * Log batch embedding operation
     */
    public void logBatchEmbedding(int batchSize, String modelName, int successCount, int failureCount, long totalTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "batch_embedding");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(BATCH_SIZE, String.valueOf(batchSize));
        MDC.put(SUCCESS_COUNT, String.valueOf(successCount));
        MDC.put(FAILURE_COUNT, String.valueOf(failureCount));
        MDC.put(EMBEDDING_TIME, String.valueOf(totalTimeMs));
        
        log.info("[{}] Batch embedding: model={}, batch_size={}, success={}, failure={}, time={}ms", 
                correlationId, modelName, batchSize, successCount, failureCount, totalTimeMs);
        
        clearMdc();
    }

    /**
     * Log embedding generation failure
     */
    public void logEmbeddingFailure(String text, String modelName, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "embedding_failure");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(TEXT_LENGTH, String.valueOf(text != null ? text.length() : 0));
        MDC.put("error_type", e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] Embedding generation failed: model={}, text_length={}, error={}", 
                correlationId, modelName, text != null ? text.length() : 0, e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log vector storage operation
     */
    public void logVectorStorage(String documentId, String chunkHash, int vectorDimension) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "vector_storage");
        MDC.put(DOCUMENT_ID, documentId);
        MDC.put(CHUNK_HASH, chunkHash);
        MDC.put(VECTOR_DIMENSION, String.valueOf(vectorDimension));
        
        log.debug("[{}] Vector storage: document_id={}, chunk_hash={}, dimension={}", 
                correlationId, documentId, chunkHash, vectorDimension);
        
        clearMdc();
    }

    /**
     * Log vector search operation
     */
    public void logVectorSearch(String query, int topK, String modelName, long searchTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "vector_search");
        MDC.put("query_length", String.valueOf(query != null ? query.length() : 0));
        MDC.put("top_k", String.valueOf(topK));
        MDC.put(MODEL_NAME, modelName);
        MDC.put(EMBEDDING_TIME, String.valueOf(searchTimeMs));
        
        log.debug("[{}] Vector search: query_length={}, top_k={}, model={}, time={}ms", 
                correlationId, query != null ? query.length() : 0, topK, modelName, searchTimeMs);
        
        clearMdc();
    }

    /**
     * Log vector search results
     */
    public void logVectorSearchResults(int resultsCount, long searchTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "vector_search_results");
        MDC.put("results_count", String.valueOf(resultsCount));
        MDC.put(EMBEDDING_TIME, String.valueOf(searchTimeMs));
        
        log.debug("[{}] Vector search results: count={}, time={}ms", 
                correlationId, resultsCount, searchTimeMs);
        
        clearMdc();
    }

    /**
     * Log duplicate chunk detection
     */
    public void logDuplicateChunk(String chunkHash, String documentId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "duplicate_chunk");
        MDC.put(CHUNK_HASH, chunkHash);
        MDC.put(DOCUMENT_ID, documentId);
        
        log.debug("[{}] Duplicate chunk skipped: hash={}, document_id={}", 
                correlationId, chunkHash, documentId);
        
        clearMdc();
    }

    /**
     * Log embedding model information
     */
    public void logModelInfo(String modelName, int vectorDimension) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "model_info");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(VECTOR_DIMENSION, String.valueOf(vectorDimension));
        
        log.info("[{}] Embedding model info: model={}, dimension={}", 
                correlationId, modelName, vectorDimension);
        
        clearMdc();
    }

    /**
     * Log embedding service initialization
     */
    public void logServiceInitialization(String modelName, int vectorDimension) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "service_initialization");
        MDC.put(MODEL_NAME, modelName);
        MDC.put(VECTOR_DIMENSION, String.valueOf(vectorDimension));
        
        log.info("[{}] Embedding service initialized: model={}, dimension={}", 
                correlationId, modelName, vectorDimension);
        
        clearMdc();
    }

    /**
     * Log embedding operation error
     */
    public void logEmbeddingError(String operation, String documentId, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, operation + "_error");
        MDC.put(DOCUMENT_ID, documentId);
        MDC.put("error_type", e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] Embedding error in {}: document_id={}, error={}", 
                correlationId, operation, documentId, e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log embedding cache hit (if caching is implemented)
     */
    public void logCacheHit(String chunkHash) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "cache_hit");
        MDC.put(CHUNK_HASH, chunkHash);
        
        log.debug("[{}] Embedding cache hit: hash={}", correlationId, chunkHash);
        
        clearMdc();
    }

    /**
     * Log embedding cache miss (if caching is implemented)
     */
    public void logCacheMiss(String chunkHash) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "cache_miss");
        MDC.put(CHUNK_HASH, chunkHash);
        
        log.debug("[{}] Embedding cache miss: hash={}", correlationId, chunkHash);
        
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
        MDC.remove(MODEL_NAME);
        MDC.remove(TEXT_LENGTH);
        MDC.remove(VECTOR_DIMENSION);
        MDC.remove(EMBEDDING_TIME);
        MDC.remove(BATCH_SIZE);
        MDC.remove(SUCCESS_COUNT);
        MDC.remove(FAILURE_COUNT);
        MDC.remove(CHUNK_HASH);
        MDC.remove(DOCUMENT_ID);
    }
}