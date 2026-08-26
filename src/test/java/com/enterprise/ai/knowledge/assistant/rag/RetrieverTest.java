package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.logging.RAGLogger;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import com.enterprise.ai.knowledge.assistant.vector.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Retriever component.
 * Tests the retrieval logic and context building.
 */
@ExtendWith(MockitoExtension.class)
public class RetrieverTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private MetaDataFilter metaDataFilter;

    @Mock
    private ReRanker reRanker;

    @Mock
    private RAGLogger ragLogger;

    @Mock
    private PerformanceLogger performanceLogger;

    private Retriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new Retriever(embeddingService, vectorStoreService, metaDataFilter, reRanker, ragLogger, performanceLogger, 20, 3);
    }

    /**
     * Test retrieve with default K (5).
     */
    @Test
    void testRetrieveWithDefaultK() {
        String query = "What is the vacation policy?";
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, 5)).thenReturn(new ArrayList<>());

        List<SearchResult> results = retriever.retrieve(query);

        assertNotNull(results);
        verify(embeddingService, times(1)).generateEmbedding(query);
        verify(vectorStoreService, times(1)).findNearest(testVector, 5);
    }

    /**
     * Test retrieve with custom K.
     */
    @Test
    void testRetrieveWithCustomK() {
        String query = "What is the vacation policy?";
        int customK = 10;
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, customK)).thenReturn(new ArrayList<>());

        List<SearchResult> results = retriever.retrieve(query, customK);

        assertNotNull(results);
        verify(vectorStoreService, times(1)).findNearest(testVector, customK);
    }

    /**
     * Test retrieve with null embedding result (graceful degradation).
     */
    @Test
    void testRetrieveWithNullEmbedding() {
        String query = "What is the vacation policy?";

        when(embeddingService.generateEmbedding(query)).thenReturn(null);

        List<SearchResult> results = retriever.retrieve(query);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test retrieve with null vector in embedding result.
     */
    @Test
    void testRetrieveWithNullVector() {
        String query = "What is the vacation policy?";
        EmbeddingResult embeddingResult = new EmbeddingResult(null, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);

        List<SearchResult> results = retriever.retrieve(query);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test retrieve with empty search results.
     */
    @Test
    void testRetrieveWithEmptyResults() {
        String query = "Obscure question that returns no results";
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, 5)).thenReturn(new ArrayList<>());

        List<SearchResult> results = retriever.retrieve(query);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test retrieve with exception from vector store (graceful degradation).
     */
    @Test
    void testRetrieveWithVectorStoreException() {
        String query = "What is the vacation policy?";
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, 5)).thenThrow(new RuntimeException("Vector store error"));

        List<SearchResult> results = retriever.retrieve(query);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test retrieveAndRerank with successful pipeline.
     */
    @Test
    void testRetrieveAndRerankSuccess() {
        String query = "Test query";
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");
        
        SearchResult result = new SearchResult("Content", 0.9, 1, "Doc.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> initialResults = List.of(result);
        List<SearchResult> filteredResults = List.of(result);
        List<SearchResult> rerankedResults = List.of(result);

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, 20)).thenReturn(initialResults);
        when(metaDataFilter.filter(initialResults, null)).thenReturn(filteredResults);
        when(reRanker.rerank(filteredResults, query, 3)).thenReturn(rerankedResults);

        List<SearchResult> results = retriever.retrieveAndRerank(query, 20, 3);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(reRanker, times(1)).rerank(filteredResults, query, 3);
    }

    /**
     * Test retrieveAndRerank with fallback on exception.
     */
    @Test
    void testRetrieveAndRerankFallback() {
        String query = "Test query";
        float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
        EmbeddingResult embeddingResult = new EmbeddingResult(testVector, 1536, "test-model");

        when(embeddingService.generateEmbedding(query)).thenReturn(embeddingResult);
        when(vectorStoreService.findNearest(testVector, 20)).thenThrow(new RuntimeException("Error"));
        when(vectorStoreService.findNearest(testVector, 3)).thenReturn(new ArrayList<>());

        List<SearchResult> results = retriever.retrieveAndRerank(query, 20, 3);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test buildContext with results.
     */
    @Test
    void testBuildContextWithResults() {
        SearchResult result1 = new SearchResult(
                "Employees receive 20 days of PTO.",
                0.95,
                2,
                "EmployeeHandbook.pdf",
                0,
                "doc-id-1",
                "hash-1",
                "chunk-hash-1",
                "text-embedding-3-small",
                1536,
                "en",
                1,
                java.time.Instant.now()
        );
        SearchResult result2 = new SearchResult(
                "Additional unpaid leave available upon request.",
                0.85,
                3,
                "EmployeeHandbook.pdf",
                1,
                "doc-id-1",
                "hash-1",
                "chunk-hash-2",
                "text-embedding-3-small",
                1536,
                "en",
                1,
                java.time.Instant.now()
        );
        List<SearchResult> results = List.of(result1, result2);

        String context = retriever.buildContext(results);

        assertNotNull(context);
        assertTrue(context.contains("EmployeeHandbook.pdf"));
        assertTrue(context.contains("Employees receive 20 days of PTO"));
        assertTrue(context.contains("Additional unpaid leave available"));
        assertTrue(context.contains("Relevance Score"));
    }

    /**
     * Test buildContext with empty results.
     */
    @Test
    void testBuildContextWithEmptyResults() {
        List<SearchResult> results = new ArrayList<>();

        String context = retriever.buildContext(results);

        assertNotNull(context);
        assertTrue(context.isEmpty());
    }

    /**
     * Test buildContext formats multiple documents correctly.
     */
    @Test
    void testBuildContextFormatting() {
        SearchResult result1 = new SearchResult("Content 1", 0.9, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult result2 = new SearchResult("Content 2", 0.8, null, "Doc2.pdf", null, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result1, result2);

        String context = retriever.buildContext(results);

        assertNotNull(context);
        assertTrue(context.contains("Doc1.pdf"));
        assertTrue(context.contains("Doc2.pdf"));
        assertTrue(context.contains("Content 1"));
        assertTrue(context.contains("Content 2"));
        assertFalse(context.contains("(Page null)"));
    }

    /**
     * Test buildContext handles null page numbers and chunk indices.
     */
    @Test
    void testBuildContextHandlesNullValues() {
        SearchResult result = new SearchResult("Content", 0.9, null, "Doc.pdf", null, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result);

        String context = retriever.buildContext(results);

        assertNotNull(context);
        assertTrue(context.contains("Doc.pdf"));
        assertFalse(context.contains("(Page null)"));
        assertTrue(context.contains("Content"));
    }
}
