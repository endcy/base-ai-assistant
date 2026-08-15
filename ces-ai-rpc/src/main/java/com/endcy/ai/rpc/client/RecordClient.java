package com.endcy.ai.rpc.client;

import com.endcy.ai.rpc.constant.RpcConfigConstant;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.response.RecordResponse;
import com.endcy.ai.rpc.utils.ClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 对话记录客户端
 *
 * @author endcy
 * @since 2026/1/17 9:50
 */
@Slf4j
@ConditionalOnProperty(name = "ai.service.client.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class RecordClient {
    private final WebClient aiWebClient;

    @Value("${ai.service.client.access-token:}")
    private String accessToken;

    public CommonResMsgDTO<List<RecordResponse>> getByChatId(Long chatId) {
        ParameterizedTypeReference<CommonResMsgDTO<List<RecordResponse>>> typeRef = new ParameterizedTypeReference<CommonResMsgDTO<List<RecordResponse>>>() {
        };
        CommonResMsgDTO<List<RecordResponse>> ret = aiWebClient.get()
                                                               .uri((uriBuilder -> uriBuilder
                                                                       .path("/api/admin/record")
                                                                       .queryParam("chatId", chatId)
                                                                       .build()))
                                                               .accept(MediaType.APPLICATION_JSON)
                                                               .header(RpcConfigConstant.AUTH_TOKEN, accessToken)
                                                               .retrieve()
                                                               .bodyToMono(String.class)
                                                               .map(body -> ClientUtils.parseResponse(body, typeRef))
                                                               .onErrorResume(WebClientResponseException.class, ex -> fromError(ex, typeRef))
                                                               .block();
        return ClientUtils.resolveTipsData(ret, "user record");
    }

    private <T> Mono<CommonResMsgDTO<T>> fromError(WebClientResponseException ex, ParameterizedTypeReference<CommonResMsgDTO<T>> typeRef) {
        String body = ex.getResponseBodyAsString();
        try {
            return Mono.just(ClientUtils.parseResponse(body, typeRef));
        } catch (Exception e) {
            return Mono.just(CommonResMsgDTO.failureDeviceRes(null, body));
        }
    }
}
