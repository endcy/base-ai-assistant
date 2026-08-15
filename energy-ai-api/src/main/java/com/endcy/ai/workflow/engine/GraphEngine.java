package com.endcy.ai.workflow.engine;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Workflow graph execution engine — DAG scheduling + Layer chains (inspired by Dify graphon GraphEngine).
 *
 * <p>Currently linear execution (topologically sorted node list); branching/parallel capabilities to be enhanced later.</p>
 *
 * <p><b>Execution flow</b>:</p>
 * <ol>
 *   <li>onGraphStart (all Layers)</li>
 *   <li>Iterate nodes in topological order:
 *     <ul>
 *       <li>onNodeStart → node.run(pool) → onNodeEnd</li>
 *       <li>Node failure → record error, continue or terminate (depends on failFast)</li>
 *     </ul>
 *   </li>
 *   <li>onGraphEnd (including errors)</li>
 * </ol>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
public class GraphEngine {

    private final List<WorkflowNode> nodes;
    private final List<GraphEngineLayer> layers;
    private final boolean failFast;

    public GraphEngine(List<WorkflowNode> nodes, List<GraphEngineLayer> layers, boolean failFast) {
        this.nodes = nodes;
        this.layers = layers != null ? new ArrayList<>(layers) : new ArrayList<>();
        this.layers.sort(Comparator.comparingInt(GraphEngineLayer::order));
        this.failFast = failFast;
    }

    /**
     * Execute the entire graph.
     *
     * @param pool variable pool (contains initial input)
     * @return final output (output of the last node)
     */
    public String run(VariablePool pool) {
        layers.forEach(l -> safe(() -> l.onGraphStart(pool), "onGraphStart"));

        String finalOutput = "";
        Throwable graphError = null;

        for (WorkflowNode node : nodes) {
            layers.forEach(l -> safe(() -> l.onNodeStart(node, pool), "onNodeStart " + node.getNodeId()));

            WorkflowNode.NodeResult result;
            try {
                result = node.run(pool);
                if (!result.isSuccess()) {
                    log.warn("节点 {} 失败: {}", node.getNodeId(), result.error());
                    if (failFast) {
                        graphError = new RuntimeException("Node " + node.getNodeId() + " failed: " + result.error());
                        break;
                    }
                } else {
                    // Write node output to pool (key is always "output")
                    pool.put(node.getNodeId(), "output", result.output());
                    finalOutput = result.output();
                }
            } catch (Exception e) {
                log.error("节点 {} 异常: {}", node.getNodeId(), e.getMessage(), e);
                result = WorkflowNode.NodeResult.failure(e.getMessage());
                if (failFast) {
                    graphError = e;
                    break;
                }
            }

            WorkflowNode.NodeResult finalResult = result;
            layers.forEach(l -> safe(() -> l.onNodeEnd(node, finalResult, pool), "onNodeEnd " + node.getNodeId()));
        }

        final Throwable err = graphError;
        layers.forEach(l -> safe(() -> l.onGraphEnd(err), "onGraphEnd"));
        return finalOutput;
    }

    private void safe(Runnable r, String ctx) {
        try {
            r.run();
        } catch (Exception e) {
            log.warn("Layer {} 异常（被吞掉，不影响主流程）: {}", ctx, e.getMessage());
        }
    }

    /**
     * Builder
     */
    public static Builder builder(List<WorkflowNode> nodes) {
        return new Builder(nodes);
    }

    public static class Builder {
        private final List<WorkflowNode> nodes;
        private final List<GraphEngineLayer> layers = new ArrayList<>();
        private boolean failFast = false;

        public Builder(List<WorkflowNode> nodes) {
            this.nodes = nodes;
        }

        public Builder layer(GraphEngineLayer layer) {
            this.layers.add(layer);
            return this;
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public GraphEngine build() {
            return new GraphEngine(nodes, layers, failFast);
        }
    }
}
