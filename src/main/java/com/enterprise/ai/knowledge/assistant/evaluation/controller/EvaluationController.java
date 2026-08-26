package com.enterprise.ai.knowledge.assistant.evaluation.controller;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationResult;
import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationRun;
import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationTest;
import com.enterprise.ai.knowledge.assistant.evaluation.service.EvaluationService;
import com.enterprise.ai.knowledge.assistant.evaluation.service.TestDataLoaderService;
import com.enterprise.ai.knowledge.assistant.evaluation.service.TestGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Evaluation API", description = "Endpoints for running and managing RAG evaluation tests")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final TestGeneratorService testGeneratorService;
    private final TestDataLoaderService testDataLoaderService;

    @PostMapping("/tests")
    @Operation(summary = "Create Evaluation Test", description = "Create a new evaluation test with expected results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<EvaluationTest> createTest(
            @Parameter(description = "Test name", required = true)
            @RequestParam String name,
            @Parameter(description = "Test query", required = true)
            @RequestParam String query,
            @Parameter(description = "Expected chunk IDs", required = true)
            @RequestBody List<String> expectedChunkIds) {
        log.info("Creating evaluation test: {}", name);
        EvaluationTest test = evaluationService.createTest(name, query, expectedChunkIds);
        return ResponseEntity.ok(test);
    }

    @PostMapping("/tests/enhanced")
    @Operation(summary = "Create Enhanced Evaluation Test", description = "Create a new evaluation test with categorization and answer quality fields")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<EvaluationTest> createEnhancedTest(
            @Parameter(description = "Test name", required = true)
            @RequestParam String name,
            @Parameter(description = "Test query", required = true)
            @RequestParam String query,
            @Parameter(description = "Expected chunk IDs", required = true)
            @RequestBody List<String> expectedChunkIds,
            @Parameter(description = "Test category (FACTUAL, CONCEPTUAL, COMPARATIVE, NUMERICAL, MULTI_HOP)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Document language (ENGLISH, TELUGU, MIXED)")
            @RequestParam(required = false) String language,
            @Parameter(description = "Difficulty level (EASY, MEDIUM, HARD)")
            @RequestParam(required = false) String difficulty,
            @Parameter(description = "Document scope (single_doc, multi_doc, cross_chapter)")
            @RequestParam(required = false) String documentScope,
            @Parameter(description = "Expected answer for quality evaluation")
            @RequestParam(required = false) String expectedAnswer,
            @Parameter(description = "Key points that should be included in answer")
            @RequestBody(required = false) List<String> keyPoints) {
        log.info("Creating enhanced evaluation test: {}", name);
        EvaluationTest test = evaluationService.createTest(name, query, expectedChunkIds, 
                category, language, difficulty, documentScope, expectedAnswer, keyPoints);
        return ResponseEntity.ok(test);
    }

    @GetMapping("/tests")
    @Operation(summary = "List Evaluation Tests", description = "Get all evaluation tests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of evaluation tests")
    })
    public ResponseEntity<List<EvaluationTest>> getAllTests() {
        List<EvaluationTest> tests = evaluationService.getAllTests();
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/tests/category/{category}")
    @Operation(summary = "Get Tests by Category", description = "Get evaluation tests filtered by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of evaluation tests")
    })
    public ResponseEntity<List<EvaluationTest>> getTestsByCategory(
            @Parameter(description = "Test category", required = true)
            @PathVariable String category) {
        List<EvaluationTest> tests = evaluationService.getTestsByCategory(category);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/tests/language/{language}")
    @Operation(summary = "Get Tests by Language", description = "Get evaluation tests filtered by language")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of evaluation tests")
    })
    public ResponseEntity<List<EvaluationTest>> getTestsByLanguage(
            @Parameter(description = "Document language", required = true)
            @PathVariable String language) {
        List<EvaluationTest> tests = evaluationService.getTestsByLanguage(language);
        return ResponseEntity.ok(tests);
    }

    @PostMapping("/tests/generate")
    @Operation(summary = "Generate Tests from Sample", description = "Generate evaluation tests using LLM from document sample")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tests generated successfully"),
            @ApiResponse(responseCode = "500", description = "Generation failed")
    })
    public ResponseEntity<List<EvaluationTest>> generateTests(
            @Parameter(description = "Sample size for generation", required = false)
            @RequestParam(defaultValue = "10") int sampleSize) {
        log.info("Generating {} evaluation tests from sample", sampleSize);
        List<EvaluationTest> tests = testGeneratorService.generateTestsFromSample(sampleSize);
        return ResponseEntity.ok(tests);
    }

    @PostMapping("/tests/load-manual")
    @Operation(summary = "Load Manual Tests", description = "Load manual evaluation tests from JSON file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tests loaded successfully"),
            @ApiResponse(responseCode = "500", description = "Loading failed")
    })
    public ResponseEntity<List<EvaluationTest>> loadManualTests(
            @Parameter(description = "Path to JSON file", required = false)
            @RequestParam(defaultValue = "manual-evaluation-tests.json") String filePath) {
        log.info("Loading manual evaluation tests from: {}", filePath);
        List<EvaluationTest> tests = testDataLoaderService.loadTestsFromJson(filePath);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/tests/count")
    @Operation(summary = "Get Test Count", description = "Get total number of evaluation tests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test count returned")
    })
    public ResponseEntity<Long> getTestCount() {
        long count = testDataLoaderService.getTestCount();
        return ResponseEntity.ok(count);
    }

    @PostMapping("/tests/{testId}/run")
    @Operation(summary = "Run Single Test", description = "Run a single evaluation test and return metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test executed successfully"),
            @ApiResponse(responseCode = "404", description = "Test not found")
    })
    public ResponseEntity<Map<String, Object>> runSingleTest(
            @Parameter(description = "Test ID", required = true)
            @PathVariable UUID testId) {
        try {
            Map<String, Object> metrics = evaluationService.runSingleTest(testId);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/runs")
    @Operation(summary = "Run All Tests", description = "Run all evaluation tests as a batch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch execution started/completed")
    })
    public ResponseEntity<EvaluationRun> runAllTests(
            @Parameter(description = "Run name", required = true)
            @RequestParam String name,
            @Parameter(description = "Run description")
            @RequestParam(required = false) String description) {
        log.info("Starting evaluation run: {}", name);
        EvaluationRun run = evaluationService.runAllTests(name, description);
        return ResponseEntity.ok(run);
    }

    @GetMapping("/runs")
    @Operation(summary = "List Evaluation Runs", description = "Get all evaluation runs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of evaluation runs")
    })
    public ResponseEntity<List<EvaluationRun>> getAllRuns() {
        List<EvaluationRun> runs = evaluationService.getAllRuns();
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/runs/{runId}/results")
    @Operation(summary = "Get Run Results", description = "Get results for a specific evaluation run")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of results")
    })
    public ResponseEntity<List<EvaluationResult>> getResultsByRun(
            @Parameter(description = "Run ID", required = true)
            @PathVariable UUID runId) {
        List<EvaluationResult> results = evaluationService.getResultsByRun(runId);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/tests/{testId}")
    @Operation(summary = "Delete Test", description = "Delete an evaluation test")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test deleted successfully")
    })
    public ResponseEntity<Void> deleteTest(
            @Parameter(description = "Test ID", required = true)
            @PathVariable UUID testId) {
        evaluationService.deleteTest(testId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/runs/{runId}")
    @Operation(summary = "Delete Run", description = "Delete an evaluation run and its results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Run deleted successfully")
    })
    public ResponseEntity<Void> deleteRun(
            @Parameter(description = "Run ID", required = true)
            @PathVariable UUID runId) {
        evaluationService.deleteRun(runId);
        return ResponseEntity.ok().build();
    }
}
