package com.endcy.ai.rpc.api;

import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.response.RecordResponse;

import java.util.List;

/**
 * 对话记录 Feign 接口
 *
 * @author endcy
 * @since 2026/1/16 17:55
 */
public interface RecordFeignService {
    CommonResMsgDTO<List<RecordResponse>> getByChatId(Long chatId);
}
