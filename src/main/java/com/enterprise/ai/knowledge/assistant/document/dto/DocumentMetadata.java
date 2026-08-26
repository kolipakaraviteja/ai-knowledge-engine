package com.enterprise.ai.knowledge.assistant.document.dto;

import java.time.Instant;

/**
 * Lightweight metadata captured during ingestion.
 */
public record DocumentMetadata(
        String documentId,
        String documentName,
        String documentHash,
        int chunkCount,
        Long fileSize,
        int pages,
        int characters,
        Instant uploadedAt,
        Instant indexedAt,
        String knowledgeBaseId,
        String collectionId
) {
}

