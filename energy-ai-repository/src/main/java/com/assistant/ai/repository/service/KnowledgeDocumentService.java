package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.query.KnowledgeDocumentQueryParam;
import com.assistant.service.common.base.PageInfo;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 20:55:57
 */
public interface KnowledgeDocumentService {
    String CACHE_KEY = "ces:knowledge-doc";

    /**
     * 查询未加载的文档
     * 最多一万条数据
     */
    List<KnowledgeDocumentDTO> getUnloadedDocuments();

    void updateDocumentEnabledStatus(List<Long> documentIds, Boolean status);

    void updateDocumentLoadedStatus(List<Long> documentIds, Boolean status);

    /**
     * 查询数据分页
     */
    PageInfo<KnowledgeDocumentDTO> queryAll(KnowledgeDocumentQueryParam query, Pageable pageable);

    /**
     * 查询所有数据不分页
     */
    List<KnowledgeDocumentDTO> queryAll(KnowledgeDocumentQueryParam query);


    KnowledgeDocumentDTO getById(Long id);

    int insert(KnowledgeDocumentDTO res);

    int updateById(KnowledgeDocumentDTO res);

    int removeByIds(Set<Long> ids);
}
