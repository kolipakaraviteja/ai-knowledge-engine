package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Component-specific logger for document operations including ingestion, parsing, chunking, and management.
 * Provides structured logging with correlation IDs and key-value pairs for observability.
 */
@Slf4j
@Component
public class DocumentLogger {

    private static final String COMPONENT = "DOCUMENT";
    private static final String OPERATION = "operation";
    private static final String DOCUMENT_NAME = "document_name";
    private static final String DOCUMENT_ID = "document_id";
    private static final String DOCUMENT_HASH = "document_hash";
    private static final String FILE_SIZE = "file_size";
    private static final String PAGE_COUNT = "page_count";
    private static final String CHUNK_COUNT = "chunk_count";
    private static final String CHARACTER_COUNT = "character_count";
    private static final String FILE_TYPE = "file_type";
    private static final String INGESTION_TIME = "ingestion_time_ms";
    private static final String PARSING_TIME = "parsing_time_ms";
    private static final String CHUNKING_TIME = "chunking_time_ms";
    private static final String EMBEDDING_TIME = "embedding_time_ms";
    private static final String STORING_TIME = "storing_time_ms";
    private static final String CHUNK_HASH = "chunk_hash";

    /**
     * Log document ingestion start
     */
    public void logIngestionStart(String documentName, long fileSize, String fileType) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "ingestion_start");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(FILE_SIZE, String.valueOf(fileSize));
        MDC.put(FILE_TYPE, fileType);
        
        log.info("[{}] Document ingestion started: name={}, size={}, type={}", 
                correlationId, documentName, fileSize, fileType);
        
        clearMdc();
    }

    /**
     * Log document ingestion completion
     */
    public void logIngestionComplete(String documentName, String documentId, String documentHash, 
                                      int pageCount, int chunkCount, long ingestionTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "ingestion_complete");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(DOCUMENT_ID, documentId);
        MDC.put(DOCUMENT_HASH, documentHash);
        MDC.put(PAGE_COUNT, String.valueOf(pageCount));
        MDC.put(CHUNK_COUNT, String.valueOf(chunkCount));
        MDC.put(INGESTION_TIME, String.valueOf(ingestionTimeMs));
        
        log.info("[{}] Document ingestion completed: name={}, id={}, hash={}, pages={}, chunks={}, time={}ms", 
                correlationId, documentName, documentId, documentHash, pageCount, chunkCount, ingestionTimeMs);
        
        clearMdc();
    }

    /**
     * Log duplicate document detection
     */
    public void logDuplicateDocument(String documentName, String documentHash) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "duplicate_detected");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(DOCUMENT_HASH, documentHash);
        
        log.info("[{}] Duplicate document skipped: name={}, hash={}", 
                correlationId, documentName, documentHash);
        
        clearMdc();
    }

    /**
     * Log document parsing operation
     */
    public void logParsingStart(String documentName, String fileType) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "parsing_start");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(FILE_TYPE, fileType);
        
        log.debug("[{}] Document parsing started: name={}, type={}", 
                correlationId, documentName, fileType);
        
        clearMdc();
    }

    /**
     * Log document parsing completion
     */
    public void logParsingComplete(String documentName, int characterCount, int pageCount, long parsingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "parsing_complete");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(CHARACTER_COUNT, String.valueOf(characterCount));
        MDC.put(PAGE_COUNT, String.valueOf(pageCount));
        MDC.put(PARSING_TIME, String.valueOf(parsingTimeMs));
        
        log.debug("[{}] Document parsing completed: name={}, characters={}, pages={}, time={}ms", 
                correlationId, documentName, characterCount, pageCount, parsingTimeMs);
        
        clearMdc();
    }

    /**
     * Log document chunking operation
     */
    public void logChunkingStart(String documentName, int chunkSize, int overlap) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "chunking_start");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put("chunk_size", String.valueOf(chunkSize));
        MDC.put("chunk_overlap", String.valueOf(overlap));
        
        log.debug("[{}] Document chunking started: name={}, chunk_size={}, overlap={}", 
                correlationId, documentName, chunkSize, overlap);
        
        clearMdc();
    }

    /**
     * Log document chunking completion
     */
    public void logChunkingComplete(String documentName, int chunkCount, long chunkingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "chunking_complete");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(CHUNK_COUNT, String.valueOf(chunkCount));
        MDC.put(CHUNKING_TIME, String.valueOf(chunkingTimeMs));
        
        log.debug("[{}] Document chunking completed: name={}, chunks={}, time={}ms", 
                correlationId, documentName, chunkCount, chunkingTimeMs);
        
        clearMdc();
    }

    /**
     * Log embedding generation for chunks
     */
    public void logEmbeddingGeneration(String documentName, int totalChunks, int successfulEmbeddings, 
                                       int failedEmbeddings, long embeddingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "embedding_generation");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put("total_chunks", String.valueOf(totalChunks));
        MDC.put("successful_embeddings", String.valueOf(successfulEmbeddings));
        MDC.put("failed_embeddings", String.valueOf(failedEmbeddings));
        MDC.put(EMBEDDING_TIME, String.valueOf(embeddingTimeMs));
        
        log.info("[{}] Embedding generation: name={}, total={}, successful={}, failed={}, time={}ms", 
                correlationId, documentName, totalChunks, successfulEmbeddings, failedEmbeddings, embeddingTimeMs);
        
        clearMdc();
    }

    /**
     * Log chunk storage operation
     */
    public void logChunkStorage(String documentName, int chunksStored, int duplicatesSkipped, long storingTimeMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "chunk_storage");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put("chunks_stored", String.valueOf(chunksStored));
        MDC.put("duplicates_skipped", String.valueOf(duplicatesSkipped));
        MDC.put(STORING_TIME, String.valueOf(storingTimeMs));
        
        log.debug("[{}] Chunk storage: name={}, stored={}, duplicates={}, time={}ms", 
                correlationId, documentName, chunksStored, duplicatesSkipped, storingTimeMs);
        
        clearMdc();
    }

    /**
     * Log document deletion
     */
    public void logDocumentDeletion(String documentId, String documentName) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "document_deletion");
        MDC.put(DOCUMENT_ID, documentId);
        MDC.put(DOCUMENT_NAME, documentName);
        
        log.info("[{}] Document deletion: id={}, name={}", correlationId, documentId, documentName);
        
        clearMdc();
    }

    /**
     * Log document listing operation
     */
    public void logDocumentListing(int documentCount) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "document_listing");
        MDC.put("document_count", String.valueOf(documentCount));
        
        log.debug("[{}] Document listing: count={}", correlationId, documentCount);
        
        clearMdc();
    }

    /**
     * Log document reindexing operation
     */
    public void logDocumentReindexing(String documentId, String documentName) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "document_reindexing");
        MDC.put(DOCUMENT_ID, documentId);
        MDC.put(DOCUMENT_NAME, documentName);
        
        log.info("[{}] Document reindexing started: id={}, name={}", correlationId, documentId, documentName);
        
        clearMdc();
    }

    /**
     * Log unsupported document type
     */
    public void logUnsupportedType(String documentName, String fileType) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "unsupported_type");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put(FILE_TYPE, fileType);
        
        log.warn("[{}] Unsupported document type: name={}, type={}", correlationId, documentName, fileType);
        
        clearMdc();
    }

    /**
     * Log document operation error
     */
    public void logDocumentError(String operation, String documentName, Exception e) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, operation + "_error");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put("error_type", e.getClass().getSimpleName());
        MDC.put("error_message", e.getMessage());
        
        log.error("[{}] Document error in {}: name={}, error={}", 
                correlationId, operation, documentName, e.getMessage(), e);
        
        clearMdc();
    }

    /**
     * Log metadata extraction
     */
    public void logMetadataExtraction(String documentName, Map<String, Object> metadata) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "metadata_extraction");
        MDC.put(DOCUMENT_NAME, documentName);
        MDC.put("metadata_fields", String.valueOf(metadata != null ? metadata.size() : 0));
        
        log.debug("[{}] Metadata extraction: name={}, fields={}", 
                correlationId, documentName, metadata != null ? metadata.size() : 0);
        
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
        MDC.remove(DOCUMENT_NAME);
        MDC.remove(DOCUMENT_ID);
        MDC.remove(DOCUMENT_HASH);
        MDC.remove(FILE_SIZE);
        MDC.remove(PAGE_COUNT);
        MDC.remove(CHUNK_COUNT);
        MDC.remove(CHARACTER_COUNT);
        MDC.remove(FILE_TYPE);
        MDC.remove(INGESTION_TIME);
        MDC.remove(PARSING_TIME);
        MDC.remove(CHUNKING_TIME);
        MDC.remove(EMBEDDING_TIME);
        MDC.remove(STORING_TIME);
        MDC.remove(CHUNK_HASH);
    }
}