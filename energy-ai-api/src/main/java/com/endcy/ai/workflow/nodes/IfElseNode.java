package com.endcy.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.workflow.engine.VariablePool;
import com.endcy.ai.workflow.engine.WorkflowNode;

import java.util.function.Predicate;

/**
 * If-Else node — conditional branching (simplified).
 *
 * <p>Current implementation: evaluates the predicate, writes the branch result to pool (key="branch"),
 * GraphEngine subsequently selects the path based on the branch (branch routing capability to be enhanced later).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public class IfElseNode extends WorkflowNode {

    private final Predicate<VariablePool> condition;
    private final String trueBranch;
    private final String falseBranch;

    public IfElseNode(String nodeId, Predicate<VariablePool> condition, String trueBranch, String falseBranch) {
        super(nodeId, "IF_ELSE");
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    @Override
    public NodeResult run(VariablePool pool) {
        boolean result = condition.test(pool);
        String branch = result ? trueBranch : falseBranch;
        pool.put(getNodeId(), "branch", branch);
        pool.put(getNodeId(), "output", "条件=" + result + ", 分支=" + branch);
        return NodeResult.success(StrUtil.blankToDefault(branch, "default"));
    }
}
