-- pgsql创建库energy_ai
CREATE DATABASE energy_ai
    WITH OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

-- energy_ai库启用pgVector
CREATE
EXTENSION IF NOT EXISTS vector;
CREATE
EXTENSION IF NOT EXISTS hstore;
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- 建知识库向量数据表
DROP TABLE IF EXISTS vector_store;
CREATE TABLE vector_store
(
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content      TEXT    NOT NULL,
    metadata     json    NOT NULL,
    embedding vector(1024) NOT NULL
);

-- 加注释
COMMENT ON COLUMN public.vector_store."content" IS '原始文本内容';
COMMENT ON COLUMN public.vector_store.metadata IS '元数据';
COMMENT ON COLUMN public.vector_store.embedding IS '1024维向量';
COMMENT ON TABLE public.vector_store IS '知识库向量数据表';

-- 创建HNSW索引（加速相似度搜索）
CREATE INDEX idx_vector_store_embedding_hnsw ON vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
-- 常用元数据查询索引
CREATE INDEX idx_vector_store_doc_id ON vector_store ((metadata->>'id'));
CREATE INDEX idx_vector_store_doc_group ON vector_store ((metadata->>'groupId'));
CREATE INDEX idx_vector_store_scope_type ON vector_store (
    (metadata->>'scopeType'),
    (metadata->>'businessType')
    );
