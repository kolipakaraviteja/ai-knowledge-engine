package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.rag.compression.ContextCompressor;
import com.enterprise.ai.knowledge.assistant.rag.dto.RagPrompt;
import com.enterprise.ai.knowledge.assistant.rag.template.PromptTemplate;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for PromptBuilder component.
 * Tests prompt building with context injection and RAG prompt objects.
 */
@ExtendWith(MockitoExtension.class)
public class PromptBuilderTest {

    @Mock
    private PromptTemplate defaultTemplate;

    @Mock
    private ContextCompressor contextCompressor;

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        lenient().when(defaultTemplate.getName()).thenReturn("default");
        lenient().when(defaultTemplate.renderSystem(any())).thenReturn("System prompt");
        lenient().when(defaultTemplate.renderUser(any(), anyList())).thenReturn("User prompt");
        lenient().when(contextCompressor.isEnabled()).thenReturn(false);
        
        promptBuilder = new PromptBuilder(defaultTemplate, contextCompressor);
    }

    /**
     * Test system prompt returns consistent content.
     */
    @Test
    void testGetSystemPrompt() {
        when(defaultTemplate.renderSystem(null)).thenReturn("Test system prompt");
        
        String systemPrompt = promptBuilder.getSystemPrompt();
        
        assertNotNull(systemPrompt);
        assertEquals("Test system prompt", systemPrompt);
    }

    /**
     * Test buildRagPrompt returns RagPrompt record with system and user prompts.
     */
    @Test
    void testBuildRagPromptReturnsRagPrompt() {
        String query = "What is the vacation policy?";
        SearchResult result1 = new SearchResult(
                "Employees receive 20 days of paid time off annually.",
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
        List<SearchResult> results = List.of(result1);

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.systemPrompt());
        assertNotNull(ragPrompt.userPrompt());
        assertEquals("System prompt", ragPrompt.systemPrompt());
        assertTrue(ragPrompt.userPrompt().contains(query));
        assertEquals(1, ragPrompt.metadata().get("sourceCount"));
    }

    /**
     * Test buildRagPrompt with empty results.
     */
    @Test
    void testBuildRagPromptWithEmptyResults() {
        String query = "What is the vacation policy?";
        List<SearchResult> results = new ArrayList<>();

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.systemPrompt());
        assertNotNull(ragPrompt.userPrompt());
        assertEquals(0, ragPrompt.metadata().get("sourceCount"));
    }

    /**
     * Test buildRagPrompt with null results.
     */
    @Test
    void testBuildRagPromptWithNullResults() {
        String query = "What is the vacation policy?";

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, null);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.systemPrompt());
        assertNotNull(ragPrompt.userPrompt());
        assertEquals(0, ragPrompt.metadata().get("sourceCount"));
    }

    /**
     * Test buildRagPrompt with SearchResult list includes context.
     */
    @Test
    void testBuildRagPromptWithResults() {
        String query = "What is the vacation policy?";
        SearchResult result1 = new SearchResult(
                "Employees receive 20 days of paid time off annually.",
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

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.systemPrompt());
        assertNotNull(ragPrompt.userPrompt());
        assertEquals(2, ragPrompt.metadata().get("sourceCount"));
        assertTrue(ragPrompt.userPrompt().contains(query));
    }

    /**
     * Test buildRagPrompt metadata contains source count.
     */
    @Test
    void testBuildRagPromptMetadata() {
        String query = "What is the vacation policy?";
        SearchResult result1 = new SearchResult("Content 1", 0.95, 2, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult result2 = new SearchResult("Content 2", 0.85, 3, "Doc2.pdf", 1, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result1, result2);

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt.metadata());
        assertEquals(2, ragPrompt.metadata().get("sourceCount"));
        assertEquals("default", ragPrompt.metadata().get("templateName"));
        assertTrue(ragPrompt.metadata().containsKey("averageRelevanceScore"));
    }

    /**
     * Test buildRagPrompt calculates average relevance score.
     */
    @Test
    void testBuildRagPromptAverageRelevanceScore() {
        String query = "Test query";
        SearchResult result1 = new SearchResult("Content 1", 0.9, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult result2 = new SearchResult("Content 2", 0.8, 2, "Doc2.pdf", 1, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result1, result2);

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt.metadata());
        assertTrue(ragPrompt.metadata().containsKey("averageRelevanceScore"));
        double avgScore = (double) ragPrompt.metadata().get("averageRelevanceScore");
        assertEquals(0.85, avgScore, 0.01); // (0.9 + 0.8) / 2 = 0.85
    }

    /**
     * Test buildRagPrompt includes template name in metadata.
     */
    @Test
    void testBuildRagPromptTemplateMetadata() {
        String query = "Test query";
        SearchResult result = new SearchResult("Content", 0.9, 1, "Doc.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result);

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt.metadata());
        assertTrue(ragPrompt.metadata().containsKey("templateName"));
        assertEquals("default", ragPrompt.metadata().get("templateName"));
    }

    /**
     * Test buildRagPromptWithHistory includes conversation history.
     */
    @Test
    void testBuildRagPromptWithHistory() {
        String query = "Test query";
        String history = "User: Previous question\nAssistant: Previous answer";
        SearchResult result = new SearchResult("Content", 0.9, 1, "Doc.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result);

        RagPrompt ragPrompt = promptBuilder.buildRagPromptWithHistory(query, results, history);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.userPrompt());
        assertTrue(ragPrompt.userPrompt().contains("CONVERSATION HISTORY"));
        assertTrue(ragPrompt.userPrompt().contains(history));
        assertTrue(ragPrompt.metadata().get("hasConversationHistory").equals(true));
    }

    /**
     * Test buildRagPromptWithHistory with null history.
     */
    @Test
    void testBuildRagPromptWithHistoryNull() {
        String query = "Test query";
        SearchResult result = new SearchResult("Content", 0.9, 1, "Doc.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result);

        RagPrompt ragPrompt = promptBuilder.buildRagPromptWithHistory(query, results, null);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.userPrompt());
        assertFalse(ragPrompt.userPrompt().contains("CONVERSATION HISTORY"));
        assertTrue(ragPrompt.metadata().get("hasConversationHistory").equals(false));
    }

    /**
     * Test buildMultiDocPrompt groups results by document.
     */
    @Test
    void testBuildMultiDocPrompt() {
        String query = "Test query";
        SearchResult result1 = new SearchResult("Content 1", 0.9, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult result2 = new SearchResult("Content 2", 0.8, 2, "Doc2.pdf", 1, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result1, result2);

        RagPrompt ragPrompt = promptBuilder.buildMultiDocPrompt(query, results, null);

        assertNotNull(ragPrompt);
        assertNotNull(ragPrompt.userPrompt());
        assertTrue(ragPrompt.userPrompt().contains("MULTIPLE DOCUMENTS"));
        assertTrue(ragPrompt.metadata().get("multiDocumentMode").equals(true));
        assertTrue(ragPrompt.metadata().containsKey("uniqueDocuments"));
    }

    /**
     * Test RagPrompt record helpers.
     */
    @Test
    void testRagPromptHelpers() {
        String query = "Test query";
        SearchResult result1 = new SearchResult("Content 1", 0.9, 1, "Doc1.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        SearchResult result2 = new SearchResult("Content 2", 0.8, 2, "Doc2.pdf", 1, "doc-id-2", "hash-2", "chunk-hash-2", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result1, result2);

        RagPrompt ragPrompt = promptBuilder.buildRagPrompt(query, results);

        assertNotNull(ragPrompt.systemPrompt());
        assertNotNull(ragPrompt.userPrompt());
        assertNotNull(ragPrompt.metadata());
        assertNotNull(ragPrompt.sources());
        assertEquals(2, ragPrompt.sources().size());
    }

    /**
     * Test context compression is applied when enabled.
     */
    @Test
    void testContextCompressionApplied() {
        when(contextCompressor.isEnabled()).thenReturn(true);
        when(contextCompressor.compressChunk(any(), any())).thenReturn("Compressed content");
        
        String query = "Test query";
        SearchResult result = new SearchResult("Original content", 0.9, 1, "Doc.pdf", 0, "doc-id-1", "hash-1", "chunk-hash-1", "text-embedding-3-small", 1536, "en", 1, java.time.Instant.now());
        List<SearchResult> results = List.of(result);

        RagPrompt ragPrompt = promptBuilder.buildRagPromptWithHistory(query, results, null);

        assertNotNull(ragPrompt);
        assertTrue(ragPrompt.metadata().get("compressionEnabled").equals(true));
        assertEquals("Compressed content", ragPrompt.sources().get(0).getContent());
    }
}
