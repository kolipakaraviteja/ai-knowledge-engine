package com.enterprise.ai.knowledge.assistant.evaluation.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EvaluationResult {
    private UUID id;
    private UUID testId;
    private UUID runId;
    private String query;
    private String expectedAnswer;
    private List<String> retrievedChunkIds;
    private Map<String, Object> metrics;
    private Long latencyMs;
    private Instant createdAt;

    public EvaluationResult() {}

    public EvaluationResult(UUID id, UUID testId, UUID runId, List<String> retrievedChunkIds, Map<String, Object> metrics, Long latencyMs, Instant createdAt) {
        this.id = id;
        this.testId = testId;
        this.runId = runId;
        this.retrievedChunkIds = retrievedChunkIds;
        this.metrics = metrics;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTestId() { return testId; }
    public void setTestId(UUID testId) { this.testId = testId; }

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public List<String> getRetrievedChunkIds() { return retrievedChunkIds; }
    public void setRetrievedChunkIds(List<String> retrievedChunkIds) { this.retrievedChunkIds = retrievedChunkIds; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
