package com.assistant.ai.rpc.domain.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * RAG文档召回匹配测试请求参数
 *
 * @author endcy
 * @date 2026/05/19
 */
@Data
public class RagDocumentMatchParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户自定义问题
     */
    @NotBlank(message = "userQuestion不能为空")
    private String userQuestion;

    /**
     * 知识文档内容，格式示例：
     * <pre>
     * [
     * 问题："怎么导出充电站的运行数据？"
     * 相似文法：["怎么导出充电站的运行数据？", "导出运行数据？"]
     * 回复内容："..."
     * ]
     * </pre>
     */
    @NotBlank(message = "问答content不能为空")
    private String content;

}
