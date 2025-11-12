package com.assistant.service.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 仅作为规范参考 不作为强制引用分类 因为可能随时新增类型
 * 每个知识领域下有N个内容模块
 *
 * @see KnowledgeBusinessTypeEnum
 */
@Getter
@AllArgsConstructor
public enum KnowledgeContentTypeEnum {
    /**
     * 具体业务信息 理论上对应内容对应标题
     */
    STATION_INFO("xxx站点信息"),
    EQUIPMENT_INFO("xxx设备信息"),
    ACCOUNT_INFO("xxx用户信息"),
    CHARGE_ORDER_INFO("xxx充电订单信息"),
    ALARM_INFO("xxx故障内容"),
    API_INFO("xxx接口文档"),
    REQUIREMENT_INFO("xxx产品需求"),
    OPERATE_INFO("xxx操作指南"),
    MAINTENANCE_INFO("xxx维护手册"),
    REPORTER_INFO("xxx分析报告"),
    UNKNOWN("未知"),
    ;

    @EnumValue
    @JsonValue
    private final String desc;

    @JsonCreator
    public static KnowledgeContentTypeEnum create(String value) {
        for (KnowledgeContentTypeEnum gender : KnowledgeContentTypeEnum.values()) {
            if (gender.desc.equals(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
