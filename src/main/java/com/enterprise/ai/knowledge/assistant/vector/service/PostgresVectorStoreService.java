package com.enterprise.ai.knowledge.assistant.vector.service;

import com.enterprise.ai.knowledge.assistant.repository.PostgresVectorRepository;
import com.enterprise.ai.knowledge.assistant.repository.VectorRepository;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import com.enterprise.ai.knowledge.assistant.vector.entity.ChunkEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PostgresVectorStoreService implements VectorStoreService {

    private final VectorRepository repository;

    public PostgresVectorStoreService(VectorRepository repository) {
        this.repository = repository;
        this.repository.ensureTable();
    }

    @Override
    public void storeChunk(ChunkEntity chunk) {
        repository.insertChunk(chunk);
    }

    @Override
    public List<SearchResult> findNearest(float[] query, int k) {
        return repository.findNearest(query, k);
    }

    @Override
    public List<SearchResult> findNearest(float[] query, int k, UUID collectionId) {
        if (repository instanceof PostgresVectorRepository) {
            return ((PostgresVectorRepository) repository).findNearest(query, k, collectionId);
        }
        // Fallback to unscoped search if repository doesn't support collection filtering
        return repository.findNearest(query, k);
    }

    @Override
    public List<SearchResult> findNearest(float[] query, int k, UUID knowledgeBaseId, UUID collectionId) {
        if (repository instanceof PostgresVectorRepository) {
            return ((PostgresVectorRepository) repository).findNearest(query, k, knowledgeBaseId, collectionId);
        }
        // Fallback to unscoped search if repository doesn't support knowledge base/collection filtering
        return repository.findNearest(query, k);
    }

    @Override
    public boolean existsByHash(String hash) {
        return repository.existsByHash(hash);
    }
}

