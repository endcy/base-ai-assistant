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
 * AMap Maps MCP client — provides geocoding, POI search, route planning, distance measurement, weather query, etc.
 *
 * <p>MCP endpoint: configurable via {@code ai.mcp.amap.endpoint},
 * uses DashScope API Key for authentication.</p>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
@McpServer("amap")
public class AMapMcpClient extends AbstractMcpClient {

    @Value("${ai.mcp.amap.endpoint:}")
    private String endpoint;

    @Value("${ai.mcp.amap.api-key:${spring.ai.dashscope.api-key:}}")
    private String apiKey;

    @Override
    protected String getEndpoint() {
        return endpoint;
    }

    @Override
    protected String getApiKey() {
        return apiKey;
    }

    // ==================== Geocoding ====================

    @Tool(description = "Convert a structured address to longitude/latitude coordinates (forward geocoding). Supports landmarks, buildings, POI name resolution. " +
            "Example: 'Shenzhen Nanshan District Tech Park' → longitude/latitude.")
    public String geocode(
            @ToolParam(description = "Structured address or landmark name") String address,
            @ToolParam(description = "Target city (optional, improves accuracy)") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("address", address);
        if (StrUtil.isNotBlank(city))
            args.put("city", city);
        return callTool("maps_geo", args);
    }

    @Tool(description = "Convert longitude/latitude coordinates to administrative division address information (reverse geocoding). " +
            "Coordinate format: 'longitude,latitude' (e.g. '114.05,22.55').")
    public String reverseGeocode(
            @ToolParam(description = "Longitude/latitude, format: 'longitude,latitude' (e.g. '114.05,22.55')") String location) {
        Map<String, Object> args = new HashMap<>();
        args.put("location", location);
        return callTool("maps_regeocode", args);
    }

    // ==================== POI Search ====================

    @Tool(description = "Keyword POI (Point of Interest) search. Returns relevant location info based on keywords (e.g. stations, malls, restaurants).")
    public String textSearch(
            @ToolParam(description = "Query keyword, e.g. 'station', 'mall'") String keywords,
            @ToolParam(description = "Target city (optional, e.g. 'Shenzhen')") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("keywords", keywords);
        if (StrUtil.isNotBlank(city))
            args.put("city", city);
        return callTool("maps_text_search", args);
    }

    @Tool(description = "Nearby search: search POI within a specified radius (meters) from a coordinate point. " +
            "Use for scenarios like 'nearby stations', 'surrounding restaurants'.")
    public String aroundSearch(
            @ToolParam(description = "Search keyword, e.g. 'station', 'restaurant'") String keywords,
            @ToolParam(description = "Center coordinate, format: 'longitude,latitude'") String location,
            @ToolParam(description = "Search radius (meters), e.g. 1000, 3000, 5000") String radius) {
        Map<String, Object> args = new HashMap<>();
        args.put("keywords", keywords);
        args.put("location", location);
        args.put("radius", StrUtil.blankToDefault(radius, "1000"));
        return callTool("maps_around_search", args);
    }

    // ==================== Route Planning ====================

    @Tool(description = "Driving route planning. Plan driving route based on start/end longitude/latitude. Max 500km supported.")
    public String routeDriving(
            @ToolParam(description = "Start longitude/latitude, format: 'longitude,latitude'") String origin,
            @ToolParam(description = "End longitude/latitude, format: 'longitude,latitude'") String destination) {
        Map<String, Object> args = new HashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        return callTool("maps_direction_driving", args);
    }

    @Tool(description = "Walking route planning. Plan walking route based on start/end longitude/latitude. Max 100km supported.")
    public String routeWalking(
            @ToolParam(description = "Start longitude/latitude, format: 'longitude,latitude'") String origin,
            @ToolParam(description = "End longitude/latitude, format: 'longitude,latitude'") String destination) {
        Map<String, Object> args = new HashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        return callTool("maps_direction_walking", args);
    }

    // ==================== Distance Measurement ====================

    @Tool(description = "Measure distance between two or more points. Supports driving, walking, and straight-line distance.")
    public String distance(
            @ToolParam(description = "Start longitude/latitude (multiple separated by |), format: 'longitude,latitude|longitude,latitude'") String origins,
            @ToolParam(description = "End longitude/latitude, format: 'longitude,latitude'") String destination,
            @ToolParam(description = "Distance type: 0=straight-line, 1=driving, 3=walking") String type) {
        Map<String, Object> args = new HashMap<>();
        args.put("origins", origins);
        args.put("destination", destination);
        args.put("type", StrUtil.blankToDefault(type, "0"));
        return callTool("maps_distance", args);
    }

    // ==================== Weather ====================

    @Tool(description = "Query current weather for a city by name (AMap version, simple and convenient).")
    public String weather(
            @ToolParam(description = "City name or adcode, e.g. 'Shenzhen', 'Beijing'") String city) {
        Map<String, Object> args = new HashMap<>();
        args.put("city", city);
        return callTool("maps_weather", args);
    }
}
