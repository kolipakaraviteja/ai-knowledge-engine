package com.enterprise.ai.knowledge.assistant.evaluation.service;

import java.util.List;
import java.util.Map;

public class EvaluationMetrics {

    /**
     * Calculate Recall@K - percentage of expected chunks found in top K results
     */
    public static double calculateRecallAtK(List<String> retrievedChunkIds, List<String> expectedChunkIds, int k) {
        if (expectedChunkIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKRetrieved = retrievedChunkIds.stream()
                .limit(k)
                .toList();

        long found = expectedChunkIds.stream()
                .filter(topKRetrieved::contains)
                .count();

        return (double) found / expectedChunkIds.size();
    }

    /**
     * Calculate Precision@K - percentage of retrieved chunks that are relevant
     */
    public static double calculatePrecisionAtK(List<String> retrievedChunkIds, List<String> expectedChunkIds, int k) {
        if (k == 0) {
            return 0.0;
        }

        List<String> topKRetrieved = retrievedChunkIds.stream()
                .limit(k)
                .toList();

        if (topKRetrieved.isEmpty()) {
            return 0.0;
        }

        long relevant = topKRetrieved.stream()
                .filter(expectedChunkIds::contains)
                .count();

        return (double) relevant / k;
    }

    /**
     * Calculate MRR (Mean Reciprocal Rank) - average reciprocal rank of first relevant result
     */
    public static double calculateMRR(List<String> retrievedChunkIds, List<String> expectedChunkIds) {
        if (expectedChunkIds.isEmpty() || retrievedChunkIds.isEmpty()) {
            return 0.0;
        }

        for (int i = 0; i < retrievedChunkIds.size(); i++) {
            if (expectedChunkIds.contains(retrievedChunkIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }

        return 0.0;
    }

    /**
     * Calculate NDCG@K (Normalized Discounted Cumulative Gain)
     * Measures ranking quality with relevance graded by position
     */
    public static double calculateNDCGAtK(List<String> retrievedChunkIds, List<String> expectedChunkIds, int k) {
        if (expectedChunkIds.isEmpty() || retrievedChunkIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKRetrieved = retrievedChunkIds.stream()
                .limit(k)
                .toList();

        double dcg = 0.0;
        for (int i = 0; i < topKRetrieved.size(); i++) {
            double relevance = expectedChunkIds.contains(topKRetrieved.get(i)) ? 1.0 : 0.0;
            dcg += relevance / (Math.log(i + 2) / Math.log(2)); // log2(i+1+1)
        }

        // Ideal DCG: all relevant results at top positions
        double idcg = 0.0;
        for (int i = 0; i < Math.min(k, expectedChunkIds.size()); i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }

        return idcg == 0 ? 0.0 : dcg / idcg;
    }

    /**
     * Calculate MAP (Mean Average Precision)
     * Mean of precision scores at each relevant result position
     */
    public static double calculateMAP(List<String> retrievedChunkIds, List<String> expectedChunkIds) {
        if (expectedChunkIds.isEmpty() || retrievedChunkIds.isEmpty()) {
            return 0.0;
        }

        double sumPrecision = 0.0;
        int relevantCount = 0;

        for (int i = 0; i < retrievedChunkIds.size(); i++) {
            if (expectedChunkIds.contains(retrievedChunkIds.get(i))) {
                relevantCount++;
                sumPrecision += (double) relevantCount / (i + 1);
            }
        }

        return relevantCount == 0 ? 0.0 : sumPrecision / expectedChunkIds.size();
    }

    /**
     * Calculate Hit Rate@K - binary metric: 1 if any relevant result in top K, else 0
     */
    public static double calculateHitRateAtK(List<String> retrievedChunkIds, List<String> expectedChunkIds, int k) {
        if (expectedChunkIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKRetrieved = retrievedChunkIds.stream()
                .limit(k)
                .toList();

        boolean hasHit = topKRetrieved.stream()
                .anyMatch(expectedChunkIds::contains);

        return hasHit ? 1.0 : 0.0;
    }

    /**
     * Calculate Coverage - percentage of expected documents that have at least one relevant chunk
     */
    public static double calculateCoverage(List<String> retrievedChunkIds, List<String> expectedChunkIds) {
        if (expectedChunkIds.isEmpty()) {
            return 0.0;
        }

        boolean hasAnyRelevant = retrievedChunkIds.stream()
                .anyMatch(expectedChunkIds::contains);

        return hasAnyRelevant ? 1.0 : 0.0;
    }

    /**
     * Calculate all basic metrics for a single test
     */
    public static Map<String, Object> calculateAllMetrics(List<String> retrievedChunkIds, List<String> expectedChunkIds, long latencyMs) {
        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("recall_at_1", calculateRecallAtK(retrievedChunkIds, expectedChunkIds, 1));
        metrics.put("recall_at_5", calculateRecallAtK(retrievedChunkIds, expectedChunkIds, 5));
        metrics.put("recall_at_10", calculateRecallAtK(retrievedChunkIds, expectedChunkIds, 10));
        metrics.put("precision_at_1", calculatePrecisionAtK(retrievedChunkIds, expectedChunkIds, 1));
        metrics.put("precision_at_5", calculatePrecisionAtK(retrievedChunkIds, expectedChunkIds, 5));
        metrics.put("precision_at_10", calculatePrecisionAtK(retrievedChunkIds, expectedChunkIds, 10));
        metrics.put("mrr", calculateMRR(retrievedChunkIds, expectedChunkIds));
        metrics.put("ndcg_at_5", calculateNDCGAtK(retrievedChunkIds, expectedChunkIds, 5));
        metrics.put("ndcg_at_10", calculateNDCGAtK(retrievedChunkIds, expectedChunkIds, 10));
        metrics.put("map", calculateMAP(retrievedChunkIds, expectedChunkIds));
        metrics.put("hit_rate_at_5", calculateHitRateAtK(retrievedChunkIds, expectedChunkIds, 5));
        metrics.put("hit_rate_at_10", calculateHitRateAtK(retrievedChunkIds, expectedChunkIds, 10));
        metrics.put("coverage", calculateCoverage(retrievedChunkIds, expectedChunkIds));
        metrics.put("latency_ms", latencyMs);
        metrics.put("retrieved_count", retrievedChunkIds.size());
        metrics.put("expected_count", expectedChunkIds.size());

        return metrics;
    }

    /**
     * Calculate average metrics across multiple test results
     */
    public static Map<String, Object> calculateAverageMetrics(List<Map<String, Object>> metricsList) {
        if (metricsList.isEmpty()) {
            return Map.of();
        }

        double avgRecall1 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("recall_at_1", 0.0))
                .average()
                .orElse(0.0);

        double avgRecall5 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("recall_at_5", 0.0))
                .average()
                .orElse(0.0);

        double avgRecall10 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("recall_at_10", 0.0))
                .average()
                .orElse(0.0);

        double avgPrecision1 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("precision_at_1", 0.0))
                .average()
                .orElse(0.0);

        double avgPrecision5 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("precision_at_5", 0.0))
                .average()
                .orElse(0.0);

        double avgPrecision10 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("precision_at_10", 0.0))
                .average()
                .orElse(0.0);

        double avgMRR = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("mrr", 0.0))
                .average()
                .orElse(0.0);

        double avgNDCG5 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("ndcg_at_5", 0.0))
                .average()
                .orElse(0.0);

        double avgNDCG10 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("ndcg_at_10", 0.0))
                .average()
                .orElse(0.0);

        double avgMAP = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("map", 0.0))
                .average()
                .orElse(0.0);

        double avgHitRate5 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("hit_rate_at_5", 0.0))
                .average()
                .orElse(0.0);

        double avgHitRate10 = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("hit_rate_at_10", 0.0))
                .average()
                .orElse(0.0);

        double avgCoverage = metricsList.stream()
                .mapToDouble(m -> (Double) m.getOrDefault("coverage", 0.0))
                .average()
                .orElse(0.0);

        double avgLatency = metricsList.stream()
                .mapToLong(m -> (Long) m.getOrDefault("latency_ms", 0L))
                .average()
                .orElse(0.0);

        Map<String, Object> avgMetrics = new java.util.HashMap<>();
        avgMetrics.put("avg_recall_at_1", avgRecall1);
        avgMetrics.put("avg_recall_at_5", avgRecall5);
        avgMetrics.put("avg_recall_at_10", avgRecall10);
        avgMetrics.put("avg_precision_at_1", avgPrecision1);
        avgMetrics.put("avg_precision_at_5", avgPrecision5);
        avgMetrics.put("avg_precision_at_10", avgPrecision10);
        avgMetrics.put("avg_mrr", avgMRR);
        avgMetrics.put("avg_ndcg_at_5", avgNDCG5);
        avgMetrics.put("avg_ndcg_at_10", avgNDCG10);
        avgMetrics.put("avg_map", avgMAP);
        avgMetrics.put("avg_hit_rate_at_5", avgHitRate5);
        avgMetrics.put("avg_hit_rate_at_10", avgHitRate10);
        avgMetrics.put("avg_coverage", avgCoverage);
        avgMetrics.put("avg_latency_ms", avgLatency);
        avgMetrics.put("total_tests", metricsList.size());

        return avgMetrics;
    }
}
