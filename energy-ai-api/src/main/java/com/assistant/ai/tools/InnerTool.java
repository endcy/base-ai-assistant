package com.assistant.ai.tools;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 可插拔工具注册接口
 * 实现此接口的 Bean 会在启动时被自动扫描并注册到 Agent 中
 * 新增工具只需实现此接口，无需修改已有代码
 *
 * @author endcy
 * @date 2026/04/22
 */
public interface InnerTool {

    /**
     * 加载工具回调列表
     * 返回的 ToolCallback 会被统一注册到 Agent 的工具链中
     *
     * @return 工具回调列表
     */
    List<ToolCallback> loadToolCallbacks();
}
