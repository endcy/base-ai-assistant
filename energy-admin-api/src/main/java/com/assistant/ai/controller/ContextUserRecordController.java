package com.assistant.ai.controller;

import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.domain.query.ContextUserRecordQueryParam;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.enums.LogActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 用户对话记录管理
 * admin-api示例
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/context-user-record")
public class ContextUserRecordController {

    private final ContextUserRecordService contextUserRecordService;

    @GetMapping
    @LogRecord("查询用户对话记录")
//    @PreAuthorize("@el.check('contextUserRecord:list')")
    public PageInfo<ContextUserRecordDTO> query(ContextUserRecordQueryParam query, Pageable pageable) {
        return contextUserRecordService.queryAll(query, pageable);
    }

    @PostMapping
    @LogRecord(value = "新增用户对话记录", type = LogActionType.ADD)
//    @PreAuthorize("@el.check('contextUserRecord:add')")
    public Integer create(@Validated @RequestBody ContextUserRecordDTO res) {
        return contextUserRecordService.insert(res);
    }

    @PutMapping
    @LogRecord(value = "修改用户对话记录", type = LogActionType.UPDATE)
//    @PreAuthorize("@el.check('contextUserRecord:edit')")
    public Integer update(@Validated @RequestBody ContextUserRecordDTO res) {
        return contextUserRecordService.updateById(res);
    }

    @DeleteMapping
    @LogRecord(value = "删除用户对话记录", type = LogActionType.DELETE)
//    @PreAuthorize("@el.check('contextUserRecord:del')")
    public Integer delete(@RequestBody Set<Long> ids) {
        return contextUserRecordService.removeByIds(ids);
    }

}
