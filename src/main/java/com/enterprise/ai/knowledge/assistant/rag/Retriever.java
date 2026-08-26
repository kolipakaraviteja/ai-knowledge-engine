package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.logging.RAGLogger;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import com.enterprise.ai.knowledge.assistant.vector.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retriever component for RAG (Retrieval-Augmented Generation) pipeline.
 * Responsible for finding relevant context chunks based on a query.
 */
@Slf4j
@Component
public class Retriever {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final MetaDataFilter metaDataFilter;
    private final ReRanker reRanker;
    private final RAGLogger ragLogger;
    private final PerformanceLogger performanceLogger;
    private static final int DEFAULT_K = 5; // Number of chunks to retrieve

    private final int defaultVectorTopK;
    private final int defaultFinalTopN;

    public Retriever(EmbeddingService embeddingService,
                     VectorStoreService vectorStoreService,
                     MetaDataFilter metaDataFilter,
                     ReRanker reRanker,
                     RAGLogger ragLogger,
                     PerformanceLogger performanceLogger,
                     @Value("${app.rag.vectorTopK:20}") int defaultVectorTopK,
                     @Value("${app.rag.finalTopN:3}") int defaultFinalTopN) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.metaDataFilter = metaDataFilter;
        this.reRanker = reRanker;
        this.ragLogger = ragLogger;
        this.performanceLogger = performanceLogger;
        this.defaultVectorTopK = defaultVectorTopK;
        this.defaultFinalTopN = defaultFinalTopN;
    }

    /**
     * Retrieve the top-K most relevant chunks for a given query.
     *
     * @param query The user's query text
     * @return List of SearchResult containing relevant chunks
     */
    public List<SearchResult> retrieve(String query) {
        return retrieve(query, DEFAULT_K);
    }

    /**
     * Retrieve the top-K most relevant chunks for a given query.
     *
     * @param query The user's query text
     * @param k     Number of chunks to retrieve
     * @return List of SearchResult containing relevant chunks
     */
    public List<SearchResult> retrieve(String query, int k) {
        ragLogger.logRetrievalStart(query, k, "vector");
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("vector_retrieval");
        
        try {
            // Generate embedding for the query
            EmbeddingResult embeddingResult = embeddingService.generateEmbedding(query);
            if (embeddingResult == null || embeddingResult.vector() == null) {
                performanceLogger.stopTiming(timing);
                ragLogger.logRetrievalComplete(query, 0, System.currentTimeMillis() - timing.getStartTime(), "vector");
                return List.of();
            }

            // Find nearest chunks in the vector store
            List<SearchResult> results = vectorStoreService.findNearest(embeddingResult.vector(), k);
            performanceLogger.stopTiming(timing);
            ragLogger.logRetrievalComplete(query, results.size(), System.currentTimeMillis() - timing.getStartTime(), "vector");
            
            return results;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            ragLogger.logRagError("vector_retrieval", query, e);
            // Best-effort: return empty list on error
            return List.of();
        }
    }

    /**
     * Two-stage retrieval: vector search -> metadata filter -> re-rank -> top N
     */
    public List<SearchResult> retrieveAndRerank(String query, Integer vectorTopK, Integer finalTopN) {
        int k = vectorTopK == null ? this.defaultVectorTopK : vectorTopK;
        int n = finalTopN == null ? this.defaultFinalTopN : finalTopN;

        ragLogger.logRetrievalStart(query, k, "vector_rerank");
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("vector_rerank_retrieval");

        try {
            EmbeddingResult embeddingResult = embeddingService.generateEmbedding(query);
            if (embeddingResult == null || embeddingResult.vector() == null) {
                performanceLogger.stopTiming(timing);
                ragLogger.logRetrievalComplete(query, 0, System.currentTimeMillis() - timing.getStartTime(), "vector_rerank");
                return List.of();
            }

            List<SearchResult> initial = vectorStoreService.findNearest(embeddingResult.vector(), k);

            // Apply metadata filter (default pass-through)
            PerformanceLogger.TimingContext filterTiming = performanceLogger.startTiming("metadata_filter");
            List<SearchResult> filtered = metaDataFilter.filter(initial, null);
            performanceLogger.stopTiming(filterTiming);
            ragLogger.logMetadataFilter(initial.size(), filtered.size(), "none");

            // Re-rank and return top N
            PerformanceLogger.TimingContext rerankTiming = performanceLogger.startTiming("reranking");
            List<SearchResult> finalResults = reRanker.rerank(filtered, query, n);
            performanceLogger.stopTiming(rerankTiming);
            ragLogger.logReranking(query, filtered.size(), finalResults.size(), System.currentTimeMillis() - rerankTiming.getStartTime());

            performanceLogger.stopTiming(timing);
            ragLogger.logRetrievalComplete(query, finalResults.size(), System.currentTimeMillis() - timing.getStartTime(), "vector_rerank");
            
            return finalResults;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            ragLogger.logRagError("vector_rerank_retrieval", query, e);
            ragLogger.logFallback(query, e.getMessage());
            
            // On any error, best-effort: fallback to simple vector search top-n
            try {
                EmbeddingResult embeddingResult = embeddingService.generateEmbedding(query);
                if (embeddingResult == null || embeddingResult.vector() == null) return List.of();
                return vectorStoreService.findNearest(embeddingResult.vector(), n);
            } catch (Exception ex) {
                ragLogger.logRagError("vector_fallback", query, ex);
                return List.of();
            }
        }
    }

    /**
     * Build context string from retrieved results for prompt injection.
     *
     * @param results List of SearchResult from retrieve
     * @return Formatted context string to include in the prompt
     */
    public String buildContext(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "";
        }

        return results.stream()
                .map(result -> formatSearchResult(result))
                .collect(Collectors.joining("\n\n"));
    }

    private String formatSearchResult(SearchResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Source: ").append(result.getDocumentName());
        if (result.getPageNumber() != null) {
            sb.append(" (Page ").append(result.getPageNumber()).append(")");
        }
        sb.append("\n");
        sb.append("Content: ").append(result.getContent());
        sb.append("\n");
        sb.append("Relevance Score: ").append(String.format("%.4f", result.getScore()));
        return sb.toString();
    }
}
