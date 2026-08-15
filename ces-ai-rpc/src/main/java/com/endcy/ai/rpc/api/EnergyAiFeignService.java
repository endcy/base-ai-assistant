package com.endcy.ai.rpc.api;

import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.RagDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.SimpleChatParam;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.domain.response.RagDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.SimpleChatRet;
import reactor.core.publisher.Flux;

/**
 * AI服务 Feign 接口
 *
 * @author endcy
 * @since 2024/12/12 21:19:35
 */
public interface EnergyAiFeignService {

    CommonResMsgDTO<String> callTest(String content);

    CommonResMsgDTO<AIAnswerRet> qa(KnowledgeAIQueryParam query);

    Flux<AIStreamResponse> qaStream(KnowledgeAIQueryParam query);

    CommonResMsgDTO<RagDocumentMatchRet> ragDocumentCheck(RagDocumentMatchParam param);

    CommonResMsgDTO<SimpleChatRet> simpleChat(SimpleChatParam param);
}
