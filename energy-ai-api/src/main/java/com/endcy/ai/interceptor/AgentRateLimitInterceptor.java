package com.endcy.ai.interceptor;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 用户级请求限流拦截器 —— 基于 Redis 滑动窗口。
 *
 * <p>通过 Apollo 配置：
 * <ul>
 *   <li>{@code ai.ratelimit.enabled} — 开关（默认 false）</li>
 *   <li>{@code ai.ratelimit.qps} — 每秒最大请求数（默认 10）</li>
 * </ul>
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ai.ratelimit.enabled:false}")
    private boolean enabled;

    @Value("${ai.ratelimit.qps:10}")
    private int maxQps;

    private static final String RATE_LIMIT_PREFIX = "ai:ratelimit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }

        String clientId = getClientId(request);
        String key = RATE_LIMIT_PREFIX + clientId;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(1));
        }

        if (count != null && count > maxQps) {
            log.warn("限流拦截: clientId={}, count={}, max={}", clientId, count, maxQps);
            response.setStatus(429);
            return false;
        }

        return true;
    }

    private String getClientId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(auth)) {
            return auth;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xff)) {
            return xff.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
