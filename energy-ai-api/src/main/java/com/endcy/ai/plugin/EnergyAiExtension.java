package com.endcy.ai.plugin;

/**
 * Extension point marker interface.
 *
 * <p>All pluggable extensions (tools / Agent strategies / model providers / data sources / endpoints)
 * implement this interface and are auto-registered with {@link ExtensionRegistry} via Spring {@code @Component}.</p>
 *
 * <p>Uses in-process SPI (much lighter than a separate plugin daemon process).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public interface EnergyAiExtension {

    /**
     * Extension point category.
     */
    enum Category {
        /**
         * Custom tool
         */
        TOOL,
        /**
         * Agent strategy (e.g., new ReAct variant)
         */
        AGENT_STRATEGY,
        /**
         * Model provider
         */
        MODEL,
        /**
         * Data source
         */
        DATASOURCE,
        /**
         * End-to-end HTTP endpoint
         */
        ENDPOINT
    }

    /**
     * Unique extension point identifier.
     */
    String extensionId();

    /**
     * Extension point category.
     */
    Category category();

    /**
     * Human-readable name.
     */
    String displayName();

    /**
     * Version (semver).
     */
    default String version() {
        return "1.0.0";
    }
}
