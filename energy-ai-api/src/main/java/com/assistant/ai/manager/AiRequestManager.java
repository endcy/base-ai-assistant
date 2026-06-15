package com.assistant.ai.manager;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.app.EnergyAiApp;
import com.assistant.ai.app.EnergyAiDocumentApp;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.domain.context.RequestRagContext;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.ai.repository.service.VectorStoreService;
import com.assistant.ai.rpc.domain.base.AIStreamResponse;
import com.assistant.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.assistant.ai.rpc.domain.request.RagDocumentMatchParam;
import com.assistant.ai.rpc.domain.request.SimpleChatParam;
import com.assistant.ai.rpc.domain.response.AIAnswerRet;
import com.assistant.ai.rpc.domain.response.RagDocumentMatchRet;
import com.assistant.ai.rpc.domain.response.SimpleChatRet;
import com.assistant.ai.rpc.enums.ApiQaType;
import com.assistant.ai.util.DocumentConvertUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * AI 请求管理器
 * 统一管理同步问答、流式问答、RAG 文档匹配测试和简单对话等请求
 *
 * @author endcy
 * @date 2025/12/16 18:59:27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRequestManager {
    private final EnergyAiDocumentApp energyAiDocumentApp;
    private final EnergyAiApp energyAiApp;
    private final VectorStoreService vectorStoreService;
    private final ChatRagProperties chatRagProperties;

    /**
     * 同步问答
     *
     * @param query 问答查询参数
     * @return AI 回答结果
     */
    public AIAnswerRet qaSync(KnowledgeAIQueryParam query) {
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(query.getChatId());
        String answer;
        if (query.getQueryType() == ApiQaType.DEEPSEEK.getCode()) {
            query.setScopeType("deepseek");
            answer = energyAiDocumentApp.deepseek(query);
        } else {
            answer = energyAiDocumentApp.doChatRag(query, requestRagContext);
        }
        AIAnswerRet ret = new AIAnswerRet();
        ret.setText(answer);
        ret.setRelatedDocs(DocumentConvertUtils.documentConvertRelated(requestRagContext.getRelatedDocuments()));
        ret.setPromptTokens(requestRagContext.getPromptTokens());
        ret.setCompletionTokens(requestRagContext.getCompletionTokens());
        return ret;
    }

    /**
     * 流式问答
     *
     * @param query 问答查询参数
     * @return 流式 AI 回答
     */
    public Flux<AIStreamResponse> qaStream(KnowledgeAIQueryParam query) {
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(query.getChatId());
        return Flux.defer(() -> energyAiDocumentApp.doChatRagStream(query, requestRagContext)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * RAG文档召回匹配测试
     * 计算用户问题与知识文档内容的BM25匹配得分
     *
     * @param param 匹配测试参数
     * @return 匹配结果（置信度、是否可回答、推荐问题）
     */
    public RagDocumentMatchRet ragDocumentMatch(RagDocumentMatchParam param) {
        RagDocumentMatchRet ret = new RagDocumentMatchRet();
        VectorDocument result = vectorStoreService.computeContentScore(param.getUserQuestion(), param.getContent());
        double score = result != null ? result.getScore() : 0.0;
        ret.setConfidence(score);
        double threshold = chatRagProperties.getSimilarityThreshold();
        boolean canAnswer = score > threshold;
        ret.setCanAnswer(canAnswer);

        if (canAnswer) {
            // 推荐问题
            String prompt = String.format(EnergyAiConstant.PROMPT_RAG_RECOMMEND_QUESTION_TEMPLATE, param.getContent());
            String recommended = generateRecommended(param, prompt);
            ret.setRecommendedQuestions(recommended);
            // 回答
            prompt = String.format(EnergyAiConstant.PROMPT_RAG_RECOMMEND_ANSWER_TEMPLATE, param.getUserQuestion(), param.getContent());
            String questionAnswer = generateRecommended(param, prompt);
            ret.setQuestionAnswer(questionAnswer);
        } else {
            ret.setQuestionAnswer("根据资料内容暂无法回答该问题");
        }
        return ret;
    }

    /**
     * 当匹配度不足时，调用AI推荐相关问题
     */
    private String generateRecommended(RagDocumentMatchParam param, String prompt) {
        try {
            KnowledgeAIQueryParam query = new KnowledgeAIQueryParam();
            query.setChatId(System.currentTimeMillis());
            query.setScopeType("Test");
            query.setBusinessType("Test");
            query.setQueryType(ApiQaType.DOMAIN.getCode());
            query.setQuestion(prompt);
            RequestRagContext requestRagContext = new RequestRagContext();
            requestRagContext.setChatId(query.getChatId());
            String answer = energyAiApp.simpleChat(query, requestRagContext);
            return StrUtil.isNotBlank(answer) ? answer : "";
        } catch (Exception e) {
            log.error("generate recommended questions error", e);
            return "";
        }
    }


    /**
     * 简单对话问答
     *
     * @param param 匹配参数
     * @return 匹配结果
     */
    public SimpleChatRet simpleChat(SimpleChatParam param) {
        SimpleChatRet ret = new SimpleChatRet();

        if (StrUtil.isNotBlank(param.getContent())) {
            VectorDocument result = vectorStoreService.computeContentScore(param.getUserQuestion(), param.getContent());
            double score = result != null ? result.getScore() : 0.0;
            ret.setConfidence(score);
            double threshold = chatRagProperties.getSimilarityThreshold();
            boolean canAnswer = score > threshold;
            ret.setCanAnswer(canAnswer);
        }
        String prompt = param.getPrompt();
        if (StrUtil.isBlank(prompt)) {
            prompt = "简短概要回答问题：" + param.getUserQuestion();
        }
        if (StrUtil.isNotBlank(param.getContent())) {
            prompt = "参考内容：" + param.getContent() + "\n" + prompt;
        }
        KnowledgeAIQueryParam query = new KnowledgeAIQueryParam();
        query.setChatId(param.getChatId());
        query.setScopeType("Test");
        query.setBusinessType("Test");
        query.setQueryType(ApiQaType.DOMAIN.getCode());
        query.setQuestion(prompt);
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(query.getChatId());
        String answer = energyAiApp.simpleChat(query, requestRagContext);
        ret.setQuestionAnswer(answer);
        return ret;
    }

}
