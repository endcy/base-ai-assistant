package com.endcy.ai.repository.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.endcy.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.endcy.ai.repository.domain.entity.KnowledgeDocument;
import com.endcy.ai.repository.domain.query.KnowledgeDocumentQueryParam;
import com.endcy.ai.repository.service.convert.KnowledgeDocumentConverter;
import com.endcy.ai.repository.service.impl.KnowledgeDocumentServiceImpl;
import com.endcy.ai.repository.trans.mapper.KnowledgeDocumentMapper;
import com.endcy.service.common.base.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KnowledgeDocumentService 单元测试
 *
 * @author endcy
 * @since 2026/04/08
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {

    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Mock
    private KnowledgeDocumentConverter knowledgeDocumentConverter;

    @InjectMocks
    private KnowledgeDocumentServiceImpl knowledgeDocumentService;

    private List<KnowledgeDocumentDTO> mockDtoList;
    private List<KnowledgeDocument> mockEntityList;

    @BeforeEach
    void setUp() {
        mockDtoList = new ArrayList<>();
        mockEntityList = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
            dto.setId((long) i);
            dto.setTitle("文档" + i);
            dto.setContent("内容" + i);
            dto.setScopeType("test_scope");
            dto.setBusinessType("test_business");
            dto.setGroupId("1");
            dto.setLoaded(i % 2 == 0);
            dto.setEnabled(true);
            mockDtoList.add(dto);

            KnowledgeDocument entity = new KnowledgeDocument();
            entity.setId((long) i);
            entity.setTitle("文档" + i);
            entity.setContent("内容" + i);
            entity.setScopeType("test_scope");
            entity.setBusinessType("test_business");
            entity.setGroupId("1");
            entity.setLoaded(i % 2 == 0);
            entity.setEnabled(true);
            mockEntityList.add(entity);
        }
    }

    @Test
    void testGetUnloadedDocuments_Pagination() {
        // 给定
        when(knowledgeDocumentMapper.selectList(any())).thenReturn(mockEntityList);
        when(knowledgeDocumentConverter.toDto(mockEntityList)).thenReturn(mockDtoList);

        // 执行测试 - 第一页
        List<KnowledgeDocumentDTO> result = knowledgeDocumentService.getUnloadedDocuments(0, 100);

        // 验证
        assertNotNull(result);
        verify(knowledgeDocumentMapper, times(1)).selectList(any());
    }

    @Test
    void testQueryAll_WithPageable() {
        // 给定
        KnowledgeDocumentQueryParam query = new KnowledgeDocumentQueryParam();
        query.setGroupId("1");
        Pageable pageable = PageRequest.of(0, 10);

        IPage<KnowledgeDocument> mockPage = mock(IPage.class);
        when(mockPage.getTotal()).thenReturn(5L);
        when(mockPage.getRecords()).thenReturn(mockEntityList);

        when(knowledgeDocumentMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(knowledgeDocumentConverter.convertPage(mockPage)).thenReturn(new PageInfo<>());

        // 执行测试
        PageInfo<KnowledgeDocumentDTO> result = knowledgeDocumentService.queryAll(query, pageable);

        // 验证
        assertNotNull(result);
        verify(knowledgeDocumentMapper, times(1)).selectPage(any(), any());
    }

    @Test
    void testQueryAll_WithoutPageable() {
        // 给定
        KnowledgeDocumentQueryParam query = new KnowledgeDocumentQueryParam();
        query.setGroupId("1");

        when(knowledgeDocumentMapper.selectList(any())).thenReturn(mockEntityList);
        when(knowledgeDocumentConverter.toDto(mockEntityList)).thenReturn(mockDtoList);

        // 执行测试
        List<KnowledgeDocumentDTO> result = knowledgeDocumentService.queryAll(query);

        // 验证
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    void testGetById() {
        // 给定
        Long id = 1L;

        when(knowledgeDocumentMapper.selectById(id)).thenReturn(mockEntityList.getFirst());
        when(knowledgeDocumentConverter.toDto(mockEntityList.getFirst())).thenReturn(mockDtoList.getFirst());

        // 执行测试
        KnowledgeDocumentDTO result = knowledgeDocumentService.getById(id);

        // 验证
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void testInsert() {
        // 给定
        KnowledgeDocumentDTO dto = mockDtoList.getFirst();

        when(knowledgeDocumentConverter.toEntity(dto)).thenReturn(mockEntityList.getFirst());
        when(knowledgeDocumentMapper.insert(mockEntityList.getFirst())).thenReturn(1);

        // 执行测试
        int result = knowledgeDocumentService.insert(dto);

        // 验证
        assertEquals(1, result);
    }

    @Test
    void testUpdateById() {
        // 给定
        KnowledgeDocumentDTO dto = mockDtoList.getFirst();

        when(knowledgeDocumentConverter.toEntity(dto)).thenReturn(mockEntityList.getFirst());
        when(knowledgeDocumentMapper.updateById(mockEntityList.getFirst())).thenReturn(1);

        // 执行测试
        int result = knowledgeDocumentService.updateById(dto);

        // 验证
        assertEquals(1, result);
    }

    @Test
    void testRemoveByIds() {
        // 给定
        Set<Long> ids = CollUtil.newHashSet(1L, 2L);

        when(knowledgeDocumentMapper.delete(any())).thenReturn(2);

        // 执行测试
        int result = knowledgeDocumentService.removeByIds(ids);

        // 验证
        assertEquals(2, result);
    }

    @Test
    void testUpdateDocumentLoadedStatus() {
        // 给定
        List<Long> ids = CollUtil.newArrayList(1L, 2L);
        Boolean status = true;

        when(knowledgeDocumentMapper.update(null, any())).thenReturn(2);

        // 执行测试
        int result = knowledgeDocumentService.updateDocumentLoadedStatus(ids, status);

        // 验证
        assertEquals(2, result);
    }
}
