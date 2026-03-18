package com.assistant.ai.mcp.manager;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 站点和设备信息查询工具
 * 支持站点信息、设备状态查询功能
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationDeviceTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "查询站点信息，根据站点 ID、站点名称或城市查询站点详细信息")
    public String queryStationInfo(
            @ToolParam(description = "站点 ID", required = false) Long stationId,
            @ToolParam(description = "站点名称", required = false) String stationName,
            @ToolParam(description = "城市", required = false) String city
    ) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT id, station_code, station_name, province, city, district,
                       address, latitude, longitude, status, total_devices,
                       available_devices, group_id
                FROM biz_station WHERE 1=1
            """);

            if (stationId != null) {
                sqlBuilder.append(" AND id = ?");
            }
            if (StrUtil.isNotBlank(stationName)) {
                sqlBuilder.append(" AND station_name LIKE ?");
            }
            if (StrUtil.isNotBlank(city)) {
                sqlBuilder.append(" AND city = ?");
            }
            sqlBuilder.append(" LIMIT 20");

            List<Object> params = new java.util.ArrayList<>();
            if (stationId != null) params.add(stationId);
            if (StrUtil.isNotBlank(stationName)) params.add("%" + stationName + "%");
            if (StrUtil.isNotBlank(city)) params.add(city);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            if (results.isEmpty()) {
                return "未找到符合条件的站点信息";
            }

            return buildStationResponse(results);
        } catch (Exception e) {
            log.error("查询站点信息失败：{}", e.getMessage(), e);
            return "查询站点信息失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询设备状态，根据设备 ID、站点 ID 或设备状态查询设备信息")
    public String queryDeviceStatus(
            @ToolParam(description = "设备 ID", required = false) Long deviceId,
            @ToolParam(description = "站点 ID", required = false) Long stationId,
            @ToolParam(description = "设备状态", required = false) String status
    ) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT d.id, d.device_code, d.device_name, d.device_type,
                       d.station_id, s.station_name, d.status, d.power_rating,
                       d.group_id
                FROM biz_device d
                LEFT JOIN biz_station s ON d.station_id = s.id
                WHERE 1=1
            """);

            if (deviceId != null) {
                sqlBuilder.append(" AND d.id = ?");
            }
            if (stationId != null) {
                sqlBuilder.append(" AND d.station_id = ?");
            }
            if (StrUtil.isNotBlank(status)) {
                sqlBuilder.append(" AND d.status = ?");
            }
            sqlBuilder.append(" LIMIT 50");

            List<Object> params = new java.util.ArrayList<>();
            if (deviceId != null) params.add(deviceId);
            if (stationId != null) params.add(stationId);
            if (StrUtil.isNotBlank(status)) params.add(status);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            if (results.isEmpty()) {
                return "未找到符合条件的设备信息";
            }

            return buildDeviceResponse(results);
        } catch (Exception e) {
            log.error("查询设备状态失败：{}", e.getMessage(), e);
            return "查询设备状态失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询站点的可用设备数量")
    public String queryStationAvailableDevices(@ToolParam(description = "站点 ID") Long stationId) {
        try {
            String sql = """
                SELECT station_name, total_devices, available_devices, status
                FROM biz_station WHERE id = ?
            """;

            Map<String, Object> result = jdbcTemplate.queryForMap(sql, stationId);

            if (result == null) {
                return "未找到站点信息";
            }

            return String.format("站点 %s (ID:%d)：总设备 %d 台，可用 %d 台，站点状态：%s",
                    result.get("station_name"), stationId,
                    ((Number) result.get("total_devices")).intValue(),
                    ((Number) result.get("available_devices")).intValue(),
                    result.get("status"));
        } catch (Exception e) {
            log.error("查询站点设备数量失败：{}", e.getMessage(), e);
            return "查询站点设备数量失败：" + e.getMessage();
        }
    }

    @Tool(description = "按城市统计站点数量和设备情况")
    public String queryStationStatsByCity() {
        try {
            String sql = """
                SELECT city,
                       COUNT(*) as station_count,
                       SUM(total_devices) as total_devices,
                       SUM(available_devices) as available_devices
                FROM biz_station
                GROUP BY city
                ORDER BY station_count DESC
            """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                return "暂无站点统计信息";
            }

            StringBuilder sb = new StringBuilder("各城市站点统计：\n");
            for (Map<String, Object> row : results) {
                sb.append("- ").append(row.get("city"))
                  .append(": 站点数=").append(row.get("station_count"))
                  .append(", 总设备=").append(row.get("total_devices"))
                  .append("台，可用=").append(row.get("available_devices")).append("台\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询站点统计失败：{}", e.getMessage(), e);
            return "查询站点统计失败：" + e.getMessage();
        }
    }

    private String buildStationResponse(List<Map<String, Object>> stations) {
        StringBuilder sb = new StringBuilder("查询到 ").append(stations.size()).append(" 个站点信息：\n");
        for (Map<String, Object> station : stations) {
            sb.append("\n【站点】").append(station.get("station_name")).append("\n");
            sb.append("  站点编码：").append(station.get("station_code")).append("\n");
            sb.append("  站点 ID: ").append(station.get("id")).append("\n");
            sb.append("  地址：").append(station.get("province"))
              .append(station.get("city")).append(station.get("district"))
              .append(station.get("address")).append("\n");
            sb.append("  坐标：").append(station.get("latitude")).append(", ").append(station.get("longitude")).append("\n");
            sb.append("  状态：").append(station.get("status")).append("\n");
            sb.append("  设备：").append(station.get("total_devices")).append("台 (可用 ").append(station.get("available_devices")).append("台)\n");
        }
        return sb.toString();
    }

    private String buildDeviceResponse(List<Map<String, Object>> devices) {
        StringBuilder sb = new StringBuilder("查询到 ").append(devices.size()).append(" 台设备信息：\n");
        for (Map<String, Object> device : devices) {
            sb.append("\n【设备】").append(device.get("device_name")).append("\n");
            sb.append("  设备编码：").append(device.get("device_code")).append("\n");
            sb.append("  设备 ID: ").append(device.get("id")).append("\n");
            sb.append("  类型：").append(device.get("device_type")).append("\n");
            sb.append("  功率：").append(device.get("power_rating")).append(" kW\n");
            sb.append("  状态：").append(device.get("status")).append("\n");
            sb.append("  所属站点：").append(device.get("station_name")).append("\n");
        }
        return sb.toString();
    }
}
