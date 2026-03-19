package com.assistant.ai.domain.enums;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检索增强数据来源预测枚举
 */
@Getter
@AllArgsConstructor
public enum PossibleSourceTypeEnum {
    /**
     * 业务模块
     */
    LOCAL("本地文档", "包含了特殊运维配置说明、代码、平台底层操作记录等文档"),
    VECTOR("数据库文档", "包含了各类客服、售后、技术咨询、用户常见问题等文档数据"),
    CLOUD("在线云文档", "暂无文档"),
    DATABASE("表数据", "包含了放电订单、占位订单、会员订单、用户资金订单、站点信息、设备信息、各类计费策略信息等等相关的表内容数据"),
    UNKNOWN("未知", "用户其他问题或非充电运营、非能源管理等相关的问题，不属于上述可能的数据来源，无文档依赖"),
    ;

    @JsonValue
    private final String type;
    private final String desc;

    @JsonCreator
    public static PossibleSourceTypeEnum create(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (PossibleSourceTypeEnum gender : PossibleSourceTypeEnum.values()) {
            if (gender.name().equals(value) || gender.type.equals(value)) {
                return gender;
            }
        }
        return null;
    }
}
