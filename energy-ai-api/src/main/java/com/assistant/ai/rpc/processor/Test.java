package com.assistant.ai.rpc.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.rpc.domain.base.AIStreamResponse;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.assistant.ai.rpc.domain.response.AIAnswerRet;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import com.assistant.ai.rpc.enums.MessageType;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * 测试辅助类
 * 提供模拟的 AI 回答用于接口调试
 *
 * @author endcy
 * @date 2025/12/16 18:54:23
 */
public class Test {

    //测试回答
    public static CommonResMsgDTO<AIAnswerRet> testResponse(KnowledgeAIQueryParam query) {
        AIAnswerRet answer = new AIAnswerRet();
        CommonResMsgDTO<AIAnswerRet> ret = CommonResMsgDTO.successDeviceRes(answer);
        List<KnowledgeDocumentMatchItem> relatedDocs = CollUtil.newArrayList();
        for (int i = 0; i < 3; i++) {
            KnowledgeDocumentMatchItem document = new KnowledgeDocumentMatchItem();
            document.setId((long) i);
            document.setScopeType("运营客服");
            document.setBusinessType("ok");
            document.setSource("来源网页" + i);
            document.setTitle("标题" + i);
            relatedDocs.add(document);
        }
        answer.setRelatedDocs(relatedDocs);

        // 开始流式生成AI回答
        String simulatedAnswer = "这是一个关于\"" + query.getQuestion() + "\"的同步回答。这里是答案....";
        answer.setText(simulatedAnswer);
        ret.setData(answer);
        return ret;
    }

    //测试回答 Stream
    public static void testResponse(KnowledgeAIQueryParam query, FluxSink<AIStreamResponse> observer) {
        for (int i = 0; i < 3; i++) {
            KnowledgeDocumentMatchItem document = new KnowledgeDocumentMatchItem();
            document.setId((long) i);
            document.setScopeType("运营客服");
            document.setBusinessType("ok");
            document.setSource("来源网页" + i);
            document.setTitle("标题" + i);
            AIStreamResponse docResponse = new AIStreamResponse();
            docResponse.setType(MessageType.DOC);
            docResponse.setFinal(false);
            docResponse.setData(JSONUtil.toJsonStr(document));
            observer.next(docResponse);
        }

        // 开始流式生成AI回答
        String simulatedAnswer = "这是一个关于\"" + query.getQuestion() + "\"的流式回答。首先，";
        String[] answerSegments = {
                simulatedAnswer,
                "这是第一段解释。",
                "这是第二段说明。",
                "这是第三段说明。",
                "这是第四段说明。",
                "这是第五段说明。",
                "回答完毕。"
        };
        // 模拟逐句生成AI回答并推送
        for (int i = 0; i < answerSegments.length; i++) {
            AIStreamResponse textResponse = new AIStreamResponse();
            textResponse.setType(MessageType.TEXT);
            textResponse.setData(answerSegments[i]);
            textResponse.setFinal(i == answerSegments.length - 1);
            observer.next(textResponse);
            ThreadUtil.sleep(RandomUtil.randomLong(50, 500));
        }
    }
}
