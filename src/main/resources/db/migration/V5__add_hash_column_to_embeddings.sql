-- Add missing 'hash' column to embeddings table
ALTER TABLE embeddings ADD COLUMN IF NOT EXISTS hash VARCHAR(255);
