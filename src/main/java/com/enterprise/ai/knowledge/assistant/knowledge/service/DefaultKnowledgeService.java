package com.enterprise.ai.knowledge.assistant.knowledge.service;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.Collection;
import com.enterprise.ai.knowledge.assistant.knowledge.entity.KnowledgeBase;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.CollectionRepository;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.KnowledgeBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DefaultKnowledgeService {

    private static final UUID DEFAULT_KNOWLEDGE_BASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_COLLECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String DEFAULT_KNOWLEDGE_BASE_NAME = "Default Knowledge Base";
    private static final String DEFAULT_COLLECTION_NAME = "Default Collection";

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final CollectionRepository collectionRepository;

    public DefaultKnowledgeService(KnowledgeBaseRepository knowledgeBaseRepository,
                                    CollectionRepository collectionRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.collectionRepository = collectionRepository;
    }

    public UUID getDefaultKnowledgeBaseId() {
        return DEFAULT_KNOWLEDGE_BASE_ID;
    }

    public UUID getDefaultCollectionId() {
        return DEFAULT_COLLECTION_ID;
    }

    public KnowledgeBase getOrCreateDefaultKnowledgeBase() {
        Optional<KnowledgeBase> existing = knowledgeBaseRepository.findById(DEFAULT_KNOWLEDGE_BASE_ID);
        if (existing.isPresent()) {
            log.debug("Default knowledge base already exists: {}", DEFAULT_KNOWLEDGE_BASE_ID);
            return existing.get();
        }

        log.info("Creating default knowledge base: {}", DEFAULT_KNOWLEDGE_BASE_ID);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(DEFAULT_KNOWLEDGE_BASE_ID);
        kb.setName(DEFAULT_KNOWLEDGE_BASE_NAME);
        kb.setDescription("Default knowledge base for documents without specified knowledge base");
        kb.setCreatedAt(Instant.now());
        kb.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(kb);
    }

    public Collection getOrCreateDefaultCollection() {
        // Ensure default knowledge base exists first
        getOrCreateDefaultKnowledgeBase();

        Optional<Collection> existing = collectionRepository.findById(DEFAULT_COLLECTION_ID);
        if (existing.isPresent()) {
            log.debug("Default collection already exists: {}", DEFAULT_COLLECTION_ID);
            return existing.get();
        }

        log.info("Creating default collection: {} under knowledge base: {}", DEFAULT_COLLECTION_ID, DEFAULT_KNOWLEDGE_BASE_ID);
        Collection collection = new Collection();
        collection.setId(DEFAULT_COLLECTION_ID);
        collection.setKnowledgeBaseId(DEFAULT_KNOWLEDGE_BASE_ID);
        collection.setName(DEFAULT_COLLECTION_NAME);
        collection.setDescription("Default collection for documents without specified collection");
        collection.setCreatedAt(Instant.now());
        collection.setUpdatedAt(Instant.now());
        return collectionRepository.save(collection);
    }

    public Collection getOrCreateDefaultCollectionForKnowledgeBase(UUID knowledgeBaseId) {
        if (knowledgeBaseId.equals(DEFAULT_KNOWLEDGE_BASE_ID)) {
            return getOrCreateDefaultCollection();
        }

        // For non-default knowledge bases, look for a collection named "Default"
        List<Collection> collections = collectionRepository.findByKnowledgeBaseId(knowledgeBaseId);
        Optional<Collection> defaultCollection = collections.stream()
                .filter(c -> "Default Collection".equalsIgnoreCase(c.getName()))
                .findFirst();

        if (defaultCollection.isPresent()) {
            return defaultCollection.get();
        }

        // Create default collection for this knowledge base
        log.info("Creating default collection for knowledge base: {}", knowledgeBaseId);
        Collection collection = new Collection();
        collection.setKnowledgeBaseId(knowledgeBaseId);
        collection.setName("Default Collection");
        collection.setDescription("Default collection for this knowledge base");
        collection.setCreatedAt(Instant.now());
        collection.setUpdatedAt(Instant.now());
        return collectionRepository.save(collection);
    }

    public void initializeDefaults() {
        getOrCreateDefaultKnowledgeBase();
        getOrCreateDefaultCollection();
        log.info("Default knowledge base and collection initialized successfully");
    }

    @PostConstruct
    public void onStartup() {
        initializeDefaults();
    }
}
