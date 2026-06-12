package com.assistant.ai.interceptor;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.rpc.constant.RpcConfigConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单鉴权拦截器
 * 通过请求头中的 Authorization token 进行接口鉴权
 *
 * @author endcy
 * @since 2026/06/11 10:00:00
 */
@Slf4j
@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {

    @Value("${ai.service.client.access-token:123456abc}")
    private String SIMPLE_AUTH_KEY;

    private static final String ERROR_TIPS = "Authorization not valid!";

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws AuthException {
        String token = StrUtil.trimToEmpty(request.getHeader(RpcConfigConstant.AUTH_TOKEN));
        if (log.isDebugEnabled()) {
            log.debug("--- SimpleAuthInterceptor token[{}] ---", token);
        }
        if (StrUtil.isBlank(token) || !SIMPLE_AUTH_KEY.equals(token)) {
            throw new AuthException(ERROR_TIPS);
        }

        return true;
    }
}
