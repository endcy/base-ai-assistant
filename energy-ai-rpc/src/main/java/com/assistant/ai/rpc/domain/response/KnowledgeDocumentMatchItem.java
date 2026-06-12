package com.assistant.ai.rpc.domain.response;


import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.io.Serializable;

/**
 * ...
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
@Builder
public class KnowledgeDocumentMatchItem implements Serializable {
    private static final long serialVersionUID = -8331257007105947091L;

    @Tolerate
    public KnowledgeDocumentMatchItem() {
    }

    /**
     * 三方系统文档id
     */
    private Long id;

    /**
     * 存储到AI服务中的知识领域类型
     *
     * @see KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * 存储到AI服务中的知识业务模块 可选
     *
     * @see KnowledgeBusinessTypeEnum
     */
    private String businessType;

    /**
     * 文档的标题
     */
    private String title;

    /**
     * 存储到AI服务中的文档库定义的来源
     */
    private String source;

    /**
     * 匹配得分 无需展示
     */
    private Double score;
}
