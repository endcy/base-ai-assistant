package com.assistant.service.common.annotation;

import java.lang.annotation.*;

/**
 * 对象属性自然语言表达注解
 * 用于将对象属性转换为自然语言描述，便于 RAG 服务理解
 *
 * @author endcy
 * @date 2026/04/10 10:36:00
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tips {

    /**
     * 属性自然语言名称
     *
     * @return 属性的中文或自然语言描述名称
     */
    String name();

    /**
     * 枚举值解释属性名
     * 用于指定从枚举对象中获取哪个属性值作为解释
     * 如 enumDesc="name" 表示获取枚举的 name 属性
     *
     * @return 枚举解释属性名，空表示直接使用枚举名
     */
    String enumDesc() default "";

    /**
     * 单位
     * 用于在数值后附加单位，如"元"、"kg"等
     *
     * @return 单位字符串
     */
    String unit() default "";

    /**
     * 日期格式
     * 用于指定 Date 类型属性的输出格式
     *
     * @return 日期格式字符串，如"yyyy-MM-dd HH:mm:ss"
     */
    String dateFormat() default "";

    /**
     * 额外解释说明
     * 用于附加额外的描述信息
     *
     * @return 解释说明文本
     */
    String explain() default "";

    /**
     * 嵌套对象的最大展开深度
     * 用于防止无限递归，默认深度为 3
     *
     * @return 最大嵌套深度
     */
    int maxDepth() default 3;

    /**
     * 名称策略键
     * 用于指定通过 NameStrategyHandler 等处理器转换为对应的名称
     * 例如：nameStrategy = ObjectConvertConstant.ENERGY_ANGENT_NAME，则将 operatorId 值转换为运营商名称
     *
     * @return 名称策略键
     */
    String nameStrategy() default "";

    /**
     * 是否脱敏
     *
     * @return 启用脱敏
     */
    boolean desensitization() default false;

    String hiddenValue() default "";
}
