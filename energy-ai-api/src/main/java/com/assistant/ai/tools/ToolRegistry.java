package com.assistant.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心
 * 启动时自动扫描所有 InnerTool Bean，收集并注册所有工具回调
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
@Component
public class ToolRegistry {

    private final List<ToolCallback> allToolCallbacks = new ArrayList<>();

    public ToolRegistry(ListableBeanFactory beanFactory) {
        Map<String, InnerTool> innerToolBeans = beanFactory.getBeansOfType(InnerTool.class);
        if (innerToolBeans.isEmpty()) {
            log.info("No InnerTool beans found for auto-registration");
        } else {
            for (Map.Entry<String, InnerTool> entry : innerToolBeans.entrySet()) {
                InnerTool innerTool = entry.getValue();
                List<ToolCallback> callbacks = innerTool.loadToolCallbacks();
                allToolCallbacks.addAll(callbacks);
                log.info("Registered {} tools from InnerTool bean: {}", callbacks.size(), entry.getKey());
            }
        }
        log.info("Total tools registered via InnerTool: {}", allToolCallbacks.size());
    }

    /**
     * 获取所有已注册的工具回调
     */
    public ToolCallback[] getAllToolCallbacks() {
        return allToolCallbacks.toArray(new ToolCallback[0]);
    }

    /**
     * 获取已注册工具数量
     */
    public int getToolCount() {
        return allToolCallbacks.size();
    }
}
