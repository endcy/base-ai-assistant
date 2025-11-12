package com.assistant.ai.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.assistant.ai.repository.domain.query.VectorDocumentQueryParam;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.ai.repository.pgsql.mapper.VectorDocumentMapper;
import com.assistant.ai.repository.service.VectorStoreService;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 知识库-向量库数据维护
 * 不使用事务NOT_SUPPORTED
 *
 * @author endcy
 * @implSpec pgsql向量数据不适用事务维护。
 * 同事务中多数据源不可混用，多数据源使用事务时，除了主数据源务必指定事务管理器
 * @since 2025/08/04 20:55:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true, transactionManager = "pgSqlTransactionManager", rollbackFor = Exception.class)
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorDocumentMapper vectorDocumentMapper;

    @Override
    public List<VectorDocument> queryAll(VectorDocumentQueryParam query) {
        return vectorDocumentMapper.selectList(QueryHelpMybatisPlus.getPredicateSimple(query));
    }

    @Override
    public VectorDocument getById(String id) {
        return vectorDocumentMapper.selectById(id);
    }

    @Override
    public int insert(VectorDocument res) {
        return vectorDocumentMapper.insert(res);
    }

    @Override
    public int removeByDocIds(Set<Long> ids) {
        QueryWrapper<VectorDocument> updateWrapper = Wrappers.query();
        updateWrapper.isNotNull("metadata")
                     .in("metadata->>'id'", ids);
        return vectorDocumentMapper.delete(updateWrapper);
    }

    @Override
    public void batchInsert(List<VectorDocument> knowledgeDocs) {
        vectorDocumentMapper.insertBatchSomeColumn(knowledgeDocs);
    }

    @Override
    public boolean isExistsInVector(Long docId) {
        QueryWrapper<VectorDocument> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("metadata")
                    .eq("metadata->>'id'", docId);
        return vectorDocumentMapper.exists(queryWrapper);
    }
}
