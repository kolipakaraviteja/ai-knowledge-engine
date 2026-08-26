package com.enterprise.ai.knowledge.assistant.rag.retriever;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import com.enterprise.ai.knowledge.assistant.vector.service.VectorStoreService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class VectorRetriever {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public VectorRetriever(EmbeddingService embeddingService, VectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public List<SearchResult> retrieve(String query, int topK) {
        return retrieve(query, topK, null, null);
    }

    public List<SearchResult> retrieve(String query, int topK, UUID collectionId) {
        return retrieve(query, topK, null, collectionId);
    }

    public List<SearchResult> retrieve(String query, int topK, UUID knowledgeBaseId, UUID collectionId) {
        try {
            EmbeddingResult embedding = embeddingService.generateEmbedding(query);
            if (embedding == null || embedding.vector() == null) {
                return List.of();
            }
            return vectorStoreService.findNearest(embedding.vector(), topK, knowledgeBaseId, collectionId);
        } catch (Exception e) {
            return List.of();
        }
    }
}

