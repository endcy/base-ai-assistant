-- 领域类型-工具类别权限配置表
CREATE TABLE IF NOT EXISTS ai_scope_tool_config
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    scope_type    VARCHAR(50) NOT NULL COMMENT '领域类型（类似 KnowledgeScopeTypeEnum）',
    tool_category VARCHAR(50) NOT NULL COMMENT '工具类别（basic/search/geo/weather/file/admin）',
    enabled       TINYINT              DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_scope_category (scope_type, tool_category),
    INDEX idx_scope_type (scope_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='领域类型-工具类别权限配置';

-- 初始化领域类型权限配置
INSERT INTO ai_scope_tool_config (scope_type, tool_category, enabled)
VALUES
-- 市场客服
('市场客服', 'basic', 1),
('市场客服', 'search', 1),
('市场客服', 'geo', 1),
('市场客服', 'weather', 1),
-- 用户客服
('用户客服', 'basic', 1),
('用户客服', 'search', 1),
('用户客服', 'geo', 1),
('用户客服', 'weather', 1),
-- 运营服务
('运营服务', 'basic', 1),
('运营服务', 'search', 1),
('运营服务', 'geo', 1),
('运营服务', 'weather', 1),
('运营服务', 'file', 1),
-- 开发运维
('开发运维', 'basic', 1),
('开发运维', 'search', 1),
('开发运维', 'geo', 1),
('开发运维', 'weather', 1),
('开发运维', 'file', 1),
('开发运维', 'admin', 1),
-- 海外用户客服
('海外用户客服', 'basic', 1),
('海外用户客服', 'search', 1),
('海外用户客服', 'geo', 1),
('海外用户客服', 'weather', 1),
-- 海外运营客服
('海外运营客服', 'basic', 1),
('海外运营客服', 'search', 1),
('海外运营客服', 'geo', 1),
('海外运营客服', 'weather', 1),
('海外运营客服', 'file', 1);

-- 用户角色额外工具配置表
CREATE TABLE IF NOT EXISTS ai_role_tool_config
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_role     VARCHAR(50) NOT NULL COMMENT '用户角色（USER/OPERATOR/ADMIN）',
    tool_category VARCHAR(50) NOT NULL COMMENT '工具类别',
    enabled       TINYINT              DEFAULT 1 COMMENT '是否启用',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_category (user_role, tool_category),
    INDEX idx_user_role (user_role)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色-工具类别权限配置';

-- 初始化角色权限配置
INSERT INTO ai_role_tool_config (user_role, tool_category, enabled)
VALUES
-- 普通用户：基础工具
('USER', 'basic', 1),
('USER', 'search', 1),
-- 运营人员：基础 + 地理 + 天气 + 文件
('OPERATOR', 'basic', 1),
('OPERATOR', 'search', 1),
('OPERATOR', 'geo', 1),
('OPERATOR', 'weather', 1),
('OPERATOR', 'file', 1),
-- 管理员：所有工具
('ADMIN', 'basic', 1),
('ADMIN', 'search', 1),
('ADMIN', 'geo', 1),
('ADMIN', 'weather', 1),
('ADMIN', 'file', 1),
('ADMIN', 'admin', 1);
