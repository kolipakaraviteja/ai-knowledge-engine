package com.enterprise.ai.knowledge.assistant.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

public class DocumentVersion {
    private UUID id;
    private String documentId;
    private String documentName;
    private Integer versionNumber;
    private Integer chunkCount;
    private String embeddingModel;
    private Instant createdAt;
    private Boolean isActive;

    public DocumentVersion() {}

    public DocumentVersion(UUID id, String documentId, String documentName, Integer versionNumber, Integer chunkCount, String embeddingModel, Instant createdAt, Boolean isActive) {
        this.id = id;
        this.documentId = documentId;
        this.documentName = documentName;
        this.versionNumber = versionNumber;
        this.chunkCount = chunkCount;
        this.embeddingModel = embeddingModel;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
