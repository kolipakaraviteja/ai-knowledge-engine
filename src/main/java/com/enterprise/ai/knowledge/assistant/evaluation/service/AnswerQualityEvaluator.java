package com.enterprise.ai.knowledge.assistant.evaluation.service;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-as-Judge service for evaluating answer quality.
 * Evaluates faithfulness, relevance, completeness, and citation accuracy.
 */
@Slf4j
@Service
public class AnswerQualityEvaluator {

    private final ChatClient chatClient;

    public AnswerQualityEvaluator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Evaluate faithfulness - how factually consistent the answer is with retrieved context
     */
    public double evaluateFaithfulness(String query, String answer, List<SearchResult> context) {
        if (answer == null || answer.isEmpty() || context.isEmpty()) {
            return 0.0;
        }

        try {
            String contextText = buildContextString(context);
            String prompt = String.format("""
                Evaluate the faithfulness of the given answer to the retrieved context.
                Faithfulness measures how factually consistent the answer is with the provided context.
                
                Query: %s
                Answer: %s
                Context: %s
                
                Rate faithfulness on a scale of 0.0 to 1.0, where:
                - 1.0: Answer is completely faithful to context, no hallucinations
                - 0.5: Answer is mostly faithful but contains some unsupported claims
                - 0.0: Answer contains significant hallucinations or contradicts context
                
                Return only the numeric score (e.g., 0.85).
                """, query, answer, contextText);

            String response = chatClient.prompt()
                    .system("You are an objective answer quality evaluator. Return only numeric scores.")
                    .user(prompt)
                    .call()
                    .content();

            return extractScore(response);
        } catch (Exception e) {
            log.error("Error evaluating faithfulness", e);
            return 0.0;
        }
    }

    /**
     * Evaluate relevance - how well the answer addresses the original query
     */
    public double evaluateRelevance(String query, String answer) {
        if (answer == null || answer.isEmpty()) {
            return 0.0;
        }

        try {
            String prompt = String.format("""
                Evaluate the relevance of the given answer to the query.
                Relevance measures how well the answer addresses the user's question.
                
                Query: %s
                Answer: %s
                
                Rate relevance on a scale of 0.0 to 1.0, where:
                - 1.0: Answer completely and directly addresses the query
                - 0.5: Answer partially addresses the query but misses key aspects
                - 0.0: Answer is irrelevant or does not address the query
                
                Return only the numeric score (e.g., 0.90).
                """, query, answer);

            String response = chatClient.prompt()
                    .system("You are an objective answer quality evaluator. Return only numeric scores.")
                    .user(prompt)
                    .call()
                    .content();

            return extractScore(response);
        } catch (Exception e) {
            log.error("Error evaluating relevance", e);
            return 0.0;
        }
    }

    /**
     * Evaluate completeness - how well the answer covers expected key points
     */
    public double evaluateCompleteness(String answer, List<String> keyPoints) {
        if (answer == null || answer.isEmpty() || keyPoints == null || keyPoints.isEmpty()) {
            return 0.0;
        }

        try {
            String keyPointsText = String.join("\n", keyPoints);
            String prompt = String.format("""
                Evaluate the completeness of the given answer based on expected key points.
                Completeness measures how well the answer covers all required information.
                
                Answer: %s
                Expected Key Points:
                %s
                
                Rate completeness on a scale of 0.0 to 1.0, where:
                - 1.0: Answer covers all key points thoroughly
                - 0.5: Answer covers some key points but misses others
                - 0.0: Answer misses most or all key points
                
                Return only the numeric score (e.g., 0.75).
                """, answer, keyPointsText);

            String response = chatClient.prompt()
                    .system("You are an objective answer quality evaluator. Return only numeric scores.")
                    .user(prompt)
                    .call()
                    .content();

            return extractScore(response);
        } catch (Exception e) {
            log.error("Error evaluating completeness", e);
            return 0.0;
        }
    }

    /**
     * Evaluate citation accuracy - correctness of cited sources
     */
    public double evaluateCitationAccuracy(String answer, List<SearchResult> retrievedContext) {
        if (answer == null || answer.isEmpty() || retrievedContext.isEmpty()) {
            return 0.0;
        }

        try {
            String contextSummary = buildContextSummary(retrievedContext);
            String prompt = String.format("""
                Evaluate the citation accuracy of the given answer.
                Citation accuracy measures whether cited information actually comes from the retrieved context.
                
                Answer: %s
                Retrieved Context Summary:
                %s
                
                Rate citation accuracy on a scale of 0.0 to 1.0, where:
                - 1.0: All cited information is accurately from the retrieved context
                - 0.5: Some citations are accurate but others are not
                - 0.0: Citations are inaccurate or information is hallucinated
                
                Return only the numeric score (e.g., 0.80).
                """, answer, contextSummary);

            String response = chatClient.prompt()
                    .system("You are an objective answer quality evaluator. Return only numeric scores.")
                    .user(prompt)
                    .call()
                    .content();

            return extractScore(response);
        } catch (Exception e) {
            log.error("Error evaluating citation accuracy", e);
            return 0.0;
        }
    }

    /**
     * Comprehensive answer quality evaluation
     */
    public Map<String, Object> evaluateAnswerQuality(String query, String answer, 
                                                     List<SearchResult> context, 
                                                     List<String> keyPoints) {
        Map<String, Object> qualityMetrics = new java.util.HashMap<>();
        
        double faithfulness = evaluateFaithfulness(query, answer, context);
        double relevance = evaluateRelevance(query, answer);
        double completeness = keyPoints != null && !keyPoints.isEmpty() 
            ? evaluateCompleteness(answer, keyPoints) 
            : 0.0;
        double citationAccuracy = evaluateCitationAccuracy(answer, context);
        
        qualityMetrics.put("faithfulness", faithfulness);
        qualityMetrics.put("relevance", relevance);
        qualityMetrics.put("completeness", completeness);
        qualityMetrics.put("citation_accuracy", citationAccuracy);
        
        // Overall quality score (weighted average)
        double overallScore = (faithfulness * 0.3) + (relevance * 0.3) + 
                              (completeness * 0.2) + (citationAccuracy * 0.2);
        qualityMetrics.put("overall_quality", overallScore);
        
        return qualityMetrics;
    }

    /**
     * Extract numeric score from LLM response
     */
    private double extractScore(String response) {
        if (response == null || response.isEmpty()) {
            return 0.0;
        }

        Pattern pattern = Pattern.compile("([0-9]*\\.?[0-9]+)");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                // Clamp to 0.0-1.0 range
                return Math.max(0.0, Math.min(1.0, score));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse score from response: {}", response);
                return 0.0;
            }
        }
        
        return 0.0;
    }

    /**
     * Build context string from search results
     */
    private String buildContextString(List<SearchResult> context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, context.size()); i++) {
            SearchResult result = context.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append(result.getDocumentName());
            if (result.getPageNumber() != null) {
                sb.append(" (Page ").append(result.getPageNumber()).append(")");
            }
            sb.append(": ").append(result.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Build context summary for citation evaluation
     */
    private String buildContextSummary(List<SearchResult> context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(10, context.size()); i++) {
            SearchResult result = context.get(i);
            sb.append("Doc ").append(i + 1).append(": ")
              .append(result.getDocumentName());
            if (result.getPageNumber() != null) {
                sb.append(" p.").append(result.getPageNumber());
            }
            sb.append(" - ").append(result.getContent().substring(0, Math.min(200, result.getContent().length())))
              .append("...\n");
        }
        return sb.toString();
    }
}
