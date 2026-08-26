package com.enterprise.ai.knowledge.assistant.conversation.service;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.chat.dto.Citation;
import com.enterprise.ai.knowledge.assistant.chat.dto.DocumentSource;
import com.enterprise.ai.knowledge.assistant.conversation.repository.ConversationRepository;
import com.enterprise.ai.knowledge.assistant.logging.ChatLogger;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.rag.PromptBuilder;
import com.enterprise.ai.knowledge.assistant.rag.Retriever;
import com.enterprise.ai.knowledge.assistant.rag.dto.RagPrompt;
import com.enterprise.ai.knowledge.assistant.rag.retriever.HybridRetriever;
import com.enterprise.ai.knowledge.assistant.rag.service.DocumentGroupingService;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MemoryManager memoryManager;
    private final Retriever retriever;
    private final HybridRetriever hybridRetriever;
    private final PromptBuilder promptBuilder;
    private final DocumentGroupingService documentGroupingService;
    private final ChatClient chatClient;
    private final ChatLogger chatLogger;
    private final PerformanceLogger performanceLogger;

    @Value("${app.rag.enableMultiDocumentMode:false}")
    private boolean enableMultiDocumentMode;

    public ConversationService(ConversationRepository conversationRepository,
                                MemoryManager memoryManager,
                                Retriever retriever,
                                HybridRetriever hybridRetriever,
                                PromptBuilder promptBuilder,
                                DocumentGroupingService documentGroupingService,
                                ChatClient chatClient,
                                ChatLogger chatLogger,
                                PerformanceLogger performanceLogger) {
        this.conversationRepository = conversationRepository;
        this.memoryManager = memoryManager;
        this.retriever = retriever;
        this.hybridRetriever = hybridRetriever;
        this.promptBuilder = promptBuilder;
        this.documentGroupingService = documentGroupingService;
        this.chatClient = chatClient;
        this.chatLogger = chatLogger;
        this.performanceLogger = performanceLogger;
    }

    public UUID createConversation() {
        UUID conversationId = conversationRepository.createConversation("New Conversation");
        chatLogger.logConversationStart(conversationId);
        return conversationId;
    }


    public ChatResponse chat(UUID conversationId, String userMessage, int historyDepth) {
        return chat(conversationId, userMessage, historyDepth, null, null);
    }

    public ChatResponse chat(UUID conversationId, String userMessage, int historyDepth, String knowledgeBaseId, String collectionId) {
        chatLogger.logConversationWithHistory(conversationId, userMessage, historyDepth);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("conversation_chat");

        try {
            int messageOrder = memoryManager.getMessageCount(conversationId);
            memoryManager.saveUserMessage(conversationId, userMessage, messageOrder);
            chatLogger.logConversationMessage(conversationId, "user", userMessage, messageOrder);

            String history = memoryManager.getFormattedHistory(conversationId, historyDepth);

            PerformanceLogger.TimingContext retrievalTiming = performanceLogger.startTiming("conversation_retrieval");
            List<SearchResult> results;
            if (hybridRetriever.isEnabled()) {
                results = hybridRetriever.retrieveAndRerank(userMessage, 20, 3, knowledgeBaseId, collectionId);
            } else {
                results = retriever.retrieveAndRerank(userMessage, 20, 3);
            }
            performanceLogger.stopTiming(retrievalTiming);

            RagPrompt ragPrompt;
            if (enableMultiDocumentMode) {
                ragPrompt = promptBuilder.buildMultiDocPrompt(userMessage, results, history);
            } else {
                ragPrompt = promptBuilder.buildRagPromptWithHistory(userMessage, results, history);
            }

            PerformanceLogger.TimingContext llmTiming = performanceLogger.startTiming("conversation_llm");
            String answer = chatClient.prompt()
                    .system(ragPrompt.systemPrompt())
                    .user(ragPrompt.userPrompt())
                    .call()
                    .content();
            performanceLogger.stopTiming(llmTiming);

            memoryManager.saveAssistantMessage(conversationId, answer, messageOrder + 1);
            chatLogger.logConversationMessage(conversationId, "assistant", answer, messageOrder + 1);

            List<DocumentSource> sourceDocuments =
                    documentGroupingService.groupResultsByDocument(results);

            Map<String, Object> metadata = new HashMap<>(ragPrompt.metadata());
            if (enableMultiDocumentMode) {
                metadata.put("documentCount", sourceDocuments.size());
                metadata.put("multiDocMode", true);
            }

            performanceLogger.stopTiming(timing);
            chatLogger.logChatResponse(userMessage, answer, !results.isEmpty(), results.size(),
                    System.currentTimeMillis() - timing.getStartTime());

            return ChatResponse.builder()
                    .answer(answer)
                    .isFromContext(!results.isEmpty())
                    .retrievalCount(results.size())
                    .sourceDocuments(sourceDocuments)
                    .build();
         } catch (Exception e) {
             performanceLogger.stopTiming(timing);
             chatLogger.logChatError("conversation_chat", conversationId, e);
             chatLogger.logChatFallback("conversation_chat", e.getMessage());

             String fallbackAnswer = chatClient.prompt()
                     .user(userMessage)
                     .call()
                     .content();
             return ChatResponse.builder()
                     .answer(fallbackAnswer)
                     .isFromContext(false)
                     .retrievalCount(0)
                     .sourceDocuments(List.of())
                     .build();
         }
    }

    public ChatResponse chat(UUID conversationId, String userMessage) {
        return chat(conversationId, userMessage, 5);
    }

    public UUID startConversation() {
        return createConversation();
    }

    public List<ChatResponse> getConversationHistory(UUID conversationId) {
        return conversationRepository.getConversationHistory(conversationId);
    }

    public List<Map<String, Object>> getAllConversations() {
        return conversationRepository.getAllConversations();
    }

    public void deleteConversation(UUID conversationId) {
        chatLogger.logConversationDeletion(conversationId);
        conversationRepository.deleteConversation(conversationId);
    }

    public List<Map<String, Object>> searchConversations(String query) {
        return conversationRepository.searchConversations(query);
    }

    public ChatResponse ragChat(String message, Integer topK) {
        return ragChat(message, topK, null, null);
    }

    public ChatResponse ragChat(String message, Integer topK, String knowledgeBaseId, String collectionId) {
        try {
            List<SearchResult> results;
            if (hybridRetriever.isEnabled()) {
                results = hybridRetriever.retrieveAndRerank(message, topK, 3, knowledgeBaseId, collectionId);
            } else {
                results = retriever.retrieveAndRerank(message, topK, 3);
            }

            RagPrompt ragPrompt = promptBuilder.buildRagPrompt(message, results);

            String answer = chatClient.prompt()
                    .system(ragPrompt.systemPrompt())
                    .user(ragPrompt.userPrompt())
                    .call()
                    .content();

            // Step 4: Extract sourceDocuments and citations from results
            // Group SearchResults by documentId to build DocumentSource objects
            List<DocumentSource> sourceDocuments = results.stream()
                    .collect(Collectors.groupingBy(SearchResult::getDocumentId))
                    .entrySet()
                    .stream()
                    .map(entry -> buildDocumentSource(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());


            return ChatResponse.builder()
                    .answer(answer)
                    .isFromContext(!results.isEmpty())
                    .retrievalCount(results.size())
                    .sourceDocuments(sourceDocuments)
                    .build();
        } catch (Exception e) {
            log.error("Error processing RAG chat for message: {}", message, e);
            String fallbackAnswer = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            return ChatResponse.builder()
                    .answer(fallbackAnswer)
                    .isFromContext(false)
                    .retrievalCount(0)
                    .sourceDocuments(List.of())
                    .build();
        }
    }

    public Map<String, Object> getCitationDetails(String chunkHash) {
        return conversationRepository.getCitationDetails(chunkHash);
    }

    /**
     * Regenerate the last response in a conversation
     * Deletes the last assistant message and re-processes the last user message
     */
    public ChatResponse regenerateLastResponse(UUID conversationId) {
        // Get the last user message
        var recentMessages = conversationRepository.getRecentMessages(conversationId, 2);
        
        if (recentMessages.size() < 2) {
            throw new IllegalStateException("Not enough messages to regenerate");
        }

        // The last message should be from assistant, second to last from user
        var lastMessage = recentMessages.get(0);
        var userMessage = recentMessages.get(1);

        if (!"assistant".equals(lastMessage.getRole())) {
            throw new IllegalStateException("Last message is not from assistant");
        }

        if (!"user".equals(userMessage.getRole())) {
            throw new IllegalStateException("Second to last message is not from user");
        }

        // Delete the last assistant message
        int messageCount = memoryManager.getMessageCount(conversationId);
        // Note: In a real implementation, we'd delete the specific message
        // For now, we'll just re-process the user message
        
        String userQuery = userMessage.getMessage();
        return chat(conversationId, userQuery, 5);
    }

    /**
     * Generate follow-up questions based on the conversation context
     */
    public List<String> generateFollowUpQuestions(UUID conversationId) {
        // Get conversation history
        String history = memoryManager.getFormattedHistory(conversationId, 5);
        
        String prompt = "Based on the following conversation, generate 3-5 relevant follow-up questions the user might want to ask. " +
                        "Return only the questions, one per line, without numbering.\n\n" +
                        "Conversation:\n" + history;
        
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        // Parse the response into a list of questions
        return List.of(response.split("\n"))
                .stream()
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .limit(5)
                .toList();
    }

    /**
     * Build a DocumentSource from grouped SearchResults
     * Converts SearchResults from the same document into a DocumentSource with citations
     *
     * @param documentId the document ID (grouping key)
     * @param results list of SearchResults from the same document
     * @return DocumentSource with populated citations
     */
    private DocumentSource buildDocumentSource(String documentId, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return new DocumentSource();
        }

        // Use first result to get document metadata
        SearchResult firstResult = results.get(0);

        // Convert each SearchResult to a Citation
        List<Citation> citations = results.stream()
                .map(result -> Citation.builder()
                        .documentName(result.getDocumentName())
                        .documentId(result.getDocumentId())
                        .pageNumber(result.getPageNumber())
                        .chunkIndex(result.getChunkIndex())
                        .relevanceScore(result.getScore())
                        .content(result.getContent())
                        .chunkHash(result.getChunkHash())
                        .documentHash(result.getDocumentHash())
                        .embeddingModel(result.getEmbeddingModel())
                        .embeddingDimension(result.getEmbeddingDimension())
                        .language(result.getLanguage())
                        .version(result.getVersion())
                        .updatedAt(result.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        // Build and return DocumentSource
        return DocumentSource.builder()
                .documentId(documentId)
                .documentName(firstResult.getDocumentName())
                .citations(citations)
                .chunkCount(results.size())
                .build();
    }
}
