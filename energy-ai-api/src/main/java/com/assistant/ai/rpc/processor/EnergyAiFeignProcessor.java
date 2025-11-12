package com.assistant.ai.rpc.processor;

import com.assistant.ai.rpc.admin.EnergyAiFeignService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.service.common.annotation.LogReqRes;
import org.springframework.stereotype.Service;

/**
 * ...
 *
 * @author endcy
 * @date 2025/6/5 20:16:10
 */
@LogReqRes("log.enable.rpc.EnergyAiFeignService")
@Service
public class EnergyAiFeignProcessor implements EnergyAiFeignService {
    @Override
    public CommonResMsgDTO<String> callAiQa(String content) {
        return CommonResMsgDTO.successDeviceRes("test ok");
    }

}
