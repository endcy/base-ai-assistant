package com.assistant.ai.repository.domain.query;

import com.assistant.service.common.annotation.Query;
import com.assistant.service.common.base.BaseQueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * ...
 *
 * @author endcy
 * @date 2025/8/5 21:08:15
 */
@Getter
@Setter
public class VectorDocumentQueryParam extends BaseQueryParam {
    private static final long serialVersionUID = 2152860475373138899L;

    @Query
    private String id;

    @Query
    private Long knowledgeId;

    @Query
    private Integer docVersion;

}
