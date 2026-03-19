package com.assistant.ai.repository.domain.request;

import com.assistant.service.common.annotation.Query;
import com.assistant.service.common.base.BaseQueryParam;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 知识分类配置查询参数
 *
 * @author endcy
 * @since 2026/03/18
 */
@Getter
@Setter
public class KnowledgeCategoryQueryParam extends BaseQueryParam {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Query
    private Long id;

    /**
     * 分类类型 (scope-知识领域，business-业务领域)
     */
    @Query
    private String type;

    /**
     * 分类编码
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String code;

    /**
     * 分类名称
     */
    @Query(type = Query.Type.INNER_LIKE)
    private String name;

    /**
     * 父级分类编码
     */
    @Query
    private String parentCode;

    /**
     * 是否启用
     */
    @Query
    private Boolean enabled;

    /**
     * ID 列表 (用于批量查询)
     */
    private List<Long> ids;
}
