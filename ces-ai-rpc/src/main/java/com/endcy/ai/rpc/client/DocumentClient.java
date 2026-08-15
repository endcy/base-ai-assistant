package com.endcy.ai.rpc.client;

import com.endcy.ai.rpc.constant.RpcConfigConstant;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentActionParam;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentParam;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentStatusItem;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentStatusRet;
import com.endcy.ai.rpc.utils.ClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * 文档客户端
 *
 * @author endcy
 * @since 2025/12/20 10:58:46
 */
@Slf4j
@ConditionalOnProperty(name = "ai.service.client.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class DocumentClient {
    private final WebClient aiWebClient;

    @Value("${ai.service.client.access-token:}")
    private String accessToken;

    public CommonResMsgDTO<KnowledgeDocumentStatusItem> modify(KnowledgeDocumentParam document) {
        CommonResMsgDTO<KnowledgeDocumentStatusItem> ret = aiWebClient.post()
                                                                      .uri("/api/admin/document/modify")
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON)
                                                                      .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                                      .bodyValue(document)
                                                                      .retrieve()
                                                                      .bodyToMono(String.class)
                                                                      .map(body -> ClientUtils.parseResponse(body, KnowledgeDocumentStatusItem.class))
                                                                      .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, KnowledgeDocumentStatusItem.class))
                                                                      .block();
        return ClientUtils.resolveTipsData(ret, "document modify");
    }

    public CommonResMsgDTO<KnowledgeDocumentStatusRet> action(KnowledgeDocumentActionParam query) {
        CommonResMsgDTO<KnowledgeDocumentStatusRet> ret = aiWebClient.post()
                                                                     .uri("/api/admin/document/action")
                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                     .accept(MediaType.APPLICATION_JSON)
                                                                     .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                                     .bodyValue(query)
                                                                     .retrieve()
                                                                     .bodyToMono(String.class)
                                                                     .map(body -> ClientUtils.parseResponse(body, KnowledgeDocumentStatusRet.class))
                                                                     .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, KnowledgeDocumentStatusRet.class))
                                                                     .block();
        return ClientUtils.resolveTipsData(ret, "document action");
    }

    public CommonResMsgDTO<KnowledgeDocumentMatchRet> match(KnowledgeDocumentMatchParam query) {
        CommonResMsgDTO<KnowledgeDocumentMatchRet> ret = aiWebClient.post()
                                                                    .uri("/api/admin/document/match")
                                                                    .contentType(MediaType.APPLICATION_JSON)
                                                                    .accept(MediaType.APPLICATION_JSON)
                                                                    .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                                    .bodyValue(query)
                                                                    .retrieve()
                                                                    .bodyToMono(String.class)
                                                                    .map(body -> ClientUtils.parseResponse(body, KnowledgeDocumentMatchRet.class))
                                                                    .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, KnowledgeDocumentMatchRet.class))
                                                                    .block();
        return ClientUtils.resolveTipsData(ret, "document match");
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
