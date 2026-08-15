package com.endcy.ai.agent.executor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent execution event publisher — streams task progress in real time.
 *
 * <p>Uses Reactor Sinks for real-time event push, supporting SSE (Server-Sent Events).</p>
 *
 * @author endcy
 * @since 2026-08-10
 */
@Slf4j
@Component
public class AgentEventPublisher {

    /**
     * Event types.
     */
    public enum EventType {
        TASK_STARTED,
        STEP_STARTED,
        THINKING,
        TOOL_CALL,
        TOOL_RESULT,
        FINAL_ANSWER,
        TASK_COMPLETED,
        TASK_FAILED
    }

    /**
     * Task event payload.
     */
    @Data
    public static class AgentEvent {
        private EventType type;
        private String taskId;
        private int step;
        private String content;
        private String toolName;
        private String toolArgs;
        private String toolResult;
        private long timestamp;

        public static AgentEvent of(EventType type, String taskId) {
            AgentEvent event = new AgentEvent();
            event.setType(type);
            event.setTaskId(taskId);
            event.setTimestamp(System.currentTimeMillis());
            return event;
        }
    }

    /**
     * Task event stream: taskId → Sinks.Many
     */
    private final Map<String, Sinks.Many<AgentEvent>> eventSinks = new ConcurrentHashMap<>();

    /**
     * Create a task event stream.
     */
    public Sinks.Many<AgentEvent> createEventSink(String taskId) {
        Sinks.Many<AgentEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        eventSinks.put(taskId, sink);
        log.debug("Created event sink for taskId={}", taskId);
        return sink;
    }

    /**
     * Publish an event.
     */
    public void publish(AgentEvent event) {
        Sinks.Many<AgentEvent> sink = eventSinks.get(event.getTaskId());
        if (sink != null) {
            sink.tryEmitNext(event);
            log.debug("Published event: type={}, taskId={}, step={}",
                    event.getType(), event.getTaskId(), event.getStep());
        }
    }

    /**
     * Subscribe to a task event stream.
     */
    public Flux<AgentEvent> subscribe(String taskId) {
        Sinks.Many<AgentEvent> sink = eventSinks.get(taskId);
        if (sink == null) {
            return Flux.empty();
        }
        return sink.asFlux();
    }

    /**
     * Remove a task event stream (called after task completion).
     */
    public void removeEventSink(String taskId) {
        Sinks.Many<AgentEvent> sink = eventSinks.remove(taskId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.debug("Removed event sink for taskId={}", taskId);
        }
    }
}
