package com.endcy.ai.rpc.client;

import com.endcy.ai.rpc.constant.RpcConfigConstant;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.RagDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.SimpleChatParam;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.domain.response.RagDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.SimpleChatRet;
import com.endcy.ai.rpc.utils.ClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI服务客户端
 *
 * @author endcy
 * @since 2025/12/20 10:58:46
 */
@Slf4j
@ConditionalOnProperty(name = "ai.service.client.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class EnergyAiClient {

    private final WebClient aiWebClient;

    @Value("${ai.service.client.access-token:}")
    private String accessToken;

    public CommonResMsgDTO<RagDocumentMatchRet> callAiRagCheck(RagDocumentMatchParam query) {
        CommonResMsgDTO<RagDocumentMatchRet> ret = aiWebClient.post()
                                                              .uri("/api/ai/rag-check")
                                                              .contentType(MediaType.APPLICATION_JSON)
                                                              .accept(MediaType.APPLICATION_JSON)
                                                              .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                              .bodyValue(query)
                                                              .retrieve()
                                                              .bodyToMono(String.class)
                                                              .map(body -> ClientUtils.parseResponse(body, RagDocumentMatchRet.class))
                                                              .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, RagDocumentMatchRet.class))
                                                              .block();
        return ClientUtils.resolveTipsData(ret, "rag check");
    }

    public CommonResMsgDTO<SimpleChatRet> callSimpleChat(SimpleChatParam query) {
        CommonResMsgDTO<SimpleChatRet> ret = aiWebClient.post()
                                                        .uri("/api/ai/simple-chat")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .accept(MediaType.APPLICATION_JSON)
                                                        .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                        .bodyValue(query)
                                                        .retrieve()
                                                        .bodyToMono(String.class)
                                                        .map(body -> ClientUtils.parseResponse(body, SimpleChatRet.class))
                                                        .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, SimpleChatRet.class))
                                                        .block();
        return ClientUtils.resolveTipsData(ret, "simple chat");
    }

    public CommonResMsgDTO<AIAnswerRet> callAiApi(KnowledgeAIQueryParam query) {
        CommonResMsgDTO<AIAnswerRet> ret = aiWebClient.post()
                                                      .uri("/api/ai/qa")
                                                      .contentType(MediaType.APPLICATION_JSON)
                                                      .accept(MediaType.APPLICATION_JSON)
                                                      .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                      .bodyValue(query)
                                                      .retrieve()
                                                      .bodyToMono(String.class)
                                                      .map(body -> ClientUtils.parseResponse(body, AIAnswerRet.class))
                                                      .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, AIAnswerRet.class))
                                                      .block();
        return ClientUtils.resolveTipsData(ret, "ai qa");
    }

    @PostMapping(value = "/qa-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AIStreamResponse> callAiStream(@RequestBody KnowledgeAIQueryParam query) {
        return aiWebClient.post()
                          .uri("/api/ai/qa-stream")
                          .contentType(MediaType.APPLICATION_JSON)
                          .accept(MediaType.TEXT_EVENT_STREAM)
                          .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                          .bodyValue(query)
                          .retrieve()
                          .bodyToFlux(new ParameterizedTypeReference<AIStreamResponse>() {
                          })
                          .doOnComplete(() -> log.info("stream finished"))
                          .doOnError(error -> log.warn("stream error", error));
    }

    private <T> Mono<CommonResMsgDTO<T>> fromError(WebClientResponseException ex, Class<T> clazz) {
        String body = ex.getResponseBodyAsString();
        try {
            return Mono.just(ClientUtils.parseResponse(body, clazz));
        } catch (Exception e) {
            return Mono.just(CommonResMsgDTO.failureDeviceRes(null, body));
        }
    }
}
