package com.endcy.ai.tools.registry;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Tool wrapping factory — creates guarded tool callbacks.
 *
 * @author endcy
 * @since 2026/08/08
 */
@Component
@RequiredArgsConstructor
public class GuardedToolFactory {

    private final ToolPermissionGate permissionGate;

    /**
     * Wrap a tool array with permission checking and parameter validation.
     *
     * @param tools     original tool array
     * @param sessionId session ID
     * @param userId    user ID
     * @return wrapped tool array
     */
    public ToolCallback[] wrapWithGuards(ToolCallback[] tools, String sessionId, String userId) {
        return wrapWithGuards(tools, sessionId, userId, null);
    }

    /**
     * Wrap a tool array with permission checking, parameter validation, and role check.
     *
     * @param tools     original tool array
     * @param sessionId session ID
     * @param userId    user ID
     * @param userRole  user role (USER / OPERATOR / ADMIN)
     * @return wrapped tool array
     */
    public ToolCallback[] wrapWithGuards(ToolCallback[] tools, String sessionId, String userId, String userRole) {
        if (tools == null || tools.length == 0) {
            return tools;
        }

        ToolCallback[] guardedTools = new ToolCallback[tools.length];
        for (int i = 0; i < tools.length; i++) {
            guardedTools[i] = new GuardedToolCallback(tools[i], permissionGate, sessionId, userId, userRole);
        }
        return guardedTools;
    }
}
