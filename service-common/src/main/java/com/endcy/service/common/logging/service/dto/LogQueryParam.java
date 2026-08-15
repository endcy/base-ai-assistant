package com.endcy.service.common.logging.service.dto;

import com.endcy.service.common.annotation.Query;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class LogQueryParam {

    @Query
    private String username;

    /**
     * 精确
     */
    @Query
    private String logType;

    /**
     * BETWEEN
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Query(type = Query.Type.BETWEEN)
    private List<Date> createTime;
}
