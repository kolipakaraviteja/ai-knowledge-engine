package com.enterprise.ai.knowledge.assistant.document.service;

import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.document.dto.ParsedDocument;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class MetadataExtractor {

    public DocumentMetadata extract(String documentName, ParsedDocument parsedDocument, Path filePath) {
        String lower = documentName == null ? "" : documentName.toLowerCase();
        String extension = "";
        int dot = lower.lastIndexOf('.');
        if (dot >= 0) {
            extension = lower.substring(dot + 1);
        }

        long fileSize = 0L;
        try {
            if (filePath != null && Files.exists(filePath)) {
                fileSize = Files.size(filePath);
            }
        } catch (Exception e) {
            fileSize = 0L;
        }

        String text = parsedDocument == null ? null : parsedDocument.text();

        return new DocumentMetadata(
                null, // documentId - generated later
                documentName,
                null, // documentHash - generated later
                0, // chunkCount - set during ingestion
                fileSize,
                parsedDocument == null ? 0 : parsedDocument.pageCount(),
                text == null ? 0 : text.length(),
                Instant.now(), // uploadedAt
                null, // indexedAt - set during indexing
                null, // knowledgeBaseId - set during ingestion
                null // collectionId - set during ingestion
        );
    }

    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        
        // Simple language detection based on character patterns
        int nonAsciiCount = 0;
        int totalChars = Math.min(text.length(), 1000);
        
        for (int i = 0; i < totalChars; i++) {
            char ch = text.charAt(i);
            if (ch > 127) {
                nonAsciiCount++;
            }
        }
        
        double ratio = (double) nonAsciiCount / totalChars;
        
        if (ratio > 0.3) {
            return "unknown";
        }
        
        // Check for common language patterns
        if (text.contains("the ") || text.contains("and ") || text.contains("is ")) {
            return "en";
        }
        
        return "unknown";
    }

    public String extractExtension(String documentName) {
        if (documentName == null) {
            return "";
        }
        String lower = documentName.toLowerCase();
        int dot = lower.lastIndexOf('.');
        if (dot >= 0) {
            return lower.substring(dot + 1);
        }
        return "";
    }
}
