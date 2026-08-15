package com.endcy.ai.rag;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.endcy.ai.repository.domain.context.DocumentQueryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * Self-Querying 重写器 —— 把用户自然语言问题翻译成结构化元数据过滤条件。
 *
 * <p>借鉴 Dify {@code _automatic_metadata_filter_func} 实现。
 * 使用 LLM + few-shot 将 "我想查深圳的故障站点" 翻译为：
 * {@code scopeType=用户客服, businessType=故障, city=深圳}，增强检索精准度。</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfQueryRewriter {

    private static final String SYSTEM_PROMPT = """
            你是查询重写专家。把用户的自然语言问题翻译成结构化元数据过滤条件。
            可用元数据字段：scopeType（知识范围）、businessType（业务类型：CHARGE_ORDER/STATION/FAULT/BILLING/UNKNOWN）、groupId（租户）。
            严格输出 JSON：{"scopeType":"...","businessType":"...","keywords":"核心检索词"}
            不要输出其他内容。
            """;

    private final DashScopeChatModel dashscopeChatModel;

    /**
     * 重写：自然语言 → 结构化查询条件。
     *
     * @param userQuestion 用户原始问题
     * @return 结构化条件（失败时返回 null，调用方走原 query）
     */
    public QueryCondition rewrite(String userQuestion) {
        if (StrUtil.isBlank(userQuestion)) {
            return null;
        }
        try {
            String userPrompt = "用户问题：" + userQuestion + "\n请输出 JSON。";
            BeanOutputConverter<QueryCondition> converter = new BeanOutputConverter<>(QueryCondition.class);
            QueryCondition result = ChatClient.builder(dashscopeChatModel)
                                              .build()
                                              .prompt()
                                              .system(SYSTEM_PROMPT)
                                              .user(u -> u.text(userPrompt).param("format", converter.getFormat()))
                                              .call()
                                              .entity(converter);
            log.info("Self-query 重写: '{}' → {}", userQuestion, result);
            return result;
        } catch (Exception e) {
            log.warn("Self-query 重写失败，降级为原 query: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将重写结果应用到 DocumentQueryContext。
     */
    public void applyTo(QueryCondition condition, DocumentQueryContext ctx) {
        if (condition == null || ctx == null) {
            return;
        }
        if (StrUtil.isNotBlank(condition.getScopeType())) {
            ctx.setScopeType(condition.getScopeType());
        }
        if (StrUtil.isNotBlank(condition.getBusinessType())) {
            ctx.setBusinessType(condition.getBusinessType());
        }
        if (StrUtil.isNotBlank(condition.getKeywords())) {
            ctx.setReReadingQuestion(condition.getKeywords());
        }
    }

    /**
     * 结构化查询条件。
     */
    @lombok.Data
    public static class QueryCondition {
        private String scopeType;
        private String businessType;
        private String keywords;
    }
}
