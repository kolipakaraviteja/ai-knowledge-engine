package com.enterprise.ai.knowledge.assistant.knowledge.service;

import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.DocumentMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DocumentMetadataService {

    private final DocumentMetadataRepository documentMetadataRepository;

    public DocumentMetadataService(DocumentMetadataRepository documentMetadataRepository) {
        this.documentMetadataRepository = documentMetadataRepository;
    }

    public DocumentMetadata save(DocumentMetadata metadata) {
        log.info("Saving document metadata for document: {}", metadata.documentName());
        return documentMetadataRepository.save(metadata);
    }

    public Optional<DocumentMetadata> findByDocumentId(String documentId) {
        log.debug("Finding document metadata by documentId: {}", documentId);
        return documentMetadataRepository.findByDocumentId(documentId);
    }

    public Optional<DocumentMetadata> findByDocumentHash(String documentHash) {
        log.debug("Finding document metadata by documentHash: {}", documentHash);
        return documentMetadataRepository.findByDocumentHash(documentHash);
    }

    public List<DocumentMetadata> findAll() {
        log.debug("Finding all document metadata");
        return documentMetadataRepository.findAll();
    }

    public void updateChunkCount(String documentId, int chunkCount) {
        log.info("Updating chunk count to {} for documentId: {}", chunkCount, documentId);
        documentMetadataRepository.updateChunkCount(documentId, chunkCount);
    }

    public void markAsIndexed(String documentId) {
        log.info("Marking document as indexed for documentId: {}", documentId);
        documentMetadataRepository.updateIndexedAt(documentId, Instant.now());
    }

    public void deleteByDocumentId(String documentId) {
        log.info("Deleting document metadata for documentId: {}", documentId);
        documentMetadataRepository.deleteByDocumentId(documentId);
    }

    public boolean existsByDocumentId(String documentId) {
        return documentMetadataRepository.existsByDocumentId(documentId);
    }

    public boolean existsByDocumentHash(String documentHash) {
        return documentMetadataRepository.existsByDocumentHash(documentHash);
    }

    public DocumentMetadata createOrUpdate(DocumentMetadata metadata) {
        if (metadata.documentId() != null && existsByDocumentId(metadata.documentId())) {
            log.info("Updating existing document metadata for documentId: {}", metadata.documentId());
            return save(metadata);
        }
        log.info("Creating new document metadata for document: {}", metadata.documentName());
        return save(metadata);
    }
}
