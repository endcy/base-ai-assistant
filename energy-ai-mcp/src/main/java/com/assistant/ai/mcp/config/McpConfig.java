package com.assistant.ai.mcp.config;

import com.assistant.ai.mcp.manager.ElectricityPriceSearchTool;
import com.assistant.ai.mcp.manager.ImageSearchTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 本地工具mcp集成
 * mcp-servers.json 定义远程调用工具、独立运行mcp服务、其他工程定义的mcp服务等
 * 定义实现中，@Tool 和 @ToolParam的 description务必准确、清晰，这是大模型判断是否调用和如何填参的主要依据
 * 正确定义工具的输入参数 JSON Schema，确保大模型能生成格式正确的参数
 * 每个工具应功能单一且明确，避免过于复杂的功能，这有助于大模型做出更精准的决策
 *
 * @author endcy
 * @since 2025/08/03 14:19:02
 */
@Slf4j
@RequiredArgsConstructor
@ComponentScan(basePackages = {"com.assistant.ai.mcp"})
public class McpConfig {

    private final ImageSearchTool imageSearchTool;
    private final ElectricityPriceSearchTool electricityPriceSearchTool;

//    @Bean("specialMcpToolCallbackProvider")
//    public SyncMcpToolCallbackProvider specialMcpToolCallbackProvider() {
//        return new SyncMcpToolCallbackProvider();
//    }

    @Bean("imageSearchProvider")
    public ToolCallbackProvider imageSearchProvider() {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(imageSearchTool)
                                         .build();
    }

    @Bean("electricityPriceSearchProvider")
    public ToolCallbackProvider electricityPriceSearchProvider() {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(electricityPriceSearchTool)
                                         .build();
    }

}
