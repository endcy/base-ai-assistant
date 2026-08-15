package com.endcy.ai.mcp.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Streamable HTTP client base class — encapsulates JSON-RPC protocol details.
 *
 * <p>Subclasses only need to implement {@link #getEndpoint()} and {@link #getApiKey()},
 * then call {@link #callTool(String, Map)}.</p>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
public abstract class AbstractMcpClient {

    protected abstract String getEndpoint();

    protected abstract String getApiKey();

    protected int getTimeoutSeconds() {
        return 30;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(10))
                                                    .build();

    private volatile String sessionId;
    private volatile boolean initialized;

    /**
     * Call MCP tool.
     *
     * @param toolName  tool name
     * @param arguments tool arguments (key-value)
     * @return text content returned by the tool
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        try {
            ensureInitialized();
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
            params.put("arguments", arguments != null ? arguments : new HashMap<>());
            JSONObject result = rpc("tools/call", params, 2);
            return extractTextContent(result);
        } catch (Exception e) {
            log.warn("MCP callTool failed tool=[{}]: {}", toolName, e.getMessage());
            throw new RuntimeException("MCP call failed [" + toolName + "]: " + e.getMessage(), e);
        }
    }

    /**
     * List tools provided by the MCP service (for debugging/probing).
     */
    public JSONArray listTools() {
        try {
            ensureInitialized();
            JSONObject result = rpc("tools/list", new HashMap<>(), 2);
            return result.getJSONArray("tools");
        } catch (Exception e) {
            log.warn("MCP tools/list failed: {}", e.getMessage());
            return new JSONArray();
        }
    }

    protected void ensureInitialized() {
        if (initialized)
            return;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("protocolVersion", "2025-03-26");
            params.put("capabilities", new HashMap<>());
            params.put("clientInfo", Map.of("name", "base-ai-assistant", "version", "1.0"));
            rpc("initialize", params, 1);
            log.info("MCP initialized: {}", getEndpoint());
        } catch (Exception e) {
            log.debug("MCP initialize skipped: {}", e.getMessage());
        }
        initialized = true;
    }

    protected JSONObject rpc(String method, Map<String, Object> params, int id) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("method", method);
        body.put("params", params);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                                    .uri(URI.create(getEndpoint()))
                                                    .timeout(Duration.ofSeconds(getTimeoutSeconds()))
                                                    .header("Content-Type", "application/json")
                                                    .header("Accept", "application/json, text/event-stream")
                                                    .header("Authorization", "Bearer " + getApiKey())
                                                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)));
        if (sessionId != null)
            reqBuilder.header("Mcp-Session-Id", sessionId);

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        response.headers().firstValue("mcp-session-id").ifPresent(sid -> this.sessionId = sid);

        String respBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " +
                    (respBody == null ? "" : respBody.substring(0, Math.min(respBody.length(), 500))));
        }
        return parseResult(respBody);
    }

    /**
     * Extract text content from tool result (supports multiple return formats)
     */
    protected String extractTextContent(JSONObject result) {
        JSONArray content = result.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return result.toJSONString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JSONObject item = content.getJSONObject(i);
            if (item != null && "text".equals(item.getString("type"))) {
                sb.append(item.getString("text"));
            }
        }
        return sb.length() > 0 ? sb.toString() : result.toJSONString();
    }

    private JSONObject parseResult(String body) {
        if (body == null || body.isBlank())
            return new JSONObject();
        String trimmed = body.trim();
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
                            if (obj != null && obj.containsKey("result"))
                                lastWithResult = obj;
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            if (lastWithResult != null)
                return lastWithResult.getJSONObject("result");
        }
        JSONObject obj = JSON.parseObject(trimmed);
        if (obj.containsKey("error"))
            throw new RuntimeException("MCP error: " + obj.getJSONObject("error"));
        return obj.containsKey("result") ? obj.getJSONObject("result") : obj;
    }
}
