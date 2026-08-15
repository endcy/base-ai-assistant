package com.endcy.ai.workflow.nodes;

import com.endcy.ai.workflow.engine.VariablePool;
import com.endcy.ai.workflow.engine.WorkflowNode;

/**
 * Answer node — outputs the final answer to the chat stream.
 *
 * @author endcy
 * @since 2026-08-08
 */
public class AnswerNode extends WorkflowNode {

    private final String inputSelector;

    public AnswerNode(String nodeId, String inputSelector) {
        super(nodeId, "ANSWER");
        this.inputSelector = inputSelector;
    }

    @Override
    public NodeResult run(VariablePool pool) {
        String answer = pool.get(inputSelector, "output");
        if (answer == null) {
            // Fallback: return sys:query as-is
            answer = pool.get(VariablePool.SYS, "query");
        }
        pool.put(VariablePool.SYS, "answer", answer);
        return NodeResult.success(answer);
    }
}
