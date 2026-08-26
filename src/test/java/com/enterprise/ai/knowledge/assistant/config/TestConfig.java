package com.enterprise.ai.knowledge.assistant.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.rag.PromptBuilder;
import com.enterprise.ai.knowledge.assistant.rag.compression.ContextCompressor;
import com.enterprise.ai.knowledge.assistant.rag.template.PromptTemplate;
import com.enterprise.ai.knowledge.assistant.vector.service.VectorStoreService;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for unit tests.
 * Provides mock beans for testing without requiring actual services.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EmbeddingService mockEmbeddingService() {
        return mock(EmbeddingService.class);
    }

    @Bean
    @Primary
    public VectorStoreService mockVectorStoreService() {
        return mock(VectorStoreService.class);
    }

    @Bean
    @Primary
    public PromptTemplate mockPromptTemplate() {
        return mock(PromptTemplate.class);
    }

    @Bean
    @Primary
    public ContextCompressor mockContextCompressor() {
        return mock(ContextCompressor.class);
    }

    @Bean
    @Primary
    public PromptBuilder mockPromptBuilder() {
        return mock(PromptBuilder.class);
    }
}