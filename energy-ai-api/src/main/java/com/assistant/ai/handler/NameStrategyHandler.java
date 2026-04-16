package com.assistant.ai.handler;

/**
 * 名称策略处理器接口
 * 用于将 ID 值转换为对应的名称
 *
 * @author endcy
 * @date 2026/04/10 14:36:49
 */
public interface NameStrategyHandler {

    /**
     * 根据策略键和值转换为对应的名称
     *
     * @param value 当前值
     * @return 转换后的名称
     */
    Object switchName(String name, Object value);
}
