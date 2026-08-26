package com.enterprise.ai.knowledge.assistant.rag.fusion;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReciprocalRankFusion {

    @Value("${app.rag.rrfK:60}")
    private int k;

    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> keywordResults,
                                   int topN) {
        Map<String, RankedResult> scoreMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult r = vectorResults.get(i);
            String key = generateKey(r);
            double score = 1.0 / (k + i + 1);
            scoreMap.computeIfAbsent(key, k2 -> new RankedResult(r))
                    .addVectorScore(score);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult r = keywordResults.get(i);
            String key = generateKey(r);
            double score = 1.0 / (k + i + 1);
            scoreMap.computeIfAbsent(key, k2 -> new RankedResult(r))
                    .addKeywordScore(score);
        }

        return scoreMap.values().stream()
                .sorted((a, b) -> Double.compare(b.totalScore(), a.totalScore()))
                .limit(topN)
                .map(RankedResult::result)
                .collect(Collectors.toList());
    }

    private String generateKey(SearchResult r) {
        if (r.getChunkHash() != null) {
            return r.getChunkHash();
        }
        return r.getContent().hashCode() + "_" + r.getDocumentName();
    }

    private static class RankedResult {
        private final SearchResult result;
        private double vectorScore = 0;
        private double keywordScore = 0;

        RankedResult(SearchResult result) {
            this.result = result;
        }

        void addVectorScore(double s) {
            this.vectorScore = s;
        }

        void addKeywordScore(double s) {
            this.keywordScore = s;
        }

        double totalScore() {
            return vectorScore + keywordScore;
        }

        SearchResult result() {
            return result;
        }
    }
}

