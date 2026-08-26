package com.enterprise.ai.knowledge.assistant.conversation.service;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.chat.dto.DocumentSource;
import com.enterprise.ai.knowledge.assistant.conversation.entity.ConversationMessage;
import com.enterprise.ai.knowledge.assistant.conversation.repository.ConversationRepository;
import com.enterprise.ai.knowledge.assistant.rag.PromptBuilder;
import com.enterprise.ai.knowledge.assistant.rag.Retriever;
import com.enterprise.ai.knowledge.assistant.rag.dto.RagPrompt;
import com.enterprise.ai.knowledge.assistant.rag.retriever.HybridRetriever;
import com.enterprise.ai.knowledge.assistant.rag.service.DocumentGroupingService;
import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MemoryManager memoryManager;

    @Mock
    private Retriever retriever;

    @Mock
    private HybridRetriever hybridRetriever;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private DocumentGroupingService documentGroupingService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private com.enterprise.ai.knowledge.assistant.logging.ChatLogger chatLogger;

    @Mock
    private com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger performanceLogger;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
            conversationRepository,
            memoryManager,
            retriever,
            hybridRetriever,
            promptBuilder,
            documentGroupingService,
            chatClient,
            chatLogger,
            performanceLogger
        );
        
        // Set multi-document mode to false for tests
        ReflectionTestUtils.setField(conversationService, "enableMultiDocumentMode", false);
    }

    @Test
    void testCreateConversation() {
        UUID expectedId = UUID.randomUUID();
        when(conversationRepository.createConversation(anyString())).thenReturn(expectedId);
        
        UUID result = conversationService.createConversation();
        
        assertEquals(expectedId, result);
        verify(conversationRepository, times(1)).createConversation("New Conversation");
    }

    @Test
    void testStartConversation() {
        UUID expectedId = UUID.randomUUID();
        when(conversationRepository.createConversation(anyString())).thenReturn(expectedId);
        
        UUID result = conversationService.startConversation();
        
        assertEquals(expectedId, result);
    }

    @Test
    void testChat_WithValidInput() {
        UUID conversationId = UUID.randomUUID();
        String userMessage = "Test message";
        int historyDepth = 5;
        
        when(memoryManager.getMessageCount(conversationId)).thenReturn(0);
        when(memoryManager.getFormattedHistory(conversationId, historyDepth)).thenReturn("");
        when(hybridRetriever.isEnabled()).thenReturn(true);
        when(hybridRetriever.retrieveAndRerank(userMessage, 20, 3)).thenReturn(new ArrayList<>());
        
        RagPrompt ragPrompt = new RagPrompt(
            "System prompt",
            "User prompt",
            new ArrayList<>(),
            new HashMap<>()
        );
        when(promptBuilder.buildRagPromptWithHistory(userMessage, new ArrayList<>(), "")).thenReturn(ragPrompt);
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test response");
        
        List<DocumentSource> documentSources = new ArrayList<>();
        when(documentGroupingService.groupResultsByDocument(any())).thenReturn(documentSources);
        
        ChatResponse response = conversationService.chat(conversationId, userMessage, historyDepth);
        
        assertNotNull(response);
        assertEquals("Test response", response.getAnswer());
        assertFalse(response.getIsFromContext());
        assertEquals(0, response.getRetrievalCount());
        
        verify(memoryManager, times(1)).saveUserMessage(conversationId, userMessage, 0);
        verify(memoryManager, times(1)).saveAssistantMessage(conversationId, "Test response", 1);
    }

    @Test
    void testChat_WithDefaultHistoryDepth() {
        UUID conversationId = UUID.randomUUID();
        String userMessage = "Test message";
        
        when(memoryManager.getMessageCount(conversationId)).thenReturn(0);
        when(memoryManager.getFormattedHistory(conversationId, 5)).thenReturn("");
        when(hybridRetriever.isEnabled()).thenReturn(true);
        when(hybridRetriever.retrieveAndRerank(userMessage, 20, 3)).thenReturn(new ArrayList<>());
        
        RagPrompt ragPrompt = new RagPrompt(
            "System prompt",
            "User prompt",
            new ArrayList<>(),
            new HashMap<>()
        );
        when(promptBuilder.buildRagPromptWithHistory(userMessage, new ArrayList<>(), "")).thenReturn(ragPrompt);
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test response");
        
        List<DocumentSource> documentSources = new ArrayList<>();
        when(documentGroupingService.groupResultsByDocument(any())).thenReturn(documentSources);
        
        ChatResponse response = conversationService.chat(conversationId, userMessage);
        
        assertNotNull(response);
        assertEquals("Test response", response.getAnswer());
        
        verify(memoryManager, times(1)).saveUserMessage(conversationId, userMessage, 0);
    }

    @Test
    void testChat_WithException_FallbackToSimpleChat() {
        UUID conversationId = UUID.randomUUID();
        String userMessage = "Test message";
        
        when(memoryManager.getMessageCount(conversationId)).thenThrow(new RuntimeException("Test error"));
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Fallback response");
        
        ChatResponse response = conversationService.chat(conversationId, userMessage);
        
        assertNotNull(response);
        assertEquals("Fallback response", response.getAnswer());
        assertFalse(response.getIsFromContext());
        assertEquals(0, response.getRetrievalCount());
    }

    @Test
    void testGetAllConversations() {
        List<Map<String, Object>> expectedConversations = new ArrayList<>();
        Map<String, Object> conv1 = new HashMap<>();
        conv1.put("id", UUID.randomUUID().toString());
        conv1.put("title", "Conversation 1");
        expectedConversations.add(conv1);
        
        when(conversationRepository.getAllConversations()).thenReturn(expectedConversations);
        
        List<Map<String, Object>> result = conversationService.getAllConversations();
        
        assertEquals(expectedConversations, result);
        verify(conversationRepository, times(1)).getAllConversations();
    }

    @Test
    void testDeleteConversation() {
        UUID conversationId = UUID.randomUUID();
        
        conversationService.deleteConversation(conversationId);
        
        verify(conversationRepository, times(1)).deleteConversation(conversationId);
    }

    @Test
    void testSearchConversations() {
        String query = "test query";
        List<Map<String, Object>> expectedResults = new ArrayList<>();
        
        when(conversationRepository.searchConversations(query)).thenReturn(expectedResults);
        
        List<Map<String, Object>> result = conversationService.searchConversations(query);
        
        assertEquals(expectedResults, result);
        verify(conversationRepository, times(1)).searchConversations(query);
    }

    @Test
    void testRagChat_WithResults() {
        String message = "Test query";
        int topK = 5;
        
        List<SearchResult> results = new ArrayList<>();
        SearchResult result = new SearchResult(
                "Test content",
                0.95,
                1,
                "test.pdf",
                0,
                "doc-1",
                "hash-1",
                "chunk-hash-1",
                "text-embedding-3-small",
                1536,
                "en",
                1,
                java.time.Instant.now()
        );
        results.add(result);
        
        when(retriever.retrieveAndRerank(message, topK, 3)).thenReturn(results);
        
        RagPrompt ragPrompt = new RagPrompt(
                "System prompt",
                "User prompt",
                results,
                new HashMap<>()
        );
        when(promptBuilder.buildRagPrompt(message, results)).thenReturn(ragPrompt);
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("RAG response");
        
        ChatResponse response = conversationService.ragChat(message, topK);
        
        assertNotNull(response);
        assertEquals("RAG response", response.getAnswer());
        assertTrue(response.getIsFromContext());
        assertEquals(1, response.getRetrievalCount());
        assertNotNull(response.getSourceDocuments());
    }

    @Test
    void testRagChat_WithException_FallbackToSimpleChat() {
        String message = "Test query";
        int topK = 5;
        
        when(retriever.retrieveAndRerank(message, topK, 3)).thenThrow(new RuntimeException("Test error"));
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Fallback response");
        
        ChatResponse response = conversationService.ragChat(message, topK);
        
        assertNotNull(response);
        assertEquals("Fallback response", response.getAnswer());
        assertFalse(response.getIsFromContext());
        assertEquals(0, response.getRetrievalCount());
    }

    @Test
    void testRegenerateLastResponse_WithValidConversation() {
        UUID conversationId = UUID.randomUUID();
        
        List<ConversationMessage> recentMessages = new ArrayList<>();
        ConversationMessage assistantMessage = new ConversationMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setMessage("Old response");
        recentMessages.add(assistantMessage);
        
        ConversationMessage userMessage = new ConversationMessage();
        userMessage.setRole("user");
        userMessage.setMessage("User query");
        recentMessages.add(userMessage);
        
        when(conversationRepository.getRecentMessages(conversationId, 2)).thenReturn(recentMessages);
        when(memoryManager.getMessageCount(conversationId)).thenReturn(2);
        
        when(memoryManager.getFormattedHistory(conversationId, 5)).thenReturn("");
        when(hybridRetriever.isEnabled()).thenReturn(true);
        when(hybridRetriever.retrieveAndRerank("User query", 20, 3)).thenReturn(new ArrayList<>());
        
        RagPrompt ragPrompt = new RagPrompt(
            "System prompt",
            "User prompt",
            new ArrayList<>(),
            new HashMap<>()
        );
        when(promptBuilder.buildRagPromptWithHistory("User query", new ArrayList<>(), "")).thenReturn(ragPrompt);
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("New response");
        
        List<DocumentSource> documentSources = new ArrayList<>();
        when(documentGroupingService.groupResultsByDocument(any())).thenReturn(documentSources);
        
        ChatResponse response = conversationService.regenerateLastResponse(conversationId);
        
        assertNotNull(response);
        assertEquals("New response", response.getAnswer());
    }

    @Test
    void testRegenerateLastResponse_WithInsufficientMessages() {
        UUID conversationId = UUID.randomUUID();
        
        when(conversationRepository.getRecentMessages(conversationId, 2)).thenReturn(new ArrayList<>());
        
        assertThrows(IllegalStateException.class, () -> conversationService.regenerateLastResponse(conversationId));
    }

    @Test
    void testGenerateFollowUpQuestions() {
        UUID conversationId = UUID.randomUUID();
        
        when(memoryManager.getFormattedHistory(conversationId, 5)).thenReturn("User: What is the policy?\nAssistant: The policy is...");
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Question 1\nQuestion 2\nQuestion 3");
        
        List<String> questions = conversationService.generateFollowUpQuestions(conversationId);
        
        assertNotNull(questions);
        assertEquals(3, questions.size());
        assertTrue(questions.contains("Question 1"));
        assertTrue(questions.contains("Question 2"));
        assertTrue(questions.contains("Question 3"));
    }

    @Test
    void testGetCitationDetails() {
        String chunkHash = "test-hash";
        Map<String, Object> expectedDetails = new HashMap<>();
        expectedDetails.put("content", "Test content");
        
        when(conversationRepository.getCitationDetails(chunkHash)).thenReturn(expectedDetails);
        
        Map<String, Object> result = conversationService.getCitationDetails(chunkHash);
        
        assertEquals(expectedDetails, result);
        verify(conversationRepository, times(1)).getCitationDetails(chunkHash);
    }
}
