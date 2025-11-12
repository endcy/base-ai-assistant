package com.assistant.ai.repository.pgsql.config;

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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

/**
 * 向量数据库
 * 使用pgsql，请安装对应向量插件，pg库不作为业务库，和其他业务系统复用例如其他管理系统，这里pg仅存储向量给AI模型使用
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@MapperScan(basePackages = {"com.assistant.ai.repository.pgsql.mapper"},
        sqlSessionFactoryRef = "pgSqlSessionFactory",
        sqlSessionTemplateRef = "pgSqlSessionTemplate")
public class PgSqlDatabaseConfig {

    private final MybatisPlusProperties mybatisPlusProperties;
    private final GlobalConfig globalConfig;
    private final CustomizedSqlInjector customizedSqlInjector;
    private final List<MybatisPlusInterceptor> interceptorList;
    private final List<MetaObjectHandler> metaObjectHandlerList;

    @Bean(name = "pgSqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.pgsql")
    public DataSource pgSqlDataSource() {
        return new DruidDataSource();
    }

    /**
     * mybatis-plus使用多数据源改造原有的自动配置是十分痛苦的，这里不要随便变动
     */
    @Bean("pgSqlSessionFactory")
    public SqlSessionFactory pgSqlSessionFactory(@Qualifier("pgSqlDataSource") DataSource dataSource) throws Exception {
        globalConfig.setBanner(false);
        globalConfig.setSqlInjector(customizedSqlInjector);
        if (CollUtil.isNotEmpty(metaObjectHandlerList)) {
            globalConfig.setMetaObjectHandler(metaObjectHandlerList.getFirst());
        }
        MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        // 指定 Mapper 映射文件的位置
        sqlSessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:pgsql/mapper/*.xml"));
        sqlSessionFactory.setTypeAliasesPackage(mybatisPlusProperties.getTypeAliasesPackage());
        sqlSessionFactory.setTypeHandlersPackage(mybatisPlusProperties.getTypeHandlersPackage());
        sqlSessionFactory.setConfiguration(convertConfiguration(mybatisPlusProperties));
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

    @Bean("pgSqlTransactionManager")
    public PlatformTransactionManager pgSqlTransactionManager(@Qualifier("pgSqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean("pgSqlSessionTemplate")
    public SqlSessionTemplate pgSqlSessionTemplate(@Qualifier("pgSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean("pgSqlTemplate")
    public JdbcTemplate pgSqlTemplate(@Qualifier("pgSqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
