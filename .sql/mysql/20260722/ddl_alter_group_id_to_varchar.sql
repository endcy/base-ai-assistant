-- groupId 类型变更: BIGINT → VARCHAR(64)
-- 变更原因: 知识分组ID需要支持非数字类型的分组标识
-- 执行前请确认: 1.备份数据库 2.确认无活跃事务 3.确认外键/索引依赖
-- @author endcy
-- @since 2026/07/22

-- 1. 修改 ai_knowledge_document 表的 group_id 列类型
--    由于存在 UNIQUE KEY uk_document_group_doc_id (group_id, doc_id)，需要先删除再重建
ALTER TABLE ai_knowledge_document
    DROP KEY uk_document_group_doc_id;

ALTER TABLE ai_knowledge_document
    MODIFY COLUMN group_id VARCHAR(64) DEFAULT NULL COMMENT '内容分组id，如租户id';

ALTER TABLE ai_knowledge_document
    ADD UNIQUE KEY uk_document_group_doc_id (group_id, doc_id);

-- 2. 修改 ai_context_user_record 表的 group_id 列类型
ALTER TABLE ai_context_user_record
    MODIFY COLUMN group_id VARCHAR(64) DEFAULT NULL COMMENT '用户分组id，如租户id';
