package com.assistant.ai.tools;

import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 根据坐标解析地理位置
 */
public class GisGeoTool {

    //定义自己的逆地里解析服务接口
    private static final String URL = "http://192.168.21.190:8075/gis/reverse-geocoder?latitude=%s&longitude=%s";

    @Tool(description = "根据经度纬度即经纬度坐标解析成省市区地理位置；地理坐标经纬度逆解析；将用户的地理位置经纬度坐标，转换为地理位置的城市名称")
    public String reverseGeoCoderTool(@ToolParam(description = "经度，用户地理位置坐标经度") Double lan,
                                      @ToolParam(description = "纬度，用户地理位置坐标纬度") Double lat) {
        try {
            String requestUrl = String.format(URL, lat, lan);
            return HttpUtil.get(requestUrl, 8000);
        } catch (Exception e) {
            return "Error reverse geo code: " + e.getMessage();
        }
    }
}
