package com.endcy.ai.controller;

import com.endcy.ai.manager.ToolPermissionManager;
import com.endcy.ai.repository.domain.entity.RoleToolConfig;
import com.endcy.ai.repository.domain.entity.ScopeToolConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具权限配置管理接口（admin 管理后台用）。
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scope-tool-config")
public class ScopeToolConfigController {

    private final ToolPermissionManager toolPermissionManager;

    @GetMapping("/categories")
    public Map<String, Object> listCategories() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", toolPermissionManager.listAllGroupsWithTools());
        return result;
    }

    @GetMapping("/scope")
    public List<ScopeToolConfig> listScopeConfigs(@RequestParam String scopeType) {
        return toolPermissionManager.listScopeConfigs(scopeType);
    }

    @PostMapping("/scope")
    public Map<String, Object> saveScopeConfig(@RequestBody ScopeConfigRequest req) {
        toolPermissionManager.saveScopeToolConfig(req.getScopeType(), req.getToolGroup(), req.isEnabled());
        return Map.of("success", true, "message", "保存成功");
    }

    @PostMapping("/scope/batch")
    public Map<String, Object> batchSaveScopeConfig(@RequestBody BatchScopeConfigRequest req) {
        for (ScopeConfigRequest item : req.getConfigs()) {
            toolPermissionManager.saveScopeToolConfig(req.getScopeType(), item.getToolGroup(), item.isEnabled());
        }
        return Map.of("success", true, "message", "批量保存成功", "scopeType", req.getScopeType());
    }

    @GetMapping("/role")
    public List<RoleToolConfig> listRoleConfigs(@RequestParam String userRole) {
        return toolPermissionManager.listRoleConfigs(userRole);
    }

    @PostMapping("/role")
    public Map<String, Object> saveRoleConfig(@RequestBody RoleConfigRequest req) {
        toolPermissionManager.saveRoleToolConfig(req.getUserRole(), req.getToolGroup(), req.isEnabled());
        return Map.of("success", true, "message", "保存成功");
    }

    @GetMapping("/resolve")
    public Map<String, Object> resolvePermission(
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) String userRole) {
        List<String> tools = toolPermissionManager.getToolsByPermission(scopeType, userRole);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scopeType", scopeType);
        result.put("userRole", userRole);
        result.put("tools", tools);
        result.put("count", tools.size());
        return result;
    }

    @Data
    public static class ScopeConfigRequest {
        private String scopeType;
        private String toolGroup;
        private boolean enabled;
    }

    @Data
    public static class BatchScopeConfigRequest {
        private String scopeType;
        private List<ScopeConfigRequest> configs;
    }

    @Data
    public static class RoleConfigRequest {
        private String userRole;
        private String toolGroup;
        private boolean enabled;
    }
}
