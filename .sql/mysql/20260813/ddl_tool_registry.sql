-- =============================================================================
-- DDL：动态工具注册 + 工具组管理
-- @author endcy
-- @since 2026/08/13
-- =============================================================================

-- 1. 工具注册表（启动时从 ToolRegistry 自动同步）
CREATE TABLE IF NOT EXISTS ai_tool
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tool_name   VARCHAR(128) NOT NULL COMMENT '工具唯一标识（对应 @Tool 方法名或 MCP 工具名）',
    cn_name     VARCHAR(128) NULL COMMENT '工具中文名',
    description VARCHAR(512) NULL COMMENT '工具描述（LLM 可见）',
    source      VARCHAR(16)  NOT NULL DEFAULT 'CODE' COMMENT '来源：CODE / MCP',
    mcp_server  VARCHAR(64)  NULL COMMENT 'MCP 来源时填 server 名（与 @McpServer 注解值对应）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tool_name (tool_name),
    INDEX idx_source (source),
    INDEX idx_mcp_server (mcp_server)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '工具注册表';

-- 2. 工具组表（含 MCP 自动组 + 用户自定义组）
CREATE TABLE IF NOT EXISTS ai_tool_group
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_name  VARCHAR(64)  NOT NULL COMMENT '工具组唯一标识',
    cn_name     VARCHAR(128) NULL COMMENT '工具组中文名',
    description VARCHAR(512) NULL COMMENT '描述',
    group_type  VARCHAR(16)  NOT NULL DEFAULT 'CUSTOM' COMMENT '类型：MCP（自动组，只读） / CUSTOM（用户自定义，可编辑）',
    mcp_server  VARCHAR(64)  NULL COMMENT 'MCP 类型时填 server 名，与 ai_tool.mcp_server 自动匹配成员',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_name (group_name),
    INDEX idx_group_type (group_type),
    INDEX idx_mcp_server (mcp_server)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '工具组表';

-- 3. 工具组成员表（仅 CUSTOM 组使用；MCP 组按 mcp_server 自动匹配，不走这表）
CREATE TABLE IF NOT EXISTS ai_tool_group_member
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id    BIGINT   NOT NULL COMMENT '工具组 ID',
    tool_id     BIGINT   NOT NULL COMMENT '工具 ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_group_tool (group_id, tool_id),
    INDEX idx_tool_id (tool_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '工具组成员表（CUSTOM 组使用）';

-- 4. 旧表列名迁移：tool_category → tool_group
ALTER TABLE `ai_scope_tool_config`
    CHANGE COLUMN `tool_category` `tool_group` VARCHAR(50) NOT NULL COMMENT '工具组名（关联 ai_tool_group.group_name）';

ALTER TABLE `ai_role_tool_config`
    CHANGE COLUMN `tool_category` `tool_group` VARCHAR(50) NOT NULL COMMENT '工具组名（关联 ai_tool_group.group_name）';

-- 5. 初始化默认自定义工具组（6 个，对应原硬编码类别）
-- 注意：ai_tool 的初始数据由 ToolRegistrySyncService 启动时同步，这里只建空组
INSERT INTO ai_tool_group (group_name, cn_name, description, group_type)
VALUES ('basic', '基础能力', '时间/数学/知识库等基础工具', 'CUSTOM'),
       ('search', '联网搜索', 'Web 搜索、网页抓取等', 'CUSTOM'),
       ('geo', '地理与路径', '地图能力', 'CUSTOM'),
       ('weather', '天气', '实时天气、预报、AQI 等', 'CUSTOM'),
       ('file', '文件生成', 'PDF / Excel 报告生成', 'CUSTOM'),
       ('admin', '管理员特权', '数据库查询、系统配置等', 'CUSTOM');
