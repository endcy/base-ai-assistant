package com.endcy.ai.mcp.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Image search tool — MCP example tool.
 * <p>Searches image materials via Pexels API. API Key configured via properties.</p>
 *
 * @author endcy
 * @since 2025/10/27
 */
@Service
public class ImageSearchTool {

    /**
     * Pexels API key (configured via ai.mcp.pexels.api-key)
     */
    @Value("${ai.mcp.pexels.api-key:}")
    private String apiKey;

    /**
     * Pexels HTTP API base URL
     */
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "Search image materials via Pexels image library, returns a list of image URLs")
    public String searchImage(
            @ToolParam(description = "Image search keyword, e.g. station, electric vehicle, solar panel") String query) {
        try {
            if (StrUtil.isBlank(apiKey) || "pexels API Key".equals(apiKey)) {
                return "Image search not enabled (Pexels API Key not configured). Please set ai.mcp.pexels.api-key.";
            }
            List<String> urls = searchMediumImages(query);
            if (urls.isEmpty()) {
                return "No images found related to '" + query + "'";
            }
            return String.join(", ", urls);
        } catch (Exception e) {
            return "Image search failed: " + e.getMessage();
        }
    }

    /**
     * Search medium-sized image list
     */
    public List<String> searchMediumImages(String query) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", 10);

        String response = HttpUtil.createGet(API_URL)
                                  .addHeaders(headers)
                                  .form(params)
                                  .execute()
                                  .body();

        return JSONUtil.parseObj(response)
                       .getJSONArray("photos")
                       .stream()
                       .map(photoObj -> (JSONObject) photoObj)
                       .map(photoObj -> photoObj.getJSONObject("src"))
                       .map(photo -> photo.getStr("medium"))
                       .filter(StrUtil::isNotBlank)
                       .collect(Collectors.toList());
    }
}
