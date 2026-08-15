package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order query tool — Step 2.7.
 *
 * <p>Queries order information: order list, order details, user order history, etc.</p>
 *
 * <p>Currently uses mock data (replace with RPC calls in production).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class QueryOrderTool {

    // Mock data (replace with RPC calls in production)
    private static final List<Map<String, Object>> MOCK_ORDERS = new ArrayList<>();

    static {
        Map<String, Object> order1 = new HashMap<>();
        order1.put("orderId", "ORD-20260308-001");
        order1.put("stationId", "ST-A-001");
        order1.put("userId", "U001");
        order1.put("startTime", "2026-03-08 10:30:00");
        order1.put("endTime", "2026-03-08 12:15:00");
        order1.put("quantity", 45.5);
        order1.put("amount", 68.25);
        order1.put("status", "Completed");
        MOCK_ORDERS.add(order1);

        Map<String, Object> order2 = new HashMap<>();
        order2.put("orderId", "ORD-20260308-002");
        order2.put("stationId", "ST-B-001");
        order2.put("userId", "U002");
        order2.put("startTime", "2026-03-08 14:00:00");
        order2.put("endTime", "");
        order2.put("quantity", 20.0);
        order2.put("amount", 30.0);
        order2.put("status", "In progress");
        MOCK_ORDERS.add(order2);
    }

    @Tool(description = "Query order information. Can query by order ID, station ID, or user ID. Returns order details or order list.")
    public String queryOrder(
            @ToolParam(description = "Order ID (optional)") String orderId,
            @ToolParam(description = "Station ID (optional)") String stationId,
            @ToolParam(description = "User ID (optional)") String userId) {

        log.info("Query order: orderId={}, stationId={}, userId={}", orderId, stationId, userId);

        // Query a single order detail
        if (StrUtil.isNotBlank(orderId)) {
            return queryOrderDetail(orderId);
        }

        // Filter order list by conditions
        return queryOrderListByFilter(stationId, userId);
    }

    private String queryOrderDetail(String orderId) {
        for (Map<String, Object> order : MOCK_ORDERS) {
            if (orderId.equals(order.get("orderId"))) {
                return String.format(
                        "Order details [%s]:\n" +
                                "Station ID: %s\n" +
                                "User ID: %s\n" +
                                "Start time: %s\n" +
                                "End time: %s\n" +
                                "Quantity: %s\n" +
                                "Amount: %s\n" +
                                "Status: %s",
                        orderId,
                        order.get("stationId"),
                        order.get("userId"),
                        order.get("startTime"),
                        StrUtil.blankToDefault((String) order.get("endTime"), "In progress"),
                        order.get("quantity"),
                        order.get("amount"),
                        order.get("status")
                );
            }
        }
        return "Order not found: " + orderId;
    }

    private String queryOrderListByFilter(String stationId, String userId) {
        StringBuilder sb = new StringBuilder("Order list:\n");
        int count = 0;

        for (Map<String, Object> order : MOCK_ORDERS) {
            // Filter by station ID
            if (StrUtil.isNotBlank(stationId) && !stationId.equals(order.get("stationId"))) {
                continue;
            }
            // Filter by user ID
            if (StrUtil.isNotBlank(userId) && !userId.equals(order.get("userId"))) {
                continue;
            }

            sb.append("- ").append(order.get("orderId"))
              .append(": ").append(order.get("stationId"))
              .append(" | ").append(order.get("startTime"))
              .append(" | qty=").append(order.get("quantity"))
              .append(" | amount=").append(order.get("amount"))
              .append(" | ").append(order.get("status"))
              .append("\n");
            count++;
        }

        if (count == 0) {
            return "No matching orders found";
        }

        sb.insert(0, "Total " + count + " orders:\n");
        return sb.toString();
    }
}
