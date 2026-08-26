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

    public KnowledgeBase createKnowledgeBase(String name, String description) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public Optional<KnowledgeBase> getKnowledgeBase(UUID id) {
        return knowledgeBaseRepository.findById(id);
    }

    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    public KnowledgeBase updateKnowledgeBase(UUID id, String name, String description) {
        Optional<KnowledgeBase> existing = knowledgeBaseRepository.findById(id);
        if (existing.isPresent()) {
            KnowledgeBase kb = existing.get();
            kb.setName(name);
            kb.setDescription(description);
            return knowledgeBaseRepository.save(kb);
        }
        throw new IllegalArgumentException("Knowledge base not found: " + id);
    }

    public void deleteKnowledgeBase(UUID id) {
        knowledgeBaseRepository.deleteById(id);
    }
}
