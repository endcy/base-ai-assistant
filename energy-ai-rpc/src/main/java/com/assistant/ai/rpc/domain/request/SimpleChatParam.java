package com.assistant.ai.rpc.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 简单问答
 *
 * @author endcy
 * @date 2026/06/09
 */
@Data
public class SimpleChatParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户自定义问题
     */
    @NotBlank(message = "userQuestion不能为空")
    private String userQuestion;

    /**
     * 对话id
     */
    @NotNull(message = "chatId不能为空")
    private Long chatId;

    /**
     * 知识参考资料内容
     */
    private String content;

    /**
     * 提示词
     */
    private String prompt;

}
