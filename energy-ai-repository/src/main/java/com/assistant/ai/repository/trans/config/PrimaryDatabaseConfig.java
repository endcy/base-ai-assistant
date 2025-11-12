package com.assistant.ai.repository.trans.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.assistant.service.common.config.CustomizedSqlInjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

/**
 * mysql主数据库
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@MapperScan(basePackages = {"com.assistant.ai.repository.trans.mapper", "com.assistant.service.common.**.mapper"})
public class PrimaryDatabaseConfig {

    private final MybatisPlusProperties mybatisPlusProperties;
    private final GlobalConfig globalConfig;
    private final CustomizedSqlInjector customizedSqlInjector;
    private final List<MybatisPlusInterceptor> interceptorList;
    private final List<MetaObjectHandler> metaObjectHandlerList;

    @Bean("dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource dataSource() {
        return new DruidDataSource();
    }

    /**
     * mybatis-plus使用多数据源改造原有的自动配置是十分痛苦的，这里不要随便变动
     */
    @Bean("sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlServerSqlSessionFactroy(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        // 指定 Mapper 映射文件的位置
        sqlSessionFactory.setMapperLocations(mybatisPlusProperties.resolveMapperLocations());
        sqlSessionFactory.setTypeAliasesPackage(mybatisPlusProperties.getTypeAliasesPackage());
        sqlSessionFactory.setConfiguration(convertConfiguration(mybatisPlusProperties));
        sqlSessionFactory.setTypeHandlersPackage(mybatisPlusProperties.getTypeHandlersPackage());
        globalConfig.setBanner(false);
        if (CollUtil.isNotEmpty(metaObjectHandlerList)) {
            globalConfig.setMetaObjectHandler(metaObjectHandlerList.getFirst());
        }
        globalConfig.setSqlInjector(customizedSqlInjector);
        sqlSessionFactory.setGlobalConfig(globalConfig);
        // 将 MyBatis-Plus 相关配置项添加到 MyBatis 插件列表中
        sqlSessionFactory.setPlugins(interceptorList.toArray(new Interceptor[0]));
        return sqlSessionFactory.getObject();
    }

    private MybatisConfiguration convertConfiguration(MybatisPlusProperties properties) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        properties.getConfiguration().applyTo(configuration);
        return configuration;
    }

    @Bean("sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlServerSqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
