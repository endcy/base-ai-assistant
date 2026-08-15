package com.endcy.ai.plugin;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extension point registry.
 *
 * <p>Auto-collects all {@link EnergyAiExtension} Spring beans at startup, grouped by
 * {@link EnergyAiExtension.Category}. Provides list / get / enable / disable capabilities
 * (runtime dynamic control).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class ExtensionRegistry {

    private final Map<EnergyAiExtension.Category, List<EnergyAiExtension>> byCategory = new EnumMap<>(EnergyAiExtension.Category.class);
    private final Map<String, EnergyAiExtension> byId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> enabled = new ConcurrentHashMap<>();

    @Autowired
    public ExtensionRegistry(List<EnergyAiExtension> extensions) {
        if (CollUtil.isNotEmpty(extensions)) {
            for (EnergyAiExtension ext : extensions) {
                register(ext);
            }
            log.info("ExtensionRegistry 加载 {} 个扩展点", extensions.size());
            logInventory();
        } else {
            log.info("ExtensionRegistry 无扩展点（当前为空，后续按需添加）");
        }
    }

    public void register(EnergyAiExtension ext) {
        byId.put(ext.extensionId(), ext);
        enabled.put(ext.extensionId(), Boolean.TRUE);
        byCategory.computeIfAbsent(ext.category(), k -> new ArrayList<>()).add(ext);
    }

    public EnergyAiExtension get(String extensionId) {
        return byId.get(extensionId);
    }

    public List<EnergyAiExtension> listByCategory(EnergyAiExtension.Category category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public boolean isEnabled(String extensionId) {
        return enabled.getOrDefault(extensionId, Boolean.FALSE);
    }

    public void setEnabled(String extensionId, boolean enabled) {
        this.enabled.put(extensionId, enabled);
        log.info("扩展点 [{}] → {}", extensionId, enabled ? "启用" : "禁用");
    }

    public int size() {
        return byId.size();
    }

    public void logInventory() {
        log.info("===== Extension Registry ({} extensions) =====", byId.size());
        for (Map.Entry<EnergyAiExtension.Category, List<EnergyAiExtension>> e : byCategory.entrySet()) {
            log.info("  [{}]", e.getKey());
            for (EnergyAiExtension ext : e.getValue()) {
                log.info("    {} v{} : {}", ext.extensionId(), ext.version(), ext.displayName());
            }
        }
    }
}
