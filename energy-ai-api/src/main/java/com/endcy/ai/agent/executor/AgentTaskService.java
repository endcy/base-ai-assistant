package com.endcy.ai.agent.executor;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.repository.service.AgentSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous agent task service.
 *
 * <p>Provides task submission, status query, and cancellation. Tasks execute
 * {@link AgentExecutor} in a background thread; callers may poll status or subscribe to results.</p>
 *
 * <p>Persistence: in-memory table ({@code ConcurrentHashMap}) + DB ({@code ai_agent_session})
 * dual write. The in-memory table serves real-time queries; the DB serves history/audit/cross-instance recovery.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskService {

    private final AgentExecutor agentExecutor;
    private final AgentSessionRepository agentSessionRepository;
    private final AgentEventPublisher agentEventPublisher;

    /**
     * Cleanup threshold: tasks completed more than this many seconds ago are eligible for removal.
     */
    private static final int CLEANUP_THRESHOLD_SECONDS = 3600;

    private final Map<String, AgentSession> taskTable = new ConcurrentHashMap<>();

    /**
     * Submit an asynchronous task.
     *
     * @param mode         execution mode
     * @param chatId       business session ID
     * @param groupId      group ID (tenant/merchant/user group)
     * @param userQuestion user question
     * @param scopeType    domain type (for tool permission control)
     * @param userRole     user role (for tool permission enhancement)
     * @return taskId
     */
    public String submitTask(AgentMode mode, Long chatId, String groupId, String userQuestion,
                             String scopeType, String userRole) {
        AgentSession session = new AgentSession();
        session.setChatId(chatId);
        session.setGroupId(groupId);
        session.setScopeType(scopeType);
        session.setUserRole(userRole);
        session.setMode(mode);
        session.setUserQuestion(userQuestion);
        session.setMaxSteps(10);

        String taskId = session.getSessionId();
        taskTable.put(taskId, session);

        // Create event stream (for SSE push)
        agentEventPublisher.createEventSink(taskId);

        // DB persistence (create session record)
        agentSessionRepository.createSession(taskId, chatId, null, groupId,
                mode.name(), userQuestion);

        // Asynchronous execution
        CompletableFuture.runAsync(() -> {
            try {
                String answer = agentExecutor.execute(session, userQuestion);
                log.info("任务完成 taskId={} answer.len={}", taskId, StrUtil.length(answer));
            } catch (Exception e) {
                log.error("任务失败 taskId={}: {}", taskId, e.getMessage(), e);
            } finally {
                // Delay 5 seconds before cleaning the event stream (ensures client received all events)
                CompletableFuture.runAsync(() -> agentEventPublisher.removeEventSink(taskId),
                        CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS));
            }
        });

        log.info("提交异步任务 taskId={} mode={} chatId={}", taskId, mode, chatId);
        return taskId;
    }

    /**
     * Query task status.
     */
    public AgentSession getTask(String taskId) {
        return taskTable.get(taskId);
    }

    /**
     * List all tasks (admin console use).
     */
    public List<AgentSession> listTasks() {
        return List.copyOf(taskTable.values());
    }

    /**
     * Cancel a task (only non-terminal states can be cancelled).
     */
    public boolean cancel(String taskId, String reason) {
        AgentSession session = taskTable.get(taskId);
        if (session == null) {
            return false;
        }
        if (AgentStateMachine.isTerminal(session.getStatus())) {
            return false;
        }
        session.markFailed("用户取消: " + reason);
        log.info("任务取消 taskId={} reason={}", taskId, reason);
        return true;
    }

    /**
     * Clean up tasks completed more than 1 hour ago (avoid memory leak).
     */
    public int cleanupOldTasks() {
        Instant cutoff = Instant.now().minusSeconds(CLEANUP_THRESHOLD_SECONDS);
        int removed = 0;
        for (Map.Entry<String, AgentSession> e : taskTable.entrySet()) {
            AgentSession s = e.getValue();
            if (s.getCompletedAt() != null && s.getCompletedAt().isBefore(cutoff)) {
                taskTable.remove(e.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.info("清理 {} 个过期任务", removed);
        }
        return removed;
    }
}
