package com.assistant.ai.advisor.retriever;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.rag.filter.VectorStoreFilterUtil;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.util.DocumentQueryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 基础检索器抽象类
 *
 * @author endcy
 * @date 2025/12/4 14:21:58
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseDocumentRetriever implements DocumentRetriever {
    protected final DocumentQueryContext documentQueryContext;
    protected final ChatRagProperties chatRagProperties;

    public List<Document> retrieve(@NotNull List<Query> query) {
        // 查询拓展 顺序执行
        List<Document> results = CollUtil.newArrayList();
        query.parallelStream().forEach(item -> {
            if (item != null && StrUtil.isNotBlank(item.text())) {
                results.addAll(retrieve(item));
            }
        });
        return results;
    }

    protected Filter.Expression computeRequestFilterExpression(Query query, String filterExpressionKey) {
        var contextFilterExpression = query.context().get(filterExpressionKey);
        if (contextFilterExpression != null) {
            if (contextFilterExpression instanceof Filter.Expression) {
                return (Filter.Expression) contextFilterExpression;
            } else if (StringUtils.hasText(contextFilterExpression.toString())) {
                return new FilterExpressionTextParser().parse(contextFilterExpression.toString());
            }
        }
        // 构建复杂的文档过滤条件
        Map<String, Object> expressionParams = DocumentQueryUtils.convertDocumentQueryMap(documentQueryContext);
        return VectorStoreFilterUtil.buildFilterExpression(expressionParams);
    }


}
