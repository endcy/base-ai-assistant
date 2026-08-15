package com.endcy.ai.controller;

import com.endcy.ai.tools.DeepSeekWebSearchTool;
import com.endcy.service.domain.request.QuestionFormatParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智慧能源 AI 工具控制器。
 *
 * @author endcy
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/energy-ai/tool")
public class AiToolController {

    private final DeepSeekWebSearchTool deepSeekWebSearchTool;

    /**
     * Synchronous DeepSeek web search.
     */
    @PostMapping("/deepseek/sync")
    public String queryWithWeb(@Validated @RequestBody QuestionFormatParam params) {
        return deepSeekWebSearchTool.searchQuestion(params.getAnswerFormat(), params.getQuestion());
    }

}
