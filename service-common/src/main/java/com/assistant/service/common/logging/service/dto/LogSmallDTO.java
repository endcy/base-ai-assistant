package com.assistant.service.common.logging.service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class LogSmallDTO implements Serializable {

    private static final long serialVersionUID = -251182967275050600L;

    private String description;

    private String requestIp;

    private Long time;

    private String address;

    private String browser;

    private Date createTime;
}
