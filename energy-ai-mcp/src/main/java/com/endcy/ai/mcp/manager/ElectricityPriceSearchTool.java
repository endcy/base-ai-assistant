package com.endcy.ai.mcp.manager;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Electricity price query tool.
 *
 * <p>Implementation: {@link WebSearchMcpClient} web search for latest electricity prices →
 * {@link ChatModel} (qwen) parses into structured JSON.
 * Falls back to built-in default prices on any failure.</p>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Service
public class ElectricityPriceSearchTool {

    private final WebSearchMcpClient webSearchMcpClient;
    private final ChatModel chatModel;

    public ElectricityPriceSearchTool(WebSearchMcpClient webSearchMcpClient,
                                      @Lazy @Qualifier("dashScopeChatModel") ChatModel chatModel) {
        this.webSearchMcpClient = webSearchMcpClient;
        this.chatModel = chatModel;
    }

    @Tool(description = "Query peak/valley electricity prices (yuan/kWh) by city name, returns JSON: {\"peak\":0.84, \"flat\":0.52, \"valley\":0.31}. "
            + "Data source: latest public electricity price info via web search, then parsed by AI. Takes a few seconds.")
    public String searchElectricityPrice(@ToolParam(description = "City name, without '市' suffix, e.g.: Shenzhen, Beijing, Shanghai, Guangzhou") String city) {
        try {
            return getElectricityPrice(city);
        } catch (Exception e) {
            log.error("Electricity price query error city={}", city, e);
            return "Electricity price query failed: " + e.getMessage();
        }
    }

    private String getElectricityPrice(String city) {
        if (StrUtil.isBlank(city)) {
            return "{\"error\": \"Please provide a city name\"}";
        }
        String cityName = city.replace("市", "");

        // ① Web search
        String searchResult;
        try {
            String query = cityName + " industrial and commercial electricity peak valley price yuan/kWh latest";
            searchResult = webSearchMcpClient.search(query);
            log.info("Electricity price web search done city={} result length={}", cityName, searchResult.length());
        } catch (Exception e) {
            log.warn("Web search failed, falling back to default prices city={}: {}", cityName, e.getMessage());
            return defaultPrice(cityName);
        }

        // ② AI parse
        try {
            String parsed = parsePriceByAi(cityName, searchResult);
            // Check for error JSON from LLM (e.g. {"error":"..."}) — fall back to defaults
            if (parsed != null && parsed.contains("\"error\"")) {
                log.warn("AI parse returned error, falling back to defaults city={}: {}", cityName, parsed);
                return defaultPrice(cityName);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("AI parse failed, falling back to defaults city={}: {}", cityName, e.getMessage());
            return defaultPrice(cityName);
        }
    }

    /**
     * Call LLM to extract structured electricity price JSON from search results.
     */
    private String parsePriceByAi(String city, String searchResult) {
        String prompt = "You are an electricity price data extraction assistant. Based on the following web search results, "
                + "extract the current peak/valley electricity prices for [" + city + "] "
                + "(unit: yuan/kWh, keep 2 decimal places; prioritize industrial/commercial electricity).\n"
                + "Output ONLY a JSON in this format: {\"peak\":0.00, \"flat\":0.00, \"valley\":0.00}, no other text.\n"
                + "If no clear data in search results, output: {\"error\":\"no accurate price found\"}\n\n"
                + "Search results:\n" + StrUtil.maxLength(searchResult, 4000);

        ChatResponse response = chatModel.call(new Prompt(List.of(new UserMessage(prompt))));
        String text = response.getResult().getOutput().getText();
        if (StrUtil.isBlank(text)) {
            throw new RuntimeException("AI returned empty content");
        }
        // Cleanup: extract from first { to last }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Built-in default electricity prices (fallback when web search or AI parse fails).
     */
    private String defaultPrice(String city) {
        Map<String, String> defaults = Map.of(
                "深圳", "{\"peak\":0.84, \"flat\":0.52, \"valley\":0.31}",
                "北京", "{\"peak\":0.92, \"flat\":0.64, \"valley\":0.38}",
                "上海", "{\"peak\":0.86, \"flat\":0.58, \"valley\":0.36}",
                "广州", "{\"peak\":0.85, \"flat\":0.55, \"valley\":0.34}"
        );
        return defaults.getOrDefault(city,
                "{\"peak\":0.85, \"flat\":0.55, \"valley\":0.34, \"note\":\"no precise data for this city, returning national average\"}");
    }
}
