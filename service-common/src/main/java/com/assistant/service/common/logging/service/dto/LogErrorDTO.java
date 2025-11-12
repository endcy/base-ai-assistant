package com.assistant.service.common.logging.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class LogErrorDTO implements Serializable {
    private static final long serialVersionUID = 8505317772970281477L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String description;

    private String method;

    private String params;

    private String browser;

    private String requestIp;

    private String address;

    private Date createTime;
}
