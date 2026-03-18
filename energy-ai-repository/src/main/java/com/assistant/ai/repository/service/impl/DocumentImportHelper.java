package com.assistant.ai.repository.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.repository.domain.dto.BatchImportResult;
import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.entity.KnowledgeDocument;
import com.assistant.ai.repository.service.KnowledgeCategoryConfigService;
import com.assistant.ai.repository.service.convert.KnowledgeDocumentConverter;
import com.assistant.ai.repository.trans.mapper.KnowledgeDocumentMapper;
import com.assistant.service.domain.enums.DocSourceTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 文档导入工具类
 * 从文件系统批量导入文档到数据库
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentImportHelper {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentConverter knowledgeDocumentConverter;
    private final KnowledgeCategoryConfigService knowledgeCategoryConfigService;

    // 支持的文档扩展名
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList("md", "markdown", "txt", "text"));

    // 从数据库加载的分类映射（运行时缓存）
    private Map<String, String> scopeTypeCodeMap = new HashMap<>();  // keyword -> code
    private Map<String, String> scopeTypeNameMap = new HashMap<>();  // keyword -> name
    private Map<String, String> businessTypeCodeMap = new HashMap<>(); // keyword -> code
    private Map<String, String> businessTypeNameMap = new HashMap<>(); // keyword -> name

    /**
     * 初始化时加载分类配置
     */
    @PostConstruct
    public void initCategoryMappings() {
        loadCategoryMappings();
    }

    /**
     * 加载分类映射（从数据库）
     */
    private void loadCategoryMappings() {
        try {
            // 加载知识领域分类 (type='scope')
            List<KnowledgeCategoryConfigDTO> scopeConfigs = knowledgeCategoryConfigService.getByType("scope");
            for (KnowledgeCategoryConfigDTO config : scopeConfigs) {
                String code = config.getCode();
                String name = config.getName();
                // 使用 code 作为匹配关键字
                scopeTypeCodeMap.put(code.toLowerCase(), code);
                scopeTypeNameMap.put(name, code);
                // 如果有父级，也添加映射
                if (StrUtil.isNotBlank(config.getParentCode())) {
                    scopeTypeCodeMap.put(config.getParentCode().toLowerCase(), code);
                }
            }
            log.info("加载知识领域分类配置 {} 条", scopeConfigs.size());

            // 加载业务领域分类 (type='business')
            List<KnowledgeCategoryConfigDTO> businessConfigs = knowledgeCategoryConfigService.getByType("business");
            for (KnowledgeCategoryConfigDTO config : businessConfigs) {
                String code = config.getCode();
                String name = config.getName();
                businessTypeCodeMap.put(code.toLowerCase(), code);
                businessTypeNameMap.put(name, code);
                if (StrUtil.isNotBlank(config.getParentCode())) {
                    businessTypeCodeMap.put(config.getParentCode().toLowerCase(), code);
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

    /**
     * 手动刷新分类映射（用于配置变更后）
     */
    public void refreshCategoryMappings() {
        log.info("手动刷新分类映射...");
        loadCategoryMappings();
    }

    /**
     * 从指定目录批量导入文档
     *
     * @param directoryPath    目录路径
     * @param groupId          用户分组 ID（租户 ID）
     * @param defaultScopeType 默认知识领域类型
     * @return 导入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult importFromDirectory(String directoryPath, Long groupId, String defaultScopeType) {
        BatchImportResult result = BatchImportResult.builder()
                                                    .successCount(0)
                                                    .failCount(0)
                                                    .skipCount(0)
                                                    .successFiles(new ArrayList<>())
                                                    .failedFiles(new ArrayList<>())
                                                    .skippedFiles(new ArrayList<>())
                                                    .build();

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            result.addFailed(directoryPath, "目录不存在或不是有效的目录");
            return result;
        }

        // 递归获取所有文件
        List<File> files = getAllFiles(directory);
        log.info("在目录 {} 中找到 {} 个文件", directoryPath, files.size());

        for (File file : files) {
            try {
                // 检查文件扩展名
                String extension = FileUtil.getSuffix(file).toLowerCase();
                if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                    log.debug("跳过不支持的文件类型：{}", file.getAbsolutePath());
                    continue;
                }

                // 解析文件路径，推断元数据
                DocumentMetadata metadata = parseDocumentMetadata(file, directory, defaultScopeType);

                // 构建文档 DTO
                KnowledgeDocumentDTO dto = buildDocumentDTO(file, metadata, groupId);

                // 检查文档是否已存在（根据路径）
                if (isDocumentExists(dto.getSourcePath())) {
                    log.info("文档已存在，跳过：{}", file.getAbsolutePath());
                    result.addSkipped(file.getAbsolutePath());
                    continue;
                }

                // 插入数据库
                KnowledgeDocument entity = knowledgeDocumentConverter.toEntity(dto);
                int inserted = knowledgeDocumentMapper.insert(entity);

                if (inserted > 0) {
                    log.info("成功导入文档：{} -> {} ({})",
                            file.getAbsolutePath(), dto.getTitle(), dto.getScopeType());
                    result.addSuccess(file.getAbsolutePath());
                } else {
                    result.addFailed(file.getAbsolutePath(), "数据库插入返回 0");
                }

            } catch (Exception e) {
                log.error("导入文档失败：{}", file.getAbsolutePath(), e);
                result.addFailed(file.getAbsolutePath(), e.getMessage());
            }
        }

        log.info("批量导入完成：成功={}, 失败={}, 跳过={}",
                result.getSuccessCount(), result.getFailCount(), result.getSkipCount());
        return result;
    }

    /**
     * 递归获取目录下所有文件
     */
    private List<File> getAllFiles(File directory) {
        List<File> files = new ArrayList<>();
        collectFiles(directory, files);
        return files;
    }

    /**
     * 递归收集文件
     */
    private void collectFiles(File file, List<File> files) {
        if (file.isFile()) {
            files.add(file);
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    collectFiles(child, files);
                }
            }
        }
    }

    /**
     * 解析文档元数据（从文件路径推断）
     */
    private DocumentMetadata parseDocumentMetadata(File file, File rootDirectory, String defaultScopeType) {
        DocumentMetadata metadata = new DocumentMetadata();

        // 获取相对路径
        String relativePath = FileUtil.getAbsolutePath(file);
        if (relativePath.startsWith(rootDirectory.getAbsolutePath())) {
            relativePath = relativePath.substring(rootDirectory.getAbsolutePath().length() + 1);
        }
        metadata.setRelativePath(relativePath);

        // 解析路径层级
        String[] pathParts = relativePath.split("[\\\\/]+");

        // 尝试从路径中提取知识领域
        metadata.setScopeType(inferScopeType(pathParts, defaultScopeType));

        // 尝试从路径中提取业务类型
        metadata.setBusinessType(inferBusinessType(pathParts));

        // 使用文件名作为标题（去掉扩展名）
        String fileName = FileUtil.getName(file);
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileName = fileName.substring(0, lastDotIndex);
        }
        metadata.setTitle(cleanFileName(fileName));

        // 设置来源类型
        metadata.setSourceType(DocSourceTypeEnum.FILE.getChannel());

        // 设置来源路径
        metadata.setSourcePath(file.getAbsolutePath());

        return metadata;
    }

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

    /**
     * 匹配知识领域类型（从数据库配置）
     */
    private String matchScopeType(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        // 尝试从 code 映射匹配（英文关键字）
        for (Map.Entry<String, String> entry : scopeTypeCodeMap.entrySet()) {
            if (text.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        // 尝试从 name 映射匹配（中文关键字）
        for (Map.Entry<String, String> entry : scopeTypeNameMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 匹配业务类型（从数据库配置）
     */
    private String matchBusinessType(String text) {
        if (StrUtil.isBlank(text)) {
            return "unknown";
        }
        // 尝试从 code 映射匹配（英文关键字）
        for (Map.Entry<String, String> entry : businessTypeCodeMap.entrySet()) {
            if (text.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        // 尝试从 name 映射匹配（中文关键字）
        for (Map.Entry<String, String> entry : businessTypeNameMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "unknown";
    }

    /**
     * 清理文件名，移除特殊字符和编码问题
     */
    private String cleanFileName(String fileName) {
        // 移除乱码字符（替换常见乱码模式）
        fileName = fileName.replaceAll("[\\ud800-\\udfff]", "");
        fileName = fileName.replaceAll("\\p{C}", "");
        // 移除前后空格
        fileName = fileName.trim();
        // 如果文件名为空，使用默认值
        if (StrUtil.isBlank(fileName)) {
            return "未命名文档";
        }
        return fileName;
    }

    /**
     * 构建文档 DTO
     */
    private KnowledgeDocumentDTO buildDocumentDTO(File file, DocumentMetadata metadata, Long groupId) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();

        dto.setScopeType(metadata.getScopeType());
        dto.setBusinessType(metadata.getBusinessType());
        dto.setTitle(metadata.getTitle());
        dto.setGroupId(groupId);

        // 读取文件内容
        String content = readContent(file);
        dto.setContent(content);

        dto.setSourceType(metadata.getSourceType());
        dto.setSourcePath(metadata.getSourcePath());
        dto.setDocVersion(1);
        dto.setEnablePublic(true);
        dto.setLoaded(false); // 初始未加载到向量库
        dto.setEnabled(true);
        dto.setCreateUser(1L); // 默认创建人
        dto.setUpdateUser(1L);

        return dto;
    }

    /**
     * 读取文件内容
     */
    private String readContent(File file) {
        try {
            return FileUtil.readString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 尝试其他编码
            try {
                return FileUtil.readString(file, Charset.forName("GBK"));
            } catch (Exception ex) {
                log.warn("读取文件内容失败，使用默认内容：{}", file.getAbsolutePath());
                return "文件内容无法读取";
            }
        }
    }

    /**
     * 检查文档是否已存在
     */
    private boolean isDocumentExists(String sourcePath) {
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getSourcePath, sourcePath);
        return knowledgeDocumentMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 文档元数据
     */
    @Data
    private static class DocumentMetadata {
        private String relativePath;
        private String scopeType;
        private String businessType;
        private String title;
        private String sourceType;
        private String sourcePath;
    }
}
