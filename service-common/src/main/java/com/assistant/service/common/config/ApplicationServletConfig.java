package com.assistant.service.common.config;

import com.assistant.service.common.utils.SpringContextHolder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Configuration
public class ApplicationServletConfig {

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    @Bean
    public ServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory servletWebServerFactory = new TomcatServletWebServerFactory();
        servletWebServerFactory.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "[]{}"));
        return servletWebServerFactory;
    }

}
