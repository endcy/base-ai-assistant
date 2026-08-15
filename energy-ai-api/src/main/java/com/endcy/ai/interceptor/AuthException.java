package com.endcy.ai.interceptor;

import com.endcy.service.common.exception.CoException;

/**
 * 认证异常
 * 用于请求鉴权失败时抛出
 *
 * @author endcy
 * @since 2026/06/11 10:00:00
 */
public class AuthException extends CoException {

    private static final long serialVersionUID = 1L;

    public AuthException(String msg) {
        super(msg, msg);
    }

    public AuthException(String msg, String msgEn) {
        super(401, msg, msgEn);
    }
}
