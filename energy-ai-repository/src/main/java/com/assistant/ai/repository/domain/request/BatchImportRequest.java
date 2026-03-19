package com.assistant.ai.repository.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量导入请求参数
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
public class BatchImportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目录路径
     */
    private String directoryPath;

    /**
     * 用户分组 ID（租户 ID）
     */
    private Long groupId;

    /**
     * 默认知识领域类型（如果无法从路径推断）
     */
    private String defaultScopeType;
}
