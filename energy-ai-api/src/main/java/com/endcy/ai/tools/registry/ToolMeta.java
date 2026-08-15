package com.endcy.ai.tools.registry;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Tool metadata — descriptive information filled in for each tool at registration.
 *
 * <p>Inspired by Dify {@code ToolEntity} design: separates human_description (visible to admins)
 * from llm_description (visible to LLM, should be concise + include examples).</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Data
@Builder
public class ToolMeta {

    /**
     * Unique tool identifier (matches {@code @Tool(name=...)})
     */
    private String name;

    /**
     * Human-readable label
     */
    private String label;

    /**
     * Human-readable description (displayed in admin panel)
     */
    private String humanDescription;

    /**
     * Description visible to LLM (keep it concise! include input examples! This is key info for LLM tool selection)
     */
    private String llmDescription;

    /**
     * Business category tag (e.g. "general", "rag", "mcp", etc.)
     */
    private String businessType;

    /**
     * Risk level
     */
    public enum RiskLevel {LOW, MEDIUM, HIGH, CRITICAL}

    private RiskLevel riskLevel;

    /**
     * Whether manual confirmation is required
     */
    private boolean requiresApproval;

    /**
     * Allowed user roles for this tool.
     * Empty or null means all roles can access.
     * Values: "USER", "OPERATOR", "ADMIN"
     */
    private Set<String> allowedRoles;

    /**
     * Tool source: "CODE" (local Java tool) or "MCP" (remote MCP server tool).
     */
    private String source;

    /**
     * Remote MCP server name this tool belongs to (e.g. "base-ai-user-mcp").
     * Null for CODE tools. Used to resolve per-server tool groups.
     */
    private String mcpServer;

    /**
     * Check if a user role is allowed to use this tool.
     */
    public boolean isRoleAllowed(String userRole) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true; // No restriction
        }
        if (userRole == null) {
            return true; // No role specified, allow by default
        }
        return allowedRoles.contains(userRole.toUpperCase());
    }
}
