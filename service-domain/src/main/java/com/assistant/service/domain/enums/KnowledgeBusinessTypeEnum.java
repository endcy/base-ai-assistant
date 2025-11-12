package com.assistant.service.domain.enums;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 仅作为规范参考 不作为强制引用分类 因为可能随时新增类型
 * 每个知识领域下有N个业务模块
 *
 * @see KnowledgeScopeTypeEnum
 */
@Getter
@AllArgsConstructor
public enum KnowledgeBusinessTypeEnum {
    /**
     * 业务模块
     */
    HELLO("普通问候", "基本问候打招呼或者非充电运营能源领域业务内容咨询"),
    STATION("站点信息", "平台运营充放电、储能站点等信息咨询"),
    EQUIPMENT("设备信息", "平台运营充放电、储能设备等信息咨询"),
    ACCOUNT("用户信息", "平台用户信息或客户信息相关基础信息咨询"),
    CHARGE_ORDER("充电订单信息", "充电流程、充电订单内容相关信息咨询"),
    DISCHARGE_ORDER("放电订单信息", "放电流程、放电订单内容相关信息咨询"),
    ALARM("故障处理", "充放电或能源管理等过程出现的各类故障咨询"),
    NORMS("合作规范", "平台对接客户相关合作内容咨询"),
    API("接口文档", "平台中各类开发接口信息查询"),
    PRODUCTION("产品规划", "产品已支持的需求或相关规划内容问题咨询"),
    CLIENT_OPERATE("用户操作指南", "客户端中用户操作流程、操作内容相关信息咨询"),
    ADMIN_OPERATE("管理操作指南", "管理平台中商户或管理者操作流程、操作内容相关信息咨询"),
    MAINTENANCE("维护手册", "开发、运维或产品需求人员维护平台操作的各类操作内容相关信息咨询"),
    REPORTER("分析报告", "针对平台管理商户、开发运维人员或需求管理人员的数据统计需求，进行数据统计、分析，并给出相应的报告"),
    POWER_PREDICT("用电功率预测", "结合平台历史数据对未来时段的设备、站点的用电功率进行预测"),

    UNKNOWN("其他意图", "其他内容咨询"),
    ;

    @EnumValue
    @JsonValue
    private final String type;
    private final String desc;

    @JsonCreator
    public static KnowledgeBusinessTypeEnum create(String value) {
        if (StrUtil.isBlank(value)) {
            return UNKNOWN;
        }
        for (KnowledgeBusinessTypeEnum gender : KnowledgeBusinessTypeEnum.values()) {
            if (gender.name().equals( value) || gender.type.equals(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
