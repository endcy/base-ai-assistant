package com.assistant.service.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 全局请求字符集配置
 * spring.main.allow-bean-definition-overriding=true
 */
@Configuration
//@RequiredArgsConstructor
public class WebEncodingConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceEncoding(true);
        FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 清除默认的StringHttpMessageConverter以避免字符编码问题
        converters.removeIf(converter -> converter instanceof StringHttpMessageConverter);

        // 添加UTF-8编码的StringHttpMessageConverter
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.addFirst(stringConverter);

        // 确保有JSON消息转换器 否则post失败
        boolean hasJsonConverter = converters.stream()
                                             .anyMatch(converter -> converter instanceof MappingJackson2HttpMessageConverter);

        if (!hasJsonConverter) {
            converters.add(new MappingJackson2HttpMessageConverter());
        }
    }
}
