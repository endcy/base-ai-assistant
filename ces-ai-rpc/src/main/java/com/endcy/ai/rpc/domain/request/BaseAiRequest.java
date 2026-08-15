package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * AI 服务基础请求参数
 *
 * @author endcy
 * @since 2026-08-13
 */
@Data
public class BaseAiRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "chatId不能为空")
    private Long chatId;

    @NotNull(message = "groupId不能为空")
    private String groupId;

    @NotNull(message = "userId不能为空")
    private String userId;

    private String scopeType;
}
