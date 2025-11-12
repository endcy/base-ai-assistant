package com.assistant.service.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.assistant.service.common.exception.BadRequestException;
import com.assistant.service.common.exception.CoException;

/**
 * 验证工具
 */
public class ValidationUtil {

    /**
     * 验证空
     */
    public static void isNull(Object obj, String entity, String parameter, Object value) {
        if (ObjectUtil.isNull(obj)) {
            String msg = entity + " 不存在: " + parameter + " is " + value;
            throw new BadRequestException(msg);
        }
    }

    public static <T> void validate(T param, ValidateFunction<T> validateFunction) {
        validateFunction.validate(param);
    }

    @FunctionalInterface
    public interface ValidateFunction<T> {
        void validate(T t) throws CoException;
    }
}
