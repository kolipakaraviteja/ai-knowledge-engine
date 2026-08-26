package com.enterprise.ai.knowledge.assistant.knowledge.service;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.Collection;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.CollectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    public Collection createCollection(UUID knowledgeBaseId, String name, String description) {
        Collection collection = new Collection();
        collection.setKnowledgeBaseId(knowledgeBaseId);
        collection.setName(name);
        collection.setDescription(description);
        return collectionRepository.save(collection);
    }

    public Optional<Collection> getCollection(UUID id) {
        return collectionRepository.findById(id);
    }

    public List<Collection> getCollectionsByKnowledgeBase(UUID knowledgeBaseId) {
        return collectionRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }

    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }

    public Collection updateCollection(UUID id, String name, String description) {
        Optional<Collection> existing = collectionRepository.findById(id);
        if (existing.isPresent()) {
            Collection coll = existing.get();
            coll.setName(name);
            coll.setDescription(description);
            return collectionRepository.save(coll);
        }
        throw new IllegalArgumentException("Collection not found: " + id);
    }

    public void deleteCollection(UUID id) {
        collectionRepository.deleteById(id);
    }
}
