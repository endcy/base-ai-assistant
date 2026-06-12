package com.assistant.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author pengpan
 * @since 2026/1/16 18:07
 */
@Data
public class RecordResponse implements Serializable {

    private static final long serialVersionUID = 121559436960646084L;

    private String question;

    private String content;

    private Date createTime;
}
