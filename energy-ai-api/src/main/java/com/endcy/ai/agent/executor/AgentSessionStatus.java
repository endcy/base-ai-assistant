package com.endcy.ai.agent.executor;

/**
 * Agent session state machine.
 *
 * <pre>
 *   INITIALIZED → RUNNING → COMPLETED / FAILED / TERMINATED_BY_BUDGET / TERMINATED_BY_USER
 *                          ↘ WAITING_APPROVAL → RUNNING
 * </pre>
 *
 * @author endcy
 * @since 2026-08-07
 */
public enum AgentSessionStatus {
    INITIALIZED,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    TERMINATED_BY_BUDGET,
    TERMINATED_BY_USER,
}
