package com.enterprise.ai.knowledge.assistant.document.service;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.document.dto.DocumentUploadResponse;
import com.enterprise.ai.knowledge.assistant.knowledge.service.DefaultKnowledgeService;
import com.enterprise.ai.knowledge.assistant.repository.VectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class DocumentUploadService {

	private final DocumentIngestionOrchestrator ingestionOrchestrator;
	private final VectorRepository vectorRepository;
	private final DefaultKnowledgeService defaultKnowledgeService;

	public DocumentUploadService(DocumentIngestionOrchestrator ingestionOrchestrator, VectorRepository vectorRepository,
	                              DefaultKnowledgeService defaultKnowledgeService) {
		this.ingestionOrchestrator = ingestionOrchestrator;
		this.vectorRepository = vectorRepository;
		this.defaultKnowledgeService = defaultKnowledgeService;
	}

	private static final Path DEFAULT_UPLOAD_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "enterprise-ai-uploads");

	static {
		try {
			Files.createDirectories(DEFAULT_UPLOAD_DIR);
		} catch (IOException e) {
			log.warn("Failed to create upload directory: {}", DEFAULT_UPLOAD_DIR, e);
		}
	}

	public DocumentUploadResponse uploadDocument(MultipartFile file) throws IOException {
		return save(file, null, null);
	}

	public DocumentUploadResponse uploadDocument(MultipartFile file, String knowledgeBaseId, String collectionId) throws IOException {
		return save(file, knowledgeBaseId, collectionId);
	}

	public DocumentUploadResponse save(MultipartFile file) throws IOException {
		return save(file, null, null);
	}

	public DocumentUploadResponse save(MultipartFile file, String knowledgeBaseId, String collectionId) throws IOException {
		if (file == null || file.isEmpty()) {
			DocumentUploadResponse response = new DocumentUploadResponse();
			response.setDocumentName(file == null ? null : file.getOriginalFilename());
			response.setPages(0);
			response.setCharacters(0);
			response.setChunks(0);
			response.setText("");
			response.setUploadSuccess(false);
			response.setChunkContents(Collections.emptyList());
			return response;
		}

		String originalFilename = file.getOriginalFilename();
		Path destination = DEFAULT_UPLOAD_DIR.resolve(System.currentTimeMillis() + "-" + (originalFilename == null ? "upload" : originalFilename));
		Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

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

		try {
			return ingestionOrchestrator.ingest(destination, originalFilename == null ? destination.getFileName().toString() : originalFilename,
					resolvedKnowledgeBaseId, resolvedCollectionId);
		} catch (IllegalArgumentException ex) {
			String correlationId = CorrelationIdUtil.getCorrelationId();
			log.warn("[{}] Unsupported document type: {}", correlationId, originalFilename, ex);
			// Unsupported type - return minimal response
			DocumentUploadResponse response = new DocumentUploadResponse();
			response.setDocumentName(destination.getFileName().toString());
			response.setPages(0);
			response.setCharacters(0);
			response.setChunks(0);
			response.setText("");
			response.setUploadSuccess(false);
			response.setChunkContents(Collections.emptyList());
			return response;
		}
	}

	public List<DocumentMetadata> listDocuments() {
		log.info("Listing documents from database");
		return vectorRepository.listDocuments();
	}

	public void deleteDocument(String id) {
		log.info("Deleting document: {}", id);
		vectorRepository.deleteByDocumentId(id);
	}

	public void reindexDocument(String id) {
		log.info("Reindexing document: {}", id);
		// TODO: Implement document re-indexing
	}

	public Map<String, Object> getDocumentMetadata(String id) {
		log.info("Getting metadata for document: {}", id);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("fileName", "sample-document.pdf");
		metadata.put("fileSize", 1024000);
		metadata.put("chunkCount", 25);
		metadata.put("embeddingCount", 25);
		metadata.put("uploadedAt", new Date());
		metadata.put("indexedAt", new Date());
		return metadata;
	}
}
