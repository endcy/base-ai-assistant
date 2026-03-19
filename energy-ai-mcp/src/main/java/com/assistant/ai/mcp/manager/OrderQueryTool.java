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
 * 订单查询工具
 * 支持充电订单、放电订单的查询功能
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "查询充电订单详情，根据订单号、用户 ID 或站点 ID 查询充电订单信息")
    public String queryChargeOrder(
            @ToolParam(description = "订单编号") String orderNo,
            @ToolParam(description = "用户 ID", required = false) Long userId,
            @ToolParam(description = "站点 ID", required = false) Long stationId
    ) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("""
                        SELECT id, order_no, user_id, station_id, station_name, device_id, device_name,
                               order_type, status, start_time, end_time, total_power, total_amount,
                               electricity_fee, service_fee, create_time
                        FROM biz_charge_order WHERE 1=1
                    """);

            if (StrUtil.isNotBlank(orderNo)) {
                sqlBuilder.append(" AND order_no = ?");
            }
            if (userId != null) {
                sqlBuilder.append(" AND user_id = ?");
            }
            if (stationId != null) {
                sqlBuilder.append(" AND station_id = ?");
            }
            sqlBuilder.append(" ORDER BY create_time DESC LIMIT 10");

            List<Object> params = new java.util.ArrayList<>();
            if (StrUtil.isNotBlank(orderNo))
                params.add(orderNo);
            if (userId != null)
                params.add(userId);
            if (stationId != null)
                params.add(stationId);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            if (results.isEmpty()) {
                return "未找到符合条件的充电订单";
            }

            return buildOrderResponse(results, "充电订单");
        } catch (Exception e) {
            log.error("查询充电订单失败：{}", e.getMessage(), e);
            return "查询充电订单失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询放电订单详情，根据订单号、用户 ID 或站点 ID 查询放电订单信息")
    public String queryDischargeOrder(
            @ToolParam(description = "订单编号") String orderNo,
            @ToolParam(description = "用户 ID", required = false) Long userId,
            @ToolParam(description = "站点 ID", required = false) Long stationId
    ) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("""
                        SELECT id, order_no, user_id, station_id, station_name, device_id, device_name,
                               order_type, status, start_time, end_time, total_power, total_amount,
                               income_amount, create_time
                        FROM biz_discharge_order WHERE 1=1
                    """);

            if (StrUtil.isNotBlank(orderNo)) {
                sqlBuilder.append(" AND order_no = ?");
            }
            if (userId != null) {
                sqlBuilder.append(" AND user_id = ?");
            }
            if (stationId != null) {
                sqlBuilder.append(" AND station_id = ?");
            }
            sqlBuilder.append(" ORDER BY create_time DESC LIMIT 10");

            List<Object> params = new java.util.ArrayList<>();
            if (StrUtil.isNotBlank(orderNo))
                params.add(orderNo);
            if (userId != null)
                params.add(userId);
            if (stationId != null)
                params.add(stationId);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            if (results.isEmpty()) {
                return "未找到符合条件的放电订单";
            }

            return buildDischargeOrderResponse(results, "放电订单");
        } catch (Exception e) {
            log.error("查询放电订单失败：{}", e.getMessage(), e);
            return "查询放电订单失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户的订单统计信息，包括充电次数、放电次数、总消费金额等")
    public String queryUserOrderStats(@ToolParam(description = "用户 ID") Long userId) {
        try {
            String sql = """
                        SELECT
                            '充电订单' as order_type,
                            COUNT(*) as order_count,
                            COALESCE(SUM(total_amount), 0) as total_amount,
                            COALESCE(SUM(total_power), 0) as total_power
                        FROM biz_charge_order WHERE user_id = ?
                        UNION ALL
                        SELECT
                            '放电订单' as order_type,
                            COUNT(*) as order_count,
                            COALESCE(SUM(total_amount), 0) as total_amount,
                            COALESCE(SUM(total_power), 0) as total_power
                        FROM biz_discharge_order WHERE user_id = ?
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId, userId);

            if (results.isEmpty()) {
                return "未找到用户订单统计信息";
            }

            StringBuilder sb = new StringBuilder("用户 ").append(userId).append(" 的订单统计：\n");
            for (Map<String, Object> row : results) {
                sb.append("- ").append(row.get("order_type"))
                  .append(": 订单数=").append(row.get("order_count"))
                  .append(", 总金额=").append(row.get("total_amount"))
                  .append(" 元，总电量=").append(row.get("total_power")).append(" kWh\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询用户订单统计失败：{}", e.getMessage(), e);
            return "查询用户订单统计失败：" + e.getMessage();
        }
    }

    private String buildOrderResponse(List<Map<String, Object>> orders, String orderType) {
        StringBuilder sb = new StringBuilder("查询到 ").append(orders.size()).append(" 条").append(orderType).append("信息：\n");
        for (Map<String, Object> order : orders) {
            sb.append("\n【订单】").append(order.get("order_no")).append("\n");
            sb.append("  用户 ID: ").append(order.get("user_id")).append("\n");
            sb.append("  站点：").append(order.get("station_name")).append("\n");
            sb.append("  设备：").append(order.get("device_name")).append("\n");
            sb.append("  类型：").append(order.get("order_type")).append("\n");
            sb.append("  状态：").append(order.get("status")).append("\n");
            sb.append("  开始时间：").append(order.get("start_time")).append("\n");
            sb.append("  结束时间：").append(order.get("end_time")).append("\n");
            sb.append("  总电量：").append(order.get("total_power")).append(" kWh\n");
            sb.append("  总金额：").append(order.get("total_amount")).append(" 元\n");
            sb.append("  电费：").append(order.get("electricity_fee")).append(" 元\n");
            sb.append("  服务费：").append(order.get("service_fee")).append(" 元\n");
        }
        return sb.toString();
    }

    private String buildDischargeOrderResponse(List<Map<String, Object>> orders, String orderType) {
        StringBuilder sb = new StringBuilder("查询到 ").append(orders.size()).append(" 条").append(orderType).append("信息：\n");
        for (Map<String, Object> order : orders) {
            sb.append("\n【订单】").append(order.get("order_no")).append("\n");
            sb.append("  用户 ID: ").append(order.get("user_id")).append("\n");
            sb.append("  站点：").append(order.get("station_name")).append("\n");
            sb.append("  设备：").append(order.get("device_name")).append("\n");
            sb.append("  类型：").append(order.get("order_type")).append("\n");
            sb.append("  状态：").append(order.get("status")).append("\n");
            sb.append("  开始时间：").append(order.get("start_time")).append("\n");
            sb.append("  结束时间：").append(order.get("end_time")).append("\n");
            sb.append("  总放电量：").append(order.get("total_power")).append(" kWh\n");
            sb.append("  总金额：").append(order.get("total_amount")).append(" 元\n");
            sb.append("  收益：").append(order.get("income_amount")).append(" 元\n");
        }
        return sb.toString();
    }
}
