package com.assistant.ai.repository.domain.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库文档 向量查询条件
 *
 * @author endcy
 * @since 2025/12/04 10:21:26
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentQueryContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1183263090210064149L;

    private Long id;

    /**
     * 知识领域类型
     */
    private String scopeType;

    /**
     * 知识业务模块
     */
    private String businessType;

    /**
     * 内容分组 id，如租户 id
     * 支持为空，表示查询全平台任何分组的知识
     */
    private Long groupId;

    /**
     * 内容来源
     */
    private String sourceType;

    /**
     * 是否公开
     */
    private Boolean enablePublic;

    /**
     * 来源路径
     */
    private String sourcePath;


    /**
     * 原始问题
     */
    private String originalQuestion;

    /**
     * 重写问题
     */
    private String reReadingQuestion;

}
