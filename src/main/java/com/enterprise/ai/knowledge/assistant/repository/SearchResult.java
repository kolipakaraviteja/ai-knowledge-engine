package com.enterprise.ai.knowledge.assistant.repository;

import java.time.Instant;

/**
 * DTO returned by nearest-neighbor searches with enriched metadata.
 */
public class SearchResult {
    private final String content;
    private final double score;
    private final Integer pageNumber;
    private final String documentName;
    private final Integer chunkIndex;
    private final String documentId;
    private final String documentHash;
    private final String chunkHash;
    private final String embeddingModel;
    private final Integer embeddingDimension;
    private final String language;
    private final Integer version;
    private final Instant updatedAt;


    public SearchResult(String content, double score, Integer pageNumber, String documentName, Integer chunkIndex,
                        String documentId, String documentHash, String chunkHash,
                        String embeddingModel, Integer embeddingDimension, String language,
                        Integer version, Instant updatedAt) {
        this.content = content;
        this.score = score;
        this.pageNumber = pageNumber;
        this.documentName = documentName;
        this.chunkIndex = chunkIndex;
        this.documentId = documentId;
        this.documentHash = documentHash;
        this.chunkHash = chunkHash;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.language = language;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public String getContent() {
        return content;
    }

    public double getScore() {
        return score;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getDocumentName() {
        return documentName;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

