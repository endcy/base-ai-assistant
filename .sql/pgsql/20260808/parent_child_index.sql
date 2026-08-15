-- =============================================================================
-- PGVector: Parent-Child Index 支持
-- 双级切分：child chunk 用于精确检索，parent chunk 用于提供完整上下文
-- 在 PGVector 的 vector_store 表上增加 parent_id 字段
-- @author endcy
-- @since 2026/08/08
-- =============================================================================

-- 1. 检查并添加 parent_id 字段
DO $$
BEGIN IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vector_store' AND column_name = 'parent_id'
    ) THEN
ALTER TABLE vector_store
    ADD COLUMN parent_id UUID;
RAISE NOTICE 'Added parent_id column to vector_store';
ELSE
        RAISE NOTICE 'parent_id column already exists';
END IF;
END $$;

-- 2. 检查并添加 chunk_level 字段（PARENT / CHILD）
DO $$
BEGIN IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vector_store' AND column_name = 'chunk_level'
    ) THEN
ALTER TABLE vector_store
    ADD COLUMN chunk_level VARCHAR(10) DEFAULT 'CHILD';
RAISE NOTICE 'Added chunk_level column to vector_store';
ELSE
        RAISE NOTICE 'chunk_level column already exists';
END IF;
END $$;

-- 3. 添加 parent_id 索引（用于 child → parent 查找）
CREATE INDEX IF NOT EXISTS idx_vector_store_parent_id ON vector_store(parent_id);

-- 4. 添加 chunk_level 索引
CREATE INDEX IF NOT EXISTS idx_vector_store_chunk_level ON vector_store(chunk_level);

-- 说明：
-- 改造后 vector_store 表新增两个字段：
--   parent_id  UUID        — 指向 parent chunk 的 id；PARENT 记录此字段为 NULL
--   chunk_level VARCHAR(10) — 'PARENT' 或 'CHILD'，默认 'CHILD'
--
-- 检索时：用 CHILD chunk 做向量相似度检索（精确命中）
-- 回填时：通过 parent_id 查到 PARENT chunk，把完整上下文返回给 LLM
