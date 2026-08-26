-- Complete Database Schema for Enterprise AI Knowledge Assistant
-- This migration consolidates V1, V2, and V3 into a single schema

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Knowledge bases table (no dependencies)
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_name ON knowledge_bases(name);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_created ON knowledge_bases(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_updated ON knowledge_bases(updated_at DESC);

-- Collections table (depends on knowledge_bases)
CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY,
    knowledge_base_id UUID REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_collections_knowledge_base_id ON collections(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_collections_kb_id ON collections(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_collections_kb_name ON collections(knowledge_base_id, name);
CREATE INDEX IF NOT EXISTS idx_collections_created ON collections(created_at DESC);

-- Conversations table for multi-turn chat
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_conversations_created_at ON conversations(created_at);
CREATE INDEX IF NOT EXISTS idx_conversations_created ON conversations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_updated ON conversations(updated_at DESC);

-- Conversation messages table
CREATE TABLE IF NOT EXISTS conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    message TEXT NOT NULL,
    message_order INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_conversation_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_messages_order ON conversation_messages(conversation_id, message_order);
CREATE INDEX IF NOT EXISTS idx_conversation_messages_id ON conversation_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_messages_conv_order ON conversation_messages(conversation_id, message_order DESC);

-- Document versions table for tracking document changes
CREATE TABLE IF NOT EXISTS document_versions (
    id UUID PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    version_number INTEGER NOT NULL,
    chunk_count INTEGER DEFAULT 0,
    embedding_model VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_versions_document_id ON document_versions(document_id);
CREATE INDEX IF NOT EXISTS idx_document_versions_active ON document_versions(is_active);
CREATE INDEX IF NOT EXISTS idx_document_versions_doc_id ON document_versions(document_id);
CREATE INDEX IF NOT EXISTS idx_document_versions_version ON document_versions(document_id, version_number);
CREATE INDEX IF NOT EXISTS idx_document_versions_doc_version ON document_versions(document_id, version_number DESC);
CREATE INDEX IF NOT EXISTS idx_document_versions_active_only ON document_versions(document_id) WHERE is_active = true;

-- Evaluation tests table with enhanced fields
CREATE TABLE IF NOT EXISTS evaluation_tests (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    query TEXT NOT NULL,
    expected_chunk_ids TEXT[] NOT NULL,
    category VARCHAR(50),
    language VARCHAR(20),
    difficulty VARCHAR(20),
    document_scope VARCHAR(50),
    expected_answer TEXT,
    key_points TEXT[],
    expected_documents UUID[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_category 
    CHECK (category IN ('FACTUAL', 'CONCEPTUAL', 'COMPARATIVE', 'NUMERICAL', 'MULTI_HOP')),
    CONSTRAINT chk_language 
    CHECK (language IN ('ENGLISH', 'TELUGU', 'MIXED')),
    CONSTRAINT chk_difficulty 
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_document_scope 
    CHECK (document_scope IN ('single_doc', 'multi_doc', 'cross_chapter'))
);

CREATE INDEX IF NOT EXISTS idx_evaluation_tests_created ON evaluation_tests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_tests_category ON evaluation_tests(category);
CREATE INDEX IF NOT EXISTS idx_evaluation_tests_language ON evaluation_tests(language);
CREATE INDEX IF NOT EXISTS idx_evaluation_tests_difficulty ON evaluation_tests(difficulty);
CREATE INDEX IF NOT EXISTS idx_evaluation_tests_scope ON evaluation_tests(document_scope);

-- Evaluation runs table
CREATE TABLE IF NOT EXISTS evaluation_runs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_evaluation_runs_started ON evaluation_runs(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_runs_status ON evaluation_runs(status);

-- Evaluation results table for RAG evaluation
CREATE TABLE IF NOT EXISTS evaluation_results (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES evaluation_runs(id) ON DELETE CASCADE,
    test_id UUID NOT NULL REFERENCES evaluation_tests(id) ON DELETE CASCADE,
    query TEXT NOT NULL,
    expected_answer TEXT,
    actual_answer TEXT,
    relevance_score DECIMAL(5,4),
    faithfulness_score DECIMAL(5,4),
    context_retrieved BOOLEAN,
    retrieval_count INTEGER,
    retrieved_chunk_ids TEXT[] NOT NULL,
    metrics JSONB DEFAULT '{}'::jsonb,
    latency_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluation_results_run_id ON evaluation_results(run_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_results_test_id ON evaluation_results(test_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_results_scores ON evaluation_results(relevance_score, faithfulness_score);
CREATE INDEX IF NOT EXISTS idx_evaluation_results_created ON evaluation_results(created_at DESC);

-- Search metrics table
CREATE TABLE IF NOT EXISTS search_metrics (
    id UUID PRIMARY KEY,
    query TEXT,
    vector_score NUMERIC,
    keyword_score NUMERIC,
    fusion_score NUMERIC,
    retrieval_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_search_metrics_created ON search_metrics(created_at DESC);

-- Document metadata table with knowledge base and collection support
CREATE TABLE IF NOT EXISTS document_metadata (
    id UUID PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL UNIQUE,
    document_name VARCHAR(255) NOT NULL,
    document_hash VARCHAR(255),
    chunk_count INTEGER DEFAULT 0,
    file_size BIGINT,
    pages INTEGER,
    characters INTEGER,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    indexed_at TIMESTAMP,
    knowledge_base_id UUID,
    collection_id UUID,
    CONSTRAINT fk_document_metadata_kb 
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE SET NULL,
    CONSTRAINT fk_document_metadata_collection 
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_document_metadata_kb ON document_metadata(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_metadata_collection ON document_metadata(collection_id);

-- Junction table for multi-collection document membership
CREATE TABLE IF NOT EXISTS document_collections (
    id UUID PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    collection_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_collections_collection 
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    CONSTRAINT uk_document_collection UNIQUE(document_id, collection_id)
);

CREATE INDEX IF NOT EXISTS idx_document_collections_doc ON document_collections(document_id);
CREATE INDEX IF NOT EXISTS idx_document_collections_coll ON document_collections(collection_id);

-- Embeddings table for storing document chunks and vectors (depends on collections and knowledge_bases)
CREATE TABLE IF NOT EXISTS embeddings (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    embedding vector(768),
    document_id VARCHAR(255),
    document_name VARCHAR(255),
    page_number INTEGER,
    chunk_index INTEGER,
    chunk_hash VARCHAR(255),
    document_hash VARCHAR(255),
    hash VARCHAR(255),
    embedding_model VARCHAR(255),
    embedding_dimension INTEGER,
    language VARCHAR(10),
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    knowledge_base_id UUID,
    collection_id UUID,
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    CONSTRAINT fk_embeddings_collection
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL,
    CONSTRAINT fk_embeddings_kb
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE SET NULL
);

-- Create indexes for embeddings
CREATE INDEX IF NOT EXISTS idx_embeddings_document_id ON embeddings(document_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_document_hash ON embeddings(document_hash);
CREATE INDEX IF NOT EXISTS idx_embeddings_chunk_hash ON embeddings(chunk_hash);
CREATE INDEX IF NOT EXISTS idx_embeddings_created_at ON embeddings(created_at);
CREATE INDEX IF NOT EXISTS idx_embeddings_document_created ON embeddings(document_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_embeddings_fts ON embeddings USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_embedding_model ON embeddings(embedding_model);
CREATE INDEX IF NOT EXISTS idx_language ON embeddings(language);
CREATE INDEX IF NOT EXISTS idx_version ON embeddings(version);
CREATE INDEX IF NOT EXISTS idx_updated_at ON embeddings(updated_at);
CREATE INDEX IF NOT EXISTS idx_doc_version ON embeddings(document_id, version);
CREATE INDEX IF NOT EXISTS idx_model_dimension ON embeddings(embedding_model, embedding_dimension);
CREATE INDEX IF NOT EXISTS idx_embeddings_kb_id ON embeddings(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_collection_id ON embeddings(collection_id);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to automatically update updated_at
DROP TRIGGER IF EXISTS update_embeddings_updated_at ON embeddings;
CREATE TRIGGER update_embeddings_updated_at BEFORE UPDATE ON embeddings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_conversations_updated_at ON conversations;
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_collections_updated_at ON collections;
CREATE TRIGGER update_collections_updated_at BEFORE UPDATE ON collections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default knowledge base
INSERT INTO knowledge_bases (id, name, description, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Default Knowledge Base',
    'Default knowledge base for documents without specified knowledge base',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Insert default collection under default knowledge base
INSERT INTO collections (id, knowledge_base_id, name, description, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Default Collection',
    'Default collection for documents without specified collection',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Add comments for documentation
COMMENT ON COLUMN evaluation_tests.category IS 'Test category: FACTUAL, CONCEPTUAL, COMPARATIVE, NUMERICAL, MULTI_HOP';
COMMENT ON COLUMN evaluation_tests.language IS 'Document language: ENGLISH, TELUGU, MIXED';
COMMENT ON COLUMN evaluation_tests.difficulty IS 'Difficulty level: EASY, MEDIUM, HARD';
COMMENT ON COLUMN evaluation_tests.document_scope IS 'Document scope: single_doc, multi_doc, cross_chapter';
COMMENT ON COLUMN evaluation_tests.expected_answer IS 'Expected answer text for answer quality evaluation';
COMMENT ON COLUMN evaluation_tests.key_points IS 'Key points that should be included in the answer';
COMMENT ON COLUMN evaluation_tests.expected_documents IS 'Expected document IDs for retrieval evaluation';
COMMENT ON COLUMN document_metadata.knowledge_base_id IS 'Associated knowledge base ID (nullable)';
COMMENT ON COLUMN document_metadata.collection_id IS 'Primary collection ID for UI display (nullable)';
COMMENT ON COLUMN embeddings.collection_id IS 'Collection ID for RAG query scoping (nullable)';
COMMENT ON TABLE document_collections IS 'Junction table for multi-collection document membership';
COMMENT ON COLUMN document_collections.document_id IS 'Document ID from document_metadata table';
COMMENT ON COLUMN document_collections.collection_id IS 'Collection ID from collections table';
