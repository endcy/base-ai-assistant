-- ------------------------------------------------------
-- 知识分类配置表
-- 用于维护知识领域类型和业务领域类型的配置
-- 支持前端动态维护，替代原有的硬编码枚举
-- ------------------------------------------------------

-- 知识分类配置表
CREATE TABLE IF NOT EXISTS `ai_knowledge_category_config`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `type`          VARCHAR(32)  NOT NULL COMMENT '分类类型 (scope-知识领域,business-业务领域)',
    `code`          VARCHAR(64)  NOT NULL COMMENT '分类编码 (英文标识)',
    `name`          VARCHAR(128) NOT NULL COMMENT '分类名称 (中文显示)',
    `parent_code`   VARCHAR(64)  DEFAULT NULL COMMENT '父级分类编码 (业务类型可选填)',
    `description`   VARCHAR(512) DEFAULT NULL COMMENT '分类描述',
    `sort_order`    INT          DEFAULT 0 COMMENT '排序序号',
    `enabled`       TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    `create_user`   BIGINT       DEFAULT '1' COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       DEFAULT '1' COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type`, `code`),
    KEY `idx_type_enabled` (`type`, `enabled`),
    KEY `idx_parent_code` (`parent_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC
  COMMENT = '知识分类配置表';

-- ------------------------------------------------------
-- 初始化数据 - 知识领域类型 (type=scope)
-- ------------------------------------------------------
INSERT INTO `ai_knowledge_category_config` (`type`, `code`, `name`, `description`, `sort_order`, `enabled`) VALUES
('scope', 'MARKET_CUSTOMER_SERVICE', '市场客服', '市场营销、市场推广相关客服知识', 1, 1),
('scope', 'ACCOUNT_CUSTOMER_SERVICE', '用户客服', 'C 端用户相关客服知识', 2, 1),
('scope', 'OPERATOR_CUSTOMER_SERVICE', '商户客服', 'B 端商户相关客服知识', 3, 1),
('scope', 'OPERATIONS_REFERENCE', '运营资料', '平台运营相关参考资料', 4, 1),
('scope', 'DEVELOPER_REFERENCE', '开发运维资料', '开发、运维、产品相关参考资料', 5, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------
-- 初始化数据 - 业务领域类型 (type=business)
-- ------------------------------------------------------
INSERT INTO `ai_knowledge_category_config` (`type`, `code`, `name`, `description`, `sort_order`, `enabled`) VALUES
('business', 'HELLO', '普通问候', '基本问候打招呼或者非充电运营能源领域业务内容咨询', 1, 1),
('business', 'STATION', '站点信息', '平台运营充放电、储能站点等信息咨询', 2, 1),
('business', 'EQUIPMENT', '设备信息', '平台运营充放电、储能设备等信息咨询', 3, 1),
('business', 'ACCOUNT', '用户信息', '平台用户信息或客户信息相关基础信息咨询', 4, 1),
('business', 'CHARGE_ORDER', '充电订单信息', '充电流程、充电订单内容相关信息咨询', 5, 1),
('business', 'DISCHARGE_ORDER', '放电订单信息', '放电流程、放电订单内容相关信息咨询', 6, 1),
('business', 'ALARM', '故障处理', '充放电或能源管理等过程出现的各类故障咨询', 7, 1),
('business', 'NORMS', '合作规范', '平台对接客户相关合作内容咨询', 8, 1),
('business', 'API', '接口文档', '平台中各类开发接口信息查询', 9, 1),
('business', 'PRODUCTION', '产品规划', '产品已支持的需求或相关规划内容问题咨询', 10, 1),
('business', 'CLIENT_OPERATE', '用户操作指南', '客户端中用户操作流程、操作内容相关信息咨询', 11, 1),
('business', 'ADMIN_OPERATE', '管理操作指南', '管理平台中商户或管理者操作流程、操作内容相关信息咨询', 12, 1),
('business', 'MAINTENANCE', '维护手册', '开发、运维或产品需求人员维护平台操作的各类操作内容相关信息咨询', 13, 1),
('business', 'REPORTER', '分析报告', '针对平台管理商户、开发运维人员或需求管理人员的数据统计需求，进行数据统计、分析，并给出相应的报告', 14, 1),
('business', 'POWER_PREDICT', '用电功率预测', '结合平台历史数据对未来时段的设备、站点的用电功率进行预测', 15, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
