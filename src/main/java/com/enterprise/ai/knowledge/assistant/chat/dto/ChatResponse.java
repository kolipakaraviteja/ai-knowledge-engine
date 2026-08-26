package com.enterprise.ai.knowledge.assistant.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for chat endpoints with enriched RAG metadata.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    @jakarta.validation.constraints.NotBlank(message = "Answer cannot be blank")
    private String answer;
    
    private Boolean isFromContext;
    
    @jakarta.validation.constraints.Min(value = 0, message = "Retrieval count cannot be negative")
    private Integer retrievalCount;
    
    private List<DocumentSource> sourceDocuments;
}
