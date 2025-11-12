package com.assistant.ai.tools;

import com.assistant.ai.config.AiWebSearchApiProperties;
import com.assistant.ai.mcp.manager.ElectricityPriceSearchTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
@RequiredArgsConstructor
public class ToolRegistration {

    private final AiWebSearchApiProperties aiWebSearchApiProperties;


    /**
     * 通用工具
     */
    @Bean("commonWebTools")
    public ToolCallback[] commonWebTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        SimpleWebSearchTool simpleWebSearchTool = new SimpleWebSearchTool(aiWebSearchApiProperties.getSimpleKey());
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                fileOperationTool,
                simpleWebSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
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
        return ToolCallbacks.from(
                fileOperationTool,
                simpleWebSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
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
}
