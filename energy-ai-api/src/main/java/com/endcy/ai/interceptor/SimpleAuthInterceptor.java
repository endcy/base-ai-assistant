package com.endcy.ai.interceptor;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.rpc.constant.RpcConfigConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单鉴权拦截器。
 * 通过请求头中的 Authorization token 进行接口鉴权。
 * SSE (EventSource) 无法携带自定义 header，降级从 query 参数 token 取。
 *
 * @author endcy
 * @since 2026/06/11 10:00:00
 */
@Slf4j
@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {

    @Value("${ai.service.client.access-token:}")
    private String SIMPLE_AUTH_KEY;

    private static final String ERROR_TIPS = "Authorization not valid!";

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws AuthException {
        String token = StrUtil.trimToEmpty(request.getHeader(RpcConfigConstant.AUTH_TOKEN));
        // SSE (EventSource) 无法携带自定义 header，降级从 query 参数 token 取
        if (StrUtil.isBlank(token)) {
            token = StrUtil.trimToEmpty(request.getParameter("token"));
        }
        if (log.isDebugEnabled()) {
            log.debug("--- SimpleAuthInterceptor token[{}] ---", maskToken(token));
        }
        if (StrUtil.isBlank(token) || !SIMPLE_AUTH_KEY.equals(token)) {
            throw new AuthException(ERROR_TIPS);
        }

        return true;
    }

    /**
     * 对 token 做掩码处理，避免敏感信息明文落入日志。
     */
    private String maskToken(String token) {
        if (StrUtil.isBlank(token) || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
