package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.rag.compression.ContextCompressor;
import com.enterprise.ai.knowledge.assistant.rag.dto.RagPrompt;
import com.enterprise.ai.knowledge.assistant.rag.template.PromptTemplate;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PromptBuilder {

    private final PromptTemplate defaultTemplate;
    private final ContextCompressor contextCompressor;

    public PromptBuilder(PromptTemplate defaultTemplate, ContextCompressor contextCompressor) {
        this.defaultTemplate = defaultTemplate;
        this.contextCompressor = contextCompressor;
    }

    /**
     * Build a RagPrompt from a query and search results using the default template.
     */
    public RagPrompt buildRagPrompt(String query, List<SearchResult> results) {
       // return buildRagPrompt(query, results, defaultTemplate);
        return buildRagPromptWithHistory(query, results, null);
    }

    /**
     * Build a RagPrompt from a query and search results using a specified template.
     */
    public RagPrompt buildRagPrompt(String query, List<SearchResult> results, PromptTemplate template) {
        if (template == null) {
            template = defaultTemplate;
        }

        String system = template.renderSystem(results);
        String user = template.renderUser(query, results);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("templateName", template.getName());
        metadata.put("sourceCount", results == null ? 0 : results.size());
        if (results != null && !results.isEmpty()) {
            double avgScore = results.stream()
                    .mapToDouble(SearchResult::getScore)
                    .average()
                    .orElse(0.0);
            metadata.put("averageRelevanceScore", avgScore);
        }

        return new RagPrompt(system, user, results, metadata);
    }

    /**
     * Legacy method for backward compatibility - returns just the user prompt string.
     * Consider migrating to buildRagPrompt() which returns RagPrompt object.
     */
    @Deprecated(forRemoval = true)
    public String buildRagPromptLegacy(String query, List<SearchResult> results) {
        RagPrompt prompt = buildRagPrompt(query, results);
        return prompt.userPrompt();
    }

    /**
     * Get the system prompt from the default template.
     */
    public String getSystemPrompt() {
        return defaultTemplate.renderSystem(null);
    }

    public RagPrompt buildRagPromptWithHistory(String query, List<SearchResult> results, String conversationHistory) {
        if (results == null) {
            results = List.of();
        }

        List<SearchResult> compressedResults = results;
        if (contextCompressor.isEnabled()) {
            compressedResults = results.stream()
                    .map(r -> new SearchResult(
                            contextCompressor.compressChunk(r.getContent(), query),
                            r.getScore(),
                            r.getPageNumber(),
                            r.getDocumentName(),
                            r.getChunkIndex(),
                            r.getDocumentId(),
                            r.getDocumentHash(),
                            r.getChunkHash(),
                            r.getEmbeddingModel(),
                            r.getEmbeddingDimension(),
                            r.getLanguage(),
                            r.getVersion(),
                            r.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());
        }

        String system = defaultTemplate.renderSystem(compressedResults);
        String userPromptWithHistory = buildUserPromptWithHistory(query, compressedResults, conversationHistory);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("templateName", defaultTemplate.getName());
        metadata.put("sourceCount", compressedResults.size());
        metadata.put("hasConversationHistory", conversationHistory != null && !conversationHistory.isEmpty());
        metadata.put("compressionEnabled", contextCompressor.isEnabled());
        if (!compressedResults.isEmpty()) {
            double avgScore = compressedResults.stream()
                    .mapToDouble(SearchResult::getScore)
                    .average()
                    .orElse(0.0);
            metadata.put("averageRelevanceScore", avgScore);
        }

        return new RagPrompt(system, userPromptWithHistory, compressedResults, metadata);
    }

    public RagPrompt buildMultiDocPrompt(String query, List<SearchResult> results, String conversationHistory) {
        if (results == null) {
            results = List.of();
        }

        List<SearchResult> compressedResults = results;
        if (contextCompressor.isEnabled()) {
            compressedResults = results.stream()
                    .map(r -> new SearchResult(
                            contextCompressor.compressChunk(r.getContent(), query),
                            r.getScore(),
                            r.getPageNumber(),
                            r.getDocumentName(),
                            r.getChunkIndex(),
                            r.getDocumentId(),
                            r.getDocumentHash(),
                            r.getChunkHash(),
                            r.getEmbeddingModel(),
                            r.getEmbeddingDimension(),
                            r.getLanguage(),
                            r.getVersion(),
                            r.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());
        }

        String system = defaultTemplate.renderSystem(compressedResults);
        String userPromptWithMultiDoc = buildUserPromptWithMultiDoc(query, compressedResults, conversationHistory);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("templateName", defaultTemplate.getName());
        metadata.put("sourceCount", compressedResults.size());
        metadata.put("hasConversationHistory", conversationHistory != null && !conversationHistory.isEmpty());
        metadata.put("compressionEnabled", contextCompressor.isEnabled());
        metadata.put("multiDocumentMode", true);

        long uniqueDocuments = compressedResults.stream()
                .map(SearchResult::getDocumentName)
                .distinct()
                .count();
        metadata.put("uniqueDocuments", uniqueDocuments);

        if (!compressedResults.isEmpty()) {
            double avgScore = compressedResults.stream()
                    .mapToDouble(SearchResult::getScore)
                    .average()
                    .orElse(0.0);
            metadata.put("averageRelevanceScore", avgScore);
        }

        return new RagPrompt(system, userPromptWithMultiDoc, compressedResults, metadata);
    }

    private String buildUserPromptWithMultiDoc(String query, List<SearchResult> results, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("=== CONVERSATION HISTORY ===\n");
            prompt.append(conversationHistory);
            prompt.append("\n=== END HISTORY ===\n\n");
        }

        if (results != null && !results.isEmpty()) {
            Map<String, List<SearchResult>> groupedByDoc = results.stream()
                    .collect(Collectors.groupingBy(SearchResult::getDocumentName));

            prompt.append("=== SOURCES FROM MULTIPLE DOCUMENTS ===\n\n");

            for (Map.Entry<String, List<SearchResult>> entry : groupedByDoc.entrySet()) {
                String docName = entry.getKey();
                List<SearchResult> docResults = entry.getValue();

                prompt.append("Document: ").append(docName).append("\n");
                prompt.append("Chunks: ").append(docResults.size()).append("\n");
                prompt.append("Content:\n");

                for (SearchResult result : docResults) {
                    if (result.getPageNumber() != null) {
                        prompt.append("  [Page ").append(result.getPageNumber()).append("] ");
                    }
                    prompt.append(result.getContent()).append("\n");
                }
                prompt.append("\n");
            }

            prompt.append("=== END SOURCES ===\n\n");
        }

        prompt.append("Question: ").append(query);
        prompt.append("\n\nPlease provide an answer using the information from these documents, and cite which document(s) you used.");

        return prompt.toString();
    }

    private String buildUserPromptWithHistory(String query, List<SearchResult> results, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("=== CONVERSATION HISTORY ===\n");
            prompt.append(conversationHistory);
            prompt.append("\n=== END HISTORY ===\n\n");
        }

        if (results != null && !results.isEmpty()) {
            prompt.append("=== CONTEXT ===\n");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                prompt.append("Document: ").append(result.getDocumentName());
                if (result.getPageNumber() != null) {
                    prompt.append(" (Page ").append(result.getPageNumber()).append(")");
                }
                prompt.append("\n");
                prompt.append(result.getContent()).append("\n\n");
            }
            prompt.append("=== END CONTEXT ===\n\n");
        }

        prompt.append("Question: ").append(query);

        return prompt.toString();
    }
}
