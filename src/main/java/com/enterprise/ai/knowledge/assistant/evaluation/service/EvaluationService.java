package com.enterprise.ai.knowledge.assistant.evaluation.service;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationResult;
import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationRun;
import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationTest;
import com.enterprise.ai.knowledge.assistant.evaluation.repository.EvaluationResultRepository;
import com.enterprise.ai.knowledge.assistant.evaluation.repository.EvaluationRunRepository;
import com.enterprise.ai.knowledge.assistant.evaluation.repository.EvaluationTestRepository;
import com.enterprise.ai.knowledge.assistant.rag.Retriever;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EvaluationService {

    private final EvaluationTestRepository testRepository;
    private final EvaluationRunRepository runRepository;
    private final EvaluationResultRepository resultRepository;
    private final Retriever retriever;
    private final AnswerQualityEvaluator answerQualityEvaluator;
    private final ChatClient chatClient;

    public EvaluationService(EvaluationTestRepository testRepository,
                             EvaluationRunRepository runRepository,
                             EvaluationResultRepository resultRepository,
                             Retriever retriever,
                             AnswerQualityEvaluator answerQualityEvaluator,
                             ChatClient chatClient) {
        this.testRepository = testRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.retriever = retriever;
        this.answerQualityEvaluator = answerQualityEvaluator;
        this.chatClient = chatClient;
    }

    /**
     * Create a new evaluation test with enhanced fields
     */
    public EvaluationTest createTest(String name, String query, List<String> expectedChunkIds,
                                     String category, String language, String difficulty,
                                     String documentScope, String expectedAnswer, List<String> keyPoints) {
        EvaluationTest test = new EvaluationTest();
        test.setName(name);
        test.setQuery(query);
        test.setExpectedChunkIds(expectedChunkIds);
        test.setCategory(category);
        test.setLanguage(language);
        test.setDifficulty(difficulty);
        test.setDocumentScope(documentScope);
        test.setExpectedAnswer(expectedAnswer);
        test.setKeyPoints(keyPoints);
        test.setCreatedAt(Instant.now());
        return testRepository.save(test);
    }

    /**
     * Create a new evaluation test (legacy method for backward compatibility)
     */
    public EvaluationTest createTest(String name, String query, List<String> expectedChunkIds) {
        return createTest(name, query, expectedChunkIds, null, null, null, null, null, null);
    }

    /**
     * Get all evaluation tests
     */
    public List<EvaluationTest> getAllTests() {
        return testRepository.findAll();
    }

    /**
     * Get tests by category
     */
    public List<EvaluationTest> getTestsByCategory(String category) {
        // TODO: Implement findByCategory in repository
        return testRepository.findAll().stream()
                .filter(t -> category.equals(t.getCategory()))
                .toList();
    }

    /**
     * Get tests by language
     */
    public List<EvaluationTest> getTestsByLanguage(String language) {
        // TODO: Implement findByLanguage in repository
        return testRepository.findAll().stream()
                .filter(t -> language.equals(t.getLanguage()))
                .toList();
    }

    /**
     * Run a single evaluation test with enhanced metrics
     */
    public Map<String, Object> runSingleTest(UUID testId) {
        EvaluationTest test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testId));

        long startTime = System.currentTimeMillis();
        List<SearchResult> results = retriever.retrieveAndRerank(test.getQuery(), 20, 10);
        long latencyMs = System.currentTimeMillis() - startTime;

        List<String> retrievedChunkIds = results.stream()
                .map(SearchResult::getChunkHash)
                .toList();

        // Calculate retrieval metrics
        Map<String, Object> metrics = EvaluationMetrics.calculateAllMetrics(
                retrievedChunkIds, test.getExpectedChunkIds(), latencyMs);

        // Generate answer and evaluate quality if expected answer is available
        if (test.getExpectedAnswer() != null && !test.getExpectedAnswer().isEmpty()) {
            try {
                String generatedAnswer = generateAnswer(test.getQuery(), results);
                Map<String, Object> qualityMetrics = answerQualityEvaluator.evaluateAnswerQuality(
                        test.getQuery(), generatedAnswer, results, test.getKeyPoints());
                
                metrics.putAll(qualityMetrics);
                metrics.put("generated_answer", generatedAnswer);
            } catch (Exception e) {
                log.warn("Failed to evaluate answer quality for test: {}", test.getName(), e);
            }
        }

        // Add test metadata to metrics
        metrics.put("test_category", test.getCategory());
        metrics.put("test_language", test.getLanguage());
        metrics.put("test_difficulty", test.getDifficulty());
        metrics.put("test_document_scope", test.getDocumentScope());

        EvaluationResult result = new EvaluationResult();
        result.setTestId(testId);
        result.setQuery(test.getQuery());
        result.setExpectedAnswer(test.getExpectedAnswer());
        result.setRetrievedChunkIds(retrievedChunkIds);
        result.setMetrics(metrics);
        result.setLatencyMs(latencyMs);
        resultRepository.save(result);

        return metrics;
    }

    /**
     * Generate answer using RAG pipeline
     */
    private String generateAnswer(String query, List<SearchResult> results) {
        try {
            String context = results.stream()
                    .map(r -> r.getDocumentName() + " (Page " + r.getPageNumber() + "): " + r.getContent())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");

            String prompt = String.format("""
                Answer the following question based on the provided context.
                If the answer is not in the context, say "I don't have enough information to answer this question."
                
                Context:
                %s
                
                Question: %s
                
                Answer:
                """, context, query);

            return chatClient.prompt()
                    .system("You are a helpful assistant that answers questions based on provided context.")
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error generating answer", e);
            return "Error generating answer";
        }
    }

    /**
     * Run all evaluation tests as a batch
     */
    public EvaluationRun runAllTests(String runName, String description) {
        EvaluationRun run = new EvaluationRun();
        run.setName(runName);
        run.setDescription(description);
        run.setStartedAt(Instant.now());
        run.setStatus("IN_PROGRESS");
        run = runRepository.save(run);

        List<EvaluationTest> tests = testRepository.findAll();
        List<Map<String, Object>> allMetrics = new ArrayList<>();

        for (EvaluationTest test : tests) {
            try {
                Map<String, Object> metrics = runSingleTest(test.getId());
                allMetrics.add(metrics);
            } catch (Exception e) {
                // Continue with other tests even if one fails
                System.err.println("Failed to run test: " + test.getName() + " - " + e.getMessage());
            }
        }

        Map<String, Object> averageMetrics = EvaluationMetrics.calculateAverageMetrics(allMetrics);

        // Add category-wise breakdown
        Map<String, Object> categoryBreakdown = calculateCategoryBreakdown(allMetrics);
        averageMetrics.putAll(categoryBreakdown);

        run.setCompletedAt(Instant.now());
        run.setStatus("COMPLETED");
        runRepository.save(run);

        return run;
    }

    /**
     * Calculate performance breakdown by category
     */
    private Map<String, Object> calculateCategoryBreakdown(List<Map<String, Object>> allMetrics) {
        Map<String, Object> breakdown = new java.util.HashMap<>();
        
        // Group by category
        Map<String, List<Map<String, Object>>> byCategory = allMetrics.stream()
                .filter(m -> m.containsKey("test_category"))
                .collect(java.util.stream.Collectors.groupingBy(m -> (String) m.get("test_category")));
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Map<String, Object>> categoryMetrics = entry.getValue();
            
            Map<String, Object> categoryAvg = EvaluationMetrics.calculateAverageMetrics(categoryMetrics);
            breakdown.put("category_" + category.toLowerCase(), categoryAvg);
        }
        
        return breakdown;
    }

    /**
     * Get all evaluation runs
     */
    public List<EvaluationRun> getAllRuns() {
        return runRepository.findAll();
    }

    /**
     * Get results for a specific run
     */
    public List<EvaluationResult> getResultsByRun(UUID runId) {
        return resultRepository.findByRunId(runId);
    }

    /**
     * Delete a test
     */
    public void deleteTest(UUID testId) {
        testRepository.deleteById(testId);
    }

    /**
     * Delete a run
     */
    public void deleteRun(UUID runId) {
        runRepository.deleteById(runId);
    }
}
