package com.assistant.ai.controller;

import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.enums.LogActionType;
import com.assistant.service.common.logging.domain.Log;
import com.assistant.service.common.logging.service.LogService;
import com.assistant.service.common.logging.service.dto.LogQueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 日志管理
 * admin-api示例
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/log")
public class LogController {

    private final LogService logService;

    @GetMapping
    @LogRecord("查询日志")
//    @PreAuthorize("@el.check('log:list')")
    public PageInfo query(LogQueryParam query, Pageable pageable) {
        return logService.queryAll(query, pageable);
    }

    @PostMapping
    @LogRecord(value = "新增日志", type = LogActionType.ADD)
//    @PreAuthorize("@el.check('log:add')")
    public Integer create(@Validated @RequestBody Log res) {
        return logService.insert(res);
    }

    @PutMapping
    @LogRecord(value = "修改日志", type = LogActionType.UPDATE)
//    @PreAuthorize("@el.check('log:edit')")
    public Integer update(@Validated @RequestBody Log res) {
        return logService.updateById(res);
    }

    @DeleteMapping
    @LogRecord(value = "删除日志", type = LogActionType.DELETE)
//    @PreAuthorize("@el.check('log:del')")
    public Integer delete(@RequestBody Set<Long> ids) {
        return logService.removeByIds(ids);
    }

}
