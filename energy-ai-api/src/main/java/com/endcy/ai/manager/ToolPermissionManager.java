package com.endcy.ai.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.endcy.ai.repository.domain.entity.*;
import com.endcy.ai.repository.trans.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工具权限服务 — 根据 scopeType + userRole 返回可用工具名称列表。
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolPermissionManager {

    private final ScopeToolConfigMapper scopeToolConfigMapper;
    private final RoleToolConfigMapper roleToolConfigMapper;
    private final ToolGroupMapper toolGroupMapper;
    private final ToolGroupMemberMapper toolGroupMemberMapper;
    private final ToolMapper toolMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "agent:tool_perm:";
    private static final long CACHE_TTL_MINUTES = 60;

    public List<String> getToolsByPermission(String scopeType, String userRole) {
        String scope = StrUtil.blankToDefault(scopeType, "general");
        String role = StrUtil.blankToDefault(userRole, "USER");
        String cacheKey = CACHE_PREFIX + scope + ":" + role;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSON.parseArray(cached, String.class);
        }

        List<String> tools = loadToolsFromDb(scope, role);

        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(tools), CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        log.debug("加载工具权限：scopeType={}, userRole={}, tools={}", scope, role, tools);
        return tools;
    }

    private List<String> loadToolsFromDb(String scopeType, String userRole) {
        Set<String> groupNames = new LinkedHashSet<>();

        List<ScopeToolConfig> scopeConfigs = scopeToolConfigMapper.selectList(
                new LambdaQueryWrapper<ScopeToolConfig>()
                        .eq(ScopeToolConfig::getScopeType, scopeType)
                        .eq(ScopeToolConfig::getEnabled, true));
        scopeConfigs.stream().map(ScopeToolConfig::getToolGroup).forEach(groupNames::add);

        List<RoleToolConfig> roleConfigs = roleToolConfigMapper.selectList(
                new LambdaQueryWrapper<RoleToolConfig>()
                        .eq(RoleToolConfig::getUserRole, userRole)
                        .eq(RoleToolConfig::getEnabled, true));
        roleConfigs.stream().map(RoleToolConfig::getToolGroup).forEach(groupNames::add);

        Set<String> toolNames = new LinkedHashSet<>();
        for (String groupName : groupNames) {
            ToolGroup group = toolGroupMapper.selectOne(
                    new LambdaQueryWrapper<ToolGroup>().eq(ToolGroup::getGroupName, groupName));
            if (group == null)
                continue;

            if ("MCP".equals(group.getGroupType())) {
                List<Tool> tools = toolMapper.selectList(
                        new LambdaQueryWrapper<Tool>()
                                .eq(Tool::getMcpServer, group.getMcpServer())
                                .eq(Tool::getSource, "MCP"));
                tools.stream().map(Tool::getToolName).forEach(toolNames::add);
            } else {
                List<ToolGroupMember> members = toolGroupMemberMapper.selectList(
                        new LambdaQueryWrapper<ToolGroupMember>()
                                .eq(ToolGroupMember::getGroupId, group.getId()));
                if (CollUtil.isNotEmpty(members)) {
                    List<Long> toolIds = members.stream().map(ToolGroupMember::getToolId).collect(Collectors.toList());
                    List<Tool> tools = toolMapper.selectBatchIds(toolIds);
                    tools.stream().map(Tool::getToolName).forEach(toolNames::add);
                }
            }
        }

        return new ArrayList<>(toolNames);
    }

    public void saveScopeToolConfig(String scopeType, String toolGroup, boolean enabled) {
        ScopeToolConfig config = new ScopeToolConfig();
        config.setScopeType(scopeType);
        config.setToolGroup(toolGroup);
        config.setEnabled(enabled);

        ScopeToolConfig existing = scopeToolConfigMapper.selectOne(
                new LambdaQueryWrapper<ScopeToolConfig>()
                        .eq(ScopeToolConfig::getScopeType, scopeType)
                        .eq(ScopeToolConfig::getToolGroup, toolGroup));
        if (existing != null) {
            config.setId(existing.getId());
            scopeToolConfigMapper.updateById(config);
        } else {
            scopeToolConfigMapper.insert(config);
        }
        invalidateCache(scopeType, null);
    }

    public void saveRoleToolConfig(String userRole, String toolGroup, boolean enabled) {
        RoleToolConfig config = new RoleToolConfig();
        config.setUserRole(userRole);
        config.setToolGroup(toolGroup);
        config.setEnabled(enabled);

        RoleToolConfig existing = roleToolConfigMapper.selectOne(
                new LambdaQueryWrapper<RoleToolConfig>()
                        .eq(RoleToolConfig::getUserRole, userRole)
                        .eq(RoleToolConfig::getToolGroup, toolGroup));
        if (existing != null) {
            config.setId(existing.getId());
            roleToolConfigMapper.updateById(config);
        } else {
            roleToolConfigMapper.insert(config);
        }
        invalidateCache(null, userRole);
    }

    public List<ScopeToolConfig> listScopeConfigs(String scopeType) {
        return scopeToolConfigMapper.selectList(
                new LambdaQueryWrapper<ScopeToolConfig>()
                        .eq(ScopeToolConfig::getScopeType, scopeType)
                        .orderByAsc(ScopeToolConfig::getToolGroup));
    }

    public List<RoleToolConfig> listRoleConfigs(String userRole) {
        return roleToolConfigMapper.selectList(
                new LambdaQueryWrapper<RoleToolConfig>()
                        .eq(RoleToolConfig::getUserRole, userRole)
                        .orderByAsc(RoleToolConfig::getToolGroup));
    }

    public Map<String, List<String>> listAllGroupsWithTools() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<ToolGroup> groups = toolGroupMapper.selectList(null);
        groups.sort(Comparator.comparing(ToolGroup::getGroupName));
        for (ToolGroup g : groups) {
            result.put(g.getGroupName(), resolveGroupToolNames(g));
        }
        return result;
    }

    public List<String> resolveGroupToolNames(ToolGroup group) {
        if ("MCP".equals(group.getGroupType())) {
            return toolMapper.selectList(
                                     new LambdaQueryWrapper<Tool>()
                                             .eq(Tool::getMcpServer, group.getMcpServer())
                                             .eq(Tool::getSource, "MCP"))
                             .stream().map(Tool::getToolName).collect(Collectors.toList());
        } else {
            List<ToolGroupMember> members = toolGroupMemberMapper.selectList(
                    new LambdaQueryWrapper<ToolGroupMember>()
                            .eq(ToolGroupMember::getGroupId, group.getId()));
            if (CollUtil.isEmpty(members))
                return Collections.emptyList();
            List<Long> toolIds = members.stream().map(ToolGroupMember::getToolId).collect(Collectors.toList());
            return toolMapper.selectBatchIds(toolIds)
                             .stream().map(Tool::getToolName).collect(Collectors.toList());
        }
    }

    private void invalidateCache(String scopeType, String userRole) {
        if (scopeType != null) {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + scopeType + ":*");
            if (CollUtil.isNotEmpty(keys))
                redisTemplate.delete(keys);
        }
        if (userRole != null) {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*:" + userRole);
            if (CollUtil.isNotEmpty(keys))
                redisTemplate.delete(keys);
        }
    }
}
