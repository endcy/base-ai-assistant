package com.endcy.ai.repository.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.endcy.ai.repository.domain.entity.AgentSession;
import com.endcy.ai.repository.domain.entity.AgentThought;
import com.endcy.ai.repository.trans.mapper.AgentSessionMapper;
import com.endcy.ai.repository.trans.mapper.AgentThoughtMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * AgentSession / AgentThought 持久化服务。
 *
 * <p>由 {@code AgentTaskService}（energy-ai-api）调用，把内存态的 AgentSession
 * 落库到 {@code ai_agent_session} / {@code ai_agent_thought} 表。</p>
 *
 * <p>所有方法吞掉 DB 异常（仅日志），避免持久化失败影响主流程（思考过程丢失可接受，
 * 主流程答案不能丢）。</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class AgentSessionRepository {

    private final AgentSessionMapper agentSessionMapper;
    private final AgentThoughtMapper agentThoughtMapper;

    /**
     * 创建会话记录（执行开始时）。
     */
    public void createSession(String sessionId, Long chatId, String userId, String groupId,
                              String mode, String userQuestion) {
        try {
            AgentSession entity = new AgentSession();
            entity.setSessionId(sessionId);
            entity.setChatId(chatId);
            entity.setUserId(userId);
            entity.setGroupId(groupId);
            entity.setMode(mode);
            entity.setStatus("RUNNING");
            entity.setUserQuestion(userQuestion);
            entity.setTotalPromptTokens(0);
            entity.setTotalCompletionTokens(0);
            entity.setCurrentStep(0);
            entity.setStartedAt(new Date());
            agentSessionMapper.insert(entity);
        } catch (Exception e) {
            log.warn("createSession 失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 记录单步思考。
     */
    public void recordThought(String sessionId, int stepIndex, String thought,
                              String toolCalls, String toolResults,
                              long durationMs, int promptTokens, int completionTokens) {
        try {
            AgentThought entity = new AgentThought();
            entity.setSessionId(sessionId);
            entity.setStepIndex(stepIndex);
            entity.setThought(thought);
            entity.setToolCalls(toolCalls);
            entity.setToolResults(toolResults);
            entity.setDurationMs(durationMs);
            entity.setPromptTokens(promptTokens);
            entity.setCompletionTokens(completionTokens);
            agentThoughtMapper.insert(entity);
        } catch (Exception e) {
            log.warn("recordThought 失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 更新会话最终状态。
     */
    public void completeSession(String sessionId, String status, String finalAnswer,
                                String errorMessage, int totalPromptTokens,
                                int totalCompletionTokens, int currentStep) {
        try {
            AgentSession update = new AgentSession();
            update.setStatus(status);
            update.setFinalAnswer(finalAnswer);
            update.setErrorMessage(errorMessage);
            update.setTotalPromptTokens(totalPromptTokens);
            update.setTotalCompletionTokens(totalCompletionTokens);
            update.setCurrentStep(currentStep);
            update.setCompletedAt(new Date());

            LambdaQueryWrapper<AgentSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentSession::getSessionId, sessionId);
            agentSessionMapper.update(update, wrapper);
        } catch (Exception e) {
            log.warn("completeSession 失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 按 sessionId 查会话。
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public AgentSession getBySessionId(String sessionId) {
        try {
            LambdaQueryWrapper<AgentSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentSession::getSessionId, sessionId);
            return agentSessionMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("getBySessionId 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查会话的所有思考步骤。
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<AgentThought> listThoughts(String sessionId) {
        try {
            LambdaQueryWrapper<AgentThought> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentThought::getSessionId, sessionId)
                   .orderByAsc(AgentThought::getStepIndex);
            return agentThoughtMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("listThoughts 失败: {}", e.getMessage());
            return CollUtil.newArrayList();
        }
    }

    /**
     * 按 chatId 查会话列表。
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<AgentSession> listByChatId(Long chatId) {
        try {
            LambdaQueryWrapper<AgentSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentSession::getChatId, chatId)
                   .orderByDesc(AgentSession::getStartedAt);
            return agentSessionMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("listByChatId 失败: {}", e.getMessage());
            return CollUtil.newArrayList();
        }
    }
}
