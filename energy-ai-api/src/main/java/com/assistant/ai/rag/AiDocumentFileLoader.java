package com.assistant.ai.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.service.common.constant.FileConstant;
import com.assistant.service.domain.enums.DocSourceTypeEnum;
import com.assistant.service.domain.enums.KnowledgeScopeTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 智慧能源AI助手文档加载器
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class AiDocumentFileLoader {

    private final ResourcePatternResolver resourcePatternResolver;
    private final ChatRagProperties chatRagProperties;

    private static void addLocalRagDocument(List<Document> allDocuments, Resource[] resources, String docInfoType) {
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (StrUtil.isEmpty(filename)) {
                continue;
            }
            String title = StrUtil.subBefore(filename, ".", true);
            String scopeType = KnowledgeScopeTypeEnum.DEVELOPER_REFERENCE.getDesc();
            String sourceType = DocSourceTypeEnum.FILE.getChannel();
            long combinedId = generateDocId(docInfoType, resource, scopeType, sourceType, title);
            //文档和数据库知识内容 构造相似元数据
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                                                                              .withHorizontalRuleCreateDocument(true)
                                                                              .withIncludeCodeBlock(false)
                                                                              .withIncludeBlockquote(false)
                                                                              .withAdditionalMetadata("id", combinedId)
                                                                              .withAdditionalMetadata("scopeType", scopeType)
                                                                              .withAdditionalMetadata("businessType", docInfoType)
                                                                              .withAdditionalMetadata("title", title)
                                                                              .withAdditionalMetadata("sourceType", sourceType)
                                                                              .withAdditionalMetadata("sourcePath", filename)
                                                                              .build();
            MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
            List<Document> documents = markdownDocumentReader.get();
            if (CollUtil.isEmpty(documents)) {
                continue;
            }
            allDocuments.addAll(documents);
        }
    }

    private static long generateDocId(String docInfoType, Resource resource, String scopeType, String sourceType, String title) {
        long hash = (scopeType + docInfoType + sourceType + title).hashCode();
        long combinedId = 0;
        try {
            long timestamp = resource.getFile().lastModified();
            // 将时间戳左移32位（保留高32位），哈希值放在低32位
            combinedId = (timestamp << 32) | (hash & 0xFFFFFFFFL);
        } catch (IOException e) {
            // 拿不到id就取hash也不是不行
            combinedId = hash;
        }
        return combinedId;
    }

    /**
     * 加载多篇 Markdown 文档
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        if (StrUtil.isNotBlank(chatRagProperties.getResourceDocumentPath())) {
            List<String> docTypes = getResourceSubDirectories(chatRagProperties.getResourceDocumentPath());
            for (String docInfoType : docTypes) {
                try {
                    String path = FileConstant.CLASS_RESOURCES_PATH_PREFIX + chatRagProperties.getResourceDocumentPath() + "/" + docInfoType + "/*.md";
                    Resource[] resources = resourcePatternResolver.getResources(path);
                    addLocalRagDocument(allDocuments, resources, docInfoType);
                } catch (IOException e) {
                    log.error("Markdown Doc load failed", e);
                }
            }
        }
        if (StrUtil.isNotBlank(chatRagProperties.getLocalDocumentPaths())) {
            String[] paths = chatRagProperties.getLocalDocumentPaths().split(",");
            for (String path : paths) {
                //定位到文档分类的父级目录 开始加载分类
                File[] subFiles = FileUtil.ls(path);
                if (ArrayUtil.isEmpty(subFiles)) {
                    continue;
                }
                for (File subFile : subFiles) {
                    if (!FileUtil.isDirectory(subFile)) {
                        continue;
                    }
                    String docInfoType = FileUtil.getName(subFile);
                    File[] docs = subFile.listFiles();
                    if (ArrayUtil.isEmpty(docs)) {
                        continue;
                    }
                    Resource[] resources = new Resource[docs.length];
                    for (int i = 0; i < docs.length; i++) {
                        File fullPath = docs[i];
                        resources[i] = new PathResource(fullPath.toPath());
                    }
                    try {
                        addLocalRagDocument(allDocuments, resources, docInfoType);
                    } catch (Exception e) {
                        log.error("Markdown documents load failed", e);
                    }
                }
            }
        }
        // 根据相同id 文档去重
        // 保留第一个出现的文档
        return new ArrayList<>(allDocuments.stream()
                                           .collect(Collectors.toMap(
                                                   doc -> doc.getMetadata().get("id"),
                                                   Function.identity(),
                                                   (existing, value) -> existing
                                           ))
                                           .values());
    }

    public List<String> getResourceSubDirectories(String resourcePath) {
        String baseDir = resourcePath;
        List<String> directories = new ArrayList<>();

        // 处理classpath路径
        if (!baseDir.startsWith(FileConstant.CLASS_RESOURCES_PATH_PREFIX)) {
            baseDir = FileConstant.CLASS_RESOURCES_PATH_PREFIX + baseDir;
        }

        // 确保路径以/结尾
        if (!baseDir.endsWith("/")) {
            baseDir += "/";
        }

        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(baseDir + "*");

            for (Resource res : resources) {
                String uri = res.getURI().toString();
                if (!uri.endsWith("/")) {
                    continue;
                }
                //移除末尾的/
                uri = uri.substring(0, uri.length() - 1);
                //取剩余字符串 最后一个/之后的名称
                String name = uri.substring(uri.lastIndexOf("/") + 1);
                if (!directories.contains(name)) {
                    directories.add(name);
                }
            }
        } catch (Exception e) {
            log.error("Error while reading directory, {}", e.getMessage());
        }

        return directories;
    }
}
