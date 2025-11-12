package com.assistant.service.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExMqMsgRequest extends BaseMqMsgRequest {
    private static final long serialVersionUID = -8947103097315308014L;

    private String params;

}
