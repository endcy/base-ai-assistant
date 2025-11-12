package com.assistant.service.common.base;

import cn.hutool.json.JSONUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class BaseQueryParam implements Serializable {

    private static final long serialVersionUID = -6929130029894803378L;
    /**
     * BETWEEN
     */
    @Getter
    private List<Date> createTime;

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }

}
