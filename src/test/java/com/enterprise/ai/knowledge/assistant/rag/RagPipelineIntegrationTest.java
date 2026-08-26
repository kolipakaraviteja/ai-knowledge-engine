package com.enterprise.ai.knowledge.assistant.rag;

import com.enterprise.ai.knowledge.assistant.conversation.service.ConversationService;
import com.enterprise.ai.knowledge.assistant.document.service.DocumentUploadService;
import com.enterprise.ai.knowledge.assistant.repository.VectorRepository;
import com.enterprise.ai.knowledge.assistant.rag.retriever.HybridRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete RAG pipeline.
 * Tests document ingestion, embedding generation, retrieval, re-ranking, and response generation.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.llm.provider=lmstudio",
    "spring.ai.openai.base-url=http://localhost:1234",
    "spring.ai.openai.api-key=test-key",
    "app.rag.enableHybridSearch=true",
    "app.rag.enableQueryRewriting=true",
    "app.rag.enableContextCompression=true"
})
class RagPipelineIntegrationTest {

    @Autowired
    private DocumentUploadService documentUploadService;

    @Autowired
    private VectorRepository vectorRepository;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        vectorRepository.ensureTable();
    }

    @Test
    void testDocumentIngestion() throws IOException {
        String testContent = "This is a test document about vacation policy. " +
                "Employees receive 20 days of paid time off annually. " +
                "Sick leave is separate from vacation days.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-policy.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        var response = documentUploadService.uploadDocument(file);

        assertNotNull(response);
        assertTrue(response.isUploadSuccess());
        assertNotNull(response.getDocumentName());
        assertTrue(response.getChunks() > 0);
    }

    @Test
    void testVectorRetrieval() throws IOException {
        // First ingest a document
        String testContent = "The company provides health insurance to all full-time employees. " +
                "Dental and vision coverage are included in the standard plan.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "benefits.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        documentUploadService.uploadDocument(file);

        // Test retrieval
        var results = hybridRetriever.retrieve("health insurance", 5);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> 
                r.getContent().toLowerCase().contains("health") ||
                r.getContent().toLowerCase().contains("insurance")
        ));
    }

    @Test
    void testHybridRetrievalWithReranking() throws IOException {
        String testContent = "Remote work policy: Employees can work from home up to 3 days per week. " +
                "Manager approval is required for extended remote work periods.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "remote-work.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        documentUploadService.uploadDocument(file);

        // Test hybrid retrieval with re-ranking
        var results = hybridRetriever.retrieveAndRerank("work from home", 10, 3);

        assertNotNull(results);
        assertTrue(results.size() <= 3); // Should be limited to finalTopN
        
        // Results should be sorted by relevance
        if (results.size() > 1) {
            assertTrue(results.get(0).getScore() <= results.get(1).getScore());
        }
    }

    @Test
    void testConversationWithRag() throws IOException {
        String testContent = "Quarterly reviews are conducted in March, June, September, and December. " +
                "Employees receive performance feedback from their managers.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "reviews.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        documentUploadService.uploadDocument(file);

        // Create a conversation
        var conversationId = conversationService.createConversation();
        assertNotNull(conversationId);

        // Send a RAG-enabled message
        var response = conversationService.chat(conversationId, "When are quarterly reviews conducted?", 5);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertTrue(response.getRetrievalCount() > 0 || !response.getAnswer().isEmpty());
    }

    @Test
    void testDocumentListing() throws IOException {
        // Clear existing documents by listing first
        var initialDocs = documentUploadService.listDocuments();
        initialDocs.forEach(doc -> documentUploadService.deleteDocument(doc.documentId()));

        // Ingest test documents
        String content1 = "Document 1 content for testing.";
        String content2 = "Document 2 content for testing.";

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "doc1.txt", "text/plain", content1.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "doc2.txt", "text/plain", content2.getBytes(StandardCharsets.UTF_8)
        );

        documentUploadService.uploadDocument(file1);
        documentUploadService.uploadDocument(file2);

        // List documents
        var documents = documentUploadService.listDocuments();

        assertNotNull(documents);
        assertTrue(documents.size() >= 2);
    }

    @Test
    void testDocumentDeletion() throws IOException {
        String testContent = "This document will be deleted.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        documentUploadService.uploadDocument(file);

        var documentsBefore = documentUploadService.listDocuments();
        var docToDelete = documentsBefore.stream()
                .filter(d -> d.documentName().equals("to-delete.txt"))
                .findFirst();

        if (docToDelete.isPresent()) {
            documentUploadService.deleteDocument(docToDelete.get().documentId());

            var documentsAfter = documentUploadService.listDocuments();
            var deleted = documentsAfter.stream()
                    .noneMatch(d -> d.documentId().equals(docToDelete.get().documentId()));

            assertTrue(deleted);
        }
    }

    @Test
    void testConversationSearch() {
        // Create test conversations
        var conv1 = conversationService.createConversation();
        var conv2 = conversationService.createConversation();

        // Add messages to conversations
        conversationService.chat(conv1, "What is the vacation policy?", 5);
        conversationService.chat(conv2, "How do I request sick leave?", 5);

        // Search for conversations
        var results = conversationService.searchConversations("vacation");

        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }
}
