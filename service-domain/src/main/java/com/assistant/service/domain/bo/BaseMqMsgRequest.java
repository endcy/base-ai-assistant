package com.assistant.service.domain.bo;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.assistant.service.domain.enums.MqMsgTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 推送基础参数信息，由业务过程决定使用什么首推方案
 * 执行策略之前查出模板，组装参数，然后推送
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Data
@NoArgsConstructor
public class BaseMqMsgRequest implements Serializable {
    private static final long serialVersionUID = 5538264460777189717L;
    /**
     * 推送通知业务类型
     */
    private MqMsgTypeEnum bizType;

    private Date requestDate;


    public Date getRequestDate() {
        return ObjectUtil.defaultIfNull(requestDate, DateUtil.date());
    }
}
