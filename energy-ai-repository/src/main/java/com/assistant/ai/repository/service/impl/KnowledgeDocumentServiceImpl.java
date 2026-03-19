package com.assistant.ai.repository.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.entity.KnowledgeDocument;
import com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam;
import com.assistant.ai.repository.domain.result.BatchImportResult;
import com.assistant.ai.repository.helper.DocumentImportHelper;
import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.ai.repository.service.convert.KnowledgeDocumentConverter;
import com.assistant.ai.repository.trans.mapper.KnowledgeDocumentMapper;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.utils.PageUtil;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 20:55:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = KnowledgeDocumentService.CACHE_KEY)
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentConverter knowledgeDocumentConverter;
    private final VectorStoreServiceImpl vectorStoreService;
    private final DocumentImportHelper documentImportHelper;

    @Override
    public List<KnowledgeDocumentDTO> getUnloadedDocuments() {
        KnowledgeDocumentQueryParam query = new KnowledgeDocumentQueryParam();
        query.setEnabled(true);
        query.setLoaded(false);
        QueryWrapper<KnowledgeDocument> queryWrapper = QueryHelpMybatisPlus.getPredicateSimple(query);
        LambdaQueryWrapper<KnowledgeDocument> lambdaQueryWrapper = queryWrapper.lambda().last("limit 10000");
        return knowledgeDocumentConverter.toDto(knowledgeDocumentMapper.selectList(lambdaQueryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocumentEnabledStatus(List<Long> documentIds, Boolean status) {
        LambdaUpdateWrapper<KnowledgeDocument> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(KnowledgeDocument::getEnabled, status)
                     .in(KnowledgeDocument::getId, documentIds);
        knowledgeDocumentMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocumentLoadedStatus(List<Long> documentIds, Boolean status) {
        LambdaUpdateWrapper<KnowledgeDocument> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(KnowledgeDocument::getLoaded, status)
                     .in(KnowledgeDocument::getId, documentIds);
        knowledgeDocumentMapper.update(null, updateWrapper);
    }

    @Override
    public PageInfo<KnowledgeDocumentDTO> queryAll(KnowledgeDocumentQueryParam query, Pageable pageable) {
        IPage<KnowledgeDocument> queryPage = PageUtil.toMybatisPage(pageable);
        IPage<KnowledgeDocument> page = knowledgeDocumentMapper.selectPage(queryPage, QueryHelpMybatisPlus.getPredicateSimple(query));
        return knowledgeDocumentConverter.convertPage(page);
    }

    @Override
    public List<KnowledgeDocumentDTO> queryAll(KnowledgeDocumentQueryParam query) {
        return knowledgeDocumentConverter.toDto(knowledgeDocumentMapper.selectList(QueryHelpMybatisPlus.getPredicateSimple(query)));
    }

    @Override
    public KnowledgeDocumentDTO getById(Long id) {
        return knowledgeDocumentConverter.toDto(knowledgeDocumentMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(KnowledgeDocumentDTO res) {
        KnowledgeDocument entity = knowledgeDocumentConverter.toEntity(res);
        return knowledgeDocumentMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateById(KnowledgeDocumentDTO res) {
        if (res.getId() == null) {
            return 0;
        }
        res.setLoaded(false);
        vectorStoreService.removeByDocIds(CollUtil.newHashSet(res.getId()));
        KnowledgeDocument entity = knowledgeDocumentConverter.toEntity(res);
        return knowledgeDocumentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeByIds(Set<Long> ids) {
        return knowledgeDocumentMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchImportFromDirectory(String directoryPath, Long groupId, String defaultScopeType) {
        log.info("批量导入文档：directoryPath={}, groupId={}, defaultScopeType={}", directoryPath, groupId, defaultScopeType);
        return documentImportHelper.importFromDirectory(directoryPath, groupId, defaultScopeType);
    }

}
