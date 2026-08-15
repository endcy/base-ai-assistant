package com.endcy.ai.util;

import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * UserChatPromptUtils 单元测试
 * 覆盖：纯文本、mediaContext 注入、mediaList 透传、遗留单参重载
 *
 * @author endcy
 * @since 2026/08/03
 */
class UserChatPromptUtilsTest {

    @Test
    void testGeneratePrompt_TextOnly_NoMedia() {
        KnowledgeAIQueryParam query = buildQuery("你好", null);
        ChatClient.PromptUserSpec spec = mockSpec();

        Consumer<ChatClient.PromptUserSpec> consumer = UserChatPromptUtils.generatePromptUserSpecConsumer(query);
        consumer.accept(spec);

        verify(spec).text("你好");
        verify(spec, never()).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_WithMediaContext_TextAppended_NoMediaObjects() {
        KnowledgeAIQueryParam query = buildQuery("这是什么故障？",
                List.of(MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build()));
        ChatClient.PromptUserSpec spec = mockSpec();

        String mediaContext = "【图片】这是充电桩故障码E001的截图";
        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query, mediaContext);
        consumer.accept(spec);

        // 验证 text 拼接了 mediaContext
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(spec).text(textCaptor.capture());
        String captured = textCaptor.getValue();
        assertTrue(captured.contains("这是什么故障？"));
        assertTrue(captured.contains(mediaContext));
        assertTrue(captured.contains("多媒体文件内容描述"));

        // 启用前置解析时，不再传 Media 对象
        verify(spec, never()).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_NoMediaContext_MediaListPassedThrough() {
        KnowledgeAIQueryParam query = buildQuery("这是什么？",
                List.of(MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build()));
        ChatClient.PromptUserSpec spec = mockSpec();

        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query, null);
        consumer.accept(spec);

        // 走原有逻辑：传 text + media
        verify(spec).text("这是什么？");
        verify(spec, times(1)).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_EmptyMediaContext_FallsBackToOriginalLogic() {
        KnowledgeAIQueryParam query = buildQuery("这是什么？",
                List.of(MediaAttachment.builder().type("IMAGE").url("https://example.com/a.png").build()));
        ChatClient.PromptUserSpec spec = mockSpec();

        // 空字符串等价于未启用
        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query, "");
        consumer.accept(spec);

        verify(spec).text("这是什么？");
        verify(spec, times(1)).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_LegacySingleArgOverload() {
        KnowledgeAIQueryParam query = buildQuery("你好", null);
        ChatClient.PromptUserSpec spec = mockSpec();

        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query);
        consumer.accept(spec);

        verify(spec).text("你好");
        verify(spec, never()).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_MediaContextWithInvalidMediaUrl_StillInjectsText() {
        // 即使 mediaList 的 URL 非法，只要 mediaContext 非空，就只走文本拼接分支
        KnowledgeAIQueryParam query = buildQuery("这是什么？",
                List.of(MediaAttachment.builder().type("IMAGE").url("非法 url").build()));
        ChatClient.PromptUserSpec spec = mockSpec();

        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query, "【图片】解析文本");
        consumer.accept(spec);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(spec).text(textCaptor.capture());
        assertTrue(textCaptor.getValue().contains("解析文本"));
        // 不会触发 buildMedia，因此不会因非法 URL 抛异常
        verify(spec, never()).media(any(Media[].class));
    }

    @Test
    void testGeneratePrompt_NoMediaContext_InvalidUrl_ThrowsAsBefore() {
        // 未启用前置解析时，非法 URL 走 buildMedia 会抛 URISyntaxException→RuntimeException
        KnowledgeAIQueryParam query = buildQuery("这是什么？",
                List.of(MediaAttachment.builder().type("IMAGE").url("非法 url").build()));

        Consumer<ChatClient.PromptUserSpec> consumer =
                UserChatPromptUtils.generatePromptUserSpecConsumer(query, null);

        assertThrows(RuntimeException.class, () -> consumer.accept(mock(ChatClient.PromptUserSpec.class)));
    }

    // ============ helper ============

    private KnowledgeAIQueryParam buildQuery(String question, List<MediaAttachment> mediaList) {
        KnowledgeAIQueryParam query = new KnowledgeAIQueryParam();
        query.setQuestion(question);
        query.setMediaList(mediaList);
        return query;
    }

    /**
     * mock PromptUserSpec：其 fluent 方法返回自身，便于 Mockito 校验
     */
    private ChatClient.PromptUserSpec mockSpec() {
        ChatClient.PromptUserSpec spec = mock(ChatClient.PromptUserSpec.class);
        when(spec.text(anyString())).thenReturn(spec);
        lenient().when(spec.media(any(Media[].class))).thenReturn(spec);
        return spec;
    }

}
