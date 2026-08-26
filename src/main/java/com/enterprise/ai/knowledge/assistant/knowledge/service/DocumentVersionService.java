package com.enterprise.ai.knowledge.assistant.knowledge.service;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.DocumentVersion;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.DocumentVersionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;

    public DocumentVersionService(DocumentVersionRepository documentVersionRepository) {
        this.documentVersionRepository = documentVersionRepository;
    }

    public DocumentVersion createDocumentVersion(String documentId, String documentName, Integer versionNumber, Integer chunkCount, String embeddingModel) {
        DocumentVersion documentVersion = new DocumentVersion();
        documentVersion.setDocumentId(documentId);
        documentVersion.setDocumentName(documentName);
        documentVersion.setVersionNumber(versionNumber);
        documentVersion.setChunkCount(chunkCount);
        documentVersion.setEmbeddingModel(embeddingModel);
        documentVersion.setIsActive(true);
        return documentVersionRepository.save(documentVersion);
    }

    public Optional<DocumentVersion> getDocumentVersion(UUID id) {
        return documentVersionRepository.findById(id);
    }

    public List<DocumentVersion> getDocumentVersions(String documentId) {
        return documentVersionRepository.findByDocumentId(documentId);
    }

    public Optional<DocumentVersion> getActiveDocumentVersion(String documentId) {
        return documentVersionRepository.findActiveVersion(documentId);
    }

    public void setActiveVersion(String documentId, Integer versionNumber) {
        documentVersionRepository.setActiveVersion(documentId, versionNumber);
    }

    public void deleteDocumentVersion(UUID id) {
        documentVersionRepository.deleteById(id);
    }

    public Integer getNextVersionNumber(String documentId) {
        List<DocumentVersion> versions = getDocumentVersions(documentId);
        if (versions.isEmpty()) {
            return 1;
        }
        return versions.stream()
                .mapToInt(DocumentVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;
    }
}
