package com.assistant.ai.repository.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI Token 用量 DTO
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
@Builder
public class TokenUsageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long chatId;

    private Long userId;

    private Integer userType;

    private Long groupId;

    private String scopeType;

    private String businessType;

    private String question;

    private String modelName;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private String apiType;

    private Long requestCostMs;

    private Integer status;

    private String errorMessage;

}
