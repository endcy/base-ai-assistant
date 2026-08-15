package com.endcy.ai.tools.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Tool-level permission gate — validates risk level and confirmation status before tool execution.
 *
 * <p>Step 2.8 implementation:</p>
 * <ul>
 *   <li>{@code LOW/MEDIUM} — auto-allow</li>
 *   <li>{@code HIGH} — requires runtime confirmation (AgentSession transitions to WAITING_APPROVAL)</li>
 *   <li>{@code CRITICAL} — requires admin approval (integrate with approval flow, Step 3.x)</li>
 * </ul>
 *
 * <p><b>Usage</b> (call before tool execution):</p>
 * <pre>
 *   ToolPermissionGate.Decision d = gate.check(toolName, userId);
 *   if (d.isBlocked()) {
 *       return "Tool requires confirmation: " + d.reason();
 *   }
 *   // Proceed with execution
 * </pre>
 *
 * <p>Currently a skeleton: HIGH/CRITICAL default to allow (log warning only),
 * actual confirmation flow integration in Step 3.x (task state machine + async approval API).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolPermissionGate {

    private final ToolRegistry toolRegistry;

    /**
     * Check whether a tool call is allowed.
     *
     * @param toolName tool name
     * @param userId   caller ID
     * @return decision result
     */
    public Decision check(String toolName, String userId) {
        return check(toolName, userId, null);
    }

    /**
     * Check whether a tool call is allowed with role check.
     *
     * @param toolName tool name
     * @param userId   caller ID
     * @param userRole caller role (USER / OPERATOR / ADMIN), nullable
     * @return decision result
     */
    public Decision check(String toolName, String userId, String userRole) {
        ToolMeta meta = toolRegistry.getMeta(toolName);
        if (meta == null) {
            // Tool not registered in registry, default allow (compatible with unregistered tools in existing ToolRegistration)
            return Decision.allow();
        }

        // 0. Role check (defense in depth with DB-based ToolPermissionManager)
        if (userRole != null && !meta.isRoleAllowed(userRole)) {
            log.warn("Tool call blocked by role permission: tool={}, userRole={}, userId={}", toolName, userRole, userId);
            return Decision.block("Current role (" + userRole + ") is not authorized to use this tool. This tool is restricted to " + meta.getAllowedRoles());
        }

        ToolMeta.RiskLevel risk = meta.getRiskLevel() != null ? meta.getRiskLevel() : ToolMeta.RiskLevel.LOW;

        switch (risk) {
            case LOW, MEDIUM -> {
                return Decision.allow();
            }
            case HIGH -> {
                // Step 3.x: transition to WAITING_APPROVAL, await user confirmation
                // Current skeleton: warn only, allow (avoid blocking existing functionality)
                log.warn("HIGH-risk tool call [{}] by user={} — currently auto-allowed (confirmation flow TBD Step 3.x)",
                        toolName, userId);
                return Decision.allowWithWarning("HIGH-risk tool, recommend integrating confirmation flow");
            }
            case CRITICAL -> {
                // Step 3.x: mandatory admin approval
                log.error("CRITICAL-risk tool call [{}] by user={} — currently blocked by default",
                        toolName, userId);
                return Decision.block("CRITICAL tool requires admin approval (approval flow Step 3.x TBD), currently blocked by default");
            }
            default -> {
                return Decision.allow();
            }
        }
    }

    /**
     * Mark a tool as requiring confirmation (runtime dynamic config, for Step 3.x approval flow).
     * Currently a no-op (logging only).
     */
    public void requireConfirmation(String toolName, String reason) {
        log.info("Tool [{}] marked as requiring confirmation: {}", toolName, reason);
    }

    // ==================== Decision object ====================

    public record Decision(boolean allowed, boolean warning, String reason) {
        public static Decision allow() {
            return new Decision(true, false, null);
        }

        public static Decision allowWithWarning(String reason) {
            return new Decision(true, true, reason);
        }

        public static Decision block(String reason) {
            return new Decision(false, false, reason);
        }

        public boolean isBlocked() {
            return !allowed;
        }
    }
}
