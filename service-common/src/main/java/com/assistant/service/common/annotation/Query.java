package com.assistant.service.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

    // Dong ZhaoYang 2017/8/7 基本对象的属性名
    String propName() default "";

    // Dong ZhaoYang 2017/8/7 查询方式
    Type type() default Type.EQUAL;

    /**
     * 多字段模糊搜索，仅支持String类型字段，多个用逗号隔开, 如@Query(blurry = "email,username")
     */
    String blurry() default "";

    /**
     * 同上，但是使用=等于判断
     */
    String blurryEq() default "";

    String sql() default "";

    /**
     * 表别名，一般用在关联查询防止多表条件同字段冲突问题，使用query参数的查询务必重写sql附上表别名
     */
    String tableAlias() default "";

    enum Type {
        // 相等
        EQUAL
        // 不等于
        , NOT_EQUAL
        // Dong ZhaoYang 2017/8/7 大于
        , GREATER_THAN
        // Dong ZhaoYang 2017/8/7 大于等于
        , GREATER_THAN_EQ
        // Dong ZhaoYang 2017/8/7 小于
        , LESS_THAN
        // Dong ZhaoYang 2017/8/7 小于等于
        , LESS_THAN_EQ, INNER_LIKE  // Dong ZhaoYang 2017/8/7 中模糊查询 '%abc%'

        , LEFT_LIKE  // Dong ZhaoYang 2017/8/7 左模糊查询 '%abc'

        , RIGHT_LIKE  // Dong ZhaoYang 2017/8/7 右模糊查询 'abc%'
        , NOT_LIKE
        // 包含
        , IN, NOT_IN
        // SQL 语句
        , IN_SQL
        // between
        , BETWEEN, NOT_BETWEEN
        // 不为空
        , NOT_NULL
        // 为空
        , IS_NULL, ANY_LIKE
    }

}

