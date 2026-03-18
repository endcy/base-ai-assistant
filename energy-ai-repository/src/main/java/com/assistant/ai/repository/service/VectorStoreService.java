package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.query.VectorDocumentQueryParam;
import com.assistant.ai.repository.domain.vector.VectorDocument;

import java.util.List;
import java.util.Set;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 20:55:57
 */
public interface VectorStoreService {

    /**
     * 查询所有数据不分页
     */
    List<VectorDocument> queryAll(VectorDocumentQueryParam query);

    VectorDocument getById(String id);

    int insert(VectorDocument res);

    int removeByDocIds(Set<Long> ids);

    void batchInsert(List<VectorDocument> knowledgeDocs);

    boolean isExistsInVector(Long docId);

    /**
     * 使用 BM25 全文检索 + 向量相似度检索获取关联文档
     *
     * @param documentParams      文档查询上下文
     * @param topK                返回结果数量
     * @param similarityThreshold 相似度阈值
     * @return 关联文档列表
     */
    List<VectorDocument> retrieveWithTsQuery(DocumentQueryContext documentParams, int topK, double similarityThreshold);
}
