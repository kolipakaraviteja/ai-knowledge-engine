package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.rag.strategy.ReRankStrategy;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReRanker strategy pattern.
 * Tests orchestrator and individual strategies.
 */
@ExtendWith(MockitoExtension.class)
public class ReRankerStrategyTest {

    @Mock
    private ReRankStrategy defaultStrategy;

    @Mock
    private ReRankStrategy alternativeStrategy;

    private ReRanker reRanker;

    @BeforeEach
    void setUp() {
        when(defaultStrategy.getName()).thenReturn("embedding");
        when(alternativeStrategy.getName()).thenReturn("llm");
        
        List<ReRankStrategy> strategies = List.of(defaultStrategy, alternativeStrategy);
        reRanker = new ReRanker(strategies, "embedding");
    }

    /**
     * Test ReRanker orchestrator uses default strategy.
     */
    @Test
    void testReRankerUsesDefaultStrategy() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.7, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult r2 = new SearchResult("Content 2", 0.8, 1, "Doc2.pdf", 0, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1, r2);
        List<SearchResult> reranked = List.of(r2, r1); // Reorder by score

        when(defaultStrategy.rerank(candidates, query, 2)).thenReturn(reranked);

        List<SearchResult> results = reRanker.rerank(candidates, query, 2);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(defaultStrategy, times(1)).rerank(candidates, query, 2);
    }

    /**
     * Test EmbeddingReRanker returns top N results.
     */
    @Test
    void testEmbeddingReRankerTopN() {
        String query = "test query";
        List<SearchResult> candidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            candidates.add(new SearchResult("Content " + i, 0.5 + i * 0.05, 1, "Doc.pdf", i, "doc-id-" + i, "hash-" + i, "chunk-hash-" + i, "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now()));
        }
        
        List<SearchResult> top3 = candidates.subList(7, 10);
        when(defaultStrategy.rerank(candidates, query, 3)).thenReturn(top3);

        List<SearchResult> results = reRanker.rerank(candidates, query, 3);

        assertNotNull(results);
        assertEquals(3, results.size());
        verify(defaultStrategy, times(1)).rerank(candidates, query, 3);
    }

    /**
     * Test ReRanker with empty candidates.
     */
    @Test
    void testReRankerWithEmptyCandidates() {
        String query = "test query";
        List<SearchResult> candidates = new ArrayList<>();

        List<SearchResult> results = reRanker.rerank(candidates, query, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(defaultStrategy, never()).rerank(any(), any(), anyInt());
    }

    /**
     * Test ReRanker with null candidates.
     */
    @Test
    void testReRankerWithNullCandidates() {
        String query = "test query";

        List<SearchResult> results = reRanker.rerank(null, query, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(defaultStrategy, never()).rerank(any(), any(), anyInt());
    }

    /**
     * Test ReRanker uses specified strategy.
     */
    @Test
    void testReRankerUsesSpecifiedStrategy() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.7, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1);

        when(alternativeStrategy.rerank(candidates, query, 1)).thenReturn(candidates);

        List<SearchResult> results = reRanker.rerank(candidates, query, 1, "llm");

        assertNotNull(results);
        verify(alternativeStrategy, times(1)).rerank(candidates, query, 1);
        verify(defaultStrategy, never()).rerank(any(), any(), anyInt());
    }

    /**
     * Test ReRanker lists available strategies.
     */
    @Test
    void testGetAvailableStrategies() {
        List<String> strategies = reRanker.getAvailableStrategies();

        assertNotNull(strategies);
        assertEquals(2, strategies.size());
        assertTrue(strategies.contains("embedding"));
        assertTrue(strategies.contains("llm"));
    }

    /**
     * Test ReRanker graceful fallback on strategy exception.
     */
    @Test
    void testReRankerFallbackOnException() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.8, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult r2 = new SearchResult("Content 2", 0.7, 1, "Doc2.pdf", 0, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1, r2);

        when(defaultStrategy.rerank(candidates, query, 2)).thenThrow(new RuntimeException("Strategy error"));

        List<SearchResult> results = reRanker.rerank(candidates, query, 2);

        assertNotNull(results);
        assertEquals(2, results.size()); // Should return top N as fallback
    }

    /**
     * Test ReRanker fallback to default when strategy not found.
     */
    @Test
    void testReRankerFallbackToDefaultStrategy() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.8, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1);

        when(defaultStrategy.rerank(candidates, query, 1)).thenReturn(candidates);

        List<SearchResult> results = reRanker.rerank(candidates, query, 1, "nonexistent");

        assertNotNull(results);
        verify(defaultStrategy, times(1)).rerank(candidates, query, 1);
    }

    /**
     * Test ReRanker with null strategy name uses default.
     */
    @Test
    void testReRankerWithNullStrategyName() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.8, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1);

        when(defaultStrategy.rerank(candidates, query, 1)).thenReturn(candidates);

        List<SearchResult> results = reRanker.rerank(candidates, query, 1, null);

        assertNotNull(results);
        verify(defaultStrategy, times(1)).rerank(candidates, query, 1);
    }

    /**
     * Test ReRanker case-insensitive strategy name.
     */
    @Test
    void testReRankerCaseInsensitiveStrategyName() {
        String query = "test query";
        SearchResult r1 = new SearchResult("Content 1", 0.8, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> candidates = List.of(r1);

        when(alternativeStrategy.rerank(candidates, query, 1)).thenReturn(candidates);

        List<SearchResult> results = reRanker.rerank(candidates, query, 1, "LLM");

        assertNotNull(results);
        verify(alternativeStrategy, times(1)).rerank(candidates, query, 1);
    }
}

