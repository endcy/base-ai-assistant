package com.assistant.service.common.annotation;

import com.assistant.service.common.enums.LogActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRecord {
    String value() default "";

    /**
     * 是否启用
     */
    boolean enable() default true;

    LogActionType type() default LogActionType.SELECT;
}
