package com.endcy.ai.rpc.domain.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * AI问答参数
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeAIQueryParam extends BaseAiRequest {
    private static final long serialVersionUID = -8339257007205947491L;

    @NotBlank(message = "question不能为空")
    private String question;

    private String businessType;

    /**
     * 查询类型 1=知识库问答 2=领域知识问答 3=DeepSeek在线搜索
     * <p>默认 DOMAIN(2)：调用方不传时避免拆箱 NPE，走领域知识问答链路。</p>
     *
     * @see com.endcy.ai.rpc.enums.ApiQaType
     */
    private Integer queryType = 2;

    private List<MediaAttachment> mediaList;
}
