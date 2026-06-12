-- 安装jieba分词插件
-- 增加响应分词列和触发器，更新已有数据
DROP
EXTENSION IF EXISTS pg_jieba CASCADE;
create
extension pg_jieba;

ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS content_tsvector tsvector;

CREATE INDEX IF NOT EXISTS idx_vector_store_content_tsvector
    ON vector_store USING GIN (content_tsvector);


-- 创建触发器或更新现有数据，确保新数据插入或更新时， content_tsvector 列会自动填充
CREATE
OR
REPLACE FUNCTION vector_store_tsvector_trigger() RETURNS trigger AS $$
BEGIN NEW.content_tsvector = to_tsvector('jiebacfg', COALESCE(NEW.content, ''));
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_vector_store_tsvector ON vector_store;
CREATE TRIGGER update_vector_store_tsvector
    BEFORE INSERT OR UPDATE OF content ON vector_store
    FOR EACH ROW
EXECUTE FUNCTION vector_store_tsvector_trigger();

UPDATE vector_store
SET content_tsvector = to_tsvector('jiebacfg', COALESCE(content, ''));
