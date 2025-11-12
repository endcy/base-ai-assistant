package com.assistant.ai.config;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

/**
 * WebMvcConfigurer
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${api.cors.allowOrigin:false}")
    private Boolean allowOrigin;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        if (BooleanUtil.isTrue(allowOrigin)) {
            //1,允许任何来源
            config.setAllowedOriginPatterns(Collections.singletonList("*"));
            //2,允许任何请求头
            config.addAllowedHeader(CorsConfiguration.ALL);
            //3,允许任何方法
            config.addAllowedMethod(CorsConfiguration.ALL);
            //4,允许凭证
            config.setAllowCredentials(true);
            source.registerCorsConfiguration("/**", config);
            source.registerCorsConfiguration("/upload/**", config);
        }
        return new CorsFilter(source);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保静态资源可以被访问
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 设置默认页面（可选）
        registry.addViewController("/").setViewName("redirect:/knowledge-document.html");
    }

}
