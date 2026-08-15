package com.endcy.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Abstract base agent class for managing agent state and execution flow.
 * <p>
 * Provides state transitions, memory management, and a step-based execution loop.
 * Subclasses must implement the step method.
 *
 * @author endcy
 */
@Data
@Slf4j
public abstract class BaseAgent {

    private static final int DEFAULT_MAX_STEPS = 10;
    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final String ERROR_PREFIX = "执行错误";

    // Core properties
    private String name;

    // Prompts
    private String systemPrompt;
    private String nextStepPrompt;

    // Agent state
    private AgentState state = AgentState.IDLE;

    // Execution step control
    private int currentStep = 0;
    private int maxSteps = DEFAULT_MAX_STEPS;

    // LLM model
    private ChatClient chatClient;

    // Memory (self-managed conversation context)
    private List<Message> messageList = new ArrayList<>();

    /**
     * Run the agent.
     *
     * @param userPrompt user prompt
     * @return execution result
     */
    public String run(String userPrompt) {
        // 1. Basic validation
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2. Execute and update state
        this.state = AgentState.RUNNING;
        // Record message context
        messageList.add(new UserMessage(userPrompt));
        // Store results list
        List<String> results = new ArrayList<>();
        try {
            // Execution loop
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // Single step execution
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // Check if step limit exceeded
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return ERROR_PREFIX + e.getMessage();
        } finally {
            // 3. Cleanup resources
            this.cleanup();
        }
    }

    /**
     * Run the agent (streaming output).
     *
     * @param userPrompt user prompt
     * @return SSE emitter for streaming results
     */
    public SseEmitter runStream(String userPrompt) {
        // Create an SseEmitter with a long timeout
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT_MS); // 5-minute timeout
        // Use async thread to avoid blocking the main thread
        CompletableFuture.runAsync(() -> {
            // 1. Basic validation
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
            // 2. Execute and update state
            this.state = AgentState.RUNNING;
            // Record message context
            messageList.add(new UserMessage(userPrompt));
            // Store results list
            List<String> results = new ArrayList<>();
            try {
                // Execution loop
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    // Single step execution
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);
                    // Stream each step result to SSE
                    sseEmitter.send(result);
                }
                // Check if step limit exceeded
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                    sseEmitter.send("执行结束：达到最大步骤（" + maxSteps + "）");
                }
                // Normal completion
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sseEmitter.send(ERROR_PREFIX + "：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3. Cleanup resources
                this.cleanup();
            }
        });

        // Set timeout callback
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // Set completion callback
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * Define a single execution step.
     *
     * @return step result
     */
    public abstract String step();

    /**
     * Cleanup resources.
     */
    protected void cleanup() {
        // Subclasses can override this method to clean up resources
    }
}
