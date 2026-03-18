package com.assistant.ai.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.domain.entity.ContextUserRecord;
import com.assistant.ai.repository.domain.request.ContextUserRecordQueryParam;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.ai.repository.service.convert.ContextUserRecordConverter;
import com.assistant.ai.repository.trans.mapper.ContextUserRecordMapper;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.utils.PageUtil;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
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
@CacheConfig(cacheNames = ContextUserRecordService.CACHE_KEY)
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ContextUserRecordServiceImpl implements ContextUserRecordService {

    private final ContextUserRecordMapper contextUserRecordMapper;
    private final ContextUserRecordConverter contextUserRecordConverter;

    @Override
    public List<ContextUserRecordDTO> getByChatId(Long chatId) {
        LambdaQueryWrapper<ContextUserRecord> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ContextUserRecord::getChatId, chatId);
        return contextUserRecordConverter.toDto(contextUserRecordMapper.selectList(queryWrapper));
    }

    @Override
    public List<ContextUserRecordDTO> getByUserId(Long userId, @Nullable Integer userType, @Nullable Long chatId) {
        LambdaQueryWrapper<ContextUserRecord> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ContextUserRecord::getUserId, userId)
                    .eq(userType != null, ContextUserRecord::getUserType, userType)
                    .eq(chatId != null, ContextUserRecord::getChatId, chatId);
        return contextUserRecordConverter.toDto(contextUserRecordMapper.selectList(queryWrapper));
    }

    @Override
    public PageInfo<ContextUserRecordDTO> queryAll(ContextUserRecordQueryParam query, Pageable pageable) {
        IPage<ContextUserRecord> queryPage = PageUtil.toMybatisPage(pageable);
        IPage<ContextUserRecord> page = contextUserRecordMapper.selectPage(queryPage, QueryHelpMybatisPlus.getPredicateSimple(query));
        return contextUserRecordConverter.convertPage(page);
    }

    @Override
    public List<ContextUserRecordDTO> queryAll(ContextUserRecordQueryParam query) {
        return contextUserRecordConverter.toDto(contextUserRecordMapper.selectList(QueryHelpMybatisPlus.getPredicateSimple(query)));
    }

    @Override
    public ContextUserRecordDTO getById(Long id) {
        return contextUserRecordConverter.toDto(contextUserRecordMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(ContextUserRecordDTO res) {
        ContextUserRecord entity = contextUserRecordConverter.toEntity(res);
        int ret = contextUserRecordMapper.insert(entity);
        if (ret > 0) {
            res.setId(entity.getId());
        }
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ContextUserRecordDTO res) {
        if (res.getId() == null) {
            return 0;
        }
        ContextUserRecord entity = contextUserRecordConverter.toEntity(res);
        return contextUserRecordMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeByIds(Set<Long> ids) {
        return contextUserRecordMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnswerById(Long id, String content) {
        LambdaUpdateWrapper<ContextUserRecord> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ContextUserRecord::getId, id)
                .set(ContextUserRecord::getContent, content);
        contextUserRecordMapper.update(null, updateWrapper);
    }
}
