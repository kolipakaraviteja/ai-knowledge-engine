package com.enterprise.ai.knowledge.assistant.knowledge.service;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.KnowledgeBase;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.KnowledgeBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    public KnowledgeBase createKnowledgeBase(String name, String description, UUID ownerId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        knowledgeBase.setOwnerId(ownerId);
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public Optional<KnowledgeBase> getKnowledgeBase(UUID id, UUID userId) {
        Optional<KnowledgeBase> kb = knowledgeBaseRepository.findById(id);
        if (kb.isPresent()) {
            // Admin can access all, users only their own
            if (kb.get().getOwnerId() == null || kb.get().getOwnerId().equals(userId)) {
                return kb;
            }
        }
        return Optional.empty();
    }

    public List<KnowledgeBase> getAllKnowledgeBases(UUID userId) {
        return knowledgeBaseRepository.findByOwnerId(userId);
    }

    public KnowledgeBase updateKnowledgeBase(UUID id, String name, String description, UUID userId) {
        Optional<KnowledgeBase> existing = knowledgeBaseRepository.findById(id);
        if (existing.isPresent()) {
            KnowledgeBase kb = existing.get();
            // Check ownership
            if (kb.getOwnerId() != null && !kb.getOwnerId().equals(userId)) {
                throw new IllegalArgumentException("Access denied: Knowledge base belongs to another user");
            }
            kb.setName(name);
            kb.setDescription(description);
            return knowledgeBaseRepository.save(kb);
        }
        throw new IllegalArgumentException("Knowledge base not found: " + id);
    }

    public void deleteKnowledgeBase(UUID id, UUID userId) {
        Optional<KnowledgeBase> existing = knowledgeBaseRepository.findById(id);
        if (existing.isPresent()) {
            KnowledgeBase kb = existing.get();
            // Check ownership
            if (kb.getOwnerId() != null && !kb.getOwnerId().equals(userId)) {
                throw new IllegalArgumentException("Access denied: Knowledge base belongs to another user");
            }
            knowledgeBaseRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Knowledge base not found: " + id);
        }
    }
}
