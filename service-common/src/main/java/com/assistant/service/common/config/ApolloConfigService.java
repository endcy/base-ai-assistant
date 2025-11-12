package com.assistant.service.common.config;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApolloConfigService {

    /**
     * 从 Apollo 获取开关配置
     *
     * @param key 配置键
     * @return 开关状态（默认返回 false）
     */
    public boolean getConfig(String key) {
        try {
            // 获取默认命名空间的配置
            Config config = ConfigService.getAppConfig();
            // 从 Apollo 获取配置值，默认返回 false
            return config.getBooleanProperty(key, false);
        } catch (Exception e) {
            log.warn("Failed to get config from Apollo, key:{}, msg:{}", key, e.getMessage());
            return false;
        }
    }

}
