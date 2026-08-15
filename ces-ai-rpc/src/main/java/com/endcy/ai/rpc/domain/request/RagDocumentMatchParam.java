package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * RAG文档召回匹配测试请求参数
 *
 * @author endcy
 * @since 2026/05/19
 */
@Data
public class RagDocumentMatchParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "userQuestion不能为空")
    private String userQuestion;

    @NotBlank(message = "问答content不能为空")
    private String content;
}
