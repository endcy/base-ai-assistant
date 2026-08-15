package com.endcy.ai.manager;

import cn.hutool.core.collection.CollUtil;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.config.DocumentKeywordEnricher;
import com.endcy.ai.config.DocumentTokenTextSplitter;
import com.endcy.ai.rag.AiDocumentFileLoader;
import com.endcy.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.endcy.ai.repository.service.KnowledgeDocumentService;
import com.endcy.ai.repository.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * VectorStoreManager 单元测试
 *
 * @author endcy
 * @since 2026/04/08
 */
@ExtendWith(MockitoExtension.class)
class VectorStoreManagerTest {

    @Mock
    private AiDocumentFileLoader aiDocumentFileLoader;

    @Mock
    private DocumentTokenTextSplitter tokenTextSplitter;

    @Mock
    private DocumentKeywordEnricher keywordEnricher;

    @Mock
    private KnowledgeDocumentService knowledgeDocumentService;

    @Mock
    private ChatRagProperties chatRagProperties;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private PgVectorStore pgVectorVectorStore;

    @Mock
    private SimpleVectorStore localVectorStore;

    @InjectMocks
    private VectorStoreManager vectorStoreManager;

    @BeforeEach
    void setUp() {
        // 配置 mock 行为（lenient: 本测试类只测 DB 相关方法，不触发本地文档路径）
        lenient().when(chatRagProperties.getEnableLocalDocument()).thenReturn(false);
    }

    @Test
    void testRefreshDbKnowledgeDocument_EmptyResult() {
        // 给定：没有未加载的文档
        when(knowledgeDocumentService.getUnloadedDocuments(0, 100)).thenReturn(CollUtil.newArrayList());

        // 执行测试
        vectorStoreManager.refreshDbKnowledgeDocument();

        // 验证：只调用了一次分页查询（第一页发现为空就停止了）
        verify(knowledgeDocumentService, times(1)).getUnloadedDocuments(0, 100);
        verify(pgVectorVectorStore, never()).add(anyList());
    }

    @Test
    void testRefreshDbKnowledgeDocument_WithDocuments() {
        // 给定：有 2 页未加载的文档
        List<KnowledgeDocumentDTO> page1Docs = createMockDocuments(1, 50);
        List<KnowledgeDocumentDTO> page2Docs = createMockDocuments(51, 30);

        when(knowledgeDocumentService.getUnloadedDocuments(0, 100)).thenReturn(page1Docs);
        when(knowledgeDocumentService.getUnloadedDocuments(1, 100)).thenReturn(page2Docs);
        when(knowledgeDocumentService.getUnloadedDocuments(2, 100)).thenReturn(CollUtil.newArrayList());

        // Mock 文档分割 - 直接返回输入
        when(tokenTextSplitter.splitDocuments(anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<Document> docs = (List<Document>) invocation.getArgument(0);
                    return docs;
                });

        // Mock 关键词增强 - 直接返回输入
        when(keywordEnricher.enrichDocuments(anyList()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<Document> docs = (List<Document>) invocation.getArgument(0);
                    return docs;
                });

        // 执行测试
        vectorStoreManager.refreshDbKnowledgeDocument();

        // 验证：调用了三次分页查询
        verify(knowledgeDocumentService, times(1)).getUnloadedDocuments(0, 100);
        verify(knowledgeDocumentService, times(1)).getUnloadedDocuments(1, 100);
        verify(knowledgeDocumentService, times(1)).getUnloadedDocuments(2, 100);

        // 验证：更新了加载状态
        verify(knowledgeDocumentService, atLeastOnce()).updateDocumentLoadedStatus(anyList(), eq(true));
    }

    @Test
    void testRefreshDbKnowledgeDocument_PaginationWorks() {
        // 给定：超过 100 条文档，需要多页查询
        List<KnowledgeDocumentDTO> largePage = createMockDocuments(1, 100);
        List<KnowledgeDocumentDTO> lastPage = createMockDocuments(101, 20);

        when(knowledgeDocumentService.getUnloadedDocuments(0, 100)).thenReturn(largePage);
        when(knowledgeDocumentService.getUnloadedDocuments(1, 100)).thenReturn(lastPage);
        when(knowledgeDocumentService.getUnloadedDocuments(2, 100)).thenReturn(CollUtil.newArrayList());

        // Mock 文档分割
        when(tokenTextSplitter.splitDocuments(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock 关键词增强
        when(keywordEnricher.enrichDocuments(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        vectorStoreManager.refreshDbKnowledgeDocument();

        // 验证：正确进行了分页查询
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(knowledgeDocumentService, atLeast(3))
                .getUnloadedDocuments(pageCaptor.capture(), sizeCaptor.capture());

        List<Integer> capturedPages = pageCaptor.getAllValues();
        List<Integer> capturedSizes = sizeCaptor.getAllValues();

        // 验证页码从 0 开始递增
        assertEquals(0, capturedPages.get(0));
        assertEquals(1, capturedPages.get(1));
        assertEquals(2, capturedPages.get(2));

        // 验证每页大小一致
        assertTrue(capturedSizes.stream().allMatch(s -> s.equals(100)));
    }

    @Test
    void testIncrementUpdateDocumentVector_DocumentNotFound() {
        // 给定：文档不存在
        when(knowledgeDocumentService.getById(1L)).thenReturn(null);

        // 执行测试
        vectorStoreManager.incrementUpdateDocumentVector(1L);

        // 验证：返回且未调用向量库
        verify(pgVectorVectorStore, never()).add(anyList());
    }

    @Test
    void testIncrementUpdateDocumentVector_Success() {
        // 给定：文档存在
        KnowledgeDocumentDTO mockDoc = createMockDocuments(1, 1).get(0);

        when(knowledgeDocumentService.getById(1L)).thenReturn(mockDoc);

        // Mock 文档分割 — 返回可修改列表（生产代码 removeIf 依赖可变集合）
        when(tokenTextSplitter.splitDocuments(anyList()))
                .thenAnswer(invocation -> new ArrayList<>((List<Document>) invocation.getArgument(0)));

        // Mock 关键词增强
        when(keywordEnricher.enrichDocuments(anyList()))
                .thenAnswer(invocation -> new ArrayList<>((List<Document>) invocation.getArgument(0)));

        // 执行测试
        vectorStoreManager.incrementUpdateDocumentVector(1L);

        // 验证：调用了向量库添加和状态更新
        verify(pgVectorVectorStore, times(1)).add(anyList());
        verify(knowledgeDocumentService, times(1)).updateDocumentLoadedStatus(anyList(), eq(true));
    }

    @Test
    void testRemoveDocumentVector_Success() {
        // 执行测试
        vectorStoreManager.removeDocumentVector(1L);

        // 验证：调用了移除方法
        verify(vectorStoreService, times(1)).removeByDocIds(CollUtil.newHashSet(1L));
    }

    /**
     * 创建模拟文档列表
     */
    private List<KnowledgeDocumentDTO> createMockDocuments(int startId, int count) {
        List<KnowledgeDocumentDTO> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            KnowledgeDocumentDTO doc = new KnowledgeDocumentDTO();
            doc.setId((long) (startId + i));
            doc.setTitle("测试文档" + (startId + i));
            doc.setContent("测试内容" + (startId + i));
            doc.setScopeType("test_scope");
            doc.setBusinessType("test_business");
            doc.setGroupId("1");
            doc.setLoaded(false);
            doc.setEnabled(true);
            docs.add(doc);
        }
        return docs;
    }
}
