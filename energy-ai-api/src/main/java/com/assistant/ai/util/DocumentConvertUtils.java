package com.assistant.ai.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文档转换工具类
 *
 * @author endcy
 * @date 2025/12/4 15:46:27
 */
@Slf4j
public class DocumentConvertUtils {

    private DocumentConvertUtils() {
    }

    @NotNull
    public static List<Document> vectorConvertDocument(List<VectorDocument> otherDocuments) {
        if (otherDocuments == null) {
            return CollUtil.newArrayList();
        }
        return otherDocuments.stream().map(item ->
                Document.builder()
                        .text(item.getContent())
                        .metadata(JSON.parseObject(item.getMetadata()))
                        .id(item.getId())
                        .score(item.getScore())
                        .build()
        ).collect(Collectors.toList());
    }

    /**
     * 将 Spring AI Document 列表转换为知识库文档匹配项列表
     * 用于 RAG 检索后返回关联文档信息
     *
     * @param relatedDocuments 关联文档列表
     * @return 文档匹配项列表
     */
    @NotNull
    public static List<KnowledgeDocumentMatchItem> documentConvertRelated(List<Document> relatedDocuments) {
        if (CollUtil.isEmpty(relatedDocuments)) {
            return CollUtil.newArrayList();
        }
        List<KnowledgeDocumentMatchItem> list = relatedDocuments.stream()
                                                                .map(document -> {
                                                                    if (MapUtil.isEmpty(document.getMetadata())) {
                                                                        return null;
                                                                    }
                                                                    Object id = document.getMetadata().get("id");
                                                                    return KnowledgeDocumentMatchItem.builder()
                                                                                                     .id(id == null ? 0L : Long.parseLong(id.toString()))
                                                                                                     .title((String) document.getMetadata().get("title"))
                                                                                                     .scopeType((String) document.getMetadata().get("sourceType"))
                                                                                                     .businessType((String) document.getMetadata().get("businessType"))
                                                                                                     .score(document.getScore())
                                                                                                     .build();
                                                                }).collect(Collectors.toList());
        list.removeIf(Objects::isNull);
        //移除id重复的元素
        List<KnowledgeDocumentMatchItem> resultItems = CollUtil.newArrayList();
        for (KnowledgeDocumentMatchItem item : list) {
            boolean isExistId = resultItems.stream().anyMatch(resultItem -> resultItem.getId().equals(item.getId()));
            if (!isExistId) {
                resultItems.add(item);
            }
        }
        return resultItems;
    }

}
