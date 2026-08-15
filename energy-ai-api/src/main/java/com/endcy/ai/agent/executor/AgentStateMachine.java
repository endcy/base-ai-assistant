package com.endcy.ai.agent.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent task state machine — centrally manages the legal transitions
 * of {@link AgentSessionStatus}.
 *
 * <p>Transition rules:</p>
 * <pre>
 *   INITIALIZED → RUNNING
 *   RUNNING → COMPLETED / FAILED / TERMINATED_BY_BUDGET / TERMINATED_BY_USER / WAITING_APPROVAL
 *   WAITING_APPROVAL → RUNNING / TERMINATED_BY_USER
 *   Terminal states (COMPLETED/FAILED/TERMINATED_*) cannot transition further
 * </pre>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class AgentStateMachine {

    private final Map<AgentSessionStatus, Set<AgentSessionStatus>> transitions = new EnumMap<>(AgentSessionStatus.class);

    public AgentStateMachine() {
        // INITIALIZED can only go to RUNNING
        transitions.put(AgentSessionStatus.INITIALIZED, EnumSet.of(AgentSessionStatus.RUNNING));

        // RUNNING can go to completion states or wait for approval
        transitions.put(AgentSessionStatus.RUNNING, EnumSet.of(
                AgentSessionStatus.COMPLETED,
                AgentSessionStatus.FAILED,
                AgentSessionStatus.TERMINATED_BY_BUDGET,
                AgentSessionStatus.TERMINATED_BY_USER,
                AgentSessionStatus.WAITING_APPROVAL));

        // WAITING_APPROVAL can resume running or be terminated by user
        transitions.put(AgentSessionStatus.WAITING_APPROVAL, EnumSet.of(
                AgentSessionStatus.RUNNING,
                AgentSessionStatus.TERMINATED_BY_USER));

        // Terminal states: empty set (no transitions allowed)
        transitions.put(AgentSessionStatus.COMPLETED, EnumSet.noneOf(AgentSessionStatus.class));
        transitions.put(AgentSessionStatus.FAILED, EnumSet.noneOf(AgentSessionStatus.class));
        transitions.put(AgentSessionStatus.TERMINATED_BY_BUDGET, EnumSet.noneOf(AgentSessionStatus.class));
        transitions.put(AgentSessionStatus.TERMINATED_BY_USER, EnumSet.noneOf(AgentSessionStatus.class));
    }

    /**
     * Validate whether a state transition is legal.
     */
    public boolean canTransit(AgentSessionStatus from, AgentSessionStatus to) {
        Set<AgentSessionStatus> allowed = transitions.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Execute a state transition (illegal transition throws {@link IllegalStateException}).
     *
     * @param session session
     * @param to      target status
     * @param reason  transition reason (for logging)
     */
    public void transit(AgentSession session, AgentSessionStatus to, String reason) {
        AgentSessionStatus from = session.getStatus();
        if (!canTransit(from, to)) {
            throw new IllegalStateException(
                    "Illegal state transition: " + from + " → " + to + " (sessionId=" + session.getSessionId() + ")");
        }
        session.setStatus(to);
        log.info("状态流转: {} → {} (sessionId={}, reason={})", from, to, session.getSessionId(), reason);
    }

    /**
     * Whether the given status is a terminal state (no further transitions allowed).
     */
    public static boolean isTerminal(AgentSessionStatus status) {
        return status == AgentSessionStatus.COMPLETED
                || status == AgentSessionStatus.FAILED
                || status == AgentSessionStatus.TERMINATED_BY_BUDGET
                || status == AgentSessionStatus.TERMINATED_BY_USER;
    }
}
