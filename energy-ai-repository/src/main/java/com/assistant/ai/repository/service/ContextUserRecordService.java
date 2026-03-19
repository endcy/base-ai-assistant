package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.domain.request.ContextUserRecordQueryParam;
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
public interface ContextUserRecordService {
    String CACHE_KEY = "ces:user-record";

    List<ContextUserRecordDTO> getByChatId(Long chatId);

    List<ContextUserRecordDTO> getByUserId(Long userId, Integer userType, Long chatId);

    /**
     * 查询数据分页
     */
    PageInfo<ContextUserRecordDTO> queryAll(ContextUserRecordQueryParam query, Pageable pageable);

    /**
     * 查询所有数据不分页
     */
    List<ContextUserRecordDTO> queryAll(ContextUserRecordQueryParam query);


    ContextUserRecordDTO getById(Long id);

    int insert(ContextUserRecordDTO res);

    int updateById(ContextUserRecordDTO res);

    int removeByIds(Set<Long> ids);

    void updateAnswerById(Long id, String content);
}
