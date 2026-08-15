package com.endcy.ai.rpc.api;

import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.AgentTaskParam;
import com.endcy.ai.rpc.domain.response.AgentTaskRet;
import com.endcy.ai.rpc.domain.response.ToolInventoryRet;

import java.util.List;

/**
 * 智能体任务 Feign 接口
 *
 * @author endcy
 * @since 2026/08/08
 */
public interface AgentFeignService {

    CommonResMsgDTO<AgentTaskRet> submitTask(AgentTaskParam param);

    CommonResMsgDTO<AgentTaskRet> getTaskStatus(String taskId);

    CommonResMsgDTO<Boolean> cancelTask(String taskId, String reason);

    CommonResMsgDTO<List<AgentTaskRet>> listTasksByChatId(Long chatId);

    CommonResMsgDTO<ToolInventoryRet> listTools();

    CommonResMsgDTO<AgentTaskRet> executeSync(AgentTaskParam param);
}
