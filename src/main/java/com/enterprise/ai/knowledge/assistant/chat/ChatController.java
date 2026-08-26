package com.enterprise.ai.knowledge.assistant.chat;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.chat.dto.Citation;
import com.enterprise.ai.knowledge.assistant.chat.dto.DocumentSource;
import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import com.enterprise.ai.knowledge.assistant.conversation.dto.ConversationRequest;
import com.enterprise.ai.knowledge.assistant.conversation.service.ConversationService;
import com.enterprise.ai.knowledge.assistant.logging.AuditLogger;
import com.enterprise.ai.knowledge.assistant.logging.ChatLogger;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.rag.PromptBuilder;
import com.enterprise.ai.knowledge.assistant.rag.dto.RagPrompt;
import com.enterprise.ai.knowledge.assistant.rag.retriever.HybridRetriever;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Chat REST API Controller with Swagger Documentation
 * <p>
 * Provides endpoints for:
 * - Simple LLM chat (no RAG)
 * - RAG-enhanced chat with document context and citations
 * - Conversation management
 */
@RestController
@RequestMapping("/api/chat")
@Slf4j
@Tag(
        name = "Chat API",
        description = "Chat endpoints for simple LLM queries and RAG-enhanced context-aware responses"
)
public class ChatController {

    private final ChatClient chatClient;
    private final HybridRetriever hybridRetriever;
    private final PromptBuilder promptBuilder;
    private final ConversationService conversationService;
    private final ChatLogger chatLogger;
    private final PerformanceLogger performanceLogger;
    private final AuditLogger auditLogger;

    public ChatController(ChatClient chatClient, HybridRetriever hybridRetriever, PromptBuilder promptBuilder,
                          ConversationService conversationService, ChatLogger chatLogger, PerformanceLogger performanceLogger, AuditLogger auditLogger) {
        this.chatClient = chatClient;
        this.hybridRetriever = hybridRetriever;
        this.promptBuilder = promptBuilder;
        this.conversationService = conversationService;
        this.chatLogger = chatLogger;
        this.performanceLogger = performanceLogger;
        this.auditLogger = auditLogger;
    }

    /**
     * Simple chat endpoint (No RAG)
     * <p>
     * Sends a query directly to the LLM without retrieving document context.
     * Best for general knowledge questions.
     */
    @GetMapping
    @Operation(
            summary = "Simple Chat (No RAG)",
            description = "Send a message to the LLM without document retrieval. " +
                    "Returns plain string response with no citations.",
            tags = {"Chat API"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful response from LLM",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "string", example = "This is the LLM response.")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or LLM service unavailable"
            )
    })
    public String chat(
            @Parameter(
                    name = "message",
                    description = "User query to send to the LLM",
                    required = true,
                    example = "What is Spring Boot?"
            )
            @RequestParam String message
    ) {
        chatLogger.logSimpleChatRequest(message);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("simple_chat");
        
        try {
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            
            performanceLogger.stopTiming(timing);
            chatLogger.logChatResponse(message, response, false, 0, 
                    System.currentTimeMillis() - timing.getStartTime());
            
            return response;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            chatLogger.logChatError("simple_chat", null, e);
            throw e;
        }
    }

    /**
     * RAG-Enhanced Chat endpoint
     * <p>
     * Retrieves relevant document chunks based on query, injects them as context,
     * and sends augmented prompt to LLM for grounded answers with citations.
     */
    @GetMapping("/rag")
    @Operation(
            summary = "RAG-Enhanced Chat",
            description = "Send a query with document retrieval and context injection. " +
                    "Returns answer with citations from uploaded documents. " +
                    "This endpoint is ideal for questions about uploaded knowledge base documents. " +
                    "Optionally specify knowledgeBaseId and/or collectionId to scope retrieval.",
            tags = {"Chat API"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful RAG response with citations",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters (e.g., invalid topK)",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during retrieval or LLM call"
            )
    })
    public ChatResponse ragChat(
            @Parameter(
                    name = "message",
                    description = "User query for document-based retrieval",
                    required = true,
                    example = "What is the vacation policy?"
            )
            @RequestParam String message,

            @Parameter(
                    name = "topK",
                    description = "Number of document chunks to retrieve. Default is 5. " +
                            "Higher values provide more context but may include less relevant chunks.",
                    example = "5"
            )
            @RequestParam(name = "vectorTopK", defaultValue = "20") int vectorTopK,
            @RequestParam(name = "finalTopN", defaultValue = "5") int finalTopN,

            @Parameter(
                    name = "knowledgeBaseId",
                    description = "Knowledge base ID to scope retrieval (optional). If not provided, searches across all knowledge bases.",
                    required = false
            )
            @RequestParam(value = "knowledgeBaseId", required = false) String knowledgeBaseId,

            @Parameter(
                    name = "collectionId",
                    description = "Collection ID to scope retrieval (optional). If not provided, searches across all collections within the specified knowledge base.",
                    required = false
            )
            @RequestParam(value = "collectionId", required = false) String collectionId
    ) {
        chatLogger.logRagChatRequest(message, vectorTopK, finalTopN);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("rag_chat");

        try {
            // Step 1-4: Two-stage retrieval + re-ranking with optional knowledge base and collection scoping
            PerformanceLogger.TimingContext retrievalTiming = performanceLogger.startTiming("rag_retrieval");
            List<SearchResult> results = hybridRetriever.retrieveAndRerank(message, vectorTopK, finalTopN, knowledgeBaseId, collectionId);
            performanceLogger.stopTiming(retrievalTiming);

            // Step 2: Build RAG prompt with context (returns first-class RagPrompt object)
            RagPrompt ragPrompt = promptBuilder.buildRagPrompt(message, results);

            // Step 3: Send augmented prompt to LLM
            PerformanceLogger.TimingContext llmTiming = performanceLogger.startTiming("rag_llm_call");
            String answer = chatClient.prompt()
                    .system(ragPrompt.systemPrompt())
                    .user(ragPrompt.userPrompt())
                    .call()
                    .content();
            performanceLogger.stopTiming(llmTiming);

            // Step 4: Extract sourceDocuments and citations from results
            // Group SearchResults by documentId to build DocumentSource objects
            List<DocumentSource> sourceDocuments = results.stream()
                    .collect(Collectors.groupingBy(SearchResult::getDocumentId))
                    .entrySet()
                    .stream()
                    .map(entry -> buildDocumentSource(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            performanceLogger.stopTiming(timing);
            chatLogger.logChatResponse(message, answer, !results.isEmpty(), results.size(),
                    System.currentTimeMillis() - timing.getStartTime());

            // If no results were retrieved, provide helpful guidance
            if (results.isEmpty()) {
                answer = "No documents found in the knowledge base. Please upload documents using the document upload endpoint (POST /api/documents/upload) to enable RAG-based responses.";
            }

            return ChatResponse.builder()
                    .answer(answer)
                    .isFromContext(!results.isEmpty())
                    .retrievalCount(results.size())
                    .sourceDocuments(sourceDocuments)
                    .build();
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            chatLogger.logChatError("rag_chat", null, e);
            chatLogger.logChatFallback("rag_chat", e.getMessage());

            // Fallback to simple chat on error
            String answer = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            return ChatResponse.builder()
                    .answer(answer)
                    .isFromContext(false)
                    .retrievalCount(0)
                    .sourceDocuments(List.of())
                    .build();
        }

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

    /**
     * Start a new conversation
     * <p>
     * Creates a new conversation session for multi-turn chat.
     */
    @PostMapping("/converse/start")
    @Operation(
            summary = "Start New Conversation",
            description = "Initiate a new conversation session. Returns a conversation ID " +
                    "that can be used for subsequent messages in the conversation.",
            tags = {"Chat API"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Conversation created successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to create conversation"
            )
    })
    public Object startConversation() {
        UUID conversationId = conversationService.startConversation();
        chatLogger.logConversationStart(conversationId);
        return new java.util.HashMap<String, String>() {{
            put("conversationId", conversationId.toString());
        }};
    }



    /**
     * Conversation endpoint that starts a new conversation.
     *
     */
    @Operation(
            summary = "Continue Multi-Turn Conversation",
            description = "Send a message in an existing conversation thread with history context. " +
                    "Uses conversation history (configurable depth) to provide context for responses. " +
                    "Returns RAG-enhanced answer with citations.",
            tags = {"Chat API v1"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful response with conversation context",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatResponse.class)
                    ))
            ,
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or body"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversation not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error processing conversation or retrieving history"
            )
    })
    @Parameter(
            name = "conversationId",
            description = "Unique identifier of the conversation to continue",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )

    @Parameter(
            description = "Conversation request containing message and optional history depth"
    )

    @PostMapping("/converse")
    public ChatResponse converse(@RequestParam UUID conversationId,
                                 @Valid @RequestBody ConversationRequest request) {
        int historyDepth = request.getHistoryDepth() > 0 ? request.getHistoryDepth() : 5;
        chatLogger.logConversationWithHistory(conversationId, request.getMessage(), historyDepth);
        return conversationService.chat(conversationId, request.getMessage(), historyDepth);
    }

    /**
     * Get all conversations
     */
    @GetMapping("/conversations")
    @Operation(
            summary = "Get All Conversations",
            description = "Retrieve list of all conversations with metadata",
            tags = {"Chat API"}
    )
    public List<java.util.Map<String, Object>> getAllConversations() {
        return conversationService.getAllConversations();
    }

    /**
     * Delete a conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(
            summary = "Delete Conversation",
            description = "Delete a conversation and all its messages",
            tags = {"Chat API"}
    )
    public void deleteConversation(@PathVariable UUID conversationId) {
        chatLogger.logConversationDeletion(conversationId);
        auditLogger.logConversationDeletion(conversationId.toString(), "anonymous", true, "Conversation deleted via API");
        conversationService.deleteConversation(conversationId);
    }

    /**
     * Search conversations
     */
    @GetMapping("/conversations/search")
    @Operation(
            summary = "Search Conversations",
            description = "Search conversations by title or message content",
            tags = {"Chat API"}
    )
    public List<java.util.Map<String, Object>> searchConversations(
            @RequestParam String query) {
        return conversationService.searchConversations(query);
    }

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Streaming Chat",
            description = "Send a query and receive the response as a stream of text chunks",
            tags = {"Chat API"}
    )
    public SseEmitter streamChat(
            @Parameter(description = "User query", required = true)
            @RequestParam String message) {

        chatLogger.logStreamChatStart(message);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("stream_chat");

        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout

        // Add error handling
        emitter.onCompletion(() -> {
            performanceLogger.stopTiming(timing);
            chatLogger.logStreamChatComplete(message, System.currentTimeMillis() - timing.getStartTime());
        });

        emitter.onTimeout(() -> {
            performanceLogger.stopTiming(timing);
            chatLogger.logStreamChatError(message, new RuntimeException("Stream timeout"));
            emitter.completeWithError(new RuntimeException("Stream timeout"));
        });

        ChatResponse response = this.ragChat(message, 20, 5, null, null);

        // Tokenize (by space or custom logic)
        String[] tokens = response.getAnswer() .split("\\s+");

        int index = 0;
        try {
            for (int i = 0; i < tokens.length; i += 4) {

                int end = Math.min(i + 4, tokens.length);
                try {
                    Thread.sleep(10); // Simulate delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            String[] selected = Arrays.copyOfRange(tokens, i, end);
                int endIndex = index + String.join(" ", selected).length();




                emitter.send(
                        SseEmitter.event()
                                .name("message")
                                .data(response.getAnswer().substring(index, endIndex))
                );
                index = endIndex;
            }
            // Final completion
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("Chat completed")
            );

            emitter.complete();
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            chatLogger.logStreamChatError(message, e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Error: " + e.getMessage())
                );
            } catch (Exception ex) {
                chatLogger.logChatError("stream_chat_error", null, ex);
            }
            emitter.completeWithError(e);
        }
        return emitter;
    }






        /**
     * Regenerate the last response in a conversation
     * <p>
     * Deletes the last assistant message and regenerates the response to the last user message.
     */
    @PostMapping("/conversations/{conversationId}/regenerate")
    @Operation(
            summary = "Regenerate Response",
            description = "Regenerate the last assistant response by deleting it and re-processing the last user message",
            tags = {"Chat API"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Response regenerated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversation not found or no messages to regenerate"
            )
    })
    public ChatResponse regenerateResponse(
            @Parameter(
                    name = "conversationId",
                    description = "ID of the conversation",
                    required = true
            )
            @PathVariable UUID conversationId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        log.info("[{}] Regenerating response for conversation: {}", correlationId, conversationId);
        return conversationService.regenerateLastResponse(conversationId);
    }

    /**
     * Generate follow-up questions based on the last response
     * <p>
     * Suggests relevant follow-up questions the user might want to ask.
     */
    @PostMapping("/conversations/{conversationId}/follow-up")
    @Operation(
            summary = "Generate Follow-up Questions",
            description = "Generate suggested follow-up questions based on the conversation context",
            tags = {"Chat API"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Follow-up questions generated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversation not found"
            )
    })
    public List<String> generateFollowUpQuestions(
            @Parameter(
                    name = "conversationId",
                    description = "ID of the conversation",
                    required = true
            )
            @PathVariable UUID conversationId) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        log.info("[{}] Generating follow-up questions for conversation: {}", correlationId, conversationId);
        return conversationService.generateFollowUpQuestions(conversationId);
    }
}

