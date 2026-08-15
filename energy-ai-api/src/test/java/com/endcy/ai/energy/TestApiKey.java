package com.endcy.ai.energy;

/**
 * 仅用于测试获取 API Key
 *
 * <p>从环境变量读取，避免在代码中硬编码真实密钥。</p>
 *
 * @author endcy
 * @date 2025/10/23
 */
public interface TestApiKey {

    /**
     * DashScope API Key（读取环境变量 DASHSCOPE_API_KEY）
     */
    String API_KEY = System.getenv().getOrDefault("DASHSCOPE_API_KEY", "${DASHSCOPE_API_KEY}");
}
