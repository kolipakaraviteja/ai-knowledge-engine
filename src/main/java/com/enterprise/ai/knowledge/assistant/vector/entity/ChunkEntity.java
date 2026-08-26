package com.enterprise.ai.knowledge.assistant.vector.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced chunk entity with full metadata tracking for production.
 */
public class ChunkEntity {
    private UUID id;
    private String documentName;
    private String documentId;
    private String documentHash;
    private String chunkHash;
    private Integer pageNumber;
    private Integer chunkIndex;
    private String content;
    private float[] embedding;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String language;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private String hash;
    private UUID collectionId;
    private UUID knowledgeBaseId;

    public ChunkEntity() {}

    public ChunkEntity(UUID id, String documentName, String documentId, String documentHash,
                      String chunkHash, Integer pageNumber, Integer chunkIndex, String content,
                      float[] embedding, String embeddingModel, Integer embeddingDimension,
                      String language, Integer version, Instant createdAt, Instant updatedAt, String hash, UUID collectionId, UUID knowledgeBaseId) {
        this.id = id;
        this.documentName = documentName;
        this.documentId = documentId;
        this.documentHash = documentHash;
        this.chunkHash = chunkHash;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.language = language;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.hash = hash;
        this.collectionId = collectionId;
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }

    public String getChunkHash() { return chunkHash; }
    public void setChunkHash(String chunkHash) { this.chunkHash = chunkHash; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public Integer getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(Integer embeddingDimension) { this.embeddingDimension = embeddingDimension; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public UUID getCollectionId() { return collectionId; }
    public void setCollectionId(UUID collectionId) { this.collectionId = collectionId; }

    public UUID getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(UUID knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
}
