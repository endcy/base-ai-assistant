# Update DocumentImportHelper to Use Database Configuration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hardcoded enum mappings in DocumentImportHelper with dynamic database configuration from ai_knowledge_category_config table.

**Architecture:** Inject KnowledgeCategoryConfigService into DocumentImportHelper, load category configurations at initialization time, and use code/name matching instead of hardcoded enum values. This allows runtime configuration of category mappings without code changes.

**Tech Stack:** Java 21, Spring Boot 3.3.13, MyBatis-Plus 3.5.7, MapStruct

---

### Task 1: Update DocumentImportHelper to use database configuration

**Files:**
- Modify: `energy-ai-repository/src/main/java/com/assistant/ai/repository/service/impl/DocumentImportHelper.java`
- Test: Compile verification with `mvn compile -pl energy-ai-repository`

- [ ] **Step 1: Add KnowledgeCategoryConfigService dependency**

Add the service dependency and remove hardcoded enum imports:

```java
// Remove these imports:
// import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
// import com.assistant.service.domain.enums.KnowledgeScopeTypeEnum;

// Add this import:
import com.assistant.ai.repository.service.KnowledgeCategoryConfigService;
import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;

// In class:
private final KnowledgeCategoryConfigService knowledgeCategoryConfigService;

// Remove static maps:
// private static final Map<String, KnowledgeScopeTypeEnum> SCOPE_TYPE_MAPPING = new HashMap<>();
// private static final Map<String, KnowledgeBusinessTypeEnum> BUSINESS_TYPE_MAPPING = new HashMap<>();

// Add instance maps (loaded from DB):
private Map<String, String> scopeTypeCodeMap = new HashMap<>();  // keyword -> code
private Map<String, String> scopeTypeNameMap = new HashMap<>();  // keyword -> name
private Map<String, String> businessTypeCodeMap = new HashMap<>(); // keyword -> code
private Map<String, String> businessTypeNameMap = new HashMap<>(); // keyword -> name

// Add constructor injection (already has @RequiredArgsConstructor):
// The field will be added as final, constructor will be auto-generated
```

- [ ] **Step 2: Add initialization method to load category configurations**

Add method to load and cache category configurations:

```java
/**
 * 加载分类配置（从数据库）
 */
@PostConstruct
public void initCategoryMappings() {
    loadCategoryMappings();
}

/**
 * 加载分类映射
 */
private void loadCategoryMappings() {
    try {
        // 加载知识领域分类 (type='scope')
        List<KnowledgeCategoryConfigDTO> scopeConfigs = knowledgeCategoryConfigService.getByType("scope");
        for (KnowledgeCategoryConfigDTO config : scopeConfigs) {
            String code = config.getCode();
            String name = config.getName();
            // 使用 code 作为匹配关键字
            scopeTypeCodeMap.put(code, code);
            scopeTypeNameMap.put(name, code);
            // 如果有父级，也添加映射
            if (StrUtil.isNotBlank(config.getParentCode())) {
                scopeTypeCodeMap.put(config.getParentCode(), code);
            }
        }
        log.info("加载知识领域分类配置 {} 条", scopeConfigs.size());

        // 加载业务领域分类 (type='business')
        List<KnowledgeCategoryConfigDTO> businessConfigs = knowledgeCategoryConfigService.getByType("business");
        for (KnowledgeCategoryConfigDTO config : businessConfigs) {
            String code = config.getCode();
            String name = config.getName();
            businessTypeCodeMap.put(code, code);
            businessTypeNameMap.put(name, code);
            if (StrUtil.isNotBlank(config.getParentCode())) {
                businessTypeCodeMap.put(config.getParentCode(), code);
            }
        }
        log.info("加载业务领域分类配置 {} 条", businessConfigs.size());
    } catch (Exception e) {
        log.error("加载分类配置失败，使用默认映射", e);
        // 加载默认映射作为降级方案
        loadDefaultMappings();
    }
}

/**
 * 加载默认映射（降级方案）
 */
private void loadDefaultMappings() {
    // 知识领域默认映射
    scopeTypeCodeMap.put("market", "market_customer_service");
    scopeTypeCodeMap.put("market_customer_service", "market_customer_service");
    scopeTypeCodeMap.put("account", "account_customer_service");
    scopeTypeCodeMap.put("account_customer_service", "account_customer_service");
    scopeTypeCodeMap.put("operator", "operator_customer_service");
    scopeTypeCodeMap.put("operator_customer_service", "operator_customer_service");
    scopeTypeCodeMap.put("operations", "operations_reference");
    scopeTypeCodeMap.put("operations_reference", "operations_reference");
    scopeTypeCodeMap.put("developer", "developer_reference");
    scopeTypeCodeMap.put("developer_reference", "developer_reference");
    scopeTypeNameMap.put("市场客服", "market_customer_service");
    scopeTypeNameMap.put("用户客服", "account_customer_service");
    scopeTypeNameMap.put("商户客服", "operator_customer_service");
    scopeTypeNameMap.put("运营参考", "operations_reference");
    scopeTypeNameMap.put("开发参考", "developer_reference");

    // 业务领域默认映射
    businessTypeCodeMap.put("station", "station");
    businessTypeCodeMap.put("equipment", "equipment");
    businessTypeCodeMap.put("account", "account");
    businessTypeCodeMap.put("charge_order", "charge_order");
    businessTypeCodeMap.put("discharge_order", "discharge_order");
    businessTypeCodeMap.put("alarm", "alarm");
    businessTypeCodeMap.put("norms", "norms");
    businessTypeCodeMap.put("api", "api");
    businessTypeCodeMap.put("production", "production");
    businessTypeCodeMap.put("client_operate", "client_operate");
    businessTypeCodeMap.put("admin_operate", "admin_operate");
    businessTypeCodeMap.put("maintenance", "maintenance");
    businessTypeCodeMap.put("reporter", "reporter");
    businessTypeCodeMap.put("power_predict", "power_predict");
    businessTypeNameMap.put("站点", "station");
    businessTypeNameMap.put("设备", "equipment");
    businessTypeNameMap.put("用户", "account");
    businessTypeNameMap.put("充电订单", "charge_order");
    businessTypeNameMap.put("放电订单", "discharge_order");
    businessTypeNameMap.put("故障", "alarm");
    businessTypeNameMap.put("合作", "norms");
    businessTypeNameMap.put("接口", "api");
    businessTypeNameMap.put("产品", "production");
    businessTypeNameMap.put("用户操作", "client_operate");
    businessTypeNameMap.put("管理", "admin_operate");
    businessTypeNameMap.put("维护", "maintenance");
    businessTypeNameMap.put("分析", "reporter");
    businessTypeNameMap.put("功率预测", "power_predict");
}
```

- [ ] **Step 3: Update matchScopeType method to use database configuration**

Replace the enum-based matching with database-driven matching:

```java
/**
 * 匹配知识领域类型（从数据库配置）
 */
private String matchScopeType(String text) {
    if (StrUtil.isBlank(text)) {
        return null;
    }
    // 尝试从 code 映射匹配
    for (Map.Entry<String, String> entry : scopeTypeCodeMap.entrySet()) {
        if (text.toLowerCase().contains(entry.getKey().toLowerCase())) {
            return entry.getValue();
        }
    }
    // 尝试从 name 映射匹配（中文匹配）
    for (Map.Entry<String, String> entry : scopeTypeNameMap.entrySet()) {
        if (text.contains(entry.getKey())) {
            return entry.getValue();
        }
    }
    return null;
}
```

- [ ] **Step 4: Update matchBusinessType method to use database configuration**

```java
/**
 * 匹配业务类型（从数据库配置）
 */
private String matchBusinessType(String text) {
    if (StrUtil.isBlank(text)) {
        return "unknown";
    }
    // 尝试从 code 映射匹配
    for (Map.Entry<String, String> entry : businessTypeCodeMap.entrySet()) {
        if (text.toLowerCase().contains(entry.getKey().toLowerCase())) {
            return entry.getValue();
        }
    }
    // 尝试从 name 映射匹配（中文匹配）
    for (Map.Entry<String, String> entry : businessTypeNameMap.entrySet()) {
        if (text.contains(entry.getKey())) {
            return entry.getValue();
        }
    }
    return "unknown";
}
```

- [ ] **Step 5: Update inferScopeType method to return String instead of using enum**

```java
/**
 * 从路径推断知识领域类型
 */
private String inferScopeType(String[] pathParts, String defaultScopeType) {
    // 首先尝试使用默认值
    if (StrUtil.isNotBlank(defaultScopeType)) {
        return defaultScopeType;
    }

    // 从路径各层级中推断
    for (String part : pathParts) {
        String scopeType = matchScopeType(part);
        if (scopeType != null) {
            return scopeType;
        }
    }

    // 默认返回"用户客服"
    return "account_customer_service";
}
```

- [ ] **Step 6: Update inferBusinessType method**

```java
/**
 * 从路径推断业务类型
 */
private String inferBusinessType(String[] pathParts) {
    // 从路径各层级中推断
    for (String part : pathParts) {
        String businessType = matchBusinessType(part);
        if (businessType != null && !"unknown".equals(businessType)) {
            return businessType;
        }
    }
    return "unknown";
}
```

- [ ] **Step 7: Remove static initializer blocks**

Remove the static blocks that populated SCOPE_TYPE_MAPPING and BUSINESS_TYPE_MAPPING (lines 46-77).

- [ ] **Step 8: Add @PostConstruct import**

```java
import jakarta.annotation.PostConstruct;
```

- [ ] **Step 9: Compile and verify**

Run: `mvn compile -pl energy-ai-repository -am`

Expected: BUILD SUCCESS with no errors

- [ ] **Step 10: Commit**

```bash
git add energy-ai-repository/src/main/java/com/assistant/ai/repository/service/impl/DocumentImportHelper.java
git commit -m "refactor: use database configuration for category mappings in DocumentImportHelper

- Replace hardcoded enum mappings with dynamic database configuration
- Load category configurations from ai_knowledge_category_config table
- Support runtime configuration changes without code modifications
- Add fallback to default mappings when database is unavailable"
```

---

### Task 2: Add refresh mechanism for category mappings (optional enhancement)

**Files:**
- Modify: `energy-ai-repository/src/main/java/com/assistant/ai/repository/service/impl/DocumentImportHelper.java`

- [ ] **Step 1: Add a manual refresh method**

```java
/**
 * 手动刷新分类映射（用于配置变更后）
 */
public void refreshCategoryMappings() {
    log.info("手动刷新分类映射...");
    loadCategoryMappings();
}
```

- [ ] **Step 2: Add event listener for configuration changes (optional)**

If the project uses Spring events, add:

```java
@EventListener
public void onCategoryConfigChanged(CategoryConfigChangedEvent event) {
    loadCategoryMappings();
}
```

- [ ] **Step 3: Commit**

```bash
git add energy-ai-repository/src/main/java/com/assistant/ai/repository/service/impl/DocumentImportHelper.java
git commit -m "feat: add manual refresh method for category mappings"
```

---

## Review Checklist

After implementation:

1. Verify compilation succeeds: `mvn clean compile -pl energy-ai-repository -am`
2. Verify the module still works with the rest of the project: `mvn compile`
3. Check that the fallback mechanism works when database is unavailable
4. Consider adding unit tests for the matching logic (if testing infrastructure exists)
