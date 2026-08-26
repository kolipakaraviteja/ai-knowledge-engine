package com.enterprise.ai.knowledge.assistant.rag.compression;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ContextCompressor {

    @Value("${app.rag.enableContextCompression:false}")
    private boolean enableContextCompression;

    @Value("${app.rag.compressionSentenceCount:3}")
    private int sentenceCount;

    public String compressChunk(String chunk, String query) {
        if (!enableContextCompression || chunk == null || chunk.isEmpty()) {
            return chunk;
        }
        return compressChunk(chunk, query, sentenceCount);
    }

    public String compressChunk(String chunk, String query, int topSentences) {
        if (chunk == null || chunk.isEmpty()) {
            return chunk;
        }

        String[] sentences = chunk.split("(?<=[.!?])\\s+");
        if (sentences.length <= topSentences) {
            return chunk;
        }

        List<ScoredSentence> scored = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            String sent = sentences[i].trim();
            if (sent.isEmpty()) continue;

            double score = calculateRelevance(sent, query);
            scored.add(new ScoredSentence(sent, score, i));
        }

        if (scored.isEmpty()) {
            return chunk;
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSentence::score).reversed())
                .limit(topSentences)
                .sorted(Comparator.comparingInt(ScoredSentence::position))
                .map(ScoredSentence::sentence)
                .collect(Collectors.joining(" "));
    }

    private double calculateRelevance(String sentence, String query) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String sentenceLower = sentence.toLowerCase();

        long matches = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && sentenceLower.contains(term)) {
                matches++;
            }
        }

        return matches > 0 ? matches / (double) queryTerms.length : 0.0;
    }

    public boolean isEnabled() {
        return enableContextCompression;
    }

    private static class ScoredSentence {
        private final String sentence;
        private final double score;
        private final int position;

        ScoredSentence(String sentence, double score, int position) {
            this.sentence = sentence;
            this.score = score;
            this.position = position;
        }

        public String sentence() {
            return sentence;
        }

        public double score() {
            return score;
        }

        public int position() {
            return position;
        }
    }
}

