package com.enterprise.ai.knowledge.assistant.embedding.service;

import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.logging.EmbeddingLogger;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingLogger embeddingLogger;
    private final PerformanceLogger performanceLogger;
    private final int embeddingDimensions;

    public EmbeddingService(EmbeddingModel embeddingModel, EmbeddingLogger embeddingLogger, PerformanceLogger performanceLogger,
                           @Value("${spring.ai.openai.embedding.options.dimensions:768}") int embeddingDimensions) {
        this.embeddingModel = embeddingModel;
        this.embeddingLogger = embeddingLogger;
        this.performanceLogger = performanceLogger;
        this.embeddingDimensions = embeddingDimensions;
        
        // Log service initialization with actual dimensions
        String modelName = embeddingModel.getClass().getSimpleName();
        embeddingLogger.logServiceInitialization(modelName, embeddingDimensions);
    }

    public EmbeddingResult generateEmbedding(String text) {
        String modelName = embeddingModel.getClass().getSimpleName();
        embeddingLogger.logEmbeddingGenerationStart(text, modelName);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("embedding_generation");
        
        try {
            float[] vector = embeddingModel.embed(text);
            int dims = vector == null ? 0 : vector.length;
            
            performanceLogger.stopTiming(timing);
            embeddingLogger.logEmbeddingGenerationComplete(text, modelName, dims, System.currentTimeMillis() - timing.getStartTime());
            
            return new EmbeddingResult(vector, dims, modelName);
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            embeddingLogger.logEmbeddingFailure(text, modelName, e);
            throw e;
        }
    }

    public String getModelName() {
        return embeddingModel.getClass().getSimpleName();
    }
}
