package com.endcy.ai.mcp.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MCP SSE transport client base class — handles SSE dual-channel protocol:
 * <ol>
 *   <li>GET SSE endpoint to establish connection, receive {@code event:endpoint} with sessionId's message URL</li>
 *   <li>POST JSON-RPC request to that message URL</li>
 *   <li>SSE stream returns {@code event:message} with JSON-RPC response</li>
 * </ol>
 *
 * <p>Uses HttpURLConnection (sync blocking SSE read) + separate thread for POST,
 * avoiding JDK HttpClient ofLines backpressure issues.</p>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Slf4j
public abstract class AbstractSseMcpClient {

    protected abstract String getEndpoint();

    protected abstract String getApiKey();

    protected int getTimeoutSeconds() {
        return 30;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(10))
                                                    .build();

    /**
     * Call MCP tool (auto-completes SSE handshake → POST → receive response → close connection).
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        HttpURLConnection sseConn = null;
        try {
            // 1. Establish SSE connection (GET, blocking stream read)
            URL sseUrl = URI.create(getEndpoint()).toURL();
            sseConn = (HttpURLConnection) sseUrl.openConnection();
            sseConn.setRequestProperty("Accept", "text/event-stream");
            sseConn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            sseConn.setConnectTimeout(10_000);
            sseConn.setReadTimeout(getTimeoutSeconds() * 1000);

            // reader 用 try-with-resources 确保流资源释放（disconnect 不会关闭输入流）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sseConn.getInputStream()))) {

                // 2. Read first frame: event:endpoint / data:<messageUrl>
                String messageUrl = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:") && line.contains("message")) {
                        messageUrl = line.substring(5).trim();
                        break;
                    }
                }
                if (messageUrl == null) {
                    throw new RuntimeException("SSE handshake did not receive endpoint event");
                }
                String fullMessageUrl = resolveBaseUrl(getEndpoint()) + messageUrl;
                log.debug("SSE handshake successful, messageUrl={}", fullMessageUrl);

                // 3. Prepare to await SSE response
                AtomicReference<String> resultRef = new AtomicReference<>();
                CountDownLatch latch = new CountDownLatch(1);

                // 4. Separate thread sends POST tools/call（reader 已阻塞在 readLine 上，无需 sleep 等待）
                Thread postThread = new Thread(() -> {
                    try {
                        Map<String, Object> body = new HashMap<>();
                        body.put("jsonrpc", "2.0");
                        body.put("id", 1);
                        body.put("method", "tools/call");
                        Map<String, Object> params = new HashMap<>();
                        params.put("name", toolName);
                        params.put("arguments", arguments != null ? arguments : new HashMap<>());
                        body.put("params", params);

                        HttpRequest postReq = HttpRequest.newBuilder()
                                                         .uri(URI.create(fullMessageUrl))
                                                         .timeout(Duration.ofSeconds(getTimeoutSeconds()))
                                                         .header("Content-Type", "application/json")
                                                         .header("Authorization", "Bearer " + getApiKey())
                                                         .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                                                         .build();
                        httpClient.send(postReq, HttpResponse.BodyHandlers.discarding());
                    } catch (Exception e) {
                        log.warn("SSE POST exception: {}", e.getMessage());
                        latch.countDown();
                    }
                });
                postThread.setDaemon(true);
                postThread.start();

                // 5. Main thread continues reading SSE stream, awaits JSON-RPC response (with "result")
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:") && line.contains("\"result\"")) {
                        resultRef.set(line.substring(5).trim());
                        latch.countDown();
                        break;
                    }
                }

                // 6. Await result (SSE reader already got it, or wait for POST to complete)
                if (!latch.await(getTimeoutSeconds(), TimeUnit.SECONDS)) {
                    throw new RuntimeException("SSE response timeout");
                }

                String raw = resultRef.get();
                if (raw == null) {
                    throw new RuntimeException("SSE did not return valid result");
                }
                JSONObject rpcResp = JSON.parseObject(raw);
                if (rpcResp.containsKey("error")) {
                    throw new RuntimeException("MCP error: " + rpcResp.getJSONObject("error"));
                }
                JSONObject result = rpcResp.getJSONObject("result");
                return extractTextContent(result);
            }
        } catch (Exception e) {
            log.warn("SSE MCP callTool failed tool=[{}]: {}", toolName, e.getMessage());
            throw new RuntimeException("SSE MCP call failed [" + toolName + "]: " + e.getMessage(), e);
        } finally {
            if (sseConn != null)
                sseConn.disconnect();
        }
    }

    /**
     * Extract baseUrl from SSE endpoint URL (scheme://host[:port])
     */
    private String resolveBaseUrl(String sseEndpoint) {
        try {
            URI uri = URI.create(sseEndpoint);
            String host = uri.getHost();
            int port = uri.getPort();
            return uri.getScheme() + "://" + host + (port > 0 ? ":" + port : "");
        } catch (Exception e) {
            return sseEndpoint;
        }
    }

    /**
     * Extract text content from tool result
     */
    protected String extractTextContent(JSONObject result) {
        if (result == null)
            return "";
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
}
