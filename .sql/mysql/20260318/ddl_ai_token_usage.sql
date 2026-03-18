-- Token 用量统计表
-- 用于记录每次 AI 请求的 token 消耗情况
-- Author: AI Assistant
-- Date: 2026-03-18

CREATE TABLE `ai_token_usage`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chat_id`         BIGINT       NOT NULL COMMENT '对话 id',
    `user_id`         BIGINT       DEFAULT NULL COMMENT '用户 id',
    `user_type`       TINYINT(1)   DEFAULT 1 COMMENT '用户类型：1-普通用户 2-客户租户',
    `group_id`        BIGINT       DEFAULT NULL COMMENT '用户分组 id，如租户 id',
    `scope_type`      VARCHAR(255) DEFAULT NULL COMMENT '知识领域类型',
    `business_type`   VARCHAR(255) DEFAULT NULL COMMENT '业务领域类型',
    `question`        VARCHAR(512) DEFAULT NULL COMMENT '用户问题摘要',
    `model_name`      VARCHAR(128) NOT NULL COMMENT '使用的模型名称',
    `input_tokens`    INT          DEFAULT 0 COMMENT '输入 token 数量',
    `output_tokens`   INT          DEFAULT 0 COMMENT '输出 token 数量',
    `total_tokens`    INT          DEFAULT 0 COMMENT '总 token 数量',
    `api_type`        VARCHAR(64)  DEFAULT NULL COMMENT 'API 类型：chat/embedding/rerank',
    `request_cost_ms` BIGINT       DEFAULT 0 COMMENT '请求耗时 (毫秒)',
    `status`          TINYINT(1)   DEFAULT 1 COMMENT '请求状态：0-失败 1-成功',
    `error_message`   VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `request_date`    DATE         NOT NULL COMMENT '请求日期 (用于分区查询)',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_token_usage_chat_id` (`chat_id`),
    KEY `idx_token_usage_user` (`user_id`, `user_type`),
    KEY `idx_token_usage_request_date` (`request_date`),
    KEY `idx_token_usage_model` (`model_name`),
    KEY `idx_token_usage_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='AI Token 用量统计表';

-- 对话记录表增强字段
-- 为 ai_context_user_record 表添加 token 相关字段
ALTER TABLE `ai_context_user_record`
    ADD COLUMN `input_tokens`    INT          DEFAULT 0 COMMENT '输入 token 数量' AFTER `score`,
    ADD COLUMN `output_tokens`   INT          DEFAULT 0 COMMENT '输出 token 数量' AFTER `input_tokens`,
    ADD COLUMN `total_tokens`    INT          DEFAULT 0 COMMENT '总 token 数量' AFTER `output_tokens`,
    ADD COLUMN `model_name`      VARCHAR(128) DEFAULT NULL COMMENT '使用的模型名称' AFTER `total_tokens`,
    ADD COLUMN `request_cost_ms` BIGINT       DEFAULT 0 COMMENT '请求耗时 (毫秒)' AFTER `model_name`;

-- 创建 Token 用量统计视图 (按日统计)
CREATE OR REPLACE VIEW `v_daily_token_usage` AS
SELECT request_date,
       model_name,
       scope_type,
       business_type,
       COUNT(*)             AS request_count,
       SUM(input_tokens)    AS total_input_tokens,
       SUM(output_tokens)   AS total_output_tokens,
       SUM(total_tokens)    AS grand_total_tokens,
       AVG(request_cost_ms) AS avg_request_cost_ms
FROM ai_token_usage
GROUP BY request_date, model_name, scope_type, business_type;

-- 创建 Token 用量统计视图 (按用户统计)
CREATE OR REPLACE VIEW `v_user_token_usage` AS
SELECT user_id,
       user_type,
       group_id,
       COUNT(*)          AS request_count,
       SUM(total_tokens) AS total_tokens,
       MAX(create_time)  AS last_request_time
FROM ai_token_usage
GROUP BY user_id, user_type, group_id;
