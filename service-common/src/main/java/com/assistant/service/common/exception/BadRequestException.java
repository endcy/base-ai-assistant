package com.assistant.service.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
public class BadRequestException extends CoException {

    private static final long serialVersionUID = 4549702093042618182L;

    private static final Integer STATUS = BAD_REQUEST.value();

    public BadRequestException(String msg) {
        super(msg, msg);
    }

    public BadRequestException(String msg, String msgEn) {
        super(STATUS, msg, msgEn);
    }

    public BadRequestException(HttpStatus status, String msg, String msgEn) {
        super(status.value(), msg, msgEn);
    }

    public BadRequestException(String msg, String messageEn, Object... formatParams) {
        super(msg, messageEn, formatParams);
    }
}
