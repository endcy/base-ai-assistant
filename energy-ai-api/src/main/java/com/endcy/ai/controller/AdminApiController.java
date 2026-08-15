package com.endcy.ai.controller;

import com.endcy.ai.agent.executor.AgentSession;
import com.endcy.ai.agent.executor.AgentTaskService;
import com.endcy.ai.manager.LbCredentialManager;
import com.endcy.ai.prompt.PromptVersionService;
import com.endcy.ai.tools.registry.ToolRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin console API — task / tool / prompt / credential management.
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final AgentTaskService agentTaskService;
    private final ToolRegistry toolRegistry;
    private final LbCredentialManager lbCredentialManager;
    private final PromptVersionService promptVersionService;

    @GetMapping("/tasks")
    public List<AgentSession> listAllTasks() {
        return agentTaskService.listTasks();
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public Map<String, Object> cancelTask(@PathVariable String taskId,
                                          @RequestParam(required = false) String reason) {
        boolean ok = agentTaskService.cancel(taskId, reason);
        return Map.of("success", ok, "taskId", taskId);
    }

    @PostMapping("/tasks/cleanup")
    public Map<String, Object> cleanupOldTasks() {
        int removed = agentTaskService.cleanupOldTasks();
        return Map.of("removed", removed);
    }

    @GetMapping("/tools/inventory")
    public Map<String, Object> toolInventory() {
        return Map.of(
                "total", toolRegistry.size(),
                "tools", toolRegistry.listAll().stream()
                                     .map(tc -> Map.of(
                                             "name", tc.getToolDefinition().name(),
                                             "description", tc.getToolDefinition().description()
                                     ))
                                     .toList()
        );
    }

    @GetMapping("/credentials/status")
    public Map<String, Object> credentialStatus() {
        return Map.of(
                "enabled", lbCredentialManager.hasCredentials(),
                "inCooldown", lbCredentialManager.isInCooldown(),
                "cooldownRemainingSeconds", lbCredentialManager.getCooldownRemainingSeconds()
        );
    }

    @PostMapping("/credentials/clear-cooldown")
    public Map<String, Object> clearCooldown() {
        lbCredentialManager.clearCooldown();
        return Map.of("success", true);
    }

    @GetMapping("/prompts/versions")
    public Map<String, Object> listPromptVersions() {
        return Map.of("versions", promptVersionService.listVersions());
    }

    @PostMapping("/prompts/register")
    public Map<String, Object> registerPromptVersion(@RequestBody RegisterPromptRequest req) {
        int version = promptVersionService.registerVersion(req.getName(), req.getContent());
        return Map.of("version", version, "name", req.getName());
    }

    @GetMapping("/prompts/content/{name}")
    public Map<String, Object> getPromptContent(@PathVariable String name) {
        return Map.of("name", name, "content", promptVersionService.getContent(name));
    }

    @Data
    public static class RegisterPromptRequest {
        private String name;
        private String content;
    }
}
