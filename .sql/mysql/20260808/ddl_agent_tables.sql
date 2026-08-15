-- =============================================================================
-- Agent 执行过程持久化 + 审计 + Prompt 版本 DDL
-- 执行顺序：先此文件（MySQL），再 pgsql/parent_child_index.sql（PGVector）
-- @author endcy
-- @since 2026/08/08
-- =============================================================================

-- 1. 智能体会话表
CREATE TABLE IF NOT EXISTS `ai_agent_session`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`              VARCHAR(64)     NOT NULL COMMENT '执行器内部 UUID',
    `chat_id`                 BIGINT          NULL COMMENT '业务会话 ID',
    `user_id`                 VARCHAR(64)     NULL,
    `tenant_id`               VARCHAR(64)     NULL COMMENT '租户 ID（group_id）',
    `mode`                    VARCHAR(32)     NOT NULL COMMENT 'SINGLE_SHOT/AGENTIC/PLAN_AND_ACT',
    `status`                  VARCHAR(32)     NOT NULL COMMENT 'INITIALIZED/RUNNING/...',
    `user_question`           TEXT            NOT NULL,
    `final_answer`            LONGTEXT        NULL,
    `error_message`           TEXT            NULL,
    `total_prompt_tokens`     INT             NOT NULL DEFAULT 0,
    `total_completion_tokens` INT             NOT NULL DEFAULT 0,
    `current_step`            INT             NOT NULL DEFAULT 0,
    `started_at`              DATETIME        NOT NULL,
    `completed_at`            DATETIME        NULL,
    `create_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_chat_id` (`chat_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='智能体会话表';

-- 2. 智能体思考过程表
CREATE TABLE IF NOT EXISTS `ai_agent_thought`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`        VARCHAR(64)     NOT NULL,
    `step_index`        INT             NOT NULL,
    `thought`           TEXT            NULL COMMENT '思考内容',
    `tool_calls`        TEXT            NULL COMMENT '工具调用 JSON',
    `tool_results`      TEXT            NULL COMMENT '工具结果 JSON',
    `duration_ms`       BIGINT          NOT NULL DEFAULT 0,
    `prompt_tokens`     INT             NOT NULL DEFAULT 0,
    `completion_tokens` INT             NOT NULL DEFAULT 0,
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_session_step` (`session_id`, `step_index`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='智能体思考过程表';

-- 3. LLM 调用审计表
CREATE TABLE IF NOT EXISTS `ai_llm_call_audit`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`        VARCHAR(64)     NULL,
    `tenant_id`         VARCHAR(64)     NULL,
    `user_id`           VARCHAR(64)     NULL,
    `provider`          VARCHAR(32)     NOT NULL,
    `model`             VARCHAR(64)     NOT NULL,
    `prompt_tokens`     INT             NOT NULL DEFAULT 0,
    `completion_tokens` INT             NOT NULL DEFAULT 0,
    `total_tokens`      INT             NOT NULL DEFAULT 0,
    `latency_ms`        BIGINT          NOT NULL DEFAULT 0,
    `ttft_ms`           BIGINT          NULL COMMENT '首 token 时间',
    `is_streaming`      TINYINT(1)      NOT NULL DEFAULT 0,
    `status`            VARCHAR(16)     NOT NULL COMMENT 'SUCCESS/RATE_LIMITED/...',
    `error_message`     TEXT            NULL,
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_model` (`provider`, `model`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='LLM 调用审计表';

-- 4. 工具调用审计表
CREATE TABLE IF NOT EXISTS `ai_tool_call_audit`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NULL,
    `tenant_id`     VARCHAR(64)     NULL,
    `user_id`       VARCHAR(64)     NULL,
    `tool_name`     VARCHAR(128)    NOT NULL,
    `arguments`     TEXT            NULL,
    `result`        LONGTEXT        NULL,
    `duration_ms`   BIGINT          NOT NULL DEFAULT 0,
    `status`        VARCHAR(16)     NOT NULL COMMENT 'SUCCESS/ERROR',
    `error_message` TEXT            NULL,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_tool_name` (`tool_name`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工具调用审计表';

-- 5. Prompt 版本表
CREATE TABLE IF NOT EXISTS `ai_prompt_version`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(128)    NOT NULL COMMENT '模板名',
    `version`      INT             NOT NULL,
    `content`      TEXT            NOT NULL,
    `content_hash` VARCHAR(64)     NOT NULL COMMENT 'SHA-256',
    `active`       TINYINT(1)      NOT NULL DEFAULT 1,
    `created_by`   VARCHAR(64)     NULL,
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_version` (`name`, `version`),
    KEY `idx_active` (`active`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Prompt 版本表';
