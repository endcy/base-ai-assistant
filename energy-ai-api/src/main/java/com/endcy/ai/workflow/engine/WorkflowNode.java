package com.endcy.ai.workflow.engine;

/**
 * Workflow node base class (inspired by Dify graphon Node&lt;T&gt;).
 *
 * <p>Each node implements {@link #run(VariablePool)}, returning the execution result.
 * Generic T is the node configuration type (NodeData).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public abstract class WorkflowNode {

    protected final String nodeId;
    protected final String nodeType;

    protected WorkflowNode(String nodeId, String nodeType) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    /**
     * Execute node logic.
     *
     * @param pool variable pool (read upstream inputs, write this node's outputs)
     * @return execution result
     */
    public abstract NodeResult run(VariablePool pool);

    /**
     * Node execution result.
     */
    public record NodeResult(Status status, String output, String error) {
        public static NodeResult success(String output) {
            return new NodeResult(Status.SUCCESS, output, null);
        }

        public static NodeResult failure(String error) {
            return new NodeResult(Status.FAILED, null, error);
        }

        public static NodeResult skipped(String reason) {
            return new NodeResult(Status.SKIPPED, null, reason);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }

    public enum Status {
        SUCCESS, FAILED, SKIPPED
    }
}
