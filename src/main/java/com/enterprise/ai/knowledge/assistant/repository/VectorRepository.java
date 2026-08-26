package com.enterprise.ai.knowledge.assistant.repository;

import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.vector.entity.ChunkEntity;
import java.util.List;

/**
 * Repository abstraction for vector store persistence operations.
 * Implementations may use Postgres (pgvector), Pinecone, Qdrant, etc.
 */
public interface VectorRepository {

	/** Ensure any required schema / extensions exist. */
	void ensureTable();

	/** Insert a chunk (with embedding and metadata) into the vector store. */
	void insertChunk(ChunkEntity chunk);

	/** Find the k nearest chunks to the provided query vector. */
	List<SearchResult> findNearest(float[] query, int k);

	/** Return true if a chunk with the provided content hash already exists. */
	boolean existsByHash(String hash);

	/** Delete all chunks associated with the given document ID. */
	void deleteByDocumentId(String documentId);

	/** List all unique document IDs and their metadata. */
	List<DocumentMetadata> listDocuments();
}
