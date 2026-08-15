package com.endcy.ai.prompt;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt version management service.
 *
 * <p>Maintains version number + content hash for each prompt template, supporting canary releases.</p>
 *
 * <p>Currently in-memory ({@code ConcurrentHashMap}); production deployment connects to an
 * {@code ai_prompt_version} table.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private final PromptTemplateService promptTemplateService;

    /**
     * Template name → Version record
     */
    private final Map<String, VersionRecord> versions = new ConcurrentHashMap<>();

    /**
     * Canary percentage (0-100, 0=use all old versions, 100=use all new versions)
     */
    @Value("${ai.prompt.canary.percent:100}")
    private int canaryPercent;

    /**
     * Register a new version.
     *
     * @param templateName template name (corresponds to PromptTemplateKey.fileName)
     * @param content      template content
     * @return version number
     */
    public int registerVersion(String templateName, String content) {
        String hash = DigestUtil.sha256Hex(content);
        VersionRecord existing = versions.get(templateName);
        int newVersion = (existing == null) ? 1 : existing.version() + 1;
        VersionRecord record = new VersionRecord(newVersion, content, hash, System.currentTimeMillis());
        versions.put(templateName, record);
        log.info("Prompt [{}] 注册新版本 v{} (hash={})", templateName, newVersion, hash.substring(0, 8));
        return newVersion;
    }

    /**
     * Get current version content.
     */
    public String getContent(String templateName) {
        VersionRecord record = versions.get(templateName);
        if (record != null) {
            return record.content();
        }
        // Fallback: get from PromptTemplateService (classpath .st or Apollo)
        return promptTemplateService.getTemplate(PromptTemplateKey.valueOf(templateName.toUpperCase().replace("-", "_")));
    }

    /**
     * Canary check: whether the current request should use the new version (hash-based bucketing).
     *
     * @param bucketKey bucket key (e.g., userId or chatId)
     */
    public boolean shouldUseCanary(String bucketKey) {
        if (canaryPercent >= 100)
            return true;
        if (canaryPercent <= 0)
            return false;
        int bucket = Math.floorMod(bucketKey.hashCode(), 100);
        return bucket < canaryPercent;
    }

    /**
     * Get version records (for admin dashboard display).
     */
    public Map<String, VersionRecord> listVersions() {
        return Map.copyOf(versions);
    }

    /**
     * Rollback to a specified version.
     */
    public boolean rollback(String templateName, int targetVersion) {
        // Current in-memory state only keeps the latest version; rollback requires DB history.
        log.warn("回滚 prompt [{}] → v{}（需 DB 历史支持，当前内存态仅保留最新版）", templateName, targetVersion);
        return false;
    }

    // ==================== Version record ====================

    public record VersionRecord(int version, String content, String hash, long registeredAt) {
    }
}
