package com.endcy.ai.tools.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Tool wrapper with permission checking and parameter validation.
 *
 * <p>Checks before tool execution:
 * <ul>
 *   <li>Permission (ToolPermissionGate, risk + role)</li>
 *   <li>Parameter validation (ToolValidator)</li>
 * </ul>
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
public class GuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolPermissionGate permissionGate;
    private final String sessionId;
    private final String userId;
    private final String userRole;

    public GuardedToolCallback(ToolCallback delegate, ToolPermissionGate permissionGate,
                               String sessionId, String userId) {
        this(delegate, permissionGate, sessionId, userId, null);
    }

    public GuardedToolCallback(ToolCallback delegate, ToolPermissionGate permissionGate,
                               String sessionId, String userId, String userRole) {
        this.delegate = delegate;
        this.permissionGate = permissionGate;
        this.sessionId = sessionId;
        this.userId = userId;
        this.userRole = userRole;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String toolName = delegate.getToolDefinition().name();

        // 1. Permission check (with role)
        ToolPermissionGate.Decision decision = permissionGate.check(toolName, userId, userRole);
        if (decision.isBlocked()) {
            log.warn("Tool call blocked by permission: tool={}, reason={}", toolName, decision.reason());
            return "Error: tool call rejected - " + decision.reason();
        }

        // 2. Parameter validation
        String validationError = ToolValidator.validate(toolName, toolInput);
        if (validationError != null) {
            log.warn("Tool parameter validation failed: tool={}, error={}", toolName, validationError);
            return "Error: parameter validation failed - " + validationError;
        }

        // 3. Execute tool
        try {
            return delegate.call(toolInput);
        } catch (Exception e) {
            log.error("Tool execution failed: tool={}", toolName, e);
            return "Error: tool execution failed - " + e.getMessage();
        }
    }
}
