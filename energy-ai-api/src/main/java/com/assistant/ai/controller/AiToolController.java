package com.assistant.ai.controller;

import com.assistant.ai.tools.DeepSeekWebSearchTool;
import com.assistant.service.domain.request.QuestionFormatParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/energy-ai/tool")
public class AiToolController {

    private final DeepSeekWebSearchTool deepSeekWebSearchTool;

    /**
     * 同步调用 智慧能源AI助手 DeepSeek联网搜索
     * 示例输出格式 {"launchDate":"yyyy-MM-dd","url":"参考的url"}
     * 示例问题 请使用互联网在线精准搜索车型 [岚图梦想家 2025款 EV 四驱旗舰乾崑版]，获取该车型最可能的生产日期是什么时候和参考的url
     */
    @PostMapping("/deepseek/sync")
    public String queryWithWeb(@Validated @RequestBody QuestionFormatParam params) {
        return deepSeekWebSearchTool.searchQuestion(params.getAnswerFormat(), params.getQuestion());
    }

}
