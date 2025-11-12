package com.assistant.service.common.exception;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * 乐观锁异常
 */
public class OptimisticLockException extends CoException {
    private static final Integer status = CONFLICT.value();

    public OptimisticLockException(String message, String messageEn) {
        super(message, messageEn);
    }
}
