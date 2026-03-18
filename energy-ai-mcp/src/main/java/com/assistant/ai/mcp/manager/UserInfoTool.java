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
 * 用户信息查询工具
 * 支持用户基本信息、账户余额等查询功能
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "查询用户基本信息，根据用户 ID、手机号或用户名查询用户详细信息")
    public String queryUserInfo(
            @ToolParam(description = "用户 ID", required = false) Long userId,
            @ToolParam(description = "手机号", required = false) String phone,
            @ToolParam(description = "用户名", required = false) String username
    ) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT id, user_code, username, phone, email, user_type,
                       group_id, balance, total_charge_count, total_discharge_count,
                       status, create_time
                FROM biz_user WHERE 1=1
            """);

            if (userId != null) {
                sqlBuilder.append(" AND id = ?");
            }
            if (StrUtil.isNotBlank(phone)) {
                sqlBuilder.append(" AND phone = ?");
            }
            if (StrUtil.isNotBlank(username)) {
                sqlBuilder.append(" AND username = ?");
            }
            sqlBuilder.append(" LIMIT 10");

            List<Object> params = new java.util.ArrayList<>();
            if (userId != null) params.add(userId);
            if (StrUtil.isNotBlank(phone)) params.add(phone);
            if (StrUtil.isNotBlank(username)) params.add(username);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

            if (results.isEmpty()) {
                return "未找到符合条件的用户信息";
            }

            return buildUserResponse(results);
        } catch (Exception e) {
            log.error("查询用户信息失败：{}", e.getMessage(), e);
            return "查询用户信息失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户账户余额")
    public String queryUserBalance(@ToolParam(description = "用户 ID") Long userId) {
        try {
            String sql = """
                SELECT id, username, balance, status
                FROM biz_user WHERE id = ?
            """;

            Map<String, Object> result = jdbcTemplate.queryForMap(sql, userId);

            if (result == null) {
                return "未找到用户信息";
            }

            return String.format("用户 %s (ID:%d) 的账户余额为 %.2f 元，状态：%s",
                    result.get("username"), userId,
                    ((Number) result.get("balance")).doubleValue(),
                    result.get("status"));
        } catch (Exception e) {
            log.error("查询用户余额失败：{}", e.getMessage(), e);
            return "查询用户余额失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户组（租户）下的用户列表")
    public String queryUsersByGroup(@ToolParam(description = "租户 ID/分组 ID") Long groupId) {
        try {
            String sql = """
                SELECT id, user_code, username, phone, user_type, balance,
                       total_charge_count, total_discharge_count, status
                FROM biz_user WHERE group_id = ?
                ORDER BY create_time DESC LIMIT 50
            """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, groupId);

            if (results.isEmpty()) {
                return "该租户下暂无用户";
            }

            StringBuilder sb = new StringBuilder("租户 ").append(groupId).append(" 下的用户列表 (共").append(results.size()).append("人)：\n");
            for (Map<String, Object> user : results) {
                sb.append("- ").append(user.get("username"))
                  .append(" (").append(user.get("user_code")).append(")")
                  .append(", 手机：").append(user.get("phone"))
                  .append(", 余额：").append(user.get("balance")).append(" 元")
                  .append(", 充电：").append(user.get("total_charge_count")).append("次")
                  .append(", 放电：").append(user.get("total_discharge_count")).append("次")
                  .append(", 状态：").append(user.get("status")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询租户用户列表失败：{}", e.getMessage(), e);
            return "查询租户用户列表失败：" + e.getMessage();
        }
    }

    private String buildUserResponse(List<Map<String, Object>> users) {
        StringBuilder sb = new StringBuilder("查询到 ").append(users.size()).append(" 位用户信息：\n");
        for (Map<String, Object> user : users) {
            sb.append("\n【用户】").append(user.get("username")).append("\n");
            sb.append("  用户编码：").append(user.get("user_code")).append("\n");
            sb.append("  用户 ID: ").append(user.get("id")).append("\n");
            sb.append("  手机号：").append(user.get("phone")).append("\n");
            sb.append("  邮箱：").append(user.get("email")).append("\n");
            sb.append("  用户类型：").append(user.get("user_type")).append("\n");
            sb.append("  账户余额：").append(user.get("balance")).append(" 元\n");
            sb.append("  累计充电：").append(user.get("total_charge_count")).append(" 次\n");
            sb.append("  累计放电：").append(user.get("total_discharge_count")).append(" 次\n");
            sb.append("  状态：").append(user.get("status")).append("\n");
        }
        return sb.toString();
    }
}
