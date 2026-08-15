package com.endcy.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.endcy.ai.manager.ToolPermissionManager;
import com.endcy.ai.manager.ToolRegistrySyncService;
import com.endcy.ai.repository.domain.entity.Tool;
import com.endcy.ai.repository.domain.entity.ToolGroup;
import com.endcy.ai.repository.domain.entity.ToolGroupMember;
import com.endcy.ai.repository.trans.mapper.ToolGroupMapper;
import com.endcy.ai.repository.trans.mapper.ToolGroupMemberMapper;
import com.endcy.ai.repository.trans.mapper.ToolMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具管理接口 —— 工具清单 / 工具组 / 成员分配 / 同步。
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tool")
public class ToolController {

    private final ToolMapper toolMapper;
    private final ToolGroupMapper toolGroupMapper;
    private final ToolGroupMemberMapper toolGroupMemberMapper;
    private final ToolRegistrySyncService toolRegistrySyncService;
    private final ToolPermissionManager toolPermissionManager;

    @GetMapping
    public List<ToolVO> listTools() {
        return toolMapper.selectList(null).stream()
                         .sorted(Comparator.comparing(Tool::getSource).thenComparing(Tool::getToolName))
                         .map(this::toVO)
                         .collect(Collectors.toList());
    }

    @PostMapping("/sync")
    public Map<String, Object> syncTools() {
        ToolRegistrySyncService.SyncResult result = toolRegistrySyncService.syncFromRegistry();
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("success", true);
        ret.put("upsertedCount", result.getUpsertedCount());
        ret.put("mcpServers", result.getMcpServerNames());
        ret.put("message", String.format("同步完成，upsert %d 个工具，MCP server: %s",
                result.getUpsertedCount(), result.getMcpServerNames()));
        return ret;
    }

    @PutMapping("/{id}/cn-name")
    public Map<String, Object> updateCnName(@PathVariable Long id, @RequestBody CnNameRequest req) {
        Tool tool = toolMapper.selectById(id);
        if (tool == null) {
            return Map.of("success", false, "message", "工具不存在");
        }
        String cnName = req.getCnName() == null ? "" : req.getCnName().trim();
        toolMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Tool>()
                .eq(Tool::getId, id)
                .set(Tool::getCnName, cnName.isEmpty() ? null : cnName));
        log.info("工具中文名已更新: id={}, toolName={}, cnName={}", id, tool.getToolName(), cnName);
        return Map.of("success", true, "message", cnName.isEmpty() ? "已清除中文名" : "中文名已更新为: " + cnName);
    }

    @GetMapping("/group")
    public List<GroupVO> listGroups() {
        List<ToolGroup> groups = toolGroupMapper.selectList(null);
        groups.sort(Comparator.comparing(ToolGroup::getGroupType).reversed()
                              .thenComparing(ToolGroup::getGroupName));
        return groups.stream().map(this::toGroupVO).collect(Collectors.toList());
    }

    @PostMapping("/group")
    public Map<String, Object> createGroup(@RequestBody GroupRequest req) {
        if ("MCP".equalsIgnoreCase(req.getGroupType())) {
            return Map.of("success", false, "message", "MCP 类型工具组由同步服务自动创建，不可手动创建");
        }
        ToolGroup existing = toolGroupMapper.selectOne(
                new LambdaQueryWrapper<ToolGroup>().eq(ToolGroup::getGroupName, req.getGroupName()));
        if (existing != null) {
            return Map.of("success", false, "message", "工具组名已存在: " + req.getGroupName());
        }
        ToolGroup g = new ToolGroup();
        g.setGroupName(req.getGroupName());
        g.setCnName(req.getCnName());
        g.setDescription(req.getDescription());
        g.setGroupType("CUSTOM");
        toolGroupMapper.insert(g);
        return Map.of("success", true, "id", g.getId(), "message", "创建成功");
    }

    @PutMapping("/group/{id}")
    public Map<String, Object> updateGroup(@PathVariable Long id, @RequestBody GroupRequest req) {
        ToolGroup g = toolGroupMapper.selectById(id);
        if (g == null)
            return Map.of("success", false, "message", "工具组不存在");
        if ("MCP".equals(g.getGroupType()))
            return Map.of("success", false, "message", "MCP 类型工具组只读");
        ToolGroup update = new ToolGroup();
        update.setId(id);
        if (req.getCnName() != null)
            update.setCnName(req.getCnName());
        if (req.getDescription() != null)
            update.setDescription(req.getDescription());
        toolGroupMapper.updateById(update);
        return Map.of("success", true, "message", "更新成功");
    }

    @DeleteMapping("/group/{id}")
    public Map<String, Object> deleteGroup(@PathVariable Long id) {
        ToolGroup g = toolGroupMapper.selectById(id);
        if (g == null)
            return Map.of("success", false, "message", "工具组不存在");
        if ("MCP".equals(g.getGroupType()))
            return Map.of("success", false, "message", "MCP 类型工具组不可删除");
        toolGroupMemberMapper.delete(new LambdaQueryWrapper<ToolGroupMember>().eq(ToolGroupMember::getGroupId, id));
        toolGroupMapper.deleteById(id);
        return Map.of("success", true, "message", "删除成功");
    }

    @GetMapping("/group/{id}/members")
    public List<Long> getGroupMembers(@PathVariable Long id) {
        ToolGroup g = toolGroupMapper.selectById(id);
        if (g == null || "MCP".equals(g.getGroupType()))
            return Collections.emptyList();
        return toolGroupMemberMapper.selectList(new LambdaQueryWrapper<ToolGroupMember>().eq(ToolGroupMember::getGroupId, id))
                                    .stream().map(ToolGroupMember::getToolId).collect(Collectors.toList());
    }

    @PostMapping("/group/{id}/members")
    public Map<String, Object> setGroupMembers(@PathVariable Long id, @RequestBody MembersRequest req) {
        ToolGroup g = toolGroupMapper.selectById(id);
        if (g == null)
            return Map.of("success", false, "message", "工具组不存在");
        if ("MCP".equals(g.getGroupType()))
            return Map.of("success", false, "message", "MCP 类型工具组只读");
        toolGroupMemberMapper.delete(new LambdaQueryWrapper<ToolGroupMember>().eq(ToolGroupMember::getGroupId, id));
        List<Long> toolIds = req.getToolIds() != null ? req.getToolIds() : Collections.emptyList();
        Set<Long> uniqueToolIds = new HashSet<>(toolIds);
        for (Long toolId : uniqueToolIds) {
            ToolGroupMember m = new ToolGroupMember();
            m.setGroupId(id);
            m.setToolId(toolId);
            toolGroupMemberMapper.insert(m);
        }
        return Map.of("success", true, "message", String.format("已设置 %d 个成员", uniqueToolIds.size()));
    }

    private ToolVO toVO(Tool t) {
        ToolVO vo = new ToolVO();
        vo.setId(t.getId());
        vo.setToolName(t.getToolName());
        vo.setCnName(t.getCnName());
        vo.setDescription(t.getDescription());
        vo.setSource(t.getSource());
        vo.setMcpServer(t.getMcpServer());
        return vo;
    }

    private GroupVO toGroupVO(ToolGroup g) {
        GroupVO vo = new GroupVO();
        vo.setId(g.getId());
        vo.setGroupName(g.getGroupName());
        vo.setCnName(g.getCnName());
        vo.setDescription(g.getDescription());
        vo.setGroupType(g.getGroupType());
        vo.setMcpServer(g.getMcpServer());
        vo.setToolNames(toolPermissionManager.resolveGroupToolNames(g));
        return vo;
    }

    @Data
    public static class ToolVO {
        private Long id;
        private String toolName, cnName, description, source, mcpServer;
    }

    @Data
    public static class GroupVO {
        private Long id;
        private String groupName, cnName, description, groupType, mcpServer;
        private List<String> toolNames;
    }

    @Data
    public static class GroupRequest {
        private String groupName, cnName, description, groupType;
    }

    @Data
    public static class MembersRequest {
        private List<Long> toolIds;
    }

    @Data
    public static class CnNameRequest {
        private String cnName;
    }
}
