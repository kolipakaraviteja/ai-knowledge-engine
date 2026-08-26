package com.enterprise.ai.knowledge.assistant.rag.template;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;

import java.util.List;

/**
 * Contract for pluggable prompt templates.
 * Allows different formatting/styling of prompts without changing the RAG pipeline.
 */
public interface PromptTemplate {

    /**
     * Render the system prompt.
     */
    String renderSystem(List<SearchResult> sources);

    /**
     * Render the user prompt with context.
     */
    String renderUser(String query, List<SearchResult> sources);

    /**
     * Get the name/identifier of this template.
     */
    String getName();
}

