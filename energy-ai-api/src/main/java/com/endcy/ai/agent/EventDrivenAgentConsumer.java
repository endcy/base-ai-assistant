package com.endcy.ai.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.endcy.ai.agent.executor.AgentMode;
import com.endcy.ai.agent.executor.AgentTaskService;
import com.endcy.ai.constant.EnergyAiConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven agent consumer.
 *
 * <p>Listens to a RabbitMQ business event queue and automatically triggers AgentExecutor processing.</p>
 *
 * <p><b>Event format</b> (JSON):</p>
 * <pre>
 * {
 *   "eventType": "DEVICE_OFFLINE",        // event type
 *   "stationId": "SZ001",                  // station ID
 *   "description": "充电桩离线",            // event description
 *   "mode": "AGENTIC",                     // execution mode (optional, default AGENTIC)
 *   "groupId": "-1",                       // group ID (tenant/merchant/user group)
 *   "question": "站点SZ001离线，请检查并创建工单"  // agent instruction
 * }
 * </pre>
 *
 * <p><b>Supported queues</b> (configurable):</p>
 * <ul>
 *   <li>{@code ai.agent.event.queue} — default queue name</li>
 *   <li>{@code ai.agent.event.enabled} — switch (default false)</li>
 * </ul>
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDrivenAgentConsumer {

    private final AgentTaskService agentTaskService;

    /**
     * Listen to the AI agent event queue.
     *
     * <p>Queue name configured via {@code ai.agent.event.queue}, default {@code ai-agent-event}.</p>
     * <p>Controlled by {@code ai.agent.event.enabled} switch.</p>
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${ai.agent.event.queue:ai-agent-event}", durable = "true"),
                    exchange = @Exchange(value = "${ai.agent.event.exchange:ai-agent}", type = "topic"),
                    key = "${ai.agent.event.routing-key:agent.#}"
            ),
            autoStartup = "${ai.agent.event.enabled:false}"
    )
    public void onEvent(Message message) {
        try {
            String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            log.info("收到 Agent 事件: {}", StrUtil.maxLength(body, 200));

            EventPayload event = JSONUtil.toBean(body, EventPayload.class);
            if (StrUtil.isBlank(event.getQuestion())) {
                log.warn("Agent 事件缺少 question 字段，跳过: {}", body);
                return;
            }

            AgentMode mode = StrUtil.isNotBlank(event.getMode())
                    ? AgentMode.valueOf(event.getMode().toUpperCase())
                    : AgentMode.AGENTIC;

            String taskId = agentTaskService.submitTask(
                    mode,
                    event.getChatId(),
                    StrUtil.blankToDefault(event.getGroupId(), EnergyAiConstant.DEFAULT_TENANT_ID),
                    event.getQuestion(),
                    null, null);

            log.info("Agent 事件触发任务: taskId={}, eventType={}, stationId={}",
                    taskId, event.getEventType(), event.getStationId());

        } catch (Exception e) {
            log.error("Agent 事件处理失败: {}", e.getMessage(), e);
            // Do not throw, avoid infinite message retry (RabbitMQ will ack)
        }
    }

    /**
     * Event payload.
     */
    @lombok.Data
    public static class EventPayload {
        private String eventType;
        private String stationId;
        private String description;
        private String mode;
        private Long chatId;
        private String groupId;
        private String question;
    }
}
