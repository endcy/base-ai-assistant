package com.endcy.ai.rpc.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.validation.ValidationUtil;
import com.endcy.ai.manager.AiRequestManager;
import com.endcy.ai.rpc.api.EnergyAiFeignService;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.RagDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.SimpleChatParam;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.domain.response.AIAnswerTextChunkRet;
import com.endcy.ai.rpc.domain.response.RagDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.SimpleChatRet;
import com.endcy.ai.rpc.enums.MessageType;
import com.endcy.service.common.annotation.LogReqRes;
import com.endcy.service.common.executor.TaskRunnable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * AI 服务 Feign 请求处理器
 * 仅内网调用，不暴露公网
 *
 * @author endcy
 * @date 2025/6/5 20:16:10
 */
@LogReqRes("log.enable.rpc.EnergyAiFeignProcessor")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ai")
public class EnergyAiFeignProcessor implements EnergyAiFeignService {
    private final AiRequestManager aiRequestManager;

    @Override
    @GetMapping("/qa-test")
    public CommonResMsgDTO<String> callTest(String content) {
        return CommonResMsgDTO.successDeviceRes("test ok");
    }

    @Override
    @PostMapping("/qa")
    public CommonResMsgDTO<AIAnswerRet> qa(@RequestBody KnowledgeAIQueryParam query) {
        AIAnswerRet answerRet = aiRequestManager.qaSync(query);
        return CommonResMsgDTO.successDeviceRes(answerRet);
    }

    @Override
    @PostMapping("/rag-check")
    public CommonResMsgDTO<RagDocumentMatchRet> ragDocumentCheck(@RequestBody RagDocumentMatchParam param) {
        RagDocumentMatchRet ret = aiRequestManager.ragDocumentMatch(param);
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    @Override
    @PostMapping("/simple-chat")
    public CommonResMsgDTO<SimpleChatRet> simpleChat(@RequestBody SimpleChatParam param) {
        SimpleChatRet ret = aiRequestManager.simpleChat(param);
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    @Override
    @PostMapping(value = "/qa-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AIStreamResponse> qaStream(@RequestBody KnowledgeAIQueryParam query) {
        return aiRequestManager.qaStream(query);
    }

    private static boolean validateOK(KnowledgeAIQueryParam query, FluxSink<AIStreamResponse> observer) {
        if (CollUtil.isEmpty(ValidationUtil.validate(query))) {
            return true;
        }
        AIAnswerTextChunkRet chunk = new AIAnswerTextChunkRet();
        chunk.setFinal(true);
        chunk.setText("请输入正确的参数");
        AIStreamResponse textResponse = new AIStreamResponse();
        textResponse.setType(MessageType.TEXT);
        textResponse.setFinal(true);
        textResponse.setData("请输入正确的参数");
        observer.next(textResponse);
        return false;
    }

    private void safeStreamQa(FluxSink<AIStreamResponse> emitter, TaskRunnable function) {
        try {
            function.run();
        } finally {
            emitter.complete();
        }
    }
}
