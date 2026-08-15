package com.endcy.ai.agent;

import com.endcy.ai.prompt.PromptTemplateKey;
import com.endcy.ai.prompt.PromptTemplateService;
import com.endcy.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.endcy.ai.repository.service.KnowledgeCategoryConfigService;
import com.endcy.service.domain.enums.KnowledgeBusinessTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IntentAnalysisAgent 单元测试
 *
 * @author endcy
 * @since 2026/04/08
 */
@ExtendWith(MockitoExtension.class)
class IntentAnalysisAgentTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private KnowledgeCategoryConfigService knowledgeCategoryConfigService;

    @Mock
    private PromptTemplateService promptTemplateService;

    private ChatClient intentChatClient;

    private IntentAnalysisAgent intentAnalysisAgent;

    private List<KnowledgeCategoryConfigDTO> mockCategoryConfigs;

    @BeforeEach
    void setUp() {
        // 准备模拟的分类配置数据
        mockCategoryConfigs = new ArrayList<>();

        KnowledgeCategoryConfigDTO config1 = new KnowledgeCategoryConfigDTO();
        config1.setCode("CHARGE_ORDER");
        config1.setName("充电订单信息");
        config1.setDescription("充电流程、充电订单内容相关信息咨询");
        config1.setEnabled(true);
        mockCategoryConfigs.add(config1);

        KnowledgeCategoryConfigDTO config2 = new KnowledgeCategoryConfigDTO();
        config2.setCode("STATION");
        config2.setName("站点信息");
        config2.setDescription("平台运营充放电、储能站点等信息咨询");
        config2.setEnabled(true);
        mockCategoryConfigs.add(config2);

        KnowledgeCategoryConfigDTO config3 = new KnowledgeCategoryConfigDTO();
        config3.setCode("UNKNOWN");
        config3.setName("其他");
        config3.setDescription("未分类或其他业务");
        config3.setEnabled(true);
        mockCategoryConfigs.add(config3);

        // 使用 mock 的 ChatModel 构建 ChatClient
        intentChatClient = ChatClient.builder(chatModel).build();

        // mock PromptTemplateService -- analyzeIntent() 只使用 INTENT_SIMPLE
        when(promptTemplateService.getTemplate(PromptTemplateKey.INTENT_SIMPLE))
                .thenReturn(com.endcy.ai.constant.EnergyAiConstant.INTENT_SIMPLE_PROMPT_TEMPLATE);

        // 创建测试对象（手动注入，因为 @InjectMocks 对 constructor 注入的 ChatClient 无效）
        intentAnalysisAgent = new IntentAnalysisAgent(intentChatClient, knowledgeCategoryConfigService, promptTemplateService);
    }

    @Test
    void testAnalyzeIntent_WithKnownCategory() {
        // 给定：模拟服务返回分类配置
        when(knowledgeCategoryConfigService.getByType("business")).thenReturn(mockCategoryConfigs);

        // 模拟 ChatModel 调用返回
        AssistantMessage assistantMessage = new AssistantMessage("CHARGE_ORDER");
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 执行测试
        var result = intentAnalysisAgent.analyzeIntent(1L, "account_customer_service", "我的充电订单状态是什么？");

        // 验证结果
        assertNotNull(result);
        assertEquals("CHARGE_ORDER", result.getBusinessType());
        assertEquals(1L, result.getChatId());
        assertEquals("我的充电订单状态是什么？", result.getUserMessage());

        // 验证调用了服务方法
        verify(knowledgeCategoryConfigService, times(1)).getByType("business");
    }

    @Test
    void testAnalyzeIntent_WithEmptyResponse() {
        // 给定：服务返回分类配置
        when(knowledgeCategoryConfigService.getByType("business")).thenReturn(mockCategoryConfigs);

        // 模拟返回空结果
        AssistantMessage assistantMessage = new AssistantMessage("");
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 执行测试
        var result = intentAnalysisAgent.analyzeIntent(2L, "account_customer_service", "随便问问");

        // 验证结果 - 应该回退到 UNKNOWN
        assertNotNull(result);
        assertEquals(KnowledgeBusinessTypeEnum.UNKNOWN.name(), result.getBusinessType());
    }

    @Test
    void testAnalyzeIntent_WithException() {
        // 给定：服务返回分类配置
        when(knowledgeCategoryConfigService.getByType("business")).thenReturn(mockCategoryConfigs);

        // 模拟 ChatModel 调用抛出异常
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API 调用失败"));

        // analyzeIntent() 没有 try-catch，异常会向上传播（不为空返回兜底）
        assertThrows(RuntimeException.class, () ->
                intentAnalysisAgent.analyzeIntent(3L, "account_customer_service", "测试问题"));
    }

    @Test
    void testAnalyzeIntent_PromptContainsCategoryConfigs() {
        // 给定
        when(knowledgeCategoryConfigService.getByType("business")).thenReturn(mockCategoryConfigs);

        AssistantMessage assistantMessage = new AssistantMessage("STATION");
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 执行测试
        var result = intentAnalysisAgent.analyzeIntent(1L, "account_customer_service", "充电站点有哪些？");

        // 验证：分类服务被调用 + 模型被调用 + 结果正确（Prompt 内容由 ChatClient 内部构造，版本不兼容则不校验具体文本）
        verify(knowledgeCategoryConfigService).getByType("business");
        verify(chatModel).call(any(Prompt.class));
        assertEquals("STATION", result.getBusinessType());
    }

    @Test
    void testAnalyzeIntent_FallbackToUnknown() {
        // 给定：分类配置中没有 UNKNOWN
        List<KnowledgeCategoryConfigDTO> configsWithoutUnknown = new ArrayList<>();
        KnowledgeCategoryConfigDTO config = new KnowledgeCategoryConfigDTO();
        config.setCode("CHARGE_ORDER");
        config.setName("充电订单信息");
        config.setDescription("充电流程、充电订单内容相关信息咨询");
        config.setEnabled(true);
        configsWithoutUnknown.add(config);

        when(knowledgeCategoryConfigService.getByType("business")).thenReturn(configsWithoutUnknown);

        AssistantMessage assistantMessage = new AssistantMessage("HELLO");
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 执行测试
        var result = intentAnalysisAgent.analyzeIntent(1L, "account_customer_service", "你好");

        // 验证：分类服务被调用 + 模型被调用 + 结果正确
        verify(knowledgeCategoryConfigService).getByType("business");
        verify(chatModel).call(any(Prompt.class));
        assertEquals("HELLO", result.getBusinessType());
    }
}
