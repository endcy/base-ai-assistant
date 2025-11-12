package com.assistant.ai.controller;

import com.assistant.ai.advisor.ChatClientAdvisorFactory;
import com.assistant.ai.agent.EnergyManus;
import com.assistant.ai.app.EnergyAiAliDashScopeApp;
import com.assistant.ai.app.EnergyAiApp;
import com.assistant.ai.app.EnergyAiToolsApp;
import com.assistant.ai.domain.vo.EnergyReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/energy-ai")
public class AiController {

    private final EnergyAiToolsApp energyAiToolsApp;

    private final EnergyAiApp energyAiApp;

    private final EnergyAiAliDashScopeApp energyAiAliDashScopeApp;

    private final ToolCallback[] commonWebTools;

    private final ChatModel dashscopeChatModel;

    private final ChatClientAdvisorFactory chatClientAdvisorFactory;

    /**
     * 同步调用 智慧能源AI助手 线上应用
     */
    @GetMapping("/cloudApp/sync")
    public String doChatWithAppAiSync(String message, String chatId) {
        return energyAiAliDashScopeApp.doAppChat(message, chatId);
    }

    /**
     * 同步调用 智慧能源AI助手 大模型简单问答
     */
    @GetMapping("/chat/sync")
    public String doChatWithAiSync(String message, String chatId) {
        return energyAiApp.doChat(message, chatId);
    }

    /**
     * Flux流式调用 智慧能源AI助手 大模型简单问答
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithAiSSE(String message, String chatId) {
        return energyAiApp.doChatByStream(message, chatId);
    }

    /**
     * SSE流式调用 智慧能源AI助手 大模型简单问答
     */
    @GetMapping(value = "/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithAiServerSentEvent(String message, String chatId) {
        return energyAiApp.doChatByStream(message, chatId)
                          .map(chunk -> ServerSentEvent.<String>builder()
                                                       .data(chunk)
                                                       .build());
    }

    /**
     * SseEmitter流式调用 智慧能源AI助手 大模型简单问答
     */
    @GetMapping(value = "/chat/sse_emitter")
    public SseEmitter doChatWithAiServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter 3分钟超时
        SseEmitter sseEmitter = new SseEmitter(180000L);
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        energyAiApp.doChatByStream(message, chatId)
                   .subscribe(chunk -> {
                       try {
                           sseEmitter.send(chunk);
                       } catch (IOException e) {
                           sseEmitter.completeWithError(e);
                       }
                   }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    /**
     * 调用报告 智慧能源AI助手 大模型简单问答
     */
    @GetMapping("/chat/report")
    public EnergyReport doChatWithReport(String message, String chatId) {
        return energyAiToolsApp.doChatWithReport(message, chatId);
    }

    /**
     * 检索增强调用 智慧能源AI助手 pg或本地文档rag调用
     */
    @GetMapping("/chat/rag")
    public String doChatWithRag(@RequestParam(required = false) Long groupId,
                                @RequestParam(required = false) String scopeType,
                                String message,
                                Long chatId) {
        return energyAiApp.doChatWithRag(scopeType, groupId, message, chatId);
    }

    /**
     * 调用工具链 智慧能源AI助手 工具链调用
     * 目前仅在线搜索和搜索各格式处理
     */
    @GetMapping("/chat/tools")
    public String doChatWithTools(String message, String chatId) {
        return energyAiToolsApp.doChatWithTools(message, chatId);
    }

    /**
     * 调用MCP 智慧能源AI助手
     * 目前特定搜索api获取数据 如文本加图片润色
     */
    @GetMapping("/chat/mcp")
    public String doChatWithMcp(String message, String chatId) {
        return energyAiToolsApp.doChatWithMcp(message, chatId);
    }

    /**
     * 流式调用 智慧能源AI助手超级智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        EnergyManus energyManus = new EnergyManus(commonWebTools, dashscopeChatModel, chatClientAdvisorFactory);
        return energyManus.runStream(message);
    }


}
