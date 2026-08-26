package com.enterprise.ai.knowledge.assistant.evaluation.entity;

import java.time.Instant;
import java.util.UUID;

public class EvaluationRun {
    private UUID id;
    private String name;
    private String description;
    private Instant startedAt;
    private Instant completedAt;
    private String status;

    public EvaluationRun() {}

    public EvaluationRun(UUID id, String name, String description, Instant startedAt, Instant completedAt, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
