package com.assistant.service.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.assistant.service.domain.bo.BaseMqMsgRequest;
import com.assistant.service.domain.bo.ExMqMsgRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQ消息业务类型
 */
@Getter
@AllArgsConstructor
public enum MqMsgTypeEnum {
    /**
     * 消息业务类型
     */
    CAPTCHA(0, "示例消息类型", ExMqMsgRequest.class),
    ;

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;
    private final Class<? extends BaseMqMsgRequest> paramClass;

    @JsonCreator
    public static MqMsgTypeEnum create(Object value) {
        for (MqMsgTypeEnum obj : MqMsgTypeEnum.values()) {
            if (value instanceof String) {
                if (obj.code.toString().equals((value))) {
                    return obj;
                }
            } else {
                if (obj.code.equals((value))) {
                    return obj;
                }
            }
        }
        throw new IllegalArgumentException("MqMsgTypeEnum No element matches " + value);
    }
}
