package com.enterprise.ai.knowledge.assistant.evaluation.service;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationTest;
import com.enterprise.ai.knowledge.assistant.evaluation.repository.EvaluationTestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for loading manual evaluation test data from JSON files.
 */
@Slf4j
@Service
public class TestDataLoaderService {

    private final EvaluationTestRepository testRepository;
    private final ObjectMapper objectMapper;

    public TestDataLoaderService(EvaluationTestRepository testRepository, ObjectMapper objectMapper) {
        this.testRepository = testRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Load tests from a JSON file
     */
    public List<EvaluationTest> loadTestsFromJson(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("Test data file not found: {}", filePath);
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(file, Map.class);
            List<Map<String, Object>> testsData = data.get("tests");

            List<EvaluationTest> loadedTests = new ArrayList<>();
            for (Map<String, Object> testData : testsData) {
                try {
                    EvaluationTest test = mapToEvaluationTest(testData);
                    EvaluationTest saved = testRepository.save(test);
                    loadedTests.add(saved);
                    log.info("Loaded test: {}", test.getName());
                } catch (Exception e) {
                    log.error("Failed to load test: {}", testData.get("name"), e);
                }
            }

            log.info("Successfully loaded {} evaluation tests from {}", loadedTests.size(), filePath);
            return loadedTests;

        } catch (IOException e) {
            log.error("Failed to read test data file: {}", filePath, e);
            return new ArrayList<>();
        }
    }

    /**
     * Map JSON data to EvaluationTest entity
     */
    @SuppressWarnings("unchecked")
    private EvaluationTest mapToEvaluationTest(Map<String, Object> data) {
        EvaluationTest test = new EvaluationTest();
        test.setName((String) data.get("name"));
        test.setQuery((String) data.get("query"));
        test.setCategory((String) data.get("category"));
        test.setLanguage((String) data.get("language"));
        test.setDifficulty((String) data.get("difficulty"));
        test.setDocumentScope((String) data.get("documentScope"));
        test.setExpectedAnswer((String) data.get("expectedAnswer"));
        test.setKeyPoints((List<String>) data.get("keyPoints"));
        test.setExpectedChunkIds((List<String>) data.get("expectedChunkIds"));
        test.setCreatedAt(Instant.now());
        return test;
    }

    /**
     * Clear all existing tests (use with caution)
     */
    public void clearAllTests() {
        log.warn("Clearing all evaluation tests");
        // TODO: Implement deleteAll in repository or use alternative approach
        List<EvaluationTest> allTests = testRepository.findAll();
        for (EvaluationTest test : allTests) {
            testRepository.deleteById(test.getId());
        }
    }

    /**
     * Get count of existing tests
     */
    public long getTestCount() {
        // TODO: Implement count in repository
        return testRepository.findAll().size();
    }
}
