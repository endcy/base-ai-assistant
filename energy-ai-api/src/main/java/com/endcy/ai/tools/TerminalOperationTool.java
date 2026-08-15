package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 终端命令执行工具。
 * <p>
 * 命令字符串由 LLM 生成，不可完全信任。为避免被诱导执行破坏性命令，本工具仅允许
 * {@link #ALLOWED_PREFIXES} 中列出的只读命令前缀，其余命令一律拒绝。
 * 该限制在进程层面于命令执行前完成，不依赖运行时参数过滤。
 * </p>
 *
 * @author endcy
 */
public class TerminalOperationTool {

    /**
     * 允许执行的命令前缀白名单（仅只读查询类命令，避免删除/格式化/关机等破坏性操作）。
     */
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "dir", "echo", "type", "cd", "ver", "date", "time", "whoami",
            "hostname", "ipconfig", "ping", "tracert", "systeminfo", "tasklist"
    );

    @Tool(description = "Execute a read-only command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        if (StrUtil.isBlank(command)) {
            return "命令为空";
        }
        String trimmed = command.trim();
        String lower = trimmed.toLowerCase();
        boolean allowed = ALLOWED_PREFIXES.stream().anyMatch(prefix -> lower.equals(prefix) || lower.startsWith(prefix + " "));
        if (!allowed) {
            return "命令被安全策略拒绝（仅允许只读查询命令）: " + command;
        }
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", trimmed);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
            Thread.currentThread().interrupt();
        }
        return output.toString();
    }
}
