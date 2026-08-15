package com.endcy.ai.tools;

import com.endcy.ai.util.ToolSecurityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具。
 * <p>
 * 抓取 URL 由 LLM 生成，抓取前通过 {@link ToolSecurityUtils#checkUrl} 做 SSRF 校验，
 * 仅允许访问公网 http/https 地址。
 * </p>
 *
 * @author endcy
 */
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        String check = ToolSecurityUtils.checkUrl(url);
        if (check != null) {
            return check;
        }
        try {
            Document document = Jsoup.connect(url).get();
            return document.html();
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
