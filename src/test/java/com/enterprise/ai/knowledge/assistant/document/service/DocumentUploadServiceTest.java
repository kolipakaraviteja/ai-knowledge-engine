package com.enterprise.ai.knowledge.assistant.document.service;

import com.enterprise.ai.knowledge.assistant.document.dto.DocumentUploadResponse;
import com.enterprise.ai.knowledge.assistant.knowledge.service.DefaultKnowledgeService;
import com.enterprise.ai.knowledge.assistant.repository.VectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private DocumentIngestionOrchestrator ingestionOrchestrator;

    @Mock
    private VectorRepository vectorRepository;

    @Mock
    private DefaultKnowledgeService defaultKnowledgeService;

    private DocumentUploadService documentUploadService;

    @BeforeEach
    void setUp() {
        documentUploadService = new DocumentUploadService(ingestionOrchestrator, vectorRepository, defaultKnowledgeService);
    }

    @Test
    void testUploadDocument_NullFile() throws IOException {
        MultipartFile file = null;
        
        DocumentUploadResponse response = documentUploadService.uploadDocument(file);
        
        assertNotNull(response);
        assertFalse(response.isUploadSuccess());
        assertEquals(0, response.getChunks());
        assertEquals(0, response.getPages());
        assertEquals(0, response.getCharacters());
        assertTrue(response.getChunkContents().isEmpty());
    }

    @Test
    void testUploadDocument_EmptyFile() throws IOException {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        
        DocumentUploadResponse response = documentUploadService.uploadDocument(file);
        
        assertNotNull(response);
        assertFalse(response.isUploadSuccess());
        assertEquals(0, response.getChunks());
        assertEquals(0, response.getPages());
        assertEquals(0, response.getCharacters());
        assertTrue(response.getChunkContents().isEmpty());
    }

    @Test
    void testUploadDocument_ValidFile() throws IOException {
        // Create a temporary test file
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "Test content".getBytes());
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            "test.txt", 
            "text/plain", 
            Files.readAllBytes(tempFile)
        );
        
        DocumentUploadResponse expectedResponse = new DocumentUploadResponse();
        expectedResponse.setDocumentName("test.txt");
        expectedResponse.setUploadSuccess(true);
        expectedResponse.setChunks(1);
        expectedResponse.setPages(1);
        expectedResponse.setCharacters(12);
        
        when(ingestionOrchestrator.ingest(any(java.nio.file.Path.class), anyString())).thenReturn(expectedResponse);
        
        DocumentUploadResponse response = documentUploadService.uploadDocument(file);
        
        assertNotNull(response);
        assertTrue(response.isUploadSuccess());
        assertEquals("test.txt", response.getDocumentName());
        assertEquals(1, response.getChunks());
        
        // Clean up
        Files.deleteIfExists(tempFile);
        
        verify(ingestionOrchestrator, times(1)).ingest(any(java.nio.file.Path.class), anyString());
    }

    @Test
    void testUploadDocument_UnsupportedFileType() throws IOException {
        MultipartFile file = new MockMultipartFile(
            "file", 
            "test.xyz", 
            "application/octet-stream", 
            "Test content".getBytes()
        );
        
        when(ingestionOrchestrator.ingest(any(java.nio.file.Path.class), anyString()))
            .thenThrow(new IllegalArgumentException("Unsupported file type"));
        
        DocumentUploadResponse response = documentUploadService.uploadDocument(file);
        
        assertNotNull(response);
        assertFalse(response.isUploadSuccess());
        assertEquals(0, response.getChunks());
        
        verify(ingestionOrchestrator, times(1)).ingest(any(java.nio.file.Path.class), anyString());
    }

    @Test
    void testListDocuments() {
        // This test verifies that the method calls the repository
        documentUploadService.listDocuments();
        
        verify(vectorRepository, times(1)).listDocuments();
    }

    @Test
    void testDeleteDocument() {
        String documentId = "test-doc-id";
        
        documentUploadService.deleteDocument(documentId);
        
        verify(vectorRepository, times(1)).deleteByDocumentId(documentId);
    }

    @Test
    void testReindexDocument() {
        String documentId = "test-doc-id";
        
        // Currently this is a TODO method, so we just verify it doesn't throw
        assertDoesNotThrow(() -> documentUploadService.reindexDocument(documentId));
    }

    @Test
    void testGetDocumentMetadata() {
        String documentId = "test-doc-id";
        
        // Currently returns mock data, just verify it doesn't throw
        var metadata = documentUploadService.getDocumentMetadata(documentId);
        
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("fileName"));
        assertTrue(metadata.containsKey("fileSize"));
        assertTrue(metadata.containsKey("chunkCount"));
    }

    @Test
    void testSave_NullFile() throws IOException {
        DocumentUploadResponse response = documentUploadService.save(null);
        
        assertNotNull(response);
        assertFalse(response.isUploadSuccess());
        assertNull(response.getDocumentName());
    }

    @Test
    void testSave_EmptyFile() throws IOException {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        
        DocumentUploadResponse response = documentUploadService.save(file);
        
        assertNotNull(response);
        assertFalse(response.isUploadSuccess());
        assertNotNull(response.getDocumentName());
    }
}
