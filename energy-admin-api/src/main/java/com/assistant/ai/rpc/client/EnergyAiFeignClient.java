package com.assistant.ai.rpc.client;

import com.assistant.ai.rpc.admin.EnergyAiFeignService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 请求device接口客户端
 *
 * @author endcy
 * @date 2025/6/5 20:41:17
 */
@FeignClient(name = "${service.energy-ai-api.name:energy-ai-api}", url = "${feign-energy-ai-api.url:}")
public interface EnergyAiFeignClient extends EnergyAiFeignService {

    @GetMapping("/api/llm/qa")
    CommonResMsgDTO<String> callAiQa(@RequestParam String content);
}
