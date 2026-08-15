package com.endcy.ai.agent.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentSession + AgentStateMachine 单元测试。
 *
 * @author endcy
 * @since 2026/08/08
 */
class AgentSessionTest {

    @Test
    void testSession_InitialState() {
        AgentSession session = new AgentSession();
        assertEquals(AgentSessionStatus.INITIALIZED, session.getStatus());
        assertEquals(AgentMode.SINGLE_SHOT, session.getMode());
        assertEquals(0, session.getCurrentStep());
        assertNotNull(session.getSessionId());
        assertNotNull(session.getStartedAt());
    }

    @Test
    void testSession_MarkRunning() {
        AgentSession session = new AgentSession();
        session.markRunning();
        assertEquals(AgentSessionStatus.RUNNING, session.getStatus());
    }

    @Test
    void testSession_MarkCompleted() {
        AgentSession session = new AgentSession();
        session.markRunning();
        session.markCompleted("测试答案");
        assertEquals(AgentSessionStatus.COMPLETED, session.getStatus());
        assertEquals("测试答案", session.getFinalAnswer());
        assertNotNull(session.getCompletedAt());
    }

    @Test
    void testSession_MarkFailed() {
        AgentSession session = new AgentSession();
        session.markRunning();
        session.markFailed("测试错误");
        assertEquals(AgentSessionStatus.FAILED, session.getStatus());
        assertEquals("测试错误", session.getErrorMessage());
    }

    @Test
    void testSession_BudgetExceeded_BySteps() {
        AgentSession session = new AgentSession();
        session.setMaxSteps(3);
        session.markRunning();
        session.setCurrentStep(3);
        assertTrue(session.isBudgetExceeded(), "达到 maxSteps 应该预算超限");
    }

    @Test
    void testSession_BudgetExceeded_ByTokens() {
        AgentSession session = new AgentSession();
        session.setMaxTokens(100);
        session.markRunning();
        session.recordTokens(60, 50);
        assertTrue(session.isBudgetExceeded(), "token 超预算应该超限");
    }

    @Test
    void testSession_BudgetNotExceeded() {
        AgentSession session = new AgentSession();
        session.setMaxSteps(10);
        session.setMaxTokens(10000);
        session.markRunning();
        session.setCurrentStep(2);
        session.recordTokens(100, 50);
        assertFalse(session.isBudgetExceeded(), "预算内不应超限");
    }

    @Test
    void testSession_RecordThought() {
        AgentSession session = new AgentSession();
        session.markRunning();
        AgentSession.AgentThought thought = new AgentSession.AgentThought();
        thought.setStepIndex(1);
        thought.setThought("思考中");
        session.recordThought(thought);
        assertEquals(1, session.getCurrentStep());
        assertEquals(1, session.getThoughts().size());
    }

    @Test
    void testStateMachine_ValidTransition() {
        AgentStateMachine sm = new AgentStateMachine();
        AgentSession session = new AgentSession();
        assertTrue(sm.canTransit(AgentSessionStatus.INITIALIZED, AgentSessionStatus.RUNNING));
        sm.transit(session, AgentSessionStatus.RUNNING, "开始执行");
        assertEquals(AgentSessionStatus.RUNNING, session.getStatus());
    }

    @Test
    void testStateMachine_InvalidTransition() {
        AgentStateMachine sm = new AgentStateMachine();
        assertThrows(IllegalStateException.class, () -> {
            AgentSession session = new AgentSession();
            sm.transit(session, AgentSessionStatus.COMPLETED, "非法回退");
        });
    }

    @Test
    void testStateMachine_TerminalStates() {
        assertTrue(AgentStateMachine.isTerminal(AgentSessionStatus.COMPLETED));
        assertTrue(AgentStateMachine.isTerminal(AgentSessionStatus.FAILED));
        assertTrue(AgentStateMachine.isTerminal(AgentSessionStatus.TERMINATED_BY_BUDGET));
        assertFalse(AgentStateMachine.isTerminal(AgentSessionStatus.RUNNING));
        assertFalse(AgentStateMachine.isTerminal(AgentSessionStatus.INITIALIZED));
    }
}
