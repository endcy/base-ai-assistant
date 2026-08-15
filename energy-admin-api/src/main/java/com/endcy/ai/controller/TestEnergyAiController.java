package com.endcy.ai.controller;

import com.endcy.ai.rpc.client.EnergyAiClient;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.RagDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.SimpleChatParam;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.domain.response.RagDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.SimpleChatRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 问答测试控制器。
 * 通过客户端调用 energy-ai-api 核心服务，验证 AI 问答功能。
 *
 * @author endcy
 * @date 2026/6/10 20:44:50
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/")
public class TestEnergyAiController {
    private final EnergyAiClient aiApiClient;

    @PostMapping("/qa")
    public CommonResMsgDTO<AIAnswerRet> simpleQa(@Validated @RequestBody KnowledgeAIQueryParam query) {
        CommonResMsgDTO<AIAnswerRet> ret = aiApiClient.callAiApi(query);
        log.info(">>>>>>> EnergyAi qa receive msg proc {}", System.currentTimeMillis());
        return ret;
    }

    /**
     * 流式输出问答。
     */
    @PostMapping(value = "/qa-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AIStreamResponse> qaStream(@Validated @RequestBody KnowledgeAIQueryParam query) {
        return aiApiClient.callAiStream(query);
    }

    /**
     * RAG 文档匹配检查。
     */
    @PostMapping("/rag-check")
    public CommonResMsgDTO<RagDocumentMatchRet> ragCheck(@Validated @RequestBody RagDocumentMatchParam query) {
        return aiApiClient.callAiRagCheck(query);
    }

    /**
     * 简单对话问答。
     */
    @PostMapping("/simple-chat")
    public CommonResMsgDTO<SimpleChatRet> simpleChat(@Validated @RequestBody SimpleChatParam query) {
        return aiApiClient.callSimpleChat(query);
    }

}
