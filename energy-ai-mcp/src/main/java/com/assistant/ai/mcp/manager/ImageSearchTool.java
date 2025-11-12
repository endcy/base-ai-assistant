package com.assistant.ai.mcp.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具
 * mcp示例
 */
@Service
public class ImageSearchTool {

    // 搜片搜索供应商Pexels API 密钥（需从官网申请）
    private static final String API_KEY = "pexels API Key";

    // Pexels 常规搜索接口（请以文档为准）
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "在网络上搜索图片，网络搜图搜索素材")
    public String searchImage(@ToolParam(description = "网络搜索图片搜索关键词") String query) {
        try {
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     */
    public List<String> searchMediumImages(String query) {
        // 设置请求头（包含API密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", API_KEY);

        // 设置请求参数（仅包含query，可根据文档补充page、per_page等参数）
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        // 发送 GET 请求
        String response = HttpUtil.createGet(API_URL)
                                  .addHeaders(headers)
                                  .form(params)
                                  .execute()
                                  .body();

        // 解析响应JSON（假设响应结构包含"photos"数组，每个元素包含"medium"字段）
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
