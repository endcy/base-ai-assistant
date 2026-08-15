-- =============================================================================
-- DDL：tenant_id → group_id（语义统一：group_id 才是租户/商户/用户分组标识）
-- @author endcy
-- @since 2026/08/13
-- =============================================================================

-- 1. ai_agent_session
ALTER TABLE `ai_agent_session`
    CHANGE COLUMN `tenant_id` `group_id` VARCHAR(64) NULL COMMENT '分组ID（租户/商户/用户分组）';

-- 2. ai_llm_call_audit
ALTER TABLE `ai_llm_call_audit`
    CHANGE COLUMN `tenant_id` `group_id` VARCHAR(64) NULL COMMENT '分组ID（租户/商户/用户分组）';

-- 3. ai_tool_call_audit
ALTER TABLE `ai_tool_call_audit`
    CHANGE COLUMN `tenant_id` `group_id` VARCHAR(64) NULL COMMENT '分组ID（租户/商户/用户分组）';
