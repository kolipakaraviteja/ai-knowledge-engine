package com.enterprise.ai.knowledge.assistant.rag.strategy;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Embedding-based re-ranking strategy.
 * Computes cosine similarity between query embedding and candidate embeddings.
 * Fast and cost-effective re-ranking approach.
 */
@Slf4j
@Component("embeddingReRanker")
public class EmbeddingReRanker implements ReRankStrategy {

    private final EmbeddingService embeddingService;

    public EmbeddingReRanker(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public List<SearchResult> rerank(List<SearchResult> candidates, String query, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try {
            // Generate embedding for the query
            EmbeddingResult queryEmb = embeddingService.generateEmbedding(query);
            if (queryEmb == null || queryEmb.vector() == null) {
                return candidates.stream().limit(topN).collect(Collectors.toList());
            }
            float[] qv = queryEmb.vector();

            // Score each candidate and collect results
            List<ScoredResult> scored = new ArrayList<>();
            for (SearchResult c : candidates) {
                try {
                    EmbeddingResult ce = embeddingService.generateEmbedding(c.getContent());

                    if (ce == null || ce.vector() == null) {
                        log.debug("Empty content skipped: {}", c.getContent());
                        continue;
                    }



                    double sim = cosineSimilarity(qv, ce.vector());
                    double combined = (sim + c.getScore()) / 2.0; // combine embedding similarity + stored score
                    scored.add(new ScoredResult(c, combined));
                    log.debug("Scored: {} → sim={}, combined={}", c.getContent(), sim, combined);
                } catch (Exception e) {
                    log.debug("Error scoring candidate: {}", c.getContent(), e);
                }
            }

            return scored.stream()
                    .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
                    .map(ScoredResult::result)
                    .limit(topN)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during embedding re-ranking for query: {}, candidates: {}", query, candidates.size(), e);
            // On error, return best-effort topN from original order
            return candidates.stream().limit(topN).collect(Collectors.toList());
        }
    }

    @Override
    public String getName() {
        return "embedding";
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static class ScoredResult {
        private final SearchResult result;
        private final double score;

        ScoredResult(SearchResult result, double score) {
            this.result = result;
            this.score = score;
        }

        public SearchResult result() { return result; }
        public double score() { return score; }
    }
}

