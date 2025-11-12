package com.assistant.service.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识领域 类型
 * 仅作为规范参考 不作为强制引用分类 因为可能随时新增类型
 */
@Getter
@AllArgsConstructor
public enum KnowledgeScopeTypeEnum {
    /**
     * 知识领域
     */
    MARKET_CUSTOMER_SERVICE("市场客服"),
    ACCOUNT_CUSTOMER_SERVICE("用户客服"),
    OPERATOR_CUSTOMER_SERVICE("商户客服"),
    OPERATIONS_REFERENCE("运营资料"),
    DEVELOPER_REFERENCE("开发运维资料"),
    UNKNOWN("未知"),
    ;

    @EnumValue
    @JsonValue
    private final String desc;

    @JsonCreator
    public static KnowledgeScopeTypeEnum create(String value) {
        for (KnowledgeScopeTypeEnum gender : KnowledgeScopeTypeEnum.values()) {
            if (gender.desc.equals(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
