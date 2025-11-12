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
public class ContextUserRecordQueryParam extends BaseQueryParam {
    private static final long serialVersionUID = 7352860475373138899L;

    @Query
    private Long id;

    @Query
    private Long chatId;

    @Query
    private Long userId;

    @Query
    private Integer userType;

    @Query
    private Long groupId;

    @Query
    private String scopeType;

    @Query
    private String businessType;

    @Query
    private Boolean enabled;

    @Query(type = Query.Type.BETWEEN, propName = "createTime")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private List<Date> createTime;

}
