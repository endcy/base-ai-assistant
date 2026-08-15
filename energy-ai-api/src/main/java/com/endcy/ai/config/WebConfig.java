package com.endcy.ai.config;

import cn.hutool.core.util.BooleanUtil;
import com.endcy.ai.interceptor.AgentRateLimitInterceptor;
import com.endcy.ai.interceptor.SimpleAuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

/**
 * WebMvc 配置。
 *
 * @author endcy
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${api.cors.allowOrigin:false}")
    private Boolean allowOrigin;

    @Resource
    private SimpleAuthInterceptor authInterceptor;

    @Resource
    private AgentRateLimitInterceptor rateLimitInterceptor;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        if (BooleanUtil.isTrue(allowOrigin)) {
            config.setAllowedOriginPatterns(Collections.singletonList("*"));
            config.addAllowedHeader(CorsConfiguration.ALL);
            config.addAllowedMethod(CorsConfiguration.ALL);
            config.setAllowCredentials(true);
            source.registerCorsConfiguration("/**", config);
            source.registerCorsConfiguration("/upload/**", config);
        }
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/test/**");
        registry.addInterceptor(this.rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

}
