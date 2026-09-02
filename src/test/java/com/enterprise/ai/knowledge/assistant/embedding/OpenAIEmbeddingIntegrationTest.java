package com.enterprise.ai.knowledge.assistant.embedding;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for OpenAI embedding generation.
 * Tests OpenAI connection, embedding generation, and dimensions.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.llm.provider=openai",
    "spring.ai.openai.api-key=${OPENAI_API_KEY}",
    "spring.ai.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1}",
    "spring.ai.openai.embedding.options.model=text-embedding-3-small",
    "spring.ai.openai.embedding.options.dimensions=1536"
})
public class OpenAIEmbeddingIntegrationTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${spring.ai.openai.api-key:NOT_SET}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:NOT_SET}")
    private String baseUrl;

    @Value("${spring.ai.openai.embedding.options.model:NOT_SET}")
    private String embeddingModelName;

    @Test
    public void testOpenAIConnection() {
        assertNotNull(embeddingModel, "EmbeddingModel should be autowired");
        assertNotNull(embeddingService, "EmbeddingService should be autowired");
        System.out.println("OpenAI connection test passed");
    }

    @Test
    public void testConfiguration() {
        System.out.println("=== OpenAI Configuration Debug ===");
        System.out.println("API Key: " + maskApiKey(apiKey));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Embedding Model: " + embeddingModelName);
        System.out.println("EmbeddingModel class: " + embeddingModel.getClass().getName());
        
        assertFalse(apiKey.equals("NOT_SET"), "OPENAI_API_KEY environment variable must be set");
        assertFalse(apiKey.isEmpty(), "API key cannot be empty");
        assertTrue(apiKey.startsWith("sk-"), "API key should start with 'sk-'");
        
        System.out.println("Configuration test passed");
    }

    @Test
    public void testEmbeddingModelType() {
        assertTrue(embeddingModel instanceof OpenAiEmbeddingModel, 
            "EmbeddingModel should be instance of OpenAiEmbeddingModel");
        System.out.println("Embedding model type test passed");
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("NOT_SET")) {
            return apiKey;
        }
        if (apiKey.length() <= 8) {
            return "sk-****";
        }
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    @Test
    public void testEmbeddingGeneration() {
        String testText = "This is a test sentence for embedding generation.";
        
        try {
            EmbeddingResult result = embeddingService.generateEmbedding(testText);
            
            assertNotNull(result, "EmbeddingResult should not be null");
            assertNotNull(result.vector(), "Embedding vector should not be null");
            assertTrue(result.vector().length > 0, "Embedding vector should have dimensions");
            assertNotNull(result.model(), "Model name should not be null");
            
            System.out.println("Embedding generation test passed");
            System.out.println("Model: " + result.model());
            System.out.println("Dimensions: " + result.dimensions());

            
        } catch (Exception e) {
            fail("Embedding generation failed: " + e.getMessage());
        }
    }

    @Test
    public void testEmbeddingDimensions() {
        String testText = "Test dimensions";
        
        try {
            EmbeddingResult result = embeddingService.generateEmbedding(testText);
            
            // text-embedding-3-small can produce up to 1536 dimensions
            assertEquals(1536, result.dimensions(), 
                "Embedding dimensions should be 1536 for text-embedding-3-small");
            
            System.out.println("Embedding dimensions test passed: " + result.dimensions());
            
        } catch (Exception e) {
            fail("Embedding dimensions test failed: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleEmbeddings() {
        String[] testTexts = {
            "First test sentence",
            "Second test sentence",
            "Third test sentence"
        };
        
        try {
            for (String text : testTexts) {
                EmbeddingResult result = embeddingService.generateEmbedding(text);
                assertNotNull(result, "EmbeddingResult should not be null for: " + text);
                assertEquals(1536, result.dimensions(), 
                    "All embeddings should have 1536 dimensions");
            }
            
            System.out.println("Multiple embeddings test passed");
            
        } catch (Exception e) {
            fail("Multiple embeddings test failed: " + e.getMessage());
        }
    }

    @Test
    public void testEmptyText() {
        String emptyText = "";
        
        try {
            EmbeddingResult result = embeddingService.generateEmbedding(emptyText);
            assertNotNull(result, "Empty text should still produce an embedding");
            assertEquals(1536, result.dimensions(), 
                "Empty text embedding should still have 1536 dimensions");
            
            System.out.println("Empty text test passed");
            
        } catch (Exception e) {
            fail("Empty text test failed: " + e.getMessage());
        }
    }
}
