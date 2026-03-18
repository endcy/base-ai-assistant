package com.assistant.ai.repository.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.domain.entity.KnowledgeCategoryConfig;
import com.assistant.ai.repository.domain.request.KnowledgeCategoryQueryParam;
import com.assistant.ai.repository.service.KnowledgeCategoryConfigService;
import com.assistant.ai.repository.service.convert.KnowledgeCategoryConfigConverter;
import com.assistant.ai.repository.trans.mapper.KnowledgeCategoryConfigMapper;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.utils.PageUtil;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

/**
 * 知识分类配置服务实现类
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "knowledgeCategoryConfigCache")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class KnowledgeCategoryConfigServiceImpl implements KnowledgeCategoryConfigService {

    private final KnowledgeCategoryConfigMapper knowledgeCategoryConfigMapper;
    private final KnowledgeCategoryConfigConverter knowledgeCategoryConfigConverter;

    @Override
    public PageInfo<KnowledgeCategoryConfigDTO> queryAll(KnowledgeCategoryQueryParam query, Pageable pageable) {
        IPage<KnowledgeCategoryConfig> queryPage = PageUtil.toMybatisPage(pageable);
        IPage<KnowledgeCategoryConfig> page = knowledgeCategoryConfigMapper.selectPage(queryPage, QueryHelpMybatisPlus.getPredicateSimple(query));
        return knowledgeCategoryConfigConverter.convertPage(page);
    }

    @Override
    public List<KnowledgeCategoryConfigDTO> queryAll(KnowledgeCategoryQueryParam query) {
        return knowledgeCategoryConfigConverter.toDto(knowledgeCategoryConfigMapper.selectList(QueryHelpMybatisPlus.getPredicateSimple(query)));
    }

    @Override
    public KnowledgeCategoryConfigDTO getById(Long id) {
        return knowledgeCategoryConfigConverter.toDto(knowledgeCategoryConfigMapper.selectById(id));
    }

    @Override
    public List<KnowledgeCategoryConfigDTO> getByType(String type) {
        LambdaQueryWrapper<KnowledgeCategoryConfig> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(KnowledgeCategoryConfig::getType, type)
                    .eq(KnowledgeCategoryConfig::getEnabled, true)
                    .orderByAsc(KnowledgeCategoryConfig::getSortOrder);
        return knowledgeCategoryConfigConverter.toDto(knowledgeCategoryConfigMapper.selectList(queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(KnowledgeCategoryConfigDTO dto) {
        KnowledgeCategoryConfig entity = knowledgeCategoryConfigConverter.toEntity(dto);
        return knowledgeCategoryConfigMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateById(KnowledgeCategoryConfigDTO dto) {
        if (dto.getId() == null) {
            return 0;
        }
        KnowledgeCategoryConfig entity = knowledgeCategoryConfigConverter.toEntity(dto);
        return knowledgeCategoryConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        LambdaQueryWrapper<KnowledgeCategoryConfig> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(KnowledgeCategoryConfig::getId, ids);
        return knowledgeCategoryConfigMapper.delete(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabledStatus(List<Long> ids, Boolean enabled) {
        LambdaUpdateWrapper<KnowledgeCategoryConfig> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(KnowledgeCategoryConfig::getEnabled, enabled)
                     .in(KnowledgeCategoryConfig::getId, ids);
        knowledgeCategoryConfigMapper.update(null, updateWrapper);
    }
}
