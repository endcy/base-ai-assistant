package com.endcy.ai.prompt;

import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt template service: unified loading of prompt templates, supporting Apollo
 * hot-reload and classpath fallback.
 *
 * <p><b>Loading priority</b> (when {@code ai.prompt.external.enabled=true}):</p>
 * <ol>
 *   <li>Apollo config {@code ai.prompt.<fileName>} (hot-reload, no restart needed)</li>
 *   <li>classpath:{@code /prompts/<fileName>.st}</li>
 *   <li>{@link PromptTemplateKey#getConstantFallback()} built-in constant fallback</li>
 * </ol>
 *
 * <p>When {@code ai.prompt.external.enabled=false} (default), directly returns built-in constants,
 * behavior fully identical to the historical approach. This is a safety switch:
 * if externalization fails, setting it to false provides instant rollback.</p>
 *
 * <p><b>Usage</b>:</p>
 * <pre>
 *   // %s template: use String.format
 *   String prompt = String.format(promptTemplateService.getTemplate(PromptTemplateKey.INTENT_SIMPLE), tips, question);
 *
 *   // {var} template: wrap with Spring AI PromptTemplate
 *   String rendered = new PromptTemplate(promptTemplateService.getTemplate(PromptTemplateKey.RAG_DEFAULT))
 *                       .render(Map.of("query", q, "question_answer_context", ctx));
 * </pre>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Service
public class PromptTemplateService {

    private static final String CLASSPATH_DIR = "prompts/";
    private static final String FILE_EXT = ".st";
    private static final String APOLLO_KEY_PREFIX = "ai.prompt.";
    private static final String MISSING_SENTINEL = "__PROMPT_MISSING__";

    /**
     * Externalization switch. Default false (uses built-in constants, zero regression).
     */
    @Value("${ai.prompt.external.enabled:false}")
    private boolean externalEnabled;

    @Value("${apollo.bootstrap.namespaces:ai-common}")
    private String apolloNamespaces;

    /**
     * classpath file content cache (files are immutable, load once; MISSING_SENTINEL marks not-found)
     */
    private final Map<String, String> classpathCache = new ConcurrentHashMap<>();

    /**
     * Get prompt template content (raw string; callers handle String.format or PromptTemplate themselves).
     */
    public String getTemplate(PromptTemplateKey key) {
        if (key == null) {
            throw new IllegalArgumentException("PromptTemplateKey must not be null");
        }
        if (!externalEnabled) {
            return key.getConstantFallback();
        }
        try {
            // 1. Apollo hot-reload override
            String apolloValue = getFromApollo(key.getFileName());
            if (StrUtil.isNotBlank(apolloValue)) {
                return apolloValue;
            }
            // 2. classpath .st file
            String fileValue = loadFromClasspathCached(key.getFileName());
            if (fileValue != null) {
                return fileValue;
            }
        } catch (Exception e) {
            log.warn("加载 prompt 模板 '{}' 失败，回退到内置常量: {}", key.getFileName(), e.getMessage());
        }
        // 3. Built-in constant fallback
        return key.getConstantFallback();
    }

    /**
     * Whether externalization is currently enabled (for upper-layer checks / testing).
     */
    public boolean isExternalEnabled() {
        return externalEnabled;
    }

    private String getFromApollo(String fileName) {
        if (StrUtil.isBlank(apolloNamespaces)) {
            return null;
        }
        for (String ns : apolloNamespaces.split(",")) {
            try {
                Config config = ConfigService.getConfig(ns.trim());
                String value = config.getProperty(APOLLO_KEY_PREFIX + fileName, null);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            } catch (Exception e) {
                // This namespace is unavailable, try the next one
            }
        }
        return null;
    }

    private String loadFromClasspathCached(String fileName) {
        String cached = classpathCache.get(fileName);
        if (cached != null) {
            return MISSING_SENTINEL.equals(cached) ? null : cached;
        }
        String loaded = loadFromClasspath(fileName);
        String existing = classpathCache.putIfAbsent(fileName, loaded != null ? loaded : MISSING_SENTINEL);
        if (existing != null) {
            return MISSING_SENTINEL.equals(existing) ? null : existing;
        }
        return loaded;
    }

    private String loadFromClasspath(String fileName) {
        try (InputStream is = new ClassPathResource(CLASSPATH_DIR + fileName + FILE_EXT).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("classpath 未找到 prompt 模板: {}{}", CLASSPATH_DIR, fileName);
            return null;
        }
    }
}
