package com.enterprise.ai.knowledge.assistant.embedding.dto;

/**
 * Wrapper for embedding generation results so callers know dimensions and model.
 */
public record EmbeddingResult(
        float[] vector,
        int dimensions,
        String model
) {
}

