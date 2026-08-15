package com.endcy.ai.mcp.manager;

import com.endcy.service.domain.annotation.McpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Weather MCP client — provides real-time weather, forecast, AQI, life index, weather alerts, etc.
 *
 * <p>MCP endpoint: configurable via {@code ai.mcp.weather.endpoint},
 * uses DashScope API Key for authentication. Each weather tool has an independent fixed token
 * (from schema definition).</p>
 *
 * <p>Note: requires cityId (city ID), not city name. Common city IDs are built-in;
 * if not found, try using city name as cityId (some APIs are compatible).</p>
 *
 * <p>Token configuration (set via application properties or Apollo):</p>
 * <ul>
 *   <li>{@code ai.mcp.weather.token.current} — real-time weather token</li>
 *   <li>{@code ai.mcp.weather.token.24h} — 24h forecast token</li>
 *   <li>{@code ai.mcp.weather.token.15d} — 15-day forecast token</li>
 *   <li>{@code ai.mcp.weather.token.aqi} — AQI token</li>
 *   <li>{@code ai.mcp.weather.token.life} — life index token</li>
 *   <li>{@code ai.mcp.weather.token.warn} — weather alert token</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
@McpServer("weather")
public class WeatherMcpClient extends AbstractMcpClient {

    @Value("${ai.mcp.weather.endpoint:}")
    private String endpoint;

    @Value("${ai.mcp.weather.api-key:${spring.ai.dashscope.api-key:}}")
    private String apiKey;

    // Per-tool fixed tokens (from schema definition, shared by all users)
    @Value("${ai.mcp.weather.token.current:}")
    private String tokenCurrent;

    @Value("${ai.mcp.weather.token.24h:}")
    private String token24h;

    @Value("${ai.mcp.weather.token.15d:}")
    private String token15d;

    @Value("${ai.mcp.weather.token.aqi:}")
    private String tokenAqi;

    @Value("${ai.mcp.weather.token.life:}")
    private String tokenLife;

    @Value("${ai.mcp.weather.token.warn:}")
    private String tokenWarn;

    @Override
    protected String getEndpoint() {
        return endpoint;
    }

    @Override
    protected String getApiKey() {
        return apiKey;
    }

    // Common city ID mapping (based on common weather API standards)
    private static final Map<String, String> CITY_ID_MAP = Map.ofEntries(
            Map.entry("北京", "101010100"), Map.entry("上海", "101020100"),
            Map.entry("广州", "101280101"), Map.entry("深圳", "101280601"),
            Map.entry("杭州", "101210101"), Map.entry("南京", "101190101"),
            Map.entry("成都", "101270101"), Map.entry("武汉", "101200101"),
            Map.entry("西安", "101110101"), Map.entry("重庆", "101040100"),
            Map.entry("天津", "101030100"), Map.entry("苏州", "101190401"),
            Map.entry("长沙", "101250101"), Map.entry("郑州", "101180101"),
            Map.entry("青岛", "101120201"), Map.entry("大连", "101070201"),
            Map.entry("厦门", "101230201"), Map.entry("合肥", "101220101"),
            Map.entry("福州", "101230101"), Map.entry("济南", "101120101"),
            Map.entry("哈尔滨", "101050101"), Map.entry("沈阳", "101070101"),
            Map.entry("昆明", "101290101"), Map.entry("贵阳", "101260101"),
            Map.entry("南宁", "101300101"), Map.entry("海口", "101310101"),
            Map.entry("兰州", "101160101"), Map.entry("太原", "101100101"),
            Map.entry("石家庄", "101090101"), Map.entry("南昌", "101240101")
    );

    /**
     * Resolve city name to cityId, return as-is if not found (some APIs accept city names)
     */
    private String resolveCityId(String city) {
        if (city == null)
            return "";
        String clean = city.replace("市", "").trim();
        return CITY_ID_MAP.getOrDefault(clean, city);
    }

    // ==================== Tool methods ====================

    @Tool(description = "Query city real-time weather: temperature, humidity, wind direction/speed, UV, pressure, feels-like temperature, etc.")
    public String currentWeather(
            @ToolParam(description = "City name, e.g. 'Shenzhen', 'Beijing'") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", tokenCurrent);
        return callTool("天气实况", args);
    }

    @Tool(description = "Query hourly weather forecast for the next 24 hours (temperature, weather conditions, wind, etc.).")
    public String weather24h(
            @ToolParam(description = "City name") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", token24h);
        return callTool("天气预报24小时", args);
    }

    @Tool(description = "Query 15-day weather forecast (suitable for trend analysis, travel planning).")
    public String weather15d(
            @ToolParam(description = "City name") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", token15d);
        return callTool("天气预报15天", args);
    }

    @Tool(description = "Query city Air Quality Index (AQI) and sub-item data (PM2.5, PM10, SO2, NO2, etc.).")
    public String airQuality(
            @ToolParam(description = "City name") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", tokenAqi);
        return callTool("空气质量指数", args);
    }

    @Tool(description = "Query city weather life index (clothing, car wash, exercise, UV, travel advice, etc.).")
    public String lifeIndex(
            @ToolParam(description = "City name") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", tokenLife);
        return callTool("生活指数", args);
    }

    @Tool(description = "Query city weather alerts (heavy rain, high temperature, typhoon, etc.).")
    public String weatherWarning(
            @ToolParam(description = "City name") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("cityId", resolveCityId(city));
        args.put("token", tokenWarn);
        return callTool("天气预警", args);
    }
}
