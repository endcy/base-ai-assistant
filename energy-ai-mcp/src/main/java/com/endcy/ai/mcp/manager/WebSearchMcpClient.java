package com.endcy.ai.mcp.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.endcy.service.domain.annotation.McpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSearch MCP client — web search via Streamable HTTP + JSON-RPC.
 *
 * <p>Protocol: POST to single endpoint, header {@code Authorization: Bearer ${api-key}}.
 * Flow: {@code initialize} → {@code tools/list} (probe tool name/params) → {@code tools/call}.</p>
 *
 * <p>Internal dependency of tools like {@link ElectricityPriceSearchTool}, not directly exposed to agent.
 * Reuses {@code spring.ai.dashscope.api-key} (Bailian and DashScope share the same API Key).</p>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
@McpServer("websearch")
public class WebSearchMcpClient {

    @Value("${ai.mcp.websearch.endpoint:}")
    private String endpoint;

    @Value("${ai.mcp.websearch.api-key:${spring.ai.dashscope.api-key:}}")
    private String apiKey;

    @Value("${ai.mcp.websearch.timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${ai.mcp.websearch.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(10))
                                                    .build();

    /**
     * Detected tool name / param field (lazy-loaded to avoid tools/list on every call)
     */
    private volatile String cachedToolName;
    private volatile String cachedQueryField;

    /**
     * Streamable HTTP session ID (add if returned by Bailian)
     */
    private volatile String sessionId;

    private volatile boolean initialized;

    /**
     * Execute web search, returns concatenated web page summary text.
     *
     * @param query search term
     * @return search result text (multiple content items concatenated)
     */
    public String search(String query) {
        if (!enabled) {
            throw new IllegalStateException("websearch disabled (ai.mcp.websearch.enabled=false)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("websearch api-key not configured");
        }
        try {
            ensureInitialized();
            String toolName = resolveToolName();
            String queryField = resolveQueryField();
            return callTool(toolName, queryField, query);
        } catch (Exception e) {
            log.warn("WebSearch MCP call failed query=[{}]: {}", query, e.getMessage());
            throw new RuntimeException("WebSearch failed: " + e.getMessage(), e);
        }
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        try {
            Map<String, Object> clientInfo = Map.of("name", "base-ai-assistant", "version", "1.0");
            Map<String, Object> params = new HashMap<>();
            params.put("protocolVersion", "2025-03-26");
            params.put("capabilities", new HashMap<>());
            params.put("clientInfo", clientInfo);
            rpc("initialize", params, 1);
            log.info("WebSearch MCP initialized");
        } catch (Exception e) {
            // Some stateless MCPs don't need initialize, ignore error and continue subsequent calls
            log.debug("initialize skipped (may be stateless): {}", e.getMessage());
        }
        initialized = true;
    }

    private String resolveToolName() {
        if (cachedToolName != null) {
            return cachedToolName;
        }
        String name = "web_search"; // Fallback
        try {
            JSONObject result = rpc("tools/list", new HashMap<>(), 2);
            JSONArray tools = result.getJSONArray("tools");
            if (tools != null && !tools.isEmpty()) {
                // Prefer matching search/web
                for (int i = 0; i < tools.size(); i++) {
                    String n = tools.getJSONObject(i).getString("name");
                    if (n != null) {
                        String lower = n.toLowerCase();
                        if (lower.contains("search") || lower.contains("web")) {
                            name = n;
                            break;
                        }
                    }
                }
                if ("web_search".equals(name)) {
                    name = tools.getJSONObject(0).getString("name");
                }
            }
        } catch (Exception e) {
            log.debug("tools/list failed, using fallback tool name web_search: {}", e.getMessage());
        }
        cachedToolName = name;
        log.info("WebSearch MCP tool name detection result: {}", name);
        return name;
    }

    private String resolveQueryField() {
        // Simplified handling: Bailian WebSearch input field is query, consistent with most MCP search tools
        cachedQueryField = "query";
        return cachedQueryField;
    }

    private String callTool(String toolName, String queryField, String query) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(queryField, query);
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        JSONObject result = rpc("tools/call", params, 3);
        JSONArray content = result.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return result.toJSONString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JSONObject item = content.getJSONObject(i);
            if (item != null && "text".equals(item.getString("type"))) {
                sb.append(item.getString("text")).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Send JSON-RPC request and return {@code result} object (SSE response compatible).
     */
    private JSONObject rpc(String method, Map<String, Object> params, int id) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("method", method);
        body.put("params", params);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                                    .uri(URI.create(endpoint))
                                                    .timeout(Duration.ofSeconds(timeoutSeconds))
                                                    .header("Content-Type", "application/json")
                                                    .header("Accept", "application/json, text/event-stream")
                                                    .header("Authorization", "Bearer " + apiKey)
                                                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)));
        if (sessionId != null) {
            reqBuilder.header("Mcp-Session-Id", sessionId);
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        // Capture session id (subsequent requests include it if returned by Bailian)
        response.headers().firstValue("mcp-session-id").ifPresent(sid -> this.sessionId = sid);

        String respBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": "
                    + (respBody == null ? "" : respBody.substring(0, Math.min(respBody.length(), 500))));
        }
        return parseResult(respBody);
    }

    /**
     * Parse JSON-RPC response, return {@code result} field. Supports two response formats:
     * <ul>
     *   <li>application/json — direct {"jsonrpc":...,"result":{...}}</li>
     *   <li>text/event-stream — multi-line data: {...}, take the last one with result</li>
     * </ul>
     */
    private JSONObject parseResult(String body) {
        if (body == null || body.isBlank()) {
            return new JSONObject();
        }
        String trimmed = body.trim();
        // SSE response
        if (trimmed.startsWith("event:") || trimmed.startsWith("data:") || trimmed.contains("\ndata:")) {
            String[] lines = trimmed.split("\n");
            JSONObject lastWithResult = null;
            for (String line : lines) {
                String l = line.trim();
                if (l.startsWith("data:")) {
                    String data = l.substring(5).trim();
                    if (data.startsWith("{")) {
                        try {
                            JSONObject obj = JSON.parseObject(data);
                            if (obj != null && obj.containsKey("result")) {
                                lastWithResult = obj;
                            }
                        } catch (Exception ignore) {
                            // Ignore non-JSON data lines (e.g. heartbeats)
                        }
                    }
                }
            }
            if (lastWithResult != null) {
                return lastWithResult.getJSONObject("result");
            }
        }
        // Normal JSON response
        JSONObject obj = JSON.parseObject(trimmed);
        if (obj.containsKey("error")) {
            throw new RuntimeException("MCP error: " + obj.getJSONObject("error"));
        }
        return obj.containsKey("result") ? obj.getJSONObject("result") : obj;
    }
}
