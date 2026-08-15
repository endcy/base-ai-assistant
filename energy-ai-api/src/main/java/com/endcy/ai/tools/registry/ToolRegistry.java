package com.endcy.ai.tools.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified tool registry — centralized management of all {@link ToolCallback} instances.
 *
 * <p>Currently in skeleton phase (Step 1.4): provides registration/query APIs,
 * migration of existing tools ({@code ToolRegistration.java} → this registry) is done in Step 2.9.</p>
 *
 * <p><b>Usage</b>:</p>
 * <pre>
 *   toolRegistry.register(toolCallback, ToolMeta.builder()...build());
 *   List&lt;ToolCallback&gt; allTools = toolRegistry.listAll();
 *   List&lt;ToolCallback&gt; generalTools = toolRegistry.listByBusinessType("general");
 * </pre>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component("registryToolRegistry")
public class ToolRegistry {

    /**
     * Stored grouped by businessType
     */
    private final Map<String, List<RegisteredTool>> byBusinessType = new ConcurrentHashMap<>();

    /**
     * Quick lookup by name
     */
    private final Map<String, RegisteredTool> byName = new ConcurrentHashMap<>();

    // ---- Registration ----

    /**
     * Register a tool (thread-safe).
     */
    public void register(ToolCallback tool, ToolMeta meta) {
        RegisteredTool entry = new RegisteredTool(tool, meta);
        byName.put(meta.getName(), entry);
        String type = StrUtil.blankToDefault(meta.getBusinessType(), "general");
        byBusinessType.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        log.info("Registered tool: {} (type={}, risk={})", meta.getName(), type, meta.getRiskLevel());
    }

    /**
     * Batch registration.
     */
    public void registerAll(List<RegisteredTool> tools) {
        for (RegisteredTool t : tools) {
            register(t.getCallback(), t.getMeta());
        }
    }

    // ---- Query ----

    public List<ToolCallback> listAll() {
        List<ToolCallback> result = new ArrayList<>();
        for (RegisteredTool entry : byName.values()) {
            result.add(entry.getCallback());
        }
        return result;
    }

    public List<RegisteredTool> listAllRegistered() {
        return new ArrayList<>(byName.values());
    }

    public List<ToolCallback> listByBusinessType(String businessType) {
        List<RegisteredTool> list = byBusinessType.get(businessType);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<ToolCallback> result = new ArrayList<>(list.size());
        for (RegisteredTool entry : list) {
            result.add(entry.getCallback());
        }
        return result;
    }

    public ToolCallback getByName(String name) {
        RegisteredTool entry = byName.get(name);
        return entry != null ? entry.getCallback() : null;
    }

    public ToolMeta getMeta(String name) {
        RegisteredTool entry = byName.get(name);
        return entry != null ? entry.getMeta() : null;
    }

    public int size() {
        return byName.size();
    }

    /**
     * Print metadata (masked) for each category of tools to the log for startup troubleshooting.
     */
    public void logInventory() {
        log.info("===== Tool Registry Inventory ({} tools) =====", byName.size());
        for (Map.Entry<String, List<RegisteredTool>> entry : byBusinessType.entrySet()) {
            log.info("  [{}]", entry.getKey());
            for (RegisteredTool t : entry.getValue()) {
                log.info("    {} : {}", t.getMeta().getName(), t.getMeta().getLlmDescription());
            }
        }
    }

    // ---- Internal types ----

    @Data
    @AllArgsConstructor
    public static class RegisteredTool {
        private ToolCallback callback;
        private ToolMeta meta;
    }
}
