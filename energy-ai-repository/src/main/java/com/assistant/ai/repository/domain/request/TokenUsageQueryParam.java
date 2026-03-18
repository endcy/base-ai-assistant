package com.assistant.ai.repository.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Token 用量查询参数
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
public class TokenUsageQueryParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long chatId;

    private Long userId;

    private Integer userType;

    private Long groupId;

    private String scopeType;

    private String businessType;

    private String modelName;

    private String apiType;

    private Date startTime;

    private Date endTime;

    private Integer status;

}
