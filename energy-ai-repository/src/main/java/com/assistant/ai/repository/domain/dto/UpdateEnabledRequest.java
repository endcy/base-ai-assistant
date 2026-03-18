package com.assistant.ai.repository.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新启用状态请求 DTO
 *
 * @author AI Assistant
 * @since 2026/03/18
 */
@Data
public class UpdateEnabledRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类 ID 列表
     */
    private List<Long> ids;

    /**
     * 启用状态
     */
    private Boolean enabled;
}
