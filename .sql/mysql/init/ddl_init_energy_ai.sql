-- 建库
CREATE DATABASE `energy_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- drop table if exists ai_knowledge_document;
-- 知识库文档表
CREATE TABLE `ai_knowledge_document`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scope_type`    VARCHAR(255) NOT NULL COMMENT '知识领域类型',
    `business_type` VARCHAR(255) NOT NULL COMMENT '业务领域类型',
    `title`         VARCHAR(255) NOT NULL COMMENT '内容标题',
    `group_id`      BIGINT       DEFAULT NULL COMMENT '内容分组id，如租户id',
    `content`       LONGTEXT     NOT NULL COMMENT '精细化文档内容(Markdown/纯文本)',
    `source_type`   VARCHAR(255) NOT NULL COMMENT '来源类型(1-文档 2-数据库 3-api 0-未知)',
    `source_path`   VARCHAR(512) DEFAULT NULL COMMENT '文件路径或API地址',
    `doc_version`   int          DEFAULT 1 COMMENT '文档版本号',
    `enable_public` TINYINT(1)   DEFAULT 1 COMMENT '是否公开',
    `loaded`        TINYINT(1)   DEFAULT 0 COMMENT '是否已加载到向量库',
    `enabled`       TINYINT(1)   DEFAULT 1 COMMENT '是否可用',
    `expired_time`  DATETIME     DEFAULT NULL COMMENT '过期时间',
    `create_user`   bigint       DEFAULT '1' COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   bigint       DEFAULT '1' COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_version_loaded` (`loaded`, `doc_version`),
    KEY `idx_document_expired_time` (`expired_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC COMMENT ='知识库文档表';

-- drop table if exists ai_context_user_record;
-- 用户对话记录表
CREATE TABLE `ai_context_user_record`
(
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chat_id`       BIGINT   NOT NULL COMMENT '对话id',
    `user_id`       BIGINT       DEFAULT NULL COMMENT '知识领域类型',
    `user_type`     TINYINT(1)   DEFAULT 1 COMMENT '1普通用户 2客户租户',
    `group_id`      BIGINT       DEFAULT NULL COMMENT '用户分组id，如租户id',
    `scope_type`    VARCHAR(255) DEFAULT NULL COMMENT '知识领域类型',
    `business_type` VARCHAR(255) DEFAULT NULL COMMENT '业务领域类型，意图分类',
    `question`      LONGTEXT NOT NULL COMMENT '用户对话问题',
    `content`       LONGTEXT     DEFAULT NULL COMMENT '用户对话输出结果',
    `enabled`       TINYINT(1)   DEFAULT 1 COMMENT '是否展示',
    `score`         int          DEFAULT NULL COMMENT '相关度反馈评分0~10，0表示不相关;5表示一般;10表示精准',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_record_chat_id` (`chat_id`),
    KEY `idx_user_record_user` (`user_id`, `user_type`),
    KEY `idx_user_record_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC COMMENT ='用户对话记录表';

-- 常规接口系统日志记录
CREATE TABLE `sys_log`
(
    `id`               bigint NOT NULL COMMENT 'ID',
    `description`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `log_type`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `method`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `params`           mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
    `request_ip`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `time`             bigint                                                        DEFAULT NULL,
    `username`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `address`          varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `browser`          varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `exception_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
    `create_time`      datetime                                                      DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `log_create_time_index` (`create_time`) USING BTREE,
    KEY `inx_log_type` (`log_type`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='系统日志';

