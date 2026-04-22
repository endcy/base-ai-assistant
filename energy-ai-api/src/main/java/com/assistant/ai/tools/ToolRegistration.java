package com.assistant.ai.tools;

import com.assistant.ai.config.AiWebSearchApiProperties;
import com.assistant.ai.mcp.manager.ElectricityPriceSearchTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 集中的工具注册类
 * 支持两种注册方式：
 * 1. 手动注册：通过 @Tool 注解的类，用 ToolCallbacks.from() 注册
 * 2. 自动注册：实现 InnerTool 接口，由 ToolRegistry 自动扫描发现
 *
 * @author endcy
 * @date 2025/10/31
 */
@Configuration
@RequiredArgsConstructor
public class ToolRegistration {

    private final AiWebSearchApiProperties aiWebSearchApiProperties;
    private final ToolRegistry toolRegistry;


    /**
     * 通用工具（手动注册 + InnerTool 自动注册）
     */
    @Bean("commonWebTools")
    public ToolCallback[] commonWebTools() {
        // 手动注册的工具
        FileOperationTool fileOperationTool = new FileOperationTool();
        SimpleWebSearchTool simpleWebSearchTool = new SimpleWebSearchTool(aiWebSearchApiProperties.getSimpleKey());
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        ToolCallback[] manualTools = ToolCallbacks.from(
                fileOperationTool,
                simpleWebSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
        // 合并 InnerTool 自动注册的工具
        return mergeToolCallbacks(manualTools, toolRegistry.getAllToolCallbacks());
    }


    @Bean("energyInfoTools")
    public ToolCallback[] energyInfoTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        SimpleWebSearchTool simpleWebSearchTool = new SimpleWebSearchTool(aiWebSearchApiProperties.getSimpleKey());
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        ToolCallback[] manualTools = ToolCallbacks.from(
                fileOperationTool,
                simpleWebSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
        return mergeToolCallbacks(manualTools, toolRegistry.getAllToolCallbacks());
    }


    @Bean("ragTools")
    public ToolCallback[] ragTools() {
        GisGeoTool gisGeoTool = new GisGeoTool();
        SimpleWebSearchTool simpleWebSearchTool = new SimpleWebSearchTool(aiWebSearchApiProperties.getSimpleKey());
        ElectricityPriceSearchTool electricityPriceSearchTool = new ElectricityPriceSearchTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        return ToolCallbacks.from(
                gisGeoTool,
                simpleWebSearchTool,
                webScrapingTool,
                electricityPriceSearchTool
        );
    }

    /**
     * 合并两组 ToolCallback
     */
    private ToolCallback[] mergeToolCallbacks(ToolCallback[] existing, ToolCallback[] additional) {
        List<ToolCallback> all = new ArrayList<>(List.of(existing));
        for (ToolCallback cb : additional) {
            all.add(cb);
        }
        return all.toArray(new ToolCallback[0]);
    }
}
