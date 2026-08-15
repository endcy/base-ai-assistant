package com.endcy.ai.mcp.manager;

import cn.hutool.core.util.StrUtil;
import com.endcy.service.domain.annotation.McpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebParser MCP client — web page content parsing toolkit.
 *
 * <p>MCP endpoint (SSE transport): configurable via {@code ai.mcp.webparser.endpoint},
 * uses DashScope API Key for authentication.</p>
 *
 * <p>Capabilities:</p>
 * <ul>
 *   <li>{@code parseWebPage} — fetch full web page content via URL, returned in markdown format</li>
 *   <li>{@code extractSubLinks} — extract sub-links from a web page, for exploring next-level pages</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
@Component
@McpServer("webparser")
public class WebParserMcpClient extends AbstractSseMcpClient {

    @Value("${ai.mcp.webparser.endpoint:}")
    private String endpoint;

    @Value("${ai.mcp.webparser.api-key:${spring.ai.dashscope.api-key:}}")
    private String apiKey;

    @Override
    protected String getEndpoint() {
        return endpoint;
    }

    @Override
    protected String getApiKey() {
        return apiKey;
    }

    @Override
    protected int getTimeoutSeconds() {
        return 60;
    } // Web parsing may be slow

    /**
     * Parse web page content, returns full body in markdown format.
     */
    @Tool(description = "Parse web page content, fetch the full page body and return in Markdown format. " +
            "Suitable for scenarios where detailed content of a web page is needed. " +
            "Better for structured long-text extraction than scrapeWebPage (official Bailian WebParser capability).")
    public String parseWebPage(
            @ToolParam(description = "Web page URL to parse, must include http:// or https://") String url) {
        if (StrUtil.isBlank(url)) {
            return "Please provide a web page URL";
        }
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        return callTool("网页解析", args);
    }

    /**
     * Extract sub-links from a web page, for exploring next-level pages.
     */
    @Tool(description = "Extract sub-links from a web page, for exploring next-level pages. " +
            "Suitable for scenarios where more related links need to be discovered from a page.")
    public String extractSubLinks(
            @ToolParam(description = "Web page URL to extract sub-links from") String url) {
        if (StrUtil.isBlank(url)) {
            return "Please provide a web page URL";
        }
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        return callTool("子链接提取", args);
    }
}
