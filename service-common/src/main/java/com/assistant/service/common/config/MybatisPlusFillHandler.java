package com.assistant.service.common.config;

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
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            Date currentTime = new Date();
            setFieldValByName("updateTime", currentTime, metaObject);
        }
    }

}
