package com.assistant.service.common.exception;

import cn.hutool.core.util.StrUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class CoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Integer errorCode;
    private String message;
    private String messageEn = null;

    protected CoException() {
        super();
    }

    public CoException(String messageEn) {
        super(messageEn);
        this.message = messageEn;
        this.messageEn = messageEn;
    }

    public CoException(String messageEn, Object... formatParams) {
        super(StrUtil.format(messageEn, formatParams));
        String messageEnFormat = StrUtil.format(messageEn, formatParams);
        this.message = messageEnFormat;
        this.messageEn = messageEnFormat;
    }

    public CoException(String message, String messageEn, Object... formatParams) {
        super(StrUtil.format(message, formatParams));
        this.message = StrUtil.format(message, formatParams);
        this.messageEn = StrUtil.format(messageEn, formatParams);
    }

    public CoException(String message, String messageEn) {
        super(message);
        this.message = message;
        this.messageEn = messageEn;
    }

    public CoException(Integer errorCode, String message, String messageEn) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.messageEn = messageEn;
    }

    public CoException(HttpStatus apiCode) {
        super(apiCode.getReasonPhrase());
        this.errorCode = apiCode.value();
        this.message = apiCode.getReasonPhrase();
    }

    public CoException(String message, String messageEn, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.messageEn = messageEn;
    }

    public CoException(Throwable cause) {
        super(cause);
    }
}
