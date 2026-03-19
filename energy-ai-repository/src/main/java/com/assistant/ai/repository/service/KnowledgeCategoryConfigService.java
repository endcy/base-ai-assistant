package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.domain.request.KnowledgeCategoryQueryParam;
import com.assistant.service.common.base.PageInfo;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 知识分类配置服务接口
 *
 * @author endcy
 * @since 2026/03/18
 */
public interface KnowledgeCategoryConfigService {

    /**
     * 分页查询
     */
    PageInfo<KnowledgeCategoryConfigDTO> queryAll(KnowledgeCategoryQueryParam query, Pageable pageable);

    /**
     * 查询所有 (不分页)
     */
    List<KnowledgeCategoryConfigDTO> queryAll(KnowledgeCategoryQueryParam query);

    /**
     * 根据 ID 查询
     */
    KnowledgeCategoryConfigDTO getById(Long id);

    /**
     * 根据类型查询所有启用的分类
     */
    List<KnowledgeCategoryConfigDTO> getByType(String type);

    /**
     * 新增
     */
    int insert(KnowledgeCategoryConfigDTO dto);

    /**
     * 修改
     */
    int updateById(KnowledgeCategoryConfigDTO dto);

    /**
     * 删除
     */
    int removeByIds(List<Long> ids);

    /**
     * 更新启用状态
     */
    void updateEnabledStatus(List<Long> ids, Boolean enabled);
}
