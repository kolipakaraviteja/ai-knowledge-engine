package com.enterprise.ai.knowledge.assistant.rag.retriever;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.logging.RAGLogger;
import com.enterprise.ai.knowledge.assistant.rag.MetaDataFilter;
import com.enterprise.ai.knowledge.assistant.rag.ReRanker;
import com.enterprise.ai.knowledge.assistant.rag.fusion.ReciprocalRankFusion;
import com.enterprise.ai.knowledge.assistant.rag.rewriter.QueryRewriter;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HybridRetriever {

    private final VectorRetriever vectorRetriever;
    private final KeywordRetriever keywordRetriever;
    private final ReciprocalRankFusion fusion;
    private final QueryRewriter queryRewriter;
    private final EmbeddingService embeddingService;
    private final MetaDataFilter metaDataFilter;
    private final ReRanker reRanker;
    private final RAGLogger ragLogger;
    private final PerformanceLogger performanceLogger;

    @Value("${app.rag.enableHybridSearch:false}")
    private boolean enableHybridSearch;

    @Value("20")
    private int vectorTopK;

    @Value("${app.rag.keywordTopK:20}")
    private int keywordTopK;

    @Value("${app.rag.finalTopN:5}")
    private int finalTopN;

    public HybridRetriever(VectorRetriever vectorRetriever,
                           KeywordRetriever keywordRetriever,
                           ReciprocalRankFusion fusion,
                           QueryRewriter queryRewriter, EmbeddingService embeddingService,  MetaDataFilter metaDataFilter, ReRanker reRanker, RAGLogger ragLogger, PerformanceLogger performanceLogger, @Value("${app.rag.vectorTopK:20}") int defaultVectorTopK,
                           @Value("${app.rag.finalTopN:3}") int defaultFinalTopN) {
        this.vectorRetriever = vectorRetriever;
        this.keywordRetriever = keywordRetriever;
        this.fusion = fusion;
        this.queryRewriter = queryRewriter;
        this.embeddingService = embeddingService;
        this.metaDataFilter = metaDataFilter;
        this.reRanker = reRanker;
        this.ragLogger = ragLogger;
        this.performanceLogger = performanceLogger;
        this.vectorTopK = defaultVectorTopK;
        this.keywordTopK = defaultVectorTopK;
        this.finalTopN = defaultFinalTopN;
    }

    public List<SearchResult> retrieve(String query, int topN) {
        return retrieve(query, topN, null, null, null);
    }

    public List<SearchResult> retrieve(String query, int topN, String conversationHistory) {
        return retrieve(query, topN, conversationHistory, null, null);
    }

    public List<SearchResult> retrieve(String query, int topN, String conversationHistory, String collectionId) {
        return retrieve(query, topN, conversationHistory, null, collectionId);
    }

    public List<SearchResult> retrieve(String query, int topN, String conversationHistory, String knowledgeBaseId, String collectionId) {
        ragLogger.logRetrievalStart(query, topN, "hybrid");
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("hybrid_retrieval");

        String finalQuery = query;
        if (queryRewriter.isEnabled() && conversationHistory != null && !conversationHistory.isEmpty()) {
            PerformanceLogger.TimingContext rewriteTiming = performanceLogger.startTiming("query_rewrite");
            finalQuery = queryRewriter.rewrite(query, conversationHistory);
            performanceLogger.stopTiming(rewriteTiming);
            ragLogger.logQueryRewrite(query, finalQuery, conversationHistory);
        }

        if (!enableHybridSearch) {
            performanceLogger.stopTiming(timing);
            ragLogger.logRetrievalComplete(finalQuery, 0, System.currentTimeMillis() - timing.getStartTime(), "hybrid_fallback");
            return vectorRetriever.retrieve(finalQuery, topN,
                    knowledgeBaseId != null ? java.util.UUID.fromString(knowledgeBaseId) : null,
                    collectionId != null ? java.util.UUID.fromString(collectionId) : null);
        }

        try {
            PerformanceLogger.TimingContext vectorTiming = performanceLogger.startTiming("vector_retrieval");
            List<SearchResult> vectorResults = vectorRetriever.retrieve(finalQuery, vectorTopK,
                    knowledgeBaseId != null ? java.util.UUID.fromString(knowledgeBaseId) : null,
                    collectionId != null ? java.util.UUID.fromString(collectionId) : null);
            performanceLogger.stopTiming(vectorTiming);

            PerformanceLogger.TimingContext keywordTiming = performanceLogger.startTiming("keyword_retrieval");
            List<SearchResult> keywordResults = keywordRetriever.retrieve(finalQuery, keywordTopK);
            performanceLogger.stopTiming(keywordTiming);

            PerformanceLogger.TimingContext fusionTiming = performanceLogger.startTiming("fusion");
            List<SearchResult> fused = fusion.fuse(vectorResults, keywordResults, topN);
            performanceLogger.stopTiming(fusionTiming);

            long retrievalTime = System.currentTimeMillis() - timing.getStartTime();
            performanceLogger.stopTiming(timing);
            ragLogger.logHybridRetrieval(finalQuery, vectorResults.size(), keywordResults.size(), fused.size(), retrievalTime);

            return fused;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            ragLogger.logRagError("hybrid_retrieval", query, e);
            ragLogger.logFallback(query, e.getMessage());
            return vectorRetriever.retrieve(finalQuery, topN,
                    knowledgeBaseId != null ? java.util.UUID.fromString(knowledgeBaseId) : null,
                    collectionId != null ? java.util.UUID.fromString(collectionId) : null);
        }
    }

    public boolean isEnabled() {
        return enableHybridSearch;
    }

    /**
     * Two-stage retrieval: vector search -> metadata filter -> re-rank -> top N
     */
    public List<SearchResult> retrieveAndRerank(String query, Integer vectorTopK, Integer finalTopN) {
        return retrieveAndRerank(query, vectorTopK, finalTopN, null, null);
    }

    /**
     * Two-stage retrieval with collection scoping: vector search -> metadata filter -> re-rank -> top N
     */
    public List<SearchResult> retrieveAndRerank(String query, Integer vectorTopK, Integer finalTopN, String collectionId) {
        return retrieveAndRerank(query, vectorTopK, finalTopN, null, collectionId);
    }

    /**
     * Two-stage retrieval with knowledge base and collection scoping: vector search -> metadata filter -> re-rank -> top N
     */
    public List<SearchResult> retrieveAndRerank(String query, Integer vectorTopK, Integer finalTopN, String knowledgeBaseId, String collectionId) {
        int k = vectorTopK == null ? this.vectorTopK : vectorTopK;
        int n =  finalTopN == null ? this.finalTopN : finalTopN;

        ragLogger.logRetrievalStart(query, k, "hybrid_rerank");
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("hybrid_rerank_retrieval");

        try {
            List<SearchResult> initial = retrieve(query, k, knowledgeBaseId, collectionId);

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
            ragLogger.logRetrievalComplete(query, finalResults.size(), System.currentTimeMillis() - timing.getStartTime(), "hybrid_rerank");

            return finalResults;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            ragLogger.logRagError("hybrid_rerank_retrieval", query, e);
            ragLogger.logFallback(query, e.getMessage());

            // On any error, best-effort: fallback to simple vector search top-n
            try {
                EmbeddingResult embeddingResult = embeddingService.generateEmbedding(query);
                if (embeddingResult == null || embeddingResult.vector() == null) return List.of();
                return vectorRetriever.retrieve(query, k,
                        knowledgeBaseId != null ? java.util.UUID.fromString(knowledgeBaseId) : null,
                        collectionId != null ? java.util.UUID.fromString(collectionId) : null);
            } catch (Exception ex) {
                ragLogger.logRagError("hybrid_fallback", query, ex);
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
