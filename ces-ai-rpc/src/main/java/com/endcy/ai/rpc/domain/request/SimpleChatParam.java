package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 简单问答
 *
 * @author endcy
 * @since 2026/06/09
 */
@Data
public class SimpleChatParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "userQuestion不能为空")
    private String userQuestion;

    @NotNull(message = "chatId不能为空")
    private Long chatId;

    private String content;

    private String prompt;
}
