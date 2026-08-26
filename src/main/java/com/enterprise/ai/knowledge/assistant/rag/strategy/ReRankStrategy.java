package com.enterprise.ai.knowledge.assistant.rag.strategy;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;

import java.util.List;

/**
 * Contract for pluggable re-ranking strategies.
 * Allows different re-ranking algorithms without changing the core pipeline.
 */
public interface ReRankStrategy {

    /**
     * Re-rank candidates and return top N results.
     *
     * @param candidates The candidate search results to re-rank
     * @param query The user's original query
     * @param topN Number of results to return after re-ranking
     * @return Re-ranked results, limited to top N
     */
    List<SearchResult> rerank(List<SearchResult> candidates, String query, int topN);

    /**
     * Get the name/identifier of this strategy.
     */
    String getName();
}

