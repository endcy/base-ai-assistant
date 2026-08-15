package com.endcy.ai.workflow.nodes;

import com.endcy.ai.workflow.engine.VariablePool;
import com.endcy.ai.workflow.engine.WorkflowNode;

/**
 * Start node — writes user input into the variable pool.
 *
 * @author endcy
 * @since 2026-08-08
 */
public class StartNode extends WorkflowNode {

    private final String userInput;

    public StartNode(String nodeId, String userInput) {
        super(nodeId, "START");
        this.userInput = userInput;
    }

    @Override
    public NodeResult run(VariablePool pool) {
        pool.put(VariablePool.SYS, "query", userInput);
        pool.put(getNodeId(), "output", userInput);
        return NodeResult.success(userInput);
    }
}
