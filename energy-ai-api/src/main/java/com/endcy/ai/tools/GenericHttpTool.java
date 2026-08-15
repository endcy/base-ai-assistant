package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Generic HTTP request tool — Step 2.7.
 *
 * <p>Allows the agent to call internal REST services (e.g. {@code ces-ai-rpc} / {@code energy-admin-api}).
 * URL whitelist and HTTP method restrictions are configured to prevent SSRF.</p>
 *
 * <p><b>Disabled by default</b> (enabled=false), requires user to configure whitelist before use.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class GenericHttpTool {

    @Value("${ai.tools.http.enabled:false}")
    private boolean enabled;

    @Value("${ai.tools.http.url-whitelist:}")
    private String urlWhitelistRaw;

    @Value("${ai.tools.http.allowed-methods:GET,POST}")
    private String allowedMethodsRaw;

    @Value("${ai.tools.http.timeout-ms:8000}")
    private int timeoutMs;

    @Value("${ai.tools.http.max-response-length:4000}")
    private int maxResponseLength;

    @Tool(description = "Call internal REST API to retrieve data. Pass a full URL (must start with a whitelisted prefix), HTTP method, and optional body. " +
            "Suitable for querying business system data (orders, stations, device status, etc.). Returns response text (truncated if too long).")
    public String callHttpApi(
            @ToolParam(description = "Full URL, must start with a whitelisted prefix") String url,
            @ToolParam(description = "HTTP method: GET or POST") String method,
            @ToolParam(description = "POST request body (pass null or empty string for GET)") String body) {

        if (!enabled) {
            return "Generic HTTP tool is not enabled (ai.tools.http.enabled=false)";
        }
        if (StrUtil.isBlank(url)) {
            return "URL is empty";
        }

        // Whitelist check
        if (!isUrlAllowed(url)) {
            log.warn("HTTP tool rejected non-whitelisted URL: {}", url);
            return "URL is not in the whitelist, access denied: " + url;
        }

        // Method check
        Method httpMethod;
        try {
            httpMethod = Method.valueOf(StrUtil.blankToDefault(method, "GET").toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Unsupported HTTP method: " + method;
        }
        if (!isMethodAllowed(httpMethod)) {
            return "HTTP method is not in the allowed list: " + httpMethod;
        }

        try {
            HttpRequest request = HttpRequest.of(url).method(httpMethod).timeout(timeoutMs);
            if (httpMethod == Method.POST && StrUtil.isNotBlank(body)) {
                request.body(body);
            }
            try (HttpResponse response = request.execute()) {
                String respBody = response.body();
                if (respBody != null && respBody.length() > maxResponseLength) {
                    respBody = respBody.substring(0, maxResponseLength) + "...[truncated]";
                }
                log.info("HTTP tool call {} {} -> status={} len={}",
                        httpMethod, url, response.getStatus(), StrUtil.length(respBody));
                return respBody != null ? respBody : "";
            }
        } catch (Exception e) {
            log.error("HTTP tool call failed {} {}: {}", httpMethod, url, e.getMessage());
            return "HTTP call failed: " + e.getMessage();
        }
    }

    private boolean isUrlAllowed(String url) {
        if (StrUtil.isBlank(urlWhitelistRaw)) {
            return false;
        }
        Set<String> whitelist = Set.of(urlWhitelistRaw.split(","));
        for (String prefix : whitelist) {
            if (url.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMethodAllowed(Method method) {
        Set<String> allowed = Set.of(allowedMethodsRaw.toUpperCase().split(","));
        return allowed.contains(method.name());
    }
}
