package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Account balance query tool — Step 2.7.
 *
 * <p>Queries user account balance, deposit history, consumption history, etc.</p>
 *
 * <p>Currently uses mock data (replace with RPC calls in production).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class QueryBalanceTool {

    // Mock data (replace with RPC calls in production)
    private static final Map<String, Map<String, Object>> MOCK_BALANCES = new HashMap<>();

    static {
        Map<String, Object> user1 = new HashMap<>();
        user1.put("userId", "U001");
        user1.put("balance", 256.50);
        user1.put("totalDeposit", 1000.00);
        user1.put("totalConsume", 743.50);
        user1.put("lastDepositTime", "2026-03-05 15:30:00");
        MOCK_BALANCES.put("U001", user1);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("userId", "U002");
        user2.put("balance", 89.00);
        user2.put("totalDeposit", 500.00);
        user2.put("totalConsume", 411.00);
        user2.put("lastDepositTime", "2026-03-01 10:00:00");
        MOCK_BALANCES.put("U002", user2);
    }

    @Tool(description = "Query user account information: balance, total deposits, total consumption, last deposit time, etc.")
    public String queryBalance(
            @ToolParam(description = "User ID") String userId) {

        log.info("Query balance: userId={}", userId);

        if (StrUtil.isBlank(userId)) {
            return "User ID cannot be empty";
        }

        Map<String, Object> userInfo = MOCK_BALANCES.get(userId);
        if (userInfo == null) {
            return "User not found: " + userId;
        }

        return String.format(
                "User account info [%s]:\n" +
                        "Current balance: %.2f\n" +
                        "Total deposits: %.2f\n" +
                        "Total consumption: %.2f\n" +
                        "Last deposit time: %s",
                userId,
                (Double) userInfo.get("balance"),
                (Double) userInfo.get("totalDeposit"),
                (Double) userInfo.get("totalConsume"),
                userInfo.get("lastDepositTime")
        );
    }
}
