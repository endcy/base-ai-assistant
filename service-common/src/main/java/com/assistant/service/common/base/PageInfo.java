package com.assistant.service.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Accessors(chain = true)
public class PageInfo<T> implements Serializable {
    @Schema(description = "总数量")
    private long totalElements;

    @Schema(description = "内容")
    private List<T> content;

    public PageInfo(long totalElements, List<T> content) {
        this.totalElements = totalElements;
        this.content = content;
    }

    public PageInfo() {
    }

    public static <T> PageInfo<T> empty() {
        PageInfo<T> pageInfo = new PageInfo<>();
        pageInfo.setTotalElements(0);
        pageInfo.setContent(new ArrayList<>());
        return pageInfo;
    }
}
