package com.endcy.ai.manager;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.app.EnergyAiApp;
import com.endcy.ai.app.EnergyAiDocumentApp;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.constant.EnergyAiConstant;
import com.endcy.ai.domain.context.RequestRagContext;
import com.endcy.ai.guardrail.GuardrailResult;
import com.endcy.ai.guardrail.InputGuardrailChain;
import com.endcy.ai.rag.DirectTextSimilarityService;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.RagDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.SimpleChatParam;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.domain.response.RagDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.SimpleChatRet;
import com.endcy.ai.rpc.enums.ApiQaType;
import com.endcy.ai.rpc.enums.MessageType;
import com.endcy.ai.util.CommonThreadUtils;
import com.endcy.ai.util.DocumentConvertUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

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
    private final ChatRagProperties chatRagProperties;
    private final DirectTextSimilarityService directTextSimilarityService;
    private final InputGuardrailChain inputGuardrailChain;

    /**
     * 输入护栏校验：被拦截时直接返回预设响应；命中 PII 脱敏时改写用户问题后放行。
     *
     * @return 通过返回 null；被拦截时返回预设的拦截响应文本
     */
    private String applyInputGuardrail(KnowledgeAIQueryParam query) {
        GuardrailResult result = inputGuardrailChain.check(query.getQuestion(), null);
        if (result.isBlocked()) {
            log.warn("输入护栏拦截 chatId={}: {}", query.getChatId(), result.getReason());
            return result.getPresetResponse();
        }
        if (result.isRedacted()) {
            // 用脱敏后的问题替换原始输入，避免 PII 进入模型与日志
            query.setQuestion(result.getRedactedContent());
        }
        return null;
    }

    /**
     * 同步问答
     *
     * @param query 问答查询参数
     * @return AI 回答结果
     */
    public AIAnswerRet qaSync(KnowledgeAIQueryParam query) {
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(query.getChatId());
        String guardBlocked = applyInputGuardrail(query);
        if (guardBlocked != null) {
            AIAnswerRet blocked = new AIAnswerRet();
            blocked.setText(guardBlocked);
            return blocked;
        }
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
        String guardBlocked = applyInputGuardrail(query);
        if (guardBlocked != null) {
            AIStreamResponse blockedResp = new AIStreamResponse();
            blockedResp.setChatId(query.getChatId());
            blockedResp.setType(MessageType.TEXT);
            blockedResp.setData(guardBlocked);
            blockedResp.setFinal(true);
            return Flux.just(blockedResp);
        }
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
        double score = directTextSimilarityService.calculateSimilarity(param.getUserQuestion(), param.getContent());
        if (score < 0) {
            log.warn("ragDocumentMatch match failed: {}, userQuestion: {}, content 100 chars: {}",
                    score, param.getUserQuestion(), StrUtil.maxLength(param.getContent(), 100));
            score = 0.0;
        }
        ret.setConfidence(score);
        double threshold = chatRagProperties.getSimilarityThreshold();
        boolean canAnswer = score > threshold;
        ret.setCanAnswer(canAnswer);

        if (canAnswer) {
            // 并行执行推荐问题和回答生成
//            String recommendPrompt = String.format(EnergyAiConstant.PROMPT_RAG_RECOMMEND_QUESTION_TEMPLATE, param.getContent());
//            CompletableFuture<String> recommendFuture = CompletableFuture.supplyAsync(
//                    () -> generateRecommended(param, recommendPrompt), CommonThreadUtils.AI_TASK_EXECUTOR);
//            ThreadUtil.sleep(300);
            String answerPrompt = String.format(EnergyAiConstant.PROMPT_RAG_RECOMMEND_ANSWER_TEMPLATE, param.getUserQuestion(), param.getContent());
            CompletableFuture<String> answerFuture = CompletableFuture.supplyAsync(
                    () -> generateRecommended(param, answerPrompt), CommonThreadUtils.AI_TASK_EXECUTOR);
            ret.setQuestionAnswer(answerFuture.join());

//            ret.setRecommendedQuestions(recommendFuture.join());
            ret.setRecommendedQuestions("");
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
            double score = directTextSimilarityService.calculateSimilarity(param.getUserQuestion(), param.getContent());
            if (score < 0) {
                log.warn("simpleChat match failed: {}, userQuestion: {}, content 100 chars: {}",
                        score, param.getUserQuestion(), StrUtil.maxLength(param.getContent(), 100));
                score = 0.0;
            }
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
