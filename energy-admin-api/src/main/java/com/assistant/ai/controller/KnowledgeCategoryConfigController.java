package com.assistant.ai.controller;

import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.domain.request.UpdateEnabledRequest;
import com.assistant.ai.repository.domain.request.KnowledgeCategoryQueryParam;
import com.assistant.ai.repository.service.KnowledgeCategoryConfigService;
import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.enums.LogActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识分类配置管理
 *
 * @author endcy
 * @since 2026/03/18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-category")
public class KnowledgeCategoryConfigController {

    private final KnowledgeCategoryConfigService knowledgeCategoryConfigService;

    /**
     * 分页查询知识分类
     */
    @GetMapping
    @LogRecord("查询知识分类配置")
    public PageInfo<KnowledgeCategoryConfigDTO> query(KnowledgeCategoryQueryParam query, Pageable pageable) {
        return knowledgeCategoryConfigService.queryAll(query, pageable);
    }

    /**
     * 根据类型查询所有启用的分类
     */
    @GetMapping("/by-type/{type}")
    @LogRecord("根据类型查询知识分类")
    public List<KnowledgeCategoryConfigDTO> getByType(@PathVariable String type) {
        return knowledgeCategoryConfigService.getByType(type);
    }

    /**
     * 查询所有分类 (不分页)
     */
    @GetMapping("/all")
    @LogRecord("查询所有知识分类")
    public List<KnowledgeCategoryConfigDTO> queryAll(KnowledgeCategoryQueryParam query) {
        return knowledgeCategoryConfigService.queryAll(query);
    }

    /**
     * 新增知识分类
     */
    @PostMapping
    @LogRecord(value = "新增知识分类配置", type = LogActionType.ADD)
    public Integer create(@Validated @RequestBody KnowledgeCategoryConfigDTO dto) {
        return knowledgeCategoryConfigService.insert(dto);
    }

    /**
     * 修改知识分类
     */
    @PutMapping
    @LogRecord(value = "修改知识分类配置", type = LogActionType.UPDATE)
    public Integer update(@Validated @RequestBody KnowledgeCategoryConfigDTO dto) {
        return knowledgeCategoryConfigService.updateById(dto);
    }

    /**
     * 删除知识分类
     */
    @DeleteMapping
    @LogRecord(value = "删除知识分类配置", type = LogActionType.DELETE)
    public Integer delete(@RequestBody List<Long> ids) {
        return knowledgeCategoryConfigService.removeByIds(ids);
    }

    /**
     * 更新启用状态
     */
    @PutMapping("/enabled")
    @LogRecord(value = "更新知识分类启用状态", type = LogActionType.UPDATE)
    public void updateEnabled(@RequestBody @Validated UpdateEnabledRequest request) {
        knowledgeCategoryConfigService.updateEnabledStatus(request.getIds(), request.getEnabled());
    }
}
