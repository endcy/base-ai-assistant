package com.assistant.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 智慧能源AI助手向量数据库配置
 * 初始化基于内存的向量数据库 Bean
 * 初始化基于pg库的向量数据库 Bean
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class AiVectorStoreConfig {

    private final JdbcTemplate pgSqlTemplate;
    private final EmbeddingModel dashscopeEmbeddingModel;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:1204}")
    private Integer dimensions;

    @Bean("localVectorStore")
    public SimpleVectorStore localVectorStore() {
        return SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
    }

    @Bean("pgVectorVectorStore")
    @Primary
    public PgVectorStore pgVectorVectorStore() {
        return PgVectorStore.builder(pgSqlTemplate, dashscopeEmbeddingModel)
                            .dimensions(dimensions)
                            .distanceType(COSINE_DISTANCE)
                            .indexType(HNSW)
                            .initializeSchema(true)
                            .vectorTableName("vector_store")
                            .build();
    }

}
