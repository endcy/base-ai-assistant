package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话记录返回
 *
 * @author endcy
 * @since 2026/1/16 18:07
 */
@Data
public class RecordResponse implements Serializable {

    private static final long serialVersionUID = 121559436960646084L;

    private String question;

    private String content;

    private Date createTime;
}
