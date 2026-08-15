package com.endcy.ai.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.endcy.ai.advisor.ChatClientAdvisorFactory;
import com.endcy.ai.agent.EnergyManus;
import com.endcy.ai.app.AliDashScopeApp;
import com.endcy.ai.app.EnergyAiApp;
import com.endcy.ai.app.EnergyAiToolsApp;
import com.endcy.ai.domain.context.RequestRagContext;
import com.endcy.ai.domain.vo.EnergyReport;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import com.endcy.ai.rpc.enums.ApiQaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.List;

/**
 * 智慧能源 AI 助手主控制器。
 *
 * @author endcy
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/energy-ai")
public class AiController {

    private static final long SSE_EMITTER_TIMEOUT_MS = 180_000L;

    private final EnergyAiToolsApp energyAiToolsApp;

    private final EnergyAiApp energyAiApp;

    private final AliDashScopeApp aliDashScopeApp;

    private final ToolCallback[] commonWebTools;

    private final DashScopeChatModel dashscopeChatModel;

    private final ChatClientAdvisorFactory chatClientAdvisorFactory;

    /**
     * Synchronous call for simple LLM Q&amp;A.
     * Supports multimodal input; mediaList is a JSON-format multimedia attachment list.
     */
    @GetMapping("/chat/sync")
    public String doChatWithAiSync(String message, String chatId,
                                   @RequestParam(required = false) String mediaList) {
        KnowledgeAIQueryParam query = new KnowledgeAIQueryParam();
        query.setChatId(Long.valueOf(chatId));
        query.setGroupId("-1");
        query.setScopeType("Test");
        query.setBusinessType("Test");
        query.setQueryType(ApiQaType.DOMAIN.getCode());
        query.setQuestion(message);
        query.setMediaList(parseMediaList(mediaList));
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(query.getChatId());
        return energyAiApp.simpleChat(query, requestRagContext);
    }

    /**
     * Flux streaming call for simple LLM Q&amp;A.
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithAiSSE(String message, String chatId) {
        return energyAiApp.doChatByStream(message, chatId);
    }

    /**
     * SSE streaming call for simple LLM Q&amp;A.
     */
    @GetMapping(value = "/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithAiServerSentEvent(String message, String chatId) {
        return energyAiApp.doChatByStream(message, chatId)
                          .map(chunk -> ServerSentEvent.<String>builder()
                                                       .data(chunk)
                                                       .build());
    }

    /**
     * SseEmitter streaming call for simple LLM Q&amp;A.
     */
    @GetMapping(value = "/chat/sse_emitter")
    public SseEmitter doChatWithAiServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(SSE_EMITTER_TIMEOUT_MS);
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
     * Report generation call.
     */
    @GetMapping("/chat/report")
    public EnergyReport doChatWithReport(String message, String chatId) {
        return energyAiToolsApp.doChatWithReport(message, chatId);
    }

    /**
     * RAG-augmented call: PG or local document RAG.
     * Supports multimodal input; mediaList is a JSON-format multimedia attachment list.
     */
    @GetMapping("/chat/rag")
    public String doChatWithRag(@RequestParam(required = false) String groupId,
                                @RequestParam(required = false) String scopeType,
                                String message,
                                Long chatId,
                                @RequestParam(required = false) String mediaList) {
        return energyAiApp.doChatWithRag(scopeType, groupId, message, chatId, parseMediaList(mediaList));
    }

    /**
     * Parse mediaList JSON string into a list of MediaAttachment objects.
     */
    private List<MediaAttachment> parseMediaList(String mediaListJson) {
        if (StrUtil.isBlank(mediaListJson)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(mediaListJson, MediaAttachment.class);
        } catch (Exception e) {
            log.warn("Failed to parse mediaList JSON: {}", mediaListJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * Tool chain call.
     * Currently supports online search and search result format processing.
     */
    @GetMapping("/chat/tools")
    public String doChatWithTools(String message, String chatId) {
        return energyAiToolsApp.doChatWithTools(message, chatId);
    }

    /**
     * MCP call.
     * Currently fetches data via specific search APIs.
     */
    @GetMapping("/chat/mcp")
    public String doChatWithMcp(String message, String chatId) {
        return energyAiToolsApp.doChatWithMcp(message, chatId);
    }

    /**
     * Streaming call to super-agent.
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        EnergyManus energyManus = new EnergyManus(commonWebTools, dashscopeChatModel, chatClientAdvisorFactory);
        return energyManus.runStream(message);
    }
}
