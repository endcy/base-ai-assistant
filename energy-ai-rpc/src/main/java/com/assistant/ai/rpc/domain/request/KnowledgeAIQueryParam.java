package com.assistant.ai.rpc.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI问答
 * 返回根据调用类型，response使用 流式输出StreamObserver<String> 或者 普通返回String
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
public class KnowledgeAIQueryParam implements Serializable {
    private static final long serialVersionUID = -8339257007205947491L;

    /**
     * 聊天id 请使用雪花id 以区分聊天窗口
     */
    @NotNull(message = "chatId不能为空")
    private Long chatId;

    /**
     * 内容或关键词
     * 多关键词使用,英文逗号分隔
     */
    @NotBlank(message = "question不能为空")
    private String question;

    /**
     * 知识领域类型 根据录入本系统知识库的文档定义 可选
     *
     * @see .KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * 知识业务模块 根据录入本系统知识库的文档定义 可选
     *
     * @see .KnowledgeBusinessTypeEnum
     */
    private String businessType;

    /**
     * 查询类型 1=知识库问答 2=领域知识问答 3=DeepSeek在线搜索
     *
     * @see com.assistant.ai.rpc.enums.ApiQaType
     */
    @NotNull(message = "queryType查询类型不能为空")
    private Integer queryType;

    /**
     * 多媒体附件列表（图片、音频、视频等）
     * 支持多模态输入，每个附件包含类型、URL、描述等信息
     */
    private List<MediaAttachment> mediaList;
}
