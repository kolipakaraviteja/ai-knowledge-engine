package com.enterprise.ai.knowledge.assistant.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Citation {

    private String documentName;
    private Integer pageNumber;
    private Integer chunkIndex;
    private Double relevanceScore;
    private String content;
    private String documentId;
    private String chunkHash;
    private String documentHash;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String language;
    private Integer version;
    private Instant updatedAt;
}
