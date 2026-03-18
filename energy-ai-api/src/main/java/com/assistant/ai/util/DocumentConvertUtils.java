package com.assistant.ai.util;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;

import java.util.List;
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

}
