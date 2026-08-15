package com.endcy.ai.workflow.engine;

/**
 * Workflow engine Layer — cross-cutting concern hooks (inspired by Dify graphon GraphEngineLayer).
 *
 * <p>More explicit, hot-swappable, and unit-testable than Spring AOP. Typical implementations:</p>
 * <ul>
 *   <li>{@code PersistenceLayer} — persist node execution to DB</li>
 *   <li>{@code ObservabilityLayer} — OTel tracing</li>
 *   <li>{@code QuotaLayer} — quota checks</li>
 *   <li>{@code LimitsLayer} — step/time limits</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-08
 */
public interface GraphEngineLayer {

    default void onGraphStart(VariablePool pool) {
    }

    default void onNodeStart(WorkflowNode node, VariablePool pool) {
    }

    default void onNodeEnd(WorkflowNode node, WorkflowNode.NodeResult result, VariablePool pool) {
    }

    default void onGraphEnd(Throwable error) {
    }

    /**
     * Layer order (smaller values execute first)
     */
    default int order() {
        return 100;
    }
}
