package com.assistant.ai.repository.service;

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
}
