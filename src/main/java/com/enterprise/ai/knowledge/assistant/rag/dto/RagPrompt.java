package com.enterprise.ai.knowledge.assistant.rag.dto;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * First-class RAG prompt object containing system prompt, user prompt, sources, and metadata.
 * Makes prompts observable, testable, and evolvable.
 */
public record RagPrompt(
        String systemPrompt,
        String userPrompt,
        List<SearchResult> sources,
        Map<String, Object> metadata
) {
    /**
     * Convenience method to get the full prompt (system + user).
     */
    public String getFullPrompt() {
        return systemPrompt + "\n\n" + userPrompt;
    }

    /**
     * Metadata helpers for observability.
     */
    public int getSourceCount() {
        return sources == null ? 0 : sources.size();
    }

    public double getAverageRelevanceScore() {
        if (sources == null || sources.isEmpty()) {
            return 0.0;
        }
        return sources.stream()
                .mapToDouble(SearchResult::getScore)
                .average()
                .orElse(0.0);
    }
}

