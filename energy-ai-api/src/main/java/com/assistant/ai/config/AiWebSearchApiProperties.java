package com.assistant.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 在线搜索api配置
 *
 * @author endcy
 * @date 2025/10/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.search-api")
public class AiWebSearchApiProperties {

//    @Value("${ai.search-api.tencent-secret-id}")

//    @Value("${ai.search-api.tencent-secret-key}")

    private String simpleKey = "";

    private String tencentApiSecretSk = "";

    private String tencentApiSecretId = "";

    private String tencentApiSecretKey = "";

}
