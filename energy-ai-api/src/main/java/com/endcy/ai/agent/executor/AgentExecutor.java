package com.endcy.ai.agent.executor;

import reactor.core.publisher.Flux;

/**
 * Agent execution engine — unified entry point for all AI invocations.
 *
 * <p>Replaces the scattered {@code doChatRag / doChatRagStream / deepseek} direct calls,
 * providing unified session management, mode dispatch, budget control, and thought
 * persistence capabilities.</p>
 *
 * <p>Modes:
 * <ul>
 *   <li>{@link AgentMode#SINGLE_SHOT} — single-shot RAG</li>
 *   <li>{@link AgentMode#AGENTIC} — explicit think/act loop</li>
 *   <li>{@link AgentMode#PLAN_AND_ACT} — plan first, then execute step by step</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-07
 */
public interface AgentExecutor {

    /**
     * Execute synchronously.
     *
     * @param session      session context
     * @param userQuestion user question
     * @return final answer
     */
    String execute(AgentSession session, String userQuestion);

    /**
     * Execute in streaming fashion.
     *
     * @param session      session context
     * @param userQuestion user question
     * @return stream of text responses
     */
    Flux<String> executeStream(AgentSession session, String userQuestion);
}
