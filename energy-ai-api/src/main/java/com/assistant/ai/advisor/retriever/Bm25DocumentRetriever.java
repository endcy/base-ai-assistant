package com.assistant.ai.advisor.retriever;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.ai.repository.service.VectorStoreService;
import com.assistant.ai.util.DocumentConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;

/**
 * 使用 BM25 关键词 + 元数据检索获取关联文档
 *
 * @author endcy
 * @date 2025/12/4 14:16:44
 */
@Slf4j
public class Bm25DocumentRetriever extends BaseDocumentRetriever {

    private final VectorStoreService vectorStoreService;


    public Bm25DocumentRetriever(VectorStoreService vectorStoreService,
                                 ChatRagProperties chatRagProperties,
                                 DocumentQueryContext documentQueryContext) {
        super(documentQueryContext, chatRagProperties);
        this.vectorStoreService = vectorStoreService;
    }

    @NotNull
    @Override
    public List<Document> retrieve(@NotNull Query query) {
        String question = documentQueryContext.getReReadingQuestion();
        if (StrUtil.isBlank(question) || StrUtil.isBlank(documentQueryContext.getOriginalQuestion())) {
            return List.of();
        }

        // bm25 检索
        List<VectorDocument> bm25Documents = vectorStoreService.retrieveWithTsQuery(documentQueryContext,
                chatRagProperties.getBm25TopK(), chatRagProperties.getBm25SimilarityThreshold());
        return DocumentConvertUtils.vectorConvertDocument(bm25Documents);
    }

}
