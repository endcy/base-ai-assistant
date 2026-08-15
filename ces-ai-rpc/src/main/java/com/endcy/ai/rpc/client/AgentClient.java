package com.endcy.ai.rpc.client;

import com.endcy.ai.rpc.constant.RpcConfigConstant;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.AgentTaskParam;
import com.endcy.ai.rpc.domain.response.AgentTaskRet;
import com.endcy.ai.rpc.domain.response.ToolInventoryRet;
import com.endcy.ai.rpc.utils.ClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Agent 能力调用客户端
 *
 * @author endcy
 * @since 2026-08-10
 */
@Slf4j
@ConditionalOnProperty(name = "ai.service.client.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class AgentClient {

    private final WebClient aiWebClient;

    @Value("${ai.service.client.access-token:}")
    private String accessToken;

    public CommonResMsgDTO<AgentTaskRet> submitTask(AgentTaskParam param) {
        return post("/api/agent/task/submit", param, AgentTaskRet.class);
    }

    public CommonResMsgDTO<AgentTaskRet> getTaskStatus(String taskId) {
        return get("/api/agent/task/" + taskId, AgentTaskRet.class);
    }

    public CommonResMsgDTO<AgentTaskRet> executeSync(AgentTaskParam param) {
        return post("/api/agent/task/execute-sync", param, AgentTaskRet.class);
    }

    public CommonResMsgDTO<ToolInventoryRet> listTools() {
        return get("/api/agent/tools", ToolInventoryRet.class);
    }

    @SuppressWarnings("unchecked")
    public CommonResMsgDTO<List<AgentTaskRet>> listTasksByChatId(Long chatId) {
        CommonResMsgDTO<List<AgentTaskRet>> ret = (CommonResMsgDTO<List<AgentTaskRet>>) (Object)
                getRaw("/api/agent/task/list?chatId=" + chatId);
        return ClientUtils.resolveTipsData(ret, "agent task list");
    }

    public CommonResMsgDTO<Boolean> cancelTask(String taskId, String reason) {
        String uri = "/api/agent/task/" + taskId + "/cancel";
        if (reason != null && !reason.isEmpty()) {
            try {
                uri += "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8.name());
            } catch (java.io.UnsupportedEncodingException e) {
                // UTF-8 always supported
            }
        }
        return get(uri, Boolean.class);
    }

    public Flux<String> streamTaskEvents(String taskId) {
        String uri = "/api/agent/task/" + taskId + "/stream";
        return aiWebClient.get()
                          .uri(uri)
                          .accept(MediaType.TEXT_EVENT_STREAM)
                          .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                          .retrieve()
                          .bodyToFlux(String.class)
                          .doOnError(e -> log.warn("agent stream error: {}", uri, e));
    }

    @SuppressWarnings("unchecked")
    public CommonResMsgDTO<List<Map<String, Object>>> getTaskThoughts(String taskId) {
        CommonResMsgDTO<List<Map<String, Object>>> ret = (CommonResMsgDTO<List<Map<String, Object>>>) (Object)
                getRaw("/api/agent/task/" + taskId + "/thoughts");
        return ClientUtils.resolveTipsData(ret, "agent thoughts");
    }

    /**
     * 查询凭证冷却状态。
     */
    @SuppressWarnings("unchecked")
    public CommonResMsgDTO<Map<String, Object>> credentialStatus() {
        CommonResMsgDTO<Map<String, Object>> ret = (CommonResMsgDTO<Map<String, Object>>) (Object)
                getRaw("/api/agent/credentials/status");
        return ClientUtils.resolveTipsData(ret, "agent credential status");
    }

    /**
     * 清除凭证冷却。
     */
    @SuppressWarnings("unchecked")
    public CommonResMsgDTO<Map<String, Object>> clearCredentialCooldown() {
        String uri = "/api/agent/credentials/clear-cooldown";
        CommonResMsgDTO<String> raw = executeRequest(() -> aiWebClient.post()
                                                                      .uri(uri)
                                                                      .accept(MediaType.APPLICATION_JSON)
                                                                      .header(RpcConfigConstant.AUTH_TOKEN, accessToken), String.class, uri);
        return (CommonResMsgDTO<Map<String, Object>>) (Object) ClientUtils.resolveTipsData(raw, "agent clear cooldown");
    }

    /**
     * 健康检查。
     */
    @SuppressWarnings("unchecked")
    public CommonResMsgDTO<Map<String, Object>> healthCheck() {
        CommonResMsgDTO<Map<String, Object>> ret = (CommonResMsgDTO<Map<String, Object>>) (Object)
                getRaw("/api/agent/health");
        return ClientUtils.resolveTipsData(ret, "agent health");
    }

    private <T> CommonResMsgDTO<T> post(String uri, Object body, Class<T> clazz) {
        return executeRequest(() -> aiWebClient.post()
                                               .uri(uri)
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .accept(MediaType.APPLICATION_JSON)
                                               .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                               .bodyValue(body), clazz, uri);
    }

    private <T> CommonResMsgDTO<T> get(String uri, Class<T> clazz) {
        return executeRequest(() -> aiWebClient.get()
                                               .uri(uri)
                                               .accept(MediaType.APPLICATION_JSON)
                                               .header(RpcConfigConstant.AUTH_TOKEN, accessToken), clazz, uri);
    }

    private CommonResMsgDTO<String> getRaw(String uri) {
        return executeRequest(() -> aiWebClient.get()
                                               .uri(uri)
                                               .accept(MediaType.APPLICATION_JSON)
                                               .header(RpcConfigConstant.AUTH_TOKEN, accessToken), String.class, uri);
    }

    private <T> CommonResMsgDTO<T> executeRequest(java.util.function.Supplier<WebClient.RequestHeadersSpec<?>> specSupplier,
                                                  Class<T> clazz, String uri) {
        CommonResMsgDTO<T> ret = specSupplier.get()
                                             .retrieve()
                                             .bodyToMono(String.class)
                                             .map(body -> ClientUtils.parseResponse(body, clazz))
                                             .onErrorResume(WebClientResponseException.class,
                                                     ex -> Mono.just(CommonResMsgDTO.failureDeviceRes(null, ex.getResponseBodyAsString())))
                                             .block();
        return ClientUtils.resolveTipsData(ret, "agent " + uri);
    }
}
