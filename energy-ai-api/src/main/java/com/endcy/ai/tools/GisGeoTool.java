package com.endcy.ai.tools;

import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Reverse geocoding tool — resolve coordinates to geographic location.
 *
 * <p>The geocoding service URL is injected via constructor from configuration properties.
 * No hardcoded internal IPs — fully configurable.</p>
 *
 * @author endcy
 * @since 2025/10/27
 */
public class GisGeoTool {

    // Reverse geocoding service URL template (configurable, injected via constructor)
    private final String gisGeoCoderUrl;

    public GisGeoTool(String gisGeoUrl) {
        this.gisGeoCoderUrl = gisGeoUrl;
    }

    @Tool(description = "根据经度纬度即经纬度坐标解析成省市区地理位置；地理坐标经纬度逆解析；将用户的地理位置经纬度坐标，转换为地理位置的城市名称")
    public String reverseGeoCoderTool(@ToolParam(description = "经度，用户地理位置坐标经度") Double lan,
                                      @ToolParam(description = "纬度，用户地理位置坐标纬度") Double lat) {
        try {
            String requestUrl = String.format(gisGeoCoderUrl, lat, lan);
            return HttpUtil.get(requestUrl, 8000);
        } catch (Exception e) {
            return "Error reverse geo code: " + e.getMessage();
        }
    }
}
