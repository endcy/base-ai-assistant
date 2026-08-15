package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Station query tool — Step 2.7.
 *
 * <p>Queries station information: station list, station details, device status, etc.</p>
 *
 * <p>Currently uses mock data (replace with RPC calls in production).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class QueryStationTool {

    // Mock data (replace with RPC calls in production)
    private static final Map<String, Map<String, Object>> MOCK_STATIONS = new HashMap<>();

    static {
        Map<String, Object> station1 = new HashMap<>();
        station1.put("name", "Demo Station A");
        station1.put("address", "123 Main Street, District A");
        station1.put("status", "Operational");
        station1.put("totalDevices", 10);
        station1.put("availableDevices", 7);
        station1.put("types", "Standard,Express");
        MOCK_STATIONS.put("ST-A-001", station1);

        Map<String, Object> station2 = new HashMap<>();
        station2.put("name", "Demo Station B");
        station2.put("address", "456 Commerce Road, District B");
        station2.put("status", "Operational");
        station2.put("totalDevices", 8);
        station2.put("availableDevices", 5);
        station2.put("types", "Express");
        MOCK_STATIONS.put("ST-B-001", station2);
    }

    @Tool(description = "Query station information. Can query station list or a single station's details. " +
            "Returns station name, address, status, device count and types.")
    public String queryStation(
            @ToolParam(description = "Station ID (optional, returns all stations if not provided)") String stationId,
            @ToolParam(description = "Query type: LIST=station list, DETAIL=station details") String queryType) {

        log.info("Query station: stationId={}, queryType={}", stationId, queryType);

        if ("DETAIL".equalsIgnoreCase(queryType)) {
            if (StrUtil.isBlank(stationId)) {
                return "Station detail query requires stationId";
            }
            return queryStationDetail(stationId);
        }

        // Default: return station list
        return queryStationList();
    }

    private String queryStationList() {
        if (MOCK_STATIONS.isEmpty()) {
            return "No station data available";
        }

        StringBuilder sb = new StringBuilder("Station list:\n");
        for (Map.Entry<String, Map<String, Object>> entry : MOCK_STATIONS.entrySet()) {
            Map<String, Object> station = entry.getValue();
            sb.append("- ").append(entry.getKey())
              .append(": ").append(station.get("name"))
              .append(" (").append(station.get("status")).append(")\n");
        }
        return sb.toString();
    }

    private String queryStationDetail(String stationId) {
        Map<String, Object> station = MOCK_STATIONS.get(stationId);
        if (station == null) {
            return "Station not found: " + stationId;
        }

        return String.format(
                "Station details [%s]:\n" +
                        "Name: %s\n" +
                        "Address: %s\n" +
                        "Status: %s\n" +
                        "Total devices: %s\n" +
                        "Available devices: %s\n" +
                        "Device types: %s",
                stationId,
                station.get("name"),
                station.get("address"),
                station.get("status"),
                station.get("totalDevices"),
                station.get("availableDevices"),
                station.get("types")
        );
    }
}
