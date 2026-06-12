-- ============================================================
-- 知识库文档表结构变更
-- 新增 doc_id 字段（外部业务系统文档ID）、索引、source_type 默认值
-- ============================================================

-- 1. 新增 doc_id 字段
ALTER TABLE `ai_knowledge_document`
    ADD COLUMN `doc_id` BIGINT NOT NULL DEFAULT 0 COMMENT '业务系统知识库文档id' AFTER `title`;

-- 2. 新增 doc_id 索引
ALTER TABLE `ai_knowledge_document`
    ADD KEY `idx_document_doc_id` (`doc_id`);

-- 3. source_type 改为允许默认值
ALTER TABLE `ai_knowledge_document`
    MODIFY COLUMN `source_type` VARCHAR(255) DEFAULT '1' NULL COMMENT '来源类型(1-文档 2-数据库 3-api 0-未知)';
