package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam;
import com.assistant.ai.repository.domain.result.BatchImportResult;
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

    /**
     * 批量导入文档（从指定目录）
     *
     * @param directoryPath    目录路径
     * @param groupId          用户分组 ID（租户 ID）
     * @param defaultScopeType 默认知识领域类型（如果无法从路径推断）
     * @return 导入结果：成功数量、失败数量、详细信息
     */
    BatchImportResult batchImportFromDirectory(String directoryPath, Long groupId, String defaultScopeType);
}
