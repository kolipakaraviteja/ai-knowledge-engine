package com.enterprise.ai.knowledge.assistant.evaluation.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class EvaluationTest {
    private UUID id;
    private String name;
    private String query;
    private List<String> expectedChunkIds;
    private Instant createdAt;
    
    // Enhanced fields for categorization and answer quality evaluation
    private String category; // FACTUAL, CONCEPTUAL, COMPARATIVE, NUMERICAL, MULTI_HOP
    private String language; // ENGLISH, TELUGU, MIXED
    private String difficulty; // EASY, MEDIUM, HARD
    private String documentScope; // single_doc, multi_doc, cross_chapter
    private String expectedAnswer;
    private List<String> keyPoints;
    private List<UUID> expectedDocuments;

    public EvaluationTest() {}

    public EvaluationTest(UUID id, String name, String query, List<String> expectedChunkIds, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.query = query;
        this.expectedChunkIds = expectedChunkIds;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<String> getExpectedChunkIds() { return expectedChunkIds; }
    public void setExpectedChunkIds(List<String> expectedChunkIds) { this.expectedChunkIds = expectedChunkIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDocumentScope() { return documentScope; }
    public void setDocumentScope(String documentScope) { this.documentScope = documentScope; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public List<String> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }

    public List<UUID> getExpectedDocuments() { return expectedDocuments; }
    public void setExpectedDocuments(List<UUID> expectedDocuments) { this.expectedDocuments = expectedDocuments; }
}
