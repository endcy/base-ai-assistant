-- ============================================================
-- Role -> MCP server group configuration
-- Creates the role tool-group mapping for the three base-ai
-- MCP endpoints registered in mcp-api (one process, three servers):
--   base-ai-user-mcp      : read-only queries      (USER+)
--   base-ai-operator-mcp  : write operations        (OPERATOR+)
--   base-ai-admin-mcp     : critical operations     (ADMIN only)
--
-- Prerequisites: start energy-ai-api once so that
-- ToolRegistrySyncService auto-creates the three MCP tool groups
-- (ai_tool_group rows with group_type=MCP, matching by mcp_server).
-- ============================================================

-- USER: query tools only
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'USER', 'base-ai-user-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'USER'
                    AND tool_group = 'base-ai-user-mcp');

-- OPERATOR: user queries + write operations
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'OPERATOR', 'base-ai-user-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'OPERATOR'
                    AND tool_group = 'base-ai-user-mcp');
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'OPERATOR', 'base-ai-operator-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'OPERATOR'
                    AND tool_group = 'base-ai-operator-mcp');

-- ADMIN: everything
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'ADMIN', 'base-ai-user-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'ADMIN'
                    AND tool_group = 'base-ai-user-mcp');
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'ADMIN', 'base-ai-operator-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'ADMIN'
                    AND tool_group = 'base-ai-operator-mcp');
INSERT INTO ai_role_tool_config (user_role, tool_group, enabled)
SELECT 'ADMIN', 'base-ai-admin-mcp', 1
WHERE NOT EXISTS (SELECT 1
                  FROM ai_role_tool_config
                  WHERE user_role = 'ADMIN'
                    AND tool_group = 'base-ai-admin-mcp');
