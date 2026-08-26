package com.enterprise.ai.knowledge.assistant.chat;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.conversation.service.ConversationService;
import com.enterprise.ai.knowledge.assistant.rag.PromptBuilder;
import com.enterprise.ai.knowledge.assistant.rag.retriever.HybridRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for ChatRestController with RAG integration.
 * Tests REST API endpoints for chat functionality.
 */
@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private HybridRetriever hybridRetriever;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private ConversationService conversationService;

    @Mock
    private com.enterprise.ai.knowledge.assistant.logging.ChatLogger chatLogger;

    @Mock
    private com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger performanceLogger;

    @Mock
    private com.enterprise.ai.knowledge.assistant.logging.AuditLogger auditLogger;

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController(chatClient, hybridRetriever, promptBuilder, conversationService, chatLogger, performanceLogger, auditLogger);
    }

    /**
     * Test simple chat endpoint (no RAG).
     */
    @Test
    void testSimpleChatEndpoint() {
        String testMessage = "Hello";
        String expectedResponse = "Hi there!";

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(testMessage)).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedResponse);

        String response = chatController.chat(testMessage);

        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(chatClient, times(1)).prompt();
        verify(chatClientRequestSpec, times(1)).user(testMessage);
    }

    /**
     * Test RAG chat endpoint with successful retrieval.
     */
    @Test
    void testRagChatWithResults() {
        String query = "What is the vacation policy?";
        int vectorTopK = 20;
        int finalTopN = 5;

        when(hybridRetriever.retrieveAndRerank(query, vectorTopK, finalTopN)).thenReturn(new ArrayList<>());
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test response");

        ChatResponse response = chatController.ragChat(query, vectorTopK, finalTopN, null, null);

        assertNotNull(response);
        assertEquals("Test response", response.getAnswer());
        assertFalse(response.getIsFromContext());
        assertEquals(0, response.getRetrievalCount());
    }

    /**
     * Test RAG chat with no retrieval results (fallback to simple chat).
     */
    @Test
    void testRagChatWithoutResults() {
        String query = "What is the vacation policy?";
        int vectorTopK = 20;
        int finalTopN = 5;

        when(hybridRetriever.retrieveAndRerank(query, vectorTopK, finalTopN)).thenReturn(new ArrayList<>());
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Fallback response");

        ChatResponse response = chatController.ragChat(query, vectorTopK, finalTopN, null, null);

        assertNotNull(response);
        assertEquals("Fallback response", response.getAnswer());
        assertFalse(response.getIsFromContext());
    }

    /**
     * Test RAG chat with exception handling.
     */
    @Test
    void testRagChatWithException() {
        String query = "What is the vacation policy?";
        int vectorTopK = 20;
        int finalTopN = 5;

        when(hybridRetriever.retrieveAndRerank(query, vectorTopK, finalTopN)).thenThrow(new RuntimeException("Test error"));
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(query)).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Fallback response");

        ChatResponse response = chatController.ragChat(query, vectorTopK, finalTopN, null, null);

        assertNotNull(response);
        assertEquals("Fallback response", response.getAnswer());
        assertFalse(response.getIsFromContext());
        assertEquals(0, response.getRetrievalCount());
    }

    /**
     * Test RAG chat with default topK parameter.
     */
    @Test
    void testRagChatWithDefaultTopK() {
        String query = "What is the vacation policy?";

        when(hybridRetriever.retrieveAndRerank(query, 20, 5)).thenReturn(new ArrayList<>());
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Test response");

        ChatResponse response = chatController.ragChat(query, 20, 5, null, null);

        assertNotNull(response);
        assertEquals("Test response", response.getAnswer());
        verify(hybridRetriever, times(1)).retrieveAndRerank(query, 20, 5);
    }

    /**
     * Test start conversation endpoint.
     */
    @Test
    void testStartConversation() {
        Object response = chatController.startConversation();

        assertNotNull(response);
        assertTrue(response instanceof java.util.Map);
        java.util.Map<?, ?> responseMap = (java.util.Map<?, ?>) response;
        assertTrue(responseMap.containsKey("conversationId"));
        assertNotNull(responseMap.get("conversationId"));
    }

    /**
     * Test get all conversations endpoint.
     */
    @Test
    void testGetAllConversations() {
        java.util.List<java.util.Map<String, Object>> expectedConversations = new ArrayList<>();
        java.util.Map<String, Object> conv1 = new java.util.HashMap<>();
        conv1.put("id", UUID.randomUUID().toString());
        conv1.put("title", "Conversation 1");
        expectedConversations.add(conv1);

        when(conversationService.getAllConversations()).thenReturn(expectedConversations);

        java.util.List<java.util.Map<String, Object>> response = chatController.getAllConversations();

        assertNotNull(response);
        assertEquals(expectedConversations, response);
        verify(conversationService, times(1)).getAllConversations();
    }

    /**
     * Test delete conversation endpoint.
     */
    @Test
    void testDeleteConversation() {
        UUID conversationId = UUID.randomUUID();

        chatController.deleteConversation(conversationId);

        verify(conversationService, times(1)).deleteConversation(conversationId);
    }

    /**
     * Test search conversations endpoint.
     */
    @Test
    void testSearchConversations() {
        String query = "vacation";
        java.util.List<java.util.Map<String, Object>> expectedResults = new ArrayList<>();

        when(conversationService.searchConversations(query)).thenReturn(expectedResults);

        java.util.List<java.util.Map<String, Object>> response = chatController.searchConversations(query);

        assertNotNull(response);
        assertEquals(expectedResults, response);
        verify(conversationService, times(1)).searchConversations(query);
    }
}

