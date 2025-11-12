package com.assistant.ai.repository.domain.query;

import com.assistant.service.common.annotation.Query;
import com.assistant.service.common.base.BaseQueryParam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * ...
 *
 * @author endcy
 * @date 2025/8/5 21:08:15
 */
@Getter
@Setter
public class KnowledgeDocumentQueryParam extends BaseQueryParam {
    private static final long serialVersionUID = 7352860475373138899L;

    @Query
    private Long id;

    @Query
    private String scopeType;

    @Query
    private String businessType;

    @Query(type = Query.Type.INNER_LIKE)
    private String title;

    @Query
    private Long groupId;

    @Query
    private String sourceType;

    @Query
    private String sourcePath;

    @Query
    private Integer docVersion;

    @Query
    private Boolean enablePublic;

    @Query
    private Boolean loaded;

    @Query
    private Boolean enabled;

    @Query(type = Query.Type.GREATER_THAN, propName = "expiredTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiredTimeGt;

    @Query(type = Query.Type.BETWEEN, propName = "createTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private List<Date> createTime;

}
