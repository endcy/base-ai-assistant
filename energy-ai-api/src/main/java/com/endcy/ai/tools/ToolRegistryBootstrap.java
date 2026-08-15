package com.endcy.ai.tools;

import com.endcy.ai.tools.registry.ToolMeta;
import com.endcy.ai.tools.registry.ToolRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Tool registry bootstrapper — Step 2.9.
 *
 * <p>At startup, registers the two beans from {@code ToolRegistration} (commonWebTools / ragTools)
 * plus all remote MCP tool callbacks (from {@link SyncMcpToolCallbackProvider}) into the unified
 * {@link ToolRegistry}, supplementing metadata (riskLevel / businessType / mcpServer / allowedRoles).</p>
 *
 * <p>After registration, {@code ToolPermissionGate} / {@code DefaultAgentExecutor.executeAgentic}
 * can query tool metadata via ToolRegistry for permission checks and inventory display.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class ToolRegistryBootstrap {

    private final ToolRegistry toolRegistry;

    private final ToolCallback[] commonWebTools;
    private final ToolCallback[] ragTools;

    /**
     * Optional — absent when MCP client is disabled or no connection is reachable.
     * Constructor injection of an optional bean is not possible with plain constructor args,
     * so we inject the provider lazily via ObjectProvider-style field.
     */
    private final ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider;

    public ToolRegistryBootstrap(ToolRegistry toolRegistry,
                                 @Qualifier("commonWebTools") ToolCallback[] commonWebTools,
                                 @Qualifier("ragTools") ToolCallback[] ragTools,
                                 ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {
        this.toolRegistry = toolRegistry;
        this.commonWebTools = commonWebTools;
        this.ragTools = ragTools;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    @PostConstruct
    public void registerAll() {
        // commonWebTools: general-purpose tools (file/web/terminal/PDF)
        registerGroup(commonWebTools, "general", ToolMeta.RiskLevel.MEDIUM);
        // ragTools: RAG + domain query tools
        registerGroup(ragTools, "rag", ToolMeta.RiskLevel.LOW);

        // Mark high-risk tools specifically
        markHighRisk("executeTerminalCommand", "Terminal command execution, high risk in production");
        markHighRisk("writeFile", "File write, may overwrite important files");
        markHighRisk("downloadResource", "Resource download, may introduce malicious content");

        // Register remote MCP tool callbacks (from all configured MCP connections)
        registerRemoteMcpTools();

        // Apply role/risk metadata for MCP tools grouped by MCP server
        registerMcpServerTools();

        toolRegistry.logInventory();
    }

    /**
     * Register all remote MCP tools discovered by the Spring AI MCP client.
     * Each tool's mcpServer is resolved from its callback (client bean name -> server name).
     */
    private void registerRemoteMcpTools() {
        SyncMcpToolCallbackProvider provider = mcpToolCallbackProvider.getIfAvailable();
        if (provider == null) {
            log.warn("No SyncMcpToolCallbackProvider available — remote MCP tools not registered");
            return;
        }
        ToolCallback[] mcpTools;
        try {
            mcpTools = provider.getToolCallbacks();
        } catch (Exception e) {
            log.error("Failed to load remote MCP tool callbacks: {}", e.getMessage());
            return;
        }
        if (mcpTools == null || mcpTools.length == 0) {
            log.warn("Remote MCP client connected but returned 0 tools");
            return;
        }

        int registered = 0;
        for (ToolCallback tc : mcpTools) {
            if (tc.getToolDefinition() == null)
                continue;
            String name = tc.getToolDefinition().name();
            // Skip if already registered as a local CODE tool (avoid duplicates)
            if (toolRegistry.getMeta(name) != null) {
                continue;
            }
            String desc = tc.getToolDefinition().description();
            String serverName = resolveMcpServerName(tc);
            // MCP protocol Tool.title — human-readable name declared on the server side
            // (e.g. @ToolMapping(title="查询充电订单")), used as cn_name source during sync
            String title = resolveMcpToolTitle(tc);
            ToolMeta meta = ToolMeta.builder()
                                    .name(name)
                                    .label(title != null ? title : name)
                                    .humanDescription(desc)
                                    .llmDescription(desc)
                                    .businessType("mcp")
                                    .riskLevel(ToolMeta.RiskLevel.LOW)
                                    .requiresApproval(false)
                                    .source("MCP")
                                    .mcpServer(serverName)
                                    .build();
            toolRegistry.register(tc, meta);
            registered++;
        }
        log.info("Registered {} remote MCP tools into ToolRegistry", registered);
    }

    /**
     * Resolve which MCP server a tool callback belongs to.
     * SyncMcpToolCallback keeps its McpSyncClient in a private field (no public getter),
     * so we read it via reflection and take {@code serverInfo.name()} — which is the
     * value of {@code @McpServerEndpoint(name=...)} on the server side.
     */
    private String resolveMcpServerName(ToolCallback tc) {
        try {
            java.lang.reflect.Field field = tc.getClass().getDeclaredField("mcpClient");
            field.setAccessible(true);
            Object client = field.get(tc);
            if (client != null) {
                Object serverInfo = client.getClass().getMethod("getServerInfo").invoke(client);
                if (serverInfo != null) {
                    Object name = serverInfo.getClass().getMethod("name").invoke(serverInfo);
                    if (name instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return "unknown-mcp";
    }

    /**
     * Read the MCP protocol tool title (human-readable, declared via @ToolMapping(title=...) on
     * the server). {@code McpSchema.Tool.title()} — accessed reflectively because Spring AI's
     * ToolDefinition does not expose it. Returns null when absent (title is optional in MCP spec).
     */
    private String resolveMcpToolTitle(ToolCallback tc) {
        try {
            java.lang.reflect.Field field = tc.getClass().getDeclaredField("tool");
            field.setAccessible(true);
            Object tool = field.get(tc);
            if (tool != null) {
                Object title = tool.getClass().getMethod("title").invoke(tool);
                if (title instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        } catch (Throwable ignored) {
            // title is optional — null is fine
        }
        return null;
    }

    private void registerGroup(ToolCallback[] tools, String businessType, ToolMeta.RiskLevel defaultRisk) {
        if (tools == null)
            return;
        for (ToolCallback tc : tools) {
            if (tc.getToolDefinition() == null)
                continue;
            String name = tc.getToolDefinition().name();
            String desc = tc.getToolDefinition().description();
            ToolMeta meta = ToolMeta.builder()
                                    .name(name)
                                    .label(name)
                                    .humanDescription(desc)
                                    .llmDescription(desc)
                                    .businessType(businessType)
                                    .riskLevel(defaultRisk)
                                    .requiresApproval(false)
                                    .build();
            toolRegistry.register(tc, meta);
        }
    }

    private void markHighRisk(String toolName, String reason) {
        ToolMeta existing = toolRegistry.getMeta(toolName);
        if (existing != null) {
            existing.setRiskLevel(ToolMeta.RiskLevel.HIGH);
            existing.setRequiresApproval(true);
            existing.setHumanDescription(existing.getHumanDescription() + " [WARNING] " + reason);
            log.warn("标记高危工具: {} ({})", toolName, reason);
        }
    }

    /**
     * 按 MCP server 维度为各端点的工具打角色/风险标签。
     * server 与角色的对应关系（与 mcp 模块的三个 endpoint 一一对应）：
     * <ul>
     *   <li>base-ai-user-mcp      → USER / OPERATOR / ADMIN（只读查询）</li>
     *   <li>base-ai-operator-mcp  → OPERATOR / ADMIN（写操作）</li>
     *   <li>base-ai-admin-mcp     → ADMIN（高危管理）</li>
     * </ul>
     */
    private void registerMcpServerTools() {
        // user endpoint: query tools — all roles
        for (String name : new String[]{
                "getOrderDetail", "getDischargeOrderDetail", "getParkLockOrderDetail",
                "getStationFeePlan", "getWalletBalance", "getUserCards", "getStationActivities",
                "getStationById", "getStationList", "getEquipmentById", "getEquipmentList",
                "getEquipmentStatusById", "getEquipmentStatusList",
                "getConnectorInfoList", "getConnectorStatusList", "getStationAllConnectors",
                "getChargingProgress", "diagnoseFault",
                "getUserInfo", "getUserInvoices"}) {
            updateToolMeta(name, "charging", ToolMeta.RiskLevel.LOW,
                    Set.of("USER", "OPERATOR", "ADMIN"), "base-ai-user-mcp");
        }

        // operator endpoint: write tools — OPERATOR and above
        for (String name : new String[]{
                "startCharging", "forceEndOrder", "createRefund",
                "getStationStatistics", "getFaultHistory"}) {
            updateToolMeta(name, "charging", ToolMeta.RiskLevel.HIGH,
                    Set.of("OPERATOR", "ADMIN"), "base-ai-operator-mcp");
        }

        // admin endpoint: critical tools — ADMIN only
        updateToolMeta("remoteRestart", "charging", ToolMeta.RiskLevel.CRITICAL,
                Set.of("ADMIN"), "base-ai-admin-mcp");
    }

    /**
     * 更新已有工具的元数据（角色权限 + 风险等级 + MCP server）。
     */
    private void updateToolMeta(String toolName, String businessType,
                                ToolMeta.RiskLevel riskLevel, Set<String> allowedRoles, String mcpServer) {
        ToolMeta existing = toolRegistry.getMeta(toolName);
        if (existing != null) {
            existing.setBusinessType(businessType);
            existing.setRiskLevel(riskLevel);
            existing.setAllowedRoles(allowedRoles);
            existing.setSource("MCP");
            if (mcpServer != null) {
                existing.setMcpServer(mcpServer);
            }
            if (riskLevel == ToolMeta.RiskLevel.HIGH || riskLevel == ToolMeta.RiskLevel.CRITICAL) {
                existing.setRequiresApproval(true);
            }
        } else {
            // 工具尚未注册（可能 MCP 连接未建立），记录日志
            log.debug("工具 {} 尚未在 ToolRegistry 中注册，跳过元数据更新（将在 MCP 连接建立后自动同步）", toolName);
        }
    }
}
