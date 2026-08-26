package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.rag.strategy.ReRankStrategy;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReRanker orchestrator that uses pluggable re-ranking strategies.
 * Supports multiple strategies: embedding (default), llm, cross-encoder, etc.
 */
@Slf4j
@Component
public class ReRanker {

    private final Map<String, ReRankStrategy> strategies;
    private final String defaultStrategy;

    public ReRanker(List<ReRankStrategy> strategies,
                    @Value("${app.reranker.strategy:embedding}") String defaultStrategy) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(ReRankStrategy::getName, s -> s));
        this.defaultStrategy = defaultStrategy == null ? "embedding" : defaultStrategy.toLowerCase(Locale.ROOT);
    }

    /**
     * Rerank candidates using the default strategy.
     */
    public List<SearchResult> rerank(List<SearchResult> candidates, String query, int topN) {
        return rerank(candidates, query, topN, defaultStrategy);
    }

    /**
     * Rerank candidates using a specified strategy.
     */
    public List<SearchResult> rerank(List<SearchResult> candidates, String query, int topN, String strategyName) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String strategy = strategyName == null ? defaultStrategy : strategyName.toLowerCase(Locale.ROOT);
        ReRankStrategy ranker = strategies.get(strategy);

        if (ranker == null) {
            // Fallback to default if strategy not found
            ranker = strategies.get(defaultStrategy);
            if (ranker == null) {
                // Last resort: return original order, top N
                return candidates.stream().limit(topN).collect(Collectors.toList());
            }
        }

        try {
            return ranker.rerank(candidates, query, topN);
        } catch (Exception e) {
            // Safe fallback: return best-effort topN
            return candidates.stream().limit(topN).collect(Collectors.toList());
        }
    }

    /**
     * Get list of available strategies.
     */
    public List<String> getAvailableStrategies() {
        return List.copyOf(strategies.keySet());
    }
}
