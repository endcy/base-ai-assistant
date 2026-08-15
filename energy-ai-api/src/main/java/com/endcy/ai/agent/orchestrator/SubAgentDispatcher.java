package com.endcy.ai.agent.orchestrator;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.agent.executor.AgentExecutor;
import com.endcy.ai.agent.executor.AgentMode;
import com.endcy.ai.agent.executor.AgentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sub-agent dispatcher — multi-agent orchestration.
 *
 * <p>The main agent decides to delegate a sub-task to an expert agent (operations analysis /
 * fault diagnosis / order processing). This class manages expert agent registration and
 * dispatching.</p>
 *
 * <p>Expert agents are dynamically registered via {@link #registerExpert} (configuration or code
 * injection). No experts by default; calling {@link #dispatch} without a match returns a hint message.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubAgentDispatcher {

    private final AgentExecutor agentExecutor;

    /**
     * Default max steps for a sub-agent.
     */
    private static final int SUB_AGENT_DEFAULT_MAX_STEPS = 5;

    /**
     * Expert agent registry: expertKey → ExpertConfig
     */
    private final Map<String, ExpertConfig> experts = new ConcurrentHashMap<>();

    /**
     * Register an expert agent.
     *
     * @param key    expert identifier (e.g. "operation", "fault", "order")
     * @param config expert configuration (systemPrompt + tool subset description)
     */
    public void registerExpert(String key, ExpertConfig config) {
        experts.put(key, config);
        log.info("注册专家 agent: {} ({})", key, config.getDisplayName());
    }

    /**
     * Dispatch a sub-task to an expert agent.
     *
     * @param expertKey expert identifier
     * @param subTask   sub-task description
     * @param chatId    session ID (for context inheritance)
     * @param groupId   group ID (tenant/merchant/user group)
     * @return expert agent response (returns hint if no matching expert)
     */
    public String dispatch(String expertKey, String subTask, Long chatId, String groupId) {
        ExpertConfig config = experts.get(expertKey);
        if (config == null) {
            log.warn("无匹配专家 agent: {}（已注册: {}）", expertKey, experts.keySet());
            return "无匹配专家 agent: " + expertKey + "。可用专家: " + experts.keySet();
        }

        log.info("分发子任务给专家 [{}]: {}", expertKey, StrUtil.maxLength(subTask, 80));

        // Build sub-agent session (inherits chatId/groupId, inject expert systemPrompt as question prefix)
        AgentSession session = new AgentSession();
        session.setChatId(chatId);
        session.setGroupId(groupId);
        session.setMode(AgentMode.SINGLE_SHOT);
        session.setMaxSteps(SUB_AGENT_DEFAULT_MAX_STEPS);
        // Expert systemPrompt injected as question prefix (simplified approach; full approach requires AgentExecutor to support per-session systemPrompt)
        String enhancedTask = config.getSystemPrompt() + "\n\n任务：" + subTask;

        try {
            String result = agentExecutor.execute(session, enhancedTask);
            log.info("专家 [{}] 完成，结果长度={}", expertKey, StrUtil.length(result));
            return result;
        } catch (Exception e) {
            log.error("专家 [{}] 执行失败: {}", expertKey, e.getMessage());
            return "专家 agent 执行失败: " + e.getMessage();
        }
    }

    /**
     * List all registered experts (for admin console).
     */
    public List<String> listExperts() {
        return List.copyOf(experts.keySet());
    }

    /**
     * Get expert configuration by key.
     *
     * @param key expert identifier
     * @return expert configuration, or {@code null} if not found
     */
    public ExpertConfig getExpert(String key) {
        return experts.get(key);
    }

    /**
     * Number of registered experts.
     */
    public int expertCount() {
        return experts.size();
    }

    // ==================== Expert configuration ====================

    /**
     * Expert agent configuration.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ExpertConfig {
        /**
         * Expert identifier
         */
        private String key;
        /**
         * Display name
         */
        private String displayName;
        /**
         * Expert system prompt (domain knowledge + behavioral constraints)
         */
        private String systemPrompt;
        /**
         * List of tool names available to this expert (null=all)
         */
        private List<String> allowedTools;
    }
}
