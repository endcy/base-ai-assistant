package com.endcy.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web search API configuration.
 *
 * @author endcy
 * @date 2025/10/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.search-api")
public class AiWebSearchApiProperties {

    private String simpleKey = "";

    private String tencentApiSecretSk = "";

    private String tencentApiSecretId = "";

    private String tencentApiSecretKey = "";

    /**
     * Reverse geocoding service URL template.
     * Must contain two %s placeholders for latitude and longitude.
     * Example: http://localhost:8080/gis/reverse-geocoder?latitude=%s&amp;longitude=%s
     */
    private String gisGeoCoderUrl = "";

}
