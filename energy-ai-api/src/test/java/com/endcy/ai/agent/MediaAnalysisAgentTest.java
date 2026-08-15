package com.endcy.ai.agent;

import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.prompt.PromptTemplateKey;
import com.endcy.ai.prompt.PromptTemplateService;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MediaAnalysisAgent 单元测试
 * 覆盖：开关控制、空/异常输入、单/多附件、URL 异常降级、模型调用失败兜底
 *
 * @author endcy
 * @since 2026/08/03
 */
@ExtendWith(MockitoExtension.class)
class MediaAnalysisAgentTest {

    @Mock
    private ChatModel chatModel;

    private ChatClient mediaAnalysisChatClient;

    private ChatRagProperties chatRagProperties;

    @Mock
    private PromptTemplateService promptTemplateService;

    private MediaAnalysisAgent mediaAnalysisAgent;

    @BeforeEach
    void setUp() {
        // 使用 mock 的 ChatModel 构建 ChatClient（同 IntentAnalysisAgentTest 模式）
        mediaAnalysisChatClient = ChatClient.builder(chatModel).build();

        chatRagProperties = new ChatRagProperties();
        chatRagProperties.setEnableMediaAnalysis(true);
        chatRagProperties.setMediaAnalysisModel("qwen-vl-max");

        // stub prompt templates used when model is actually invoked
        lenient().when(promptTemplateService.getTemplate(PromptTemplateKey.MEDIA_ANALYSIS_SYSTEM)).thenReturn(
                "You are a multimedia analysis assistant.");
        lenient().when(promptTemplateService.getTemplate(PromptTemplateKey.MEDIA_ANALYSIS_USER)).thenReturn(
                "Analyze the attachment content in the context of the question: %s");

        mediaAnalysisAgent = new MediaAnalysisAgent(mediaAnalysisChatClient, chatRagProperties, promptTemplateService);
    }

    // ============ 开关与空输入 ============

    @Test
    void testAnalyze_DisabledByFlag_ReturnsEmpty() {
        chatRagProperties.setEnableMediaAnalysis(false);
        mediaAnalysisAgent = new MediaAnalysisAgent(mediaAnalysisChatClient, chatRagProperties, promptTemplateService);

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertEquals("", result);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_NullMediaList_ReturnsEmpty() {
        String result = mediaAnalysisAgent.analyze(1L, "问题", null);
        assertEquals("", result);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_EmptyMediaList_ReturnsEmpty() {
        String result = mediaAnalysisAgent.analyze(1L, "问题", Collections.emptyList());
        assertEquals("", result);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    // ============ 正常路径 ============

    @Test
    void testAnalyze_SingleImage_ReturnsDescriptionWithLabel() {
        mockChatModelResponse("这是一张充电桩故障码E001的截图。");

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "这个故障码是什么意思？", mediaList);

        assertEquals("【图片】这是一张充电桩故障码E001的截图。", result);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_MultipleAttachments_ReturnsNumberedLabels() {
        mockChatModelResponse("描述内容");

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build(),
                MediaAttachment.builder().type("AUDIO").url("https://example.com/b.mp3").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertTrue(result.contains("【图片1】描述内容"));
        assertTrue(result.contains("【音频2】描述内容"));
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_DifferentTypeLabels() {
        mockChatModelResponse("x");

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build(),
                MediaAttachment.builder().type("AUDIO").url("https://example.com/b.mp3").build(),
                MediaAttachment.builder().type("VIDEO").url("https://example.com/c.mp4").build(),
                MediaAttachment.builder().type("DOCUMENT").url("https://example.com/d.pdf").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertTrue(result.contains("【图片1】"));
        assertTrue(result.contains("【音频2】"));
        assertTrue(result.contains("【视频3】"));
        assertTrue(result.contains("【文档4】"));
    }

    @Test
    void testAnalyze_UserQuestionIncludedInPrompt() {
        mockChatModelResponse("描述");

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build());

        mediaAnalysisAgent.analyze(1L, "设备故障怎么处理", mediaList);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().toString();

        assertTrue(promptText.contains("设备故障怎么处理"));
    }

    // ============ 异常与降级 ============

    @Test
    void testAnalyze_BlankUrl_Skipped() {
        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertEquals("", result);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_InvalidUrlSyntax_Skipped() {
        // 非法 URI 语法（空格导致 URISyntaxException）
        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://exa mple.com/a.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertEquals("", result);
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_ModelThrowsException_FallsBackToDescription() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("模型调用失败"));

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder()
                               .type("IMAGE")
                               .url("https://example.com/a.png")
                               .description("预设的图片描述")
                               .build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        // 模型失败时，使用 description 兜底
        assertEquals("【图片】预设的图片描述", result);
    }

    @Test
    void testAnalyze_ModelThrowsException_NoDescription_EmptyForThatAttachment() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("模型调用失败"));

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertEquals("", result);
    }

    @Test
    void testAnalyze_PartialFailure_OthersStillProcessed() {
        // 第一个附件失败（带 description），第二个成功
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("失败"))
                .thenReturn(buildChatResponse("成功描述"));

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").description("兜底1").build(),
                MediaAttachment.builder().type("IMAGE").url("https://example.com/b.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertTrue(result.contains("【图片1】兜底1"));
        assertTrue(result.contains("【图片2】成功描述"));
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void testAnalyze_BlankModelResponse_Skipped() {
        // 模型返回空白
        mockChatModelResponse("   ");

        List<MediaAttachment> mediaList = List.of(
                MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build());

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertEquals("", result);
    }

    @Test
    void testAnalyze_NullResponseType_TreatedAsOther() {
        mockChatModelResponse("描述");

        // type 为 null，应走 default 分支，标签为"附件"
        List<MediaAttachment> mediaList = List.of(
                new MediaAttachment(null, "https://example.com/a.png"));

        String result = mediaAnalysisAgent.analyze(1L, "问题", mediaList);

        assertTrue(result.contains("【附件】"));
    }

    // ============ helper ============

    private void mockChatModelResponse(String text) {
        AssistantMessage assistantMessage = new AssistantMessage(text);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    private ChatResponse buildChatResponse(String text) {
        AssistantMessage assistantMessage = new AssistantMessage(text);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }

}
