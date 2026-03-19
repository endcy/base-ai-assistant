package com.assistant.ai.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.service.common.constant.BusinessConstant;

import java.util.Map;

/**
 * 文档查询工具类
 *
 * @author endcy
 * @date 2025/12/4 14:42:26
 */
public class DocumentQueryUtils {
    private DocumentQueryUtils() {
    }

    /**
     * 知识文档元数据条件构造
     * 注意，在租户知识以外 针对平台级知识是共享的
     *
     * @param documentParams .
     * @return .
     */
    public static Map<String, Object> convertDocumentQueryMap(DocumentQueryContext documentParams) {
        DocumentQueryContext copy = BeanUtil.copyProperties(documentParams, DocumentQueryContext.class);
        // question 不能作为元数据过滤条件
        copy.setOriginalQuestion(null);
        copy.setReReadingQuestion(null);
        copy.setGroupId(null);

        Map<String, Object> filters = BeanUtil.beanToMap(copy, false, true);
        if (documentParams.getGroupId() == null) {
            return filters;
        } else if (!documentParams.getGroupId().equals(BusinessConstant.PLATFORM_GROUP_ID)) {
            // 租户/分组用户 支持查询平台知识文档
            filters.put(BusinessConstant.PLATFORM_GROUP_FIELD, CollUtil.newArrayList(documentParams.getGroupId(), BusinessConstant.PLATFORM_GROUP_ID));
            return filters;
        } else {
            return filters;
        }
    }
}
