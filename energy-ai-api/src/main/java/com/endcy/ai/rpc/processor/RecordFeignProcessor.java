package com.endcy.ai.rpc.processor;

import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import com.endcy.ai.repository.service.ContextUserRecordService;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.response.RecordResponse;
import com.endcy.service.common.annotation.LogReqRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 对话记录 RPC 处理器
 * 提供按对话ID查询问答记录的能力
 *
 * @author endcy
 * @since 2026/1/16 17:57
 */
@LogReqRes("log.enable.rpc.RecordFeignProcessor")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/record")
public class RecordFeignProcessor {
    private final ContextUserRecordService contextUserRecordService;

    /**
     * 根据对话ID查询问答记录
     *
     * @param chatId 对话ID
     * @return 问答记录列表
     */
    @GetMapping
    public CommonResMsgDTO<List<RecordResponse>> getByChatId(Long chatId) {
        List<ContextUserRecordDTO> recordDTOList = contextUserRecordService.getByChatId(chatId);
        recordDTOList = recordDTOList.stream()
                                     .sorted(Comparator.comparing(ContextUserRecordDTO::getCreateTime))
                                     .toList();
        List<RecordResponse> result = new ArrayList<>(recordDTOList.size());
        recordDTOList.forEach(e -> {
            RecordResponse response = new RecordResponse();
            response.setQuestion(e.getQuestion());
            response.setContent(e.getContent());
            response.setCreateTime(e.getCreateTime());
            result.add(response);
        });
        return CommonResMsgDTO.successDeviceRes(result);
    }
}
