package com.assistant.ai.controller;

import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.request.BatchImportRequest;
import com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam;
import com.assistant.ai.repository.domain.result.BatchImportResult;
import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.enums.LogActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 知识库文档管理
 * admin-api示例
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-document")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @GetMapping
    @LogRecord("查询知识库文档")
//    @PreAuthorize("@el.check('knowledgeDocument:list')")
    public PageInfo<KnowledgeDocumentDTO> query(KnowledgeDocumentQueryParam query, Pageable pageable) {
        return knowledgeDocumentService.queryAll(query, pageable);
    }

    @PostMapping
    @LogRecord(value = "新增知识库文档", type = LogActionType.ADD)
//    @PreAuthorize("@el.check('knowledgeDocument:add')")
    public Integer create(@Validated @RequestBody KnowledgeDocumentDTO res) {
        return knowledgeDocumentService.insert(res);
    }

    @PutMapping
    @LogRecord(value = "修改知识库文档", type = LogActionType.UPDATE)
//    @PreAuthorize("@el.check('knowledgeDocument:edit')")
    public Integer update(@Validated @RequestBody KnowledgeDocumentDTO res) {
        return knowledgeDocumentService.updateById(res);
    }

    @DeleteMapping
    @LogRecord(value = "删除知识库文档", type = LogActionType.DELETE)
//    @PreAuthorize("@el.check('knowledgeDocument:del')")
    public Integer delete(@RequestBody Set<Long> ids) {
        return knowledgeDocumentService.removeByIds(ids);
    }

    @PostMapping("/batch-import")
    @LogRecord(value = "批量导入知识文档", type = LogActionType.ADD)
//    @PreAuthorize("@el.check('knowledgeDocument:add')")
    public BatchImportResult batchImport(@RequestBody BatchImportRequest request) {
        return knowledgeDocumentService.batchImportFromDirectory(request.getDirectoryPath(), request.getGroupId(), request.getDefaultScopeType());
    }

}
