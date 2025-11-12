package com.assistant.ai.rag.filter;

import cn.hutool.core.map.MapUtil;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

/**
 * ...
 *
 * @author endcy
 * @date 2025/10/30 20:53:32
 */
public class VectorStoreFilterUtil {

    /**
     * 构建检索增强的元数据过滤条件 这里仅考虑属性等于条件的筛选，其他大于小于or者包含的筛选条件待拓展
     *
     * @param expressionParams .
     * @return .
     */
    public static Filter.Expression buildFilterExpression(Map<String, Object> expressionParams) {
        if (MapUtil.isEmpty(expressionParams)) {
            return null;
        }
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        // 创建一个空的表达式作为起点
        FilterExpressionBuilder.Op finalExpression = null;

        for (Map.Entry<String, Object> entry : expressionParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            FilterExpressionBuilder.Op currentExpression;

            if (value instanceof List<?> values) {
                // 如果值是列表，则使用in操作符
                currentExpression = builder.in(key, values.toArray());
            } else {
                // 其他情况也使用eq，可以根据需要扩展更多类型支持
                currentExpression = builder.eq(key, value);
            }

            // 将当前表达式与之前的表达式用AND连接
            if (finalExpression == null) {
                finalExpression = currentExpression;
            } else {
                finalExpression = builder.and(finalExpression, currentExpression);
            }
        }

        return finalExpression != null ? finalExpression.build() : null;
    }
}
