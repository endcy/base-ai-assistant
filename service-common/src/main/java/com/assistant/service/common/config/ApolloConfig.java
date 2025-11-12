package com.assistant.service.common.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 热更新apollo配置，监听apollo中{apollo.bootstrap.namespaces}配置的namespaces
 * ConditionalOnClass，ConditionalOnProperty满足后进行加载
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Slf4j
//@Configuration
public class ApolloConfig implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private RefreshScope refreshScope;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 监听Apollo配置更新事件实现热加载
     * 默认监听application namespace中的配置变化，如有其他namespace直接往后加
     */
//    @ApolloConfigChangeListener
//    public void apolloRefresh(ConfigChangeEvent changeEvent) {
//        log.info(">>>>>>>>>> apollo properties has changed! changeEvent:[{}] <<<<<<<<<<", JSON.toJSONString(changeEvent));
//        applicationContext.publishEvent(new EnvironmentChangeEvent(changeEvent.changedKeys()));
//        refreshScope.refreshAll();
//    }

}
