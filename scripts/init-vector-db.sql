-- Initialize vector database for Spring AI
CREATE EXTENSION IF NOT EXISTS vector;

-- Create schema for AI components
CREATE SCHEMA IF NOT EXISTS ai;

-- Create vector store table
CREATE TABLE IF NOT EXISTS ai.vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536)
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
ON ai.vector_store USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA ai TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ai TO postgres;