package com.endcy.service.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
public class MybatisPlusFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date currentTime = new Date();
        if (metaObject.hasSetter("createTime") && ObjectUtil.isNull(metaObject.getValue("createTime"))) {
            setFieldValByName("createTime", currentTime, metaObject);
        }
        if (metaObject.hasSetter("updateTime")) {
            setFieldValByName("updateTime", currentTime, metaObject);
        }
        // 业务日期字段（如 ai_token_usage.request_date 为 NOT NULL 无默认值，插入时必须填充）
        if (metaObject.hasSetter("requestDate") && ObjectUtil.isNull(metaObject.getValue("requestDate"))) {
            setFieldValByName("requestDate", currentTime, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            Date currentTime = new Date();
            setFieldValByName("updateTime", currentTime, metaObject);
        }
    }

}
