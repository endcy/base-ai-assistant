package com.assistant.service.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocSourceTypeEnum {
    /**
     * 文档来源
     */
    FILE("文件"),
    DATABASE("数据库"),
    API("api"),
    ONLINE("在线检索"),
    UNKNOWN("未知"),
    ;

    @EnumValue
    @JsonValue
    private final String channel;

    @JsonCreator
    public static DocSourceTypeEnum create(String value) {
        for (DocSourceTypeEnum gender : DocSourceTypeEnum.values()) {
            if (gender.channel.equals(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
