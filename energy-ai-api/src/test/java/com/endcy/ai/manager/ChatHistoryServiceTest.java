package com.endcy.ai.manager;

import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import com.endcy.ai.repository.service.ContextUserRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * ChatHistoryService 单元测试
 * 验证多轮历史加载的正确性：
 * 1. 排除未完成的轮次（answer 为空的当前问题/失败轮次）
 * 2. 按 id 升序保证对话时序
 * 3. 取最近 N 轮窗口
 *
 * @author endcy
 * @since 2026/08/03
 */
@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ContextUserRecordService userRecordService;
    @Mock
    private ChatRagProperties chatRagProperties;

    private ChatHistoryService chatHistoryService;

    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryService(userRecordService, chatRagProperties);
        // 设置 historyMaxRounds 默认值 10
        ReflectionTestUtils.setField(chatHistoryService, "historyMaxRounds", 10);
        // 使用 mock stub 让 getMaxContextRecords() 返回 10（而非 Mock 默认 null）
        when(chatRagProperties.getMaxContextRecords()).thenReturn(10);
    }

    @Test
    void testLoadHistory_EmptyRecords() {
        when(userRecordService.getByChatId(1L)).thenReturn(Collections.emptyList());

        List<Message> result = chatHistoryService.loadHistoryFromDb(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadHistory_NullRecords() {
        when(userRecordService.getByChatId(1L)).thenReturn(null);

        List<Message> result = chatHistoryService.loadHistoryFromDb(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadHistory_ExcludesInProgressTurns() {
        // 模拟：第 1 轮已完成（有 answer），第 2 轮进行中（answer 为空），第 3 轮已完成
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "问题1", "答案1"),
                buildRecord(2L, 100L, "问题2", null),   // 进行中/失败
                buildRecord(3L, 100L, "问题3", "答案3")
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        // 应只有第 1 和第 3 轮
        assertEquals(4, result.size());  // [user1, asst1, user3, asst3]
        assertEquals("问题1", result.get(0).getText());
        assertEquals("答案1", result.get(1).getText());
        assertEquals("问题3", result.get(2).getText());
        assertEquals("答案3", result.get(3).getText());
    }

    @Test
    void testLoadHistory_SortsByIdAscending() {
        // 模拟：记录按乱序返回（DB 无显式排序）
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(3L, 100L, "问题3", "答案3"),
                buildRecord(1L, 100L, "问题1", "答案1"),
                buildRecord(2L, 100L, "问题2", "答案2")
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        // 应按 id 升序：1,2,3
        assertEquals(6, result.size());
        assertEquals("问题1", result.get(0).getText());
        assertEquals("问题2", result.get(2).getText());
        assertEquals("问题3", result.get(4).getText());
    }

    @Test
    void testLoadHistory_WindowingByMaxRounds() {
        // 设置小窗口
        ReflectionTestUtils.setField(chatHistoryService, "historyMaxRounds", 2);

        // 模拟 4 轮已完成
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "Q1", "A1"),
                buildRecord(2L, 100L, "Q2", "A2"),
                buildRecord(3L, 100L, "Q3", "A3"),
                buildRecord(4L, 100L, "Q4", "A4")
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        // 应只取最近 2 轮（Q3, Q4）
        assertEquals(4, result.size());
        assertEquals("Q3", result.get(0).getText());
        assertEquals("A3", result.get(1).getText());
        assertEquals("Q4", result.get(2).getText());
        assertEquals("A4", result.get(3).getText());
    }

    @Test
    void testLoadHistory_WindowingCountsOnlyCompletedTurns() {
        // 窗口为 2，但有 1 轮进行中（不计入窗口）
        ReflectionTestUtils.setField(chatHistoryService, "historyMaxRounds", 2);

        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "Q1", "A1"),  // completed
                buildRecord(2L, 100L, "Q2", null),  // in-progress (excluded)
                buildRecord(3L, 100L, "Q3", "A3"),  // completed
                buildRecord(4L, 100L, "Q4", "A4")   // completed
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        // 完成的 3 轮中取最近 2 轮（Q3, Q4）
        assertEquals(4, result.size());
        assertEquals("Q3", result.get(0).getText());
        assertEquals("Q4", result.get(2).getText());
    }

    @Test
    void testLoadHistory_BlankAnswerExcluded() {
        // 空白字符串 answer 也应被排除（StrUtil.isNotBlank check）
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "Q1", "A1"),
                buildRecord(2L, 100L, "Q2", "   "),  // 空白
                buildRecord(3L, 100L, "Q3", ""),      // 空字符串
                buildRecord(4L, 100L, "Q4", "A4")
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        // 只保留 Q1 和 Q4
        assertEquals(4, result.size());
        assertEquals("Q1", result.get(0).getText());
        assertEquals("Q4", result.get(2).getText());
    }

    @Test
    void testLoadHistory_AllTurnsInProgress_ReturnsEmpty() {
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "Q1", null),
                buildRecord(2L, 100L, "Q2", null)
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadHistory_AssistantMessageCorrectType() {
        List<ContextUserRecordDTO> records = List.of(
                buildRecord(1L, 100L, "Q1", "A1")
        );
        when(userRecordService.getByChatId(100L)).thenReturn(records);

        List<Message> result = chatHistoryService.loadHistoryFromDb(100L);

        assertEquals(2, result.size());
        // 第一条应该是 UserMessage
        assertTrue(result.get(0) instanceof org.springframework.ai.chat.messages.UserMessage);
        // 第二条应该是 AssistantMessage
        assertTrue(result.get(1) instanceof AssistantMessage);
    }

    // ============ helper ============

    private ContextUserRecordDTO buildRecord(Long id, Long chatId, String question, String content) {
        ContextUserRecordDTO record = new ContextUserRecordDTO();
        record.setId(id);
        record.setChatId(chatId);
        record.setQuestion(question);
        record.setContent(content);
        return record;
    }

}
