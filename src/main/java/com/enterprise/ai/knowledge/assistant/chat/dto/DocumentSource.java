package com.enterprise.ai.knowledge.assistant.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentSource {
    private String documentName;
    private String documentId;
    private List<Citation> citations;
    private Integer chunkCount;

}
