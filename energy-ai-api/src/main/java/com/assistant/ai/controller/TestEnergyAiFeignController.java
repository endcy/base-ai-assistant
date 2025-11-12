package com.assistant.ai.controller;

import com.assistant.ai.rpc.admin.EnergyAiFeignService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ...
 *
 * @author endcy
 * @date 2023/8/21 20:44:50
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/llm")
public class TestEnergyAiFeignController {
    private final EnergyAiFeignService apiService;

    @GetMapping("/qa")
    public CommonResMsgDTO<String> simpleQa(@RequestParam(value = "content") String content) {
        CommonResMsgDTO<String> ret = apiService.callAiQa(content);
        log.info(">>>>>>> EnergyAi receive msg proc {}", System.currentTimeMillis());
        return ret;
    }

}
