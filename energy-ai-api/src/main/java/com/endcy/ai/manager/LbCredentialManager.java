package com.endcy.ai.manager;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * DashScope credential cooldown manager.
 *
 * <p>当前支持单凭证 + Redis 冷却跟踪。多凭证轮转后续通过统一执行引擎实现。</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LbCredentialManager {

    private static final String COOLDOWN_PREFIX = "ai:dashscope:cooldown:";
    private static final long COOLDOWN_RATE_LIMIT_SECONDS = 60;
    private static final long COOLDOWN_AUTH_ERROR_SECONDS = 10;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ai.dashscope.credentials.primary:}")
    private String primaryApiKey;

    public boolean hasCredentials() {
        return StrUtil.isNotBlank(primaryApiKey);
    }

    public String getPrimaryApiKey() {
        return primaryApiKey;
    }

    public void markRateLimited() {
        cooldown("primary", COOLDOWN_RATE_LIMIT_SECONDS, "RateLimit");
    }

    public void markAuthError() {
        cooldown("primary", COOLDOWN_AUTH_ERROR_SECONDS, "AuthError");
    }

    public boolean isInCooldown() {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey("primary")));
    }

    public long getCooldownRemainingSeconds() {
        Long ttl = stringRedisTemplate.getExpire(cooldownKey("primary"), TimeUnit.SECONDS);
        return ttl != null ? ttl : -2L;
    }

    public void clearCooldown() {
        stringRedisTemplate.delete(cooldownKey("primary"));
        log.info("手动清除 DashScope 凭证冷却");
    }

    private void cooldown(String label, long seconds, String reason) {
        if (StrUtil.isBlank(primaryApiKey)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(cooldownKey(label), reason, Duration.ofSeconds(seconds));
        log.warn("DashScope 凭证进入冷却 ({}s, reason={}): {}", seconds, reason, mask(primaryApiKey));
    }

    private static String cooldownKey(String label) {
        return COOLDOWN_PREFIX + label;
    }

    private static String mask(String key) {
        if (StrUtil.isBlank(key) || key.length() <= 7) {
            return "***";
        }
        return key.substring(0, 3) + "..." + key.substring(key.length() - 4);
    }
}
