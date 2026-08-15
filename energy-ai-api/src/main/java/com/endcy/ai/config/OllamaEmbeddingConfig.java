package com.endcy.ai.config;

import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Ollama 本地 Embedding 降级配置。
 *
 * <p>当 DashScope 服务不可用时，自动 fallback 到本地 Ollama 模型。</p>
 *
 * <p>配置项（Apollo ai-common namespace 或 application.properties）:</p>
 * <pre>
 * ai.rag.enable-ollama-embedding=true
 * ai.rag.ollama-base-url=http://localhost:11434
 * ai.rag.ollama-embedding-model=nomic-embed-text
 * </pre>
 *
 * @author endcy
 * @since 2026/08/10
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OllamaEmbeddingConfig {

    private final ChatRagProperties chatRagProperties;

    /**
     * Ollama embedding 模型（仅 enableOllamaEmbedding=true 时创建）。
     */
    @Bean("ollamaEmbeddingModel")
    @ConditionalOnProperty(name = "ai.rag.enable-ollama-embedding", havingValue = "true")
    public OllamaEmbeddingModel ollamaEmbeddingModel() {
        String baseUrl = chatRagProperties.getOllamaBaseUrl();
        String model = chatRagProperties.getOllamaEmbeddingModel();
        log.info("OllamaEmbeddingModel 初始化: baseUrl={}, model={}", baseUrl, model);

        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
        return OllamaEmbeddingModel.builder()
                                   .ollamaApi(ollamaApi)
                                   .defaultOptions(OllamaEmbeddingOptions.builder().model(model).build())
                                   .build();
    }

    /**
     * Fallback EmbeddingModel -- @Primary。
     * <p>容器内只存在这一个 @Primary EmbeddingModel。
     * AiVectorStoreConfig / DirectTextSimilarityService 的 EmbeddingModel 参数自动注入此 Bean。</p>
     */
    @Bean
    @Primary
    public EmbeddingModel fallbackEmbeddingModel(
            DashScopeEmbeddingModel dashscopeEmbeddingModel,
            @Autowired(required = false) @Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {

        if (ollamaEmbeddingModel == null) {
            log.info("EmbeddingModel: DashScope (Ollama 未启用)");
            return dashscopeEmbeddingModel;
        }
        log.info("EmbeddingModel: DashScope + Ollama fallback");
        return new FallbackEmbeddingModel(dashscopeEmbeddingModel, ollamaEmbeddingModel);
    }

    static class FallbackEmbeddingModel implements EmbeddingModel {

        private final EmbeddingModel primary;
        private final EmbeddingModel fallback;

        FallbackEmbeddingModel(EmbeddingModel primary, EmbeddingModel fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            try {
                return primary.embed(document);
            } catch (Exception e) {
                log.warn("DashScope Document embedding 失败，降级到 Ollama: {}", e.getMessage());
                return fallback.embed(document);
            }
        }

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse
        call(org.springframework.ai.embedding.EmbeddingRequest request) {
            try {
                return primary.call(request);
            } catch (Exception e) {
                log.warn("DashScope EmbeddingRequest 失败，降级到 Ollama: {}", e.getMessage());
                return fallback.call(request);
            }
        }

        @Override
        public float[] embed(String text) {
            try {
                return primary.embed(text);
            } catch (Exception e) {
                log.warn("DashScope embedding 失败，降级到 Ollama: {}", e.getMessage());
                try {
                    return fallback.embed(text);
                } catch (Exception ex) {
                    log.error("Ollama embedding 也失败: {}", ex.getMessage());
                    throw new RuntimeException("Embedding 降级链全部失败", ex);
                }
            }
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            try {
                return primary.embed(texts);
            } catch (Exception e) {
                log.warn("DashScope batch embedding 失败 (N={})，降级到 Ollama: {}",
                        texts.size(), e.getMessage());
                try {
                    return fallback.embed(texts);
                } catch (Exception ex) {
                    log.error("Ollama batch embedding 也失败: {}", ex.getMessage());
                    throw new RuntimeException("Batch embedding 降级链全部失败", ex);
                }
            }
        }

        @Override
        public int dimensions() {
            try {
                return primary.dimensions();
            } catch (Exception e) {
                log.warn("DashScope dimensions() 失败，降级到 Ollama");
                return fallback.dimensions();
            }
        }
    }
}
