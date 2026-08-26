package com.enterprise.ai.knowledge.assistant.evaluation.service;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationTest;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-assisted test generation service for creating evaluation test cases.
 * Generates diverse question types from document chunks.
 */
@Slf4j
@Service
public class TestGeneratorService {

    private final ChatClient chatClient;

    public TestGeneratorService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Generate evaluation tests from a sample of document chunks
     */
    public List<EvaluationTest> generateTestsFromSample(int sampleSize) {
        List<EvaluationTest> generatedTests = new ArrayList<>();
        
        try {
            // Get a sample of chunks from the vector store
            List<SearchResult> sampleChunks = getSampleChunks(sampleSize);
            log.info("Retrieved {} sample chunks for test generation", sampleChunks.size());
            
            for (SearchResult chunk : sampleChunks) {
                try {
                    List<EvaluationTest> chunkTests = generateTestsFromChunk(chunk);
                    generatedTests.addAll(chunkTests);
                } catch (Exception e) {
                    log.warn("Failed to generate tests from chunk: {}", chunk.getChunkHash(), e);
                }
            }
            
            log.info("Successfully generated {} evaluation tests", generatedTests.size());
            return generatedTests;
            
        } catch (Exception e) {
            log.error("Error generating tests from sample", e);
            return generatedTests;
        }
    }

    /**
     * Generate tests from a specific chunk
     */
    public List<EvaluationTest> generateTestsFromChunk(SearchResult chunk) {
        List<EvaluationTest> tests = new ArrayList<>();
        String content = chunk.getContent();
        String documentName = chunk.getDocumentName();
        
        // Generate different question types
        tests.add(generateFactualQuestion(chunk, content, documentName));
        tests.add(generateConceptualQuestion(chunk, content, documentName));
        
        // Generate numerical question if content contains numbers
        if (containsNumericalData(content)) {
            tests.add(generateNumericalQuestion(chunk, content, documentName));
        }
        
        // Generate comparative question if content allows
        if (content.length() > 500) {
            tests.add(generateComparativeQuestion(chunk, content, documentName));
        }
        
        return tests.stream().filter(t -> t != null).toList();
    }

    /**
     * Generate a factual question from chunk content
     */
    private EvaluationTest generateFactualQuestion(SearchResult chunk, String content, String documentName) {
        try {
            String prompt = String.format("""
                Generate a factual question based on the following document excerpt.
                The question should be specific and answerable from the given text.
                
                Document: %s
                Excerpt: %s
                
                Generate:
                1. A clear factual question
                2. The expected answer based on the text
                3. 2-3 key points that should be included in the answer
                
                Format your response as:
                QUESTION: [your question]
                ANSWER: [expected answer]
                KEY_POINTS: [key point 1; key point 2; key point 3]
                """, documentName, content.substring(0, Math.min(1000, content.length())));

            String response = chatClient.prompt()
                    .system("You are a test question generator. Follow the format exactly.")
                    .user(prompt)
                    .call()
                    .content();

            return parseGeneratedTest(response, chunk, "FACTUAL", "EASY", "single_doc");
            
        } catch (Exception e) {
            log.warn("Failed to generate factual question", e);
            return null;
        }
    }

    /**
     * Generate a conceptual question from chunk content
     */
    private EvaluationTest generateConceptualQuestion(SearchResult chunk, String content, String documentName) {
        try {
            String prompt = String.format("""
                Generate a conceptual question based on the following document excerpt.
                The question should test understanding of concepts, frameworks, or methodologies.
                
                Document: %s
                Excerpt: %s
                
                Generate:
                1. A conceptual question requiring understanding
                2. The expected answer explaining the concept
                3. 2-3 key points that should be included in the answer
                
                Format your response as:
                QUESTION: [your question]
                ANSWER: [expected answer]
                KEY_POINTS: [key point 1; key point 2; key point 3]
                """, documentName, content.substring(0, Math.min(1000, content.length())));

            String response = chatClient.prompt()
                    .system("You are a test question generator. Follow the format exactly.")
                    .user(prompt)
                    .call()
                    .content();

            return parseGeneratedTest(response, chunk, "CONCEPTUAL", "MEDIUM", "single_doc");
            
        } catch (Exception e) {
            log.warn("Failed to generate conceptual question", e);
            return null;
        }
    }

    /**
     * Generate a numerical question from chunk content
     */
    private EvaluationTest generateNumericalQuestion(SearchResult chunk, String content, String documentName) {
        try {
            String prompt = String.format("""
                Generate a numerical question based on the following document excerpt.
                The question should require extracting or calculating numerical values.
                
                Document: %s
                Excerpt: %s
                
                Generate:
                1. A question about numerical data in the text
                2. The expected answer with the specific number/value
                3. 1-2 key points about the numerical data
                
                Format your response as:
                QUESTION: [your question]
                ANSWER: [expected answer with number]
                KEY_POINTS: [key point 1; key point 2]
                """, documentName, content.substring(0, Math.min(1000, content.length())));

            String response = chatClient.prompt()
                    .system("You are a test question generator. Follow the format exactly.")
                    .user(prompt)
                    .call()
                    .content();

            return parseGeneratedTest(response, chunk, "NUMERICAL", "MEDIUM", "single_doc");

        } catch (Exception e) {
            log.warn("Failed to generate numerical question", e);
            return null;
        }
    }

    /**
     * Generate a comparative question from chunk content
     */
    private EvaluationTest generateComparativeQuestion(SearchResult chunk, String content, String documentName) {
        try {
            String prompt = String.format("""
                Generate a comparative question based on the following document excerpt.
                The question should require comparing two or more aspects mentioned in the text.
                
                Document: %s
                Excerpt: %s
                
                Generate:
                1. A question comparing different aspects
                2. The expected answer highlighting the comparison
                3. 2-3 key points about the comparison
                
                Format your response as:
                QUESTION: [your question]
                ANSWER: [expected answer]
                KEY_POINTS: [key point 1; key point 2; key point 3]
                """, documentName, content.substring(0, Math.min(1000, content.length())));

            String response = chatClient.prompt()
                    .system("You are a test question generator. Follow the format exactly.")
                    .user(prompt)
                    .call()
                    .content();

            return parseGeneratedTest(response, chunk, "COMPARATIVE", "HARD", "single_doc");
            
        } catch (Exception e) {
            log.warn("Failed to generate comparative question", e);
            return null;
        }
    }

    /**
     * Parse generated test from LLM response
     */
    private EvaluationTest parseGeneratedTest(String response, SearchResult chunk, 
                                            String category, String difficulty, String scope) {
        try {
            String question = extractField(response, "QUESTION");
            String answer = extractField(response, "ANSWER");
            String keyPointsStr = extractField(response, "KEY_POINTS");
            
            if (question == null || question.isEmpty()) {
                return null;
            }
            
            List<String> keyPoints = parseKeyPoints(keyPointsStr);
            List<String> expectedChunkIds = List.of(chunk.getChunkHash());
            
            EvaluationTest test = new EvaluationTest();
            test.setId(UUID.randomUUID());
            test.setName(String.format("%s - %s", category, chunk.getDocumentName()));
            test.setQuery(question);
            test.setExpectedChunkIds(expectedChunkIds);
            test.setCategory(category);
            test.setLanguage(detectLanguage(chunk.getContent()));
            test.setDifficulty(difficulty);
            test.setDocumentScope(scope);
            test.setExpectedAnswer(answer);
            test.setKeyPoints(keyPoints);
            test.setCreatedAt(Instant.now());
            
            return test;
            
        } catch (Exception e) {
            log.warn("Failed to parse generated test", e);
            return null;
        }
    }

    /**
     * Extract field from formatted response
     */
    private String extractField(String response, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.*?)(?=\\n[A-Z_]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Parse key points from semicolon-separated string
     */
    private List<String> parseKeyPoints(String keyPointsStr) {
        if (keyPointsStr == null || keyPointsStr.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> points = new ArrayList<>();
        String[] parts = keyPointsStr.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                points.add(trimmed);
            }
        }
        return points;
    }

    /**
     * Detect language of content (simple heuristic)
     */
    private String detectLanguage(String content) {
        // Simple Telugu detection based on character ranges
        for (char c : content.toCharArray()) {
            if (c >= 0x0C00 && c <= 0x0C7F) { // Telugu Unicode range
                return "TELUGU";
            }
        }
        return "ENGLISH";
    }

    /**
     * Check if content contains numerical data
     */
    private boolean containsNumericalData(String content) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(content);
        return matcher.find();
    }

    /**
     * Get sample chunks from vector store
     */
    private List<SearchResult> getSampleChunks(int sampleSize) {
        try {
            // For now, return empty list as this requires additional implementation
            // In production, you would:
            // 1. Query the vector store for chunks across different documents
            // 2. Sample chunks from different document sections
            // 3. Ensure diversity in content types
            
            log.warn("getSampleChunks needs implementation - returning empty list");
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error getting sample chunks", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate multi-hop questions (requires information from multiple chunks)
     */
    public List<EvaluationTest> generateMultiHopQuestions(List<SearchResult> relatedChunks) {
        List<EvaluationTest> tests = new ArrayList<>();
        
        if (relatedChunks.size() < 2) {
            return tests;
        }
        
        try {
            String combinedContent = relatedChunks.stream()
                    .map(r -> r.getDocumentName() + ": " + r.getContent())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");
            
            String prompt = String.format("""
                Generate a multi-hop question that requires information from multiple document sections.
                The question should synthesize information from different parts of the text.
                
                Combined Content:
                %s
                
                Generate:
                1. A question requiring synthesis from multiple sections
                2. The expected answer combining information
                3. 3-4 key points from different sections
                
                Format your response as:
                QUESTION: [your question]
                ANSWER: [expected answer]
                KEY_POINTS: [key point 1; key point 2; key point 3; key point 4]
                """, combinedContent.substring(0, Math.min(2000, combinedContent.length())));

            String response = chatClient.prompt()
                    .system("You are a test question generator. Follow the format exactly.")
                    .user(prompt)
                    .call()
                    .content();

            List<String> expectedChunkIds = relatedChunks.stream()
                    .map(SearchResult::getChunkHash)
                    .toList();

            EvaluationTest test = parseGeneratedTest(response, relatedChunks.get(0), 
                                                   "MULTI_HOP", "HARD", "cross_chapter");
            if (test != null) {
                test.setExpectedChunkIds(expectedChunkIds);
                tests.add(test);
            }
            
        } catch (Exception e) {
            log.warn("Failed to generate multi-hop question", e);
        }
        
        return tests;
    }
}
