package com.enterprise.ai.knowledge.assistant.document.service;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.document.dto.DocumentUploadResponse;
import com.enterprise.ai.knowledge.assistant.document.dto.ParsedDocument;
import com.enterprise.ai.knowledge.assistant.document.dto.PdfChunk;
import com.enterprise.ai.knowledge.assistant.document.parser.DocumentParser;
import com.enterprise.ai.knowledge.assistant.document.parser.DocumentParserRegistry;
import com.enterprise.ai.knowledge.assistant.embedding.dto.EmbeddingResult;
import com.enterprise.ai.knowledge.assistant.embedding.service.EmbeddingService;
import com.enterprise.ai.knowledge.assistant.knowledge.repository.DocumentCollectionRepository;
import com.enterprise.ai.knowledge.assistant.knowledge.service.DefaultKnowledgeService;
import com.enterprise.ai.knowledge.assistant.knowledge.service.DocumentMetadataService;
import com.enterprise.ai.knowledge.assistant.knowledge.service.DocumentVersionService;
import com.enterprise.ai.knowledge.assistant.logging.DocumentLogger;
import com.enterprise.ai.knowledge.assistant.logging.PerformanceLogger;
import com.enterprise.ai.knowledge.assistant.vector.entity.ChunkEntity;
import com.enterprise.ai.knowledge.assistant.vector.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentIngestionOrchestrator {

    private static final int CHUNK_SIZE = 1000;

    private final DocumentParserRegistry parserRegistry;
    private final DocumentChunkService chunkService;
    private final MetadataExtractor metadataExtractor;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentMetadataService documentMetadataService;
    private final DocumentVersionService documentVersionService;
    private final DocumentCollectionRepository documentCollectionRepository;
    private final DocumentLogger documentLogger;
    private final PerformanceLogger performanceLogger;
    private final DefaultKnowledgeService defaultKnowledgeService;

    public DocumentIngestionOrchestrator(DocumentParserRegistry parserRegistry,
                                         DocumentChunkService chunkService,
                                         MetadataExtractor metadataExtractor,
                                         EmbeddingService embeddingService,
                                         VectorStoreService vectorStoreService,
                                         DocumentMetadataService documentMetadataService,
                                         DocumentVersionService documentVersionService,
                                         DocumentCollectionRepository documentCollectionRepository,
                                         DocumentLogger documentLogger,
                                         PerformanceLogger performanceLogger,
                                         DefaultKnowledgeService defaultKnowledgeService) {
        this.parserRegistry = parserRegistry;
        this.chunkService = chunkService;
        this.metadataExtractor = metadataExtractor;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.documentMetadataService = documentMetadataService;
        this.documentVersionService = documentVersionService;
        this.documentCollectionRepository = documentCollectionRepository;
        this.documentLogger = documentLogger;
        this.performanceLogger = performanceLogger;
        this.defaultKnowledgeService = defaultKnowledgeService;
    }

    public DocumentUploadResponse ingest(Path filePath, String documentName) throws IOException {
        return ingest(filePath, documentName, null, null);
    }

    public DocumentUploadResponse ingest(Path filePath, String documentName, String knowledgeBaseId, String collectionId) throws IOException {
        long fileSize = Files.size(filePath);
        String fileType = getFileExtension(documentName);
        documentLogger.logIngestionStart(documentName, fileSize, fileType);
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("document_ingestion");

        try {
            // Ensure default knowledge base and collection exist
            defaultKnowledgeService.initializeDefaults();

            // Resolve knowledge base and collection IDs with defaults
            String resolvedKnowledgeBaseId = knowledgeBaseId;
            String resolvedCollectionId = collectionId;

            if (resolvedKnowledgeBaseId == null) {
                resolvedKnowledgeBaseId = defaultKnowledgeService.getDefaultKnowledgeBaseId().toString();
                log.debug("Using default knowledge base: {}", resolvedKnowledgeBaseId);
            }

            if (resolvedCollectionId == null) {
                resolvedCollectionId = defaultKnowledgeService.getDefaultCollectionId().toString();
                log.debug("Using default collection: {}", resolvedCollectionId);
            }

            DocumentParser parser = parserRegistry.resolve(documentName);
            ParsedDocument parsed = parser.parse(filePath);
            DocumentMetadata metadata = metadataExtractor.extract(documentName, parsed, filePath);

            // Generate document ID and hash (Phase 4: Enhanced Metadata)
            String documentId = UUID.randomUUID().toString();
            String documentHash = sha256Hex(parsed.text());

            // Check if document with same hash already exists
            if (documentMetadataService.existsByDocumentHash(documentHash)) {
                documentLogger.logDuplicateDocument(documentName, documentHash);
                performanceLogger.stopTiming(timing);

                DocumentUploadResponse response = new DocumentUploadResponse();
                response.setDocumentName(documentName);
                response.setUploadSuccess(false);
                response.setText("Document already exists");
                return response;
            }

            // Update metadata with generated IDs and collection info
            DocumentMetadata updatedMetadata = new DocumentMetadata(
                    documentId,
                    metadata.documentName(),
                    documentHash,
                    metadata.chunkCount(),
                    metadata.fileSize(),
                    metadata.pages(),
                    metadata.characters(),
                    metadata.uploadedAt(),
                    metadata.indexedAt(),
                    resolvedKnowledgeBaseId,
                    resolvedCollectionId
            );

            // Save initial metadata
            documentMetadataService.save(updatedMetadata);

            if (parsed.pageAware()) {
                return ingestPdf(filePath, updatedMetadata, documentId, documentHash, resolvedKnowledgeBaseId, resolvedCollectionId, timing);
            }
            return ingestText(filePath, updatedMetadata, documentId, documentHash, resolvedKnowledgeBaseId, resolvedCollectionId, parsed, timing);
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            documentLogger.logDocumentError("ingestion", documentName, e);
            throw e;
        }
    }

    private DocumentUploadResponse ingestText(Path filePath, DocumentMetadata metadata,
                                             String documentId, String documentHash, String knowledgeBaseId, String collectionId,
                                             ParsedDocument parsed,
                                             PerformanceLogger.TimingContext parentTiming) throws IOException {
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("text_ingestion");

        try {
            String text = parsed.text();
            if (text == null) {
                text = Files.readString(filePath, StandardCharsets.UTF_8);
            }

            documentLogger.logParsingStart(metadata.documentName(), "text");
            PerformanceLogger.TimingContext chunkingTiming = performanceLogger.startTiming("text_chunking");
            List<String> chunkList = chunkService.chunkText(text, CHUNK_SIZE, DocumentChunkService.DEFAULT_OVERLAP);
            performanceLogger.stopTiming(chunkingTiming);
            documentLogger.logChunkingComplete(metadata.documentName(), chunkList.size(),
                    System.currentTimeMillis() - chunkingTiming.getStartTime());

            int chunksCreated = persistTextChunks(metadata, documentId, documentHash, knowledgeBaseId, collectionId, chunkList);

            // Fail if no chunks were stored
            if (chunksCreated == 0 && !chunkList.isEmpty()) {
                throw new IllegalStateException("Failed to store any chunks. Check database schema and embedding service.");
            }

            // Update metadata with chunk count
            documentMetadataService.updateChunkCount(documentId, chunkList.size());
            documentMetadataService.markAsIndexed(documentId);

            // Create document version
            Integer versionNumber = documentVersionService.getNextVersionNumber(documentId);
            documentVersionService.createDocumentVersion(documentId, metadata.documentName(), versionNumber, chunkList.size(), embeddingService.getModelName());

            performanceLogger.stopTiming(timing);
            documentLogger.logIngestionComplete(metadata.documentName(), documentId, documentHash,
                    metadata.pages(), chunkList.size(), System.currentTimeMillis() - parentTiming.getStartTime());

            DocumentUploadResponse response = new DocumentUploadResponse();
            response.setDocumentName(metadata.documentName());
            response.setFileName(metadata.documentName());
            response.setDocumentId(documentId);
            response.setPages(metadata.pages());
            response.setCharacters(metadata.characters());
            response.setChunks(chunkList.size());
            response.setChunksCreated(chunksCreated);
            response.setText("");
            response.setUploadSuccess(true);
            response.setChunkContents(chunkList);
            return response;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            documentLogger.logDocumentError("text_ingestion", metadata.documentName(), e);
            throw e;
        }
    }

    private DocumentUploadResponse ingestPdf(Path filePath, DocumentMetadata metadata,
                                            String documentId, String documentHash, String knowledgeBaseId, String collectionId,
                                            PerformanceLogger.TimingContext parentTiming) throws IOException {
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("pdf_ingestion");

        try (PDDocument pdf = PDDocument.load(filePath.toFile())) {
            documentLogger.logParsingStart(metadata.documentName(), "pdf");
            PerformanceLogger.TimingContext chunkingTiming = performanceLogger.startTiming("pdf_chunking");
            List<PdfChunk> chunkList = chunkService.chunkPDFText(pdf, CHUNK_SIZE, DocumentChunkService.DEFAULT_OVERLAP);
            performanceLogger.stopTiming(chunkingTiming);
            documentLogger.logChunkingComplete(metadata.documentName(), chunkList.size(),
                    System.currentTimeMillis() - chunkingTiming.getStartTime());

            int chunksCreated = persistPdfChunks(metadata, documentId, documentHash, knowledgeBaseId, collectionId, chunkList);

            // Fail if no chunks were stored
            if (chunksCreated == 0 && !chunkList.isEmpty()) {
                throw new IllegalStateException("Failed to store any chunks. Check database schema and embedding service.");
            }

            // Update metadata with chunk count
            documentMetadataService.updateChunkCount(documentId, chunkList.size());
            documentMetadataService.markAsIndexed(documentId);

            // Create document version
            Integer versionNumber = documentVersionService.getNextVersionNumber(documentId);
            documentVersionService.createDocumentVersion(documentId, metadata.documentName(), versionNumber, chunkList.size(), embeddingService.getModelName());

            performanceLogger.stopTiming(timing);
            documentLogger.logIngestionComplete(metadata.documentName(), documentId, documentHash,
                    metadata.pages(), chunkList.size(), System.currentTimeMillis() - parentTiming.getStartTime());

            DocumentUploadResponse response = new DocumentUploadResponse();
            response.setDocumentName(metadata.documentName());
            response.setFileName(metadata.documentName());
            response.setDocumentId(documentId);
            response.setPages(metadata.pages());
            response.setCharacters(metadata.characters());
            response.setFileSize(metadata.fileSize());
            response.setChunks(chunkList.size());
            response.setChunksCreated(chunksCreated);
            response.setText("");
            response.setUploadSuccess(true);
            response.setChunkContents(Collections.emptyList());
            return response;
        } catch (Exception e) {
            performanceLogger.stopTiming(timing);
            documentLogger.logDocumentError("pdf_ingestion", metadata.documentName(), e);
            throw e;
        }
    }

    private int persistTextChunks(DocumentMetadata metadata, String documentId, String documentHash, String collectionId, List<String> chunkList) {
        return persistTextChunks(metadata, documentId, documentHash, metadata.knowledgeBaseId(), collectionId, chunkList);
    }

    private int persistTextChunks(DocumentMetadata metadata, String documentId, String documentHash, String knowledgeBaseId, String collectionId, List<String> chunkList) {
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("text_chunk_persistence");
        int successCount = 0;
        int failureCount = 0;
        int idx = 0;
        UUID collectionUuid = collectionId != null ? UUID.fromString(collectionId) : null;
        UUID knowledgeBaseUuid = knowledgeBaseId != null ? UUID.fromString(knowledgeBaseId) : null;

        for (String chunk : chunkList) {
            try {
                EmbeddingResult embedding = embeddingService.generateEmbedding(chunk);
                if (embedding == null || embedding.vector() == null) {
                    failureCount++;
                    idx++;
                    continue;
                }
                String chunkHash = sha256Hex(chunk);
                if (vectorStoreService.existsByHash(chunkHash)) {
                    documentLogger.logDuplicateChunk(chunkHash, documentId);
                    idx++;
                    continue;
                }

                String language = detectLanguage(chunk);
                Instant now = Instant.now();

                ChunkEntity entity = new ChunkEntity(
                        UUID.randomUUID(),
                        metadata.documentName(),
                        documentId,
                        documentHash,
                        chunkHash,
                        1,
                        idx,
                        chunk,
                        embedding.vector(),
                        embedding.model(),
                        embedding.dimensions(),
                        language,
                        1,
                        now,
                        now,
                        chunkHash,
                        collectionUuid,
                        knowledgeBaseUuid
                );
                vectorStoreService.storeChunk(entity);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String correlationId = CorrelationIdUtil.getCorrelationId();
                log.error("[{}] Error storing chunk for document: {}, chunk index: {}", correlationId, metadata.documentName(), idx, e);
            }
            idx++;
        }

        performanceLogger.stopTiming(timing);
        documentLogger.logEmbeddingGeneration(metadata.documentName(), chunkList.size(), successCount, failureCount,
                System.currentTimeMillis() - timing.getStartTime());
        documentLogger.logChunkStorage(documentId, successCount, chunkList.size() - successCount - failureCount,
                System.currentTimeMillis() - timing.getStartTime());

        // Associate document with collection in junction table
        if (collectionUuid != null) {
            documentCollectionRepository.associateDocumentWithCollection(documentId, collectionUuid);
        }

        return successCount;
    }

    private int persistPdfChunks(DocumentMetadata metadata, String documentId, String documentHash, String collectionId, List<PdfChunk> chunkList) {
        return persistPdfChunks(metadata, documentId, documentHash, metadata.knowledgeBaseId(), collectionId, chunkList);
    }

    private int persistPdfChunks(DocumentMetadata metadata, String documentId, String documentHash, String knowledgeBaseId, String collectionId, List<PdfChunk> chunkList) {
        PerformanceLogger.TimingContext timing = performanceLogger.startTiming("pdf_chunk_persistence");
        int successCount = 0;
        int failureCount = 0;
        UUID collectionUuid = collectionId != null ? UUID.fromString(collectionId) : null;
        UUID knowledgeBaseUuid = knowledgeBaseId != null ? UUID.fromString(knowledgeBaseId) : null;

        for (PdfChunk pdfChunk : chunkList) {
            try {
                EmbeddingResult embedding = embeddingService.generateEmbedding(pdfChunk.text());
                if (embedding == null || embedding.vector() == null) {
                    failureCount++;
                    continue;
                }
                String chunkHash = sha256Hex(pdfChunk.text());
                if (vectorStoreService.existsByHash(chunkHash)) {
                    documentLogger.logDuplicateChunk(chunkHash, documentId);
                    continue;
                }

                String language = detectLanguage(pdfChunk.text());
                Instant now = Instant.now();

                ChunkEntity entity = new ChunkEntity(
                        UUID.randomUUID(),
                        metadata.documentName(),
                        documentId,
                        documentHash,
                        chunkHash,
                        pdfChunk.pageNumber(),
                        pdfChunk.chunkIndex(),
                        pdfChunk.text(),
                        embedding.vector(),
                        embedding.model(),
                        embedding.dimensions(),
                        language,
                        1,
                        now,
                        now,
                        chunkHash,
                        collectionUuid,
                        knowledgeBaseUuid
                );
                vectorStoreService.storeChunk(entity);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String correlationId = CorrelationIdUtil.getCorrelationId();
                log.error("[{}] Error storing PDF chunk for document: {}", correlationId, metadata.documentName(), e);
            }
        }

        performanceLogger.stopTiming(timing);
        documentLogger.logEmbeddingGeneration(metadata.documentName(), chunkList.size(), successCount, failureCount,
                System.currentTimeMillis() - timing.getStartTime());
        documentLogger.logChunkStorage(documentId, successCount, chunkList.size() - successCount - failureCount,
                System.currentTimeMillis() - timing.getStartTime());

        // Associate document with collection in junction table
        if (collectionUuid != null) {
            documentCollectionRepository.associateDocumentWithCollection(documentId, collectionUuid);
        }

        return successCount;
    }

    private String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        return text.chars().anyMatch(ch -> ch > 127) ? "unknown" : "en";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            log.warn("[{}] SHA-256 algorithm not available, using fallback hash", correlationId, e);
            return Integer.toHexString(input.hashCode());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unknown";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "unknown";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }
}
