package com.assistant.ai.controller;

import com.assistant.ai.rpc.client.EnergyAiFeignClient;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 问答测试控制器
 * <p>
 * 通过 Feign 客户端调用 energy-ai-api 核心服务，验证 AI 问答功能。
 * 提供同步问答接口，用于开发和联调阶段验证 AI 服务连通性。
 * </p>
 * <p>仅在 ai.rpc.enabled=true 时启用（依赖 Feign 客户端）</p>
 *
 * @author endcy
 * @date 2026/6/10 20:44:50
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/")
@ConditionalOnProperty(name = "ai.rpc.enabled", havingValue = "true")
public class TestEnergyAiController {
    private final EnergyAiFeignClient energyAiFeignClient;

    @GetMapping("/qa")
    public CommonResMsgDTO<String> simpleQa(@RequestParam(value = "content") String content) {
        CommonResMsgDTO<String> ret = energyAiFeignClient.callAiQa(content);
        log.info(">>>>>>> EnergyAi qa receive msg proc {}", System.currentTimeMillis());
        return ret;
    }

}
