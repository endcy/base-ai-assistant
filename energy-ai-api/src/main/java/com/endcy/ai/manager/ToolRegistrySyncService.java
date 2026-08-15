package com.endcy.ai.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.endcy.ai.repository.domain.entity.Tool;
import com.endcy.ai.repository.domain.entity.ToolGroup;
import com.endcy.ai.repository.trans.mapper.ToolGroupMapper;
import com.endcy.ai.repository.trans.mapper.ToolMapper;
import com.endcy.ai.tools.registry.ToolMeta;
import com.endcy.ai.tools.registry.ToolRegistry;
import com.endcy.service.domain.annotation.McpServer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工具注册同步服务 — 启动时 + 手动触发，把 Spring ToolRegistry + McpServer 注解的 MCP 客户端
 * 同步到 ai_tool / ai_tool_group 表。
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRegistrySyncService {

    @Autowired
    private ApplicationContext applicationContext;
    private final ToolMapper toolMapper;
    private final ToolGroupMapper toolGroupMapper;
    private final ToolRegistry toolRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            SyncResult result = syncFromRegistry();
            log.info("工具同步完成：upserted={}, mcpServers={}", result.getUpsertedCount(), result.getMcpServerNames());
        } catch (Exception e) {
            log.error("工具同步失败（不影响启动）: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public SyncResult syncFromRegistry() {
        int upserted = 0;
        Set<String> mcpServers = new LinkedHashSet<>();

        Map<String, Object> mcpBeans = applicationContext.getBeansWithAnnotation(McpServer.class);
        for (Map.Entry<String, Object> entry : mcpBeans.entrySet()) {
            Object bean = entry.getValue();
            McpServer annotation = bean.getClass().getAnnotation(McpServer.class);
            if (annotation == null)
                continue;
            String mcpServer = annotation.value();
            mcpServers.add(mcpServer);

            for (Method method : bean.getClass().getDeclaredMethods()) {
                org.springframework.ai.tool.annotation.Tool toolAnno = method.getAnnotation(org.springframework.ai.tool.annotation.Tool.class);
                if (toolAnno == null)
                    continue;
                String toolName = method.getName();
                String description = toolAnno.description();
                upsertTool(toolName, null, description, "MCP", mcpServer);
                upserted++;
            }
        }

        for (ToolCallback callback : toolRegistry.listAll()) {
            String name = callback.getToolDefinition().name();
            String description = callback.getToolDefinition().description();
            ToolMeta meta = toolRegistry.getMeta(name);
            String metaSource = (meta != null && meta.getSource() != null) ? meta.getSource() : "CODE";
            String metaMcpServer = meta != null ? meta.getMcpServer() : null;
            String metaLabel = (meta != null && !meta.getLabel().equals(meta.getName())) ? meta.getLabel() : null;

            Tool existing = toolMapper.selectOne(
                    new LambdaQueryWrapper<Tool>().eq(Tool::getToolName, name));
            if (existing != null && "MCP".equals(existing.getSource())) {
                if (existing.getCnName() == null && metaLabel != null) {
                    upsertTool(name, metaLabel, description, "MCP", existing.getMcpServer());
                }
                continue;
            }
            upsertTool(name, metaLabel, description, metaSource, metaMcpServer);
            upserted++;

            if ("MCP".equals(metaSource) && metaMcpServer != null && !"unknown-mcp".equals(metaMcpServer)) {
                mcpServers.add(metaMcpServer);
            }
        }

        for (String server : mcpServers) {
            upsertMcpToolGroup(server);
        }

        return new SyncResult(upserted, mcpServers);
    }

    private void upsertTool(String toolName, String cnName, String description, String source, String mcpServer) {
        Tool existing = toolMapper.selectOne(
                new LambdaQueryWrapper<Tool>().eq(Tool::getToolName, toolName));
        if (existing != null) {
            Tool update = new Tool();
            update.setId(existing.getId());
            if (cnName != null && existing.getCnName() == null)
                update.setCnName(cnName);
            if (description != null)
                update.setDescription(description);
            update.setSource(source);
            update.setMcpServer(mcpServer);
            toolMapper.updateById(update);
        } else {
            Tool t = new Tool();
            t.setToolName(toolName);
            t.setCnName(cnName);
            t.setDescription(description);
            t.setSource(source);
            t.setMcpServer(mcpServer);
            toolMapper.insert(t);
        }
    }

    private void upsertMcpToolGroup(String mcpServer) {
        ToolGroup existingByName = toolGroupMapper.selectOne(
                new LambdaQueryWrapper<ToolGroup>().eq(ToolGroup::getGroupName, mcpServer));
        if (existingByName != null) {
            log.debug("工具组 [{}] 已存在（类型={}），跳过 MCP 自动组创建", mcpServer, existingByName.getGroupType());
            return;
        }
        ToolGroup g = new ToolGroup();
        g.setGroupName(mcpServer);
        g.setCnName(mcpServer + " 工具组");
        g.setDescription("MCP 服务 [" + mcpServer + "] 自动创建的工具组（只读）");
        g.setGroupType("MCP");
        g.setMcpServer(mcpServer);
        toolGroupMapper.insert(g);
    }

    @Data
    public static class SyncResult {
        private final int upsertedCount;
        private final Set<String> mcpServerNames;

        public SyncResult(int upsertedCount, Set<String> mcpServerNames) {
            this.upsertedCount = upsertedCount;
            this.mcpServerNames = mcpServerNames;
        }
    }
}
