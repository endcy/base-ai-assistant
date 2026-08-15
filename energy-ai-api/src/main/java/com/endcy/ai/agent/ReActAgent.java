package com.endcy.ai.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) pattern abstract agent class.
 * Implements the think-act loop pattern.
 *
 * @author endcy
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * Process current state and decide the next action.
     *
     * @return true if action should be taken, false otherwise
     */
    public abstract boolean think();

    /**
     * Execute the decided action.
     *
     * @return result of the action
     */
    public abstract String act();

    /**
     * Execute a single step: think then act.
     *
     * @return step execution result
     */
    @Override
    public String step() {
        try {
            // Think first
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 - 无需行动";
            }
            // Then act
            return act();
        } catch (Exception e) {
            // Log exception
            log.error("步骤执行失败", e);
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
