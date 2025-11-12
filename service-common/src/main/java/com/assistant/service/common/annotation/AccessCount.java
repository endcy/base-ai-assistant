package com.assistant.service.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 访问统计
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessCount {

    //接口key名称
    String key() default "";

}
