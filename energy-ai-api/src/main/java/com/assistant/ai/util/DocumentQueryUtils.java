package com.assistant.ai.util;

import cn.hutool.core.bean.BeanUtil;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;

import java.util.Map;

/**
 * 文档查询工具类
 *
 * @author cxx641
 * @date 2025/12/4 14:42:26
 */
public class DocumentQueryUtils {
    private DocumentQueryUtils() {
    }

    public static Map<String, Object> convertDocumentQueryMap(DocumentQueryContext documentParams) {
        DocumentQueryContext copy = BeanUtil.copyProperties(documentParams, DocumentQueryContext.class);
        // question 不能作为元数据过滤条件
        copy.setOriginalQuestion(null);
        copy.setReReadingQuestion(null);
        return BeanUtil.beanToMap(copy, false, true);
    }
}
