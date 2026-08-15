package com.endcy.ai.workflow.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workflow variable pool — core abstraction for inter-node variable passing (inspired by Dify graphon VariablePool).
 *
 * <p>Addressed via selector (nodeId + key tuple), supports typed values.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public class VariablePool {

    /**
     * selector → value. selector format: "nodeId:key"
     */
    private final Map<String, Object> store = new ConcurrentHashMap<>();

    /**
     * Virtual node prefixes (inspired by Dify)
     */
    public static final String SYS = "sys";
    public static final String ENV = "env";

    public void put(String nodeId, String key, Object value) {
        store.put(nodeId + ":" + key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String nodeId, String key) {
        return (T) store.get(nodeId + ":" + key);
    }

    public Object get(String selector) {
        return store.get(selector);
    }

    public boolean contains(String nodeId, String key) {
        return store.containsKey(nodeId + ":" + key);
    }

    /**
     * Get all outputs of a specific node (key prefix match)
     */
    public Map<String, Object> getByNode(String nodeId) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        String prefix = nodeId + ":";
        for (Map.Entry<String, Object> e : store.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                result.put(e.getKey().substring(prefix.length()), e.getValue());
            }
        }
        return result;
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
