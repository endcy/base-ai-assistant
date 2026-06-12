package com.assistant.ai.interceptor;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.service.common.exception.CoException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import reactor.core.publisher.Flux;

/**
 * 全局异常处理器
 * 统一捕获并处理 Controller 层抛出的异常
 *
 * @author endcy
 * @since 2026/06/11 10:00:00
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> handleAsyncTimeoutException(AsyncRequestTimeoutException e, HttpServletRequest request) {
        log.error("handleAsyncTimeoutException: path={} {}", request.getRequestURI(), ExceptionUtil.getMessage(e));
        return ResponseEntity
                .status(HttpStatus.REQUEST_TIMEOUT)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(null);
    }

    @ExceptionHandler(CoException.class)
    public ResponseEntity<?> handleCoException(Throwable e, HttpServletRequest request) {
        log.error("handleCoException: path={} {}", request.getRequestURI(), ExceptionUtil.getMessage(e));
        if (e instanceof ClientAbortException) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.error("handleCoException:", e);
        }
        CommonResMsgDTO<Object> ret = CommonResMsgDTO.errorDeviceRes("系统错误，请稍候重试");
        return responseEntity(ret, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException e, HttpServletRequest request) {
        log.error("handleAuthException: path={}", request.getRequestURI());
        if (log.isDebugEnabled()) {
            log.error("handleAuthException:", e);
        }
        CommonResMsgDTO<Object> ret = CommonResMsgDTO.errorDeviceRes("请求不合法!");
        return responseEntity(ret, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 统一返回
     */
    private <T> ResponseEntity<CommonResMsgDTO<T>> responseEntity(CommonResMsgDTO<T> ret, HttpStatus httpStatus) {
        return new ResponseEntity<>(ret, httpStatus);
    }

    /**
     * 统一返回
     */
    private <T> ResponseEntity<Flux<T>> responseEntity(Flux<T> ret) {
        return new ResponseEntity<>(ret, HttpStatus.NON_AUTHORITATIVE_INFORMATION);
    }

}
