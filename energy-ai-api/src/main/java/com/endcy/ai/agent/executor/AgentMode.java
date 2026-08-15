package com.endcy.ai.agent.executor;

/**
 * Agent execution modes.
 *
 * <p>Dispatched by mode in {@link DefaultAgentExecutor}.</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
public enum AgentMode {

    /**
     * Single-shot RAG (no explicit think/act loop, uses Spring AI built-in tool execution).
     */
    SINGLE_SHOT,

    /**
     * Explicit ReAct loop (think, act, observe, repeat).
     * Supports maxSteps, tool invocation, and DB-persisted thought process.
     */
    AGENTIC,

    /**
     * Plan first, then execute step by step, reflecting after each step.
     */
    PLAN_AND_ACT,
}
