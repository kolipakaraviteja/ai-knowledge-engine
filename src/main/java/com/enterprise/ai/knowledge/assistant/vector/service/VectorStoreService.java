package com.enterprise.ai.knowledge.assistant.vector.service;

import com.enterprise.ai.knowledge.assistant.vector.entity.ChunkEntity;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;

import java.util.List;
import java.util.UUID;

public interface VectorStoreService {
    void storeChunk(ChunkEntity chunk);
    List<SearchResult> findNearest(float[] query, int k);
    List<SearchResult> findNearest(float[] query, int k, UUID collectionId);
    List<SearchResult> findNearest(float[] query, int k, UUID knowledgeBaseId, UUID collectionId);
    boolean existsByHash(String hash);
}

