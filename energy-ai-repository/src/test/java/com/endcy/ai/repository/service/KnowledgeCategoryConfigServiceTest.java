package com.endcy.ai.repository.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.endcy.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.endcy.ai.repository.domain.entity.KnowledgeCategoryConfig;
import com.endcy.ai.repository.domain.query.KnowledgeCategoryQueryParam;
import com.endcy.ai.repository.service.convert.KnowledgeCategoryConfigConverter;
import com.endcy.ai.repository.service.impl.KnowledgeCategoryConfigServiceImpl;
import com.endcy.ai.repository.trans.mapper.KnowledgeCategoryConfigMapper;
import com.endcy.service.common.base.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KnowledgeCategoryConfigService 单元测试
 *
 * @author endcy
 * @since 2026/04/08
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeCategoryConfigServiceTest {

    @Mock
    private KnowledgeCategoryConfigMapper knowledgeCategoryConfigMapper;

    @Mock
    private KnowledgeCategoryConfigConverter knowledgeCategoryConfigConverter;

    @InjectMocks
    private KnowledgeCategoryConfigServiceImpl knowledgeCategoryConfigService;

    private List<KnowledgeCategoryConfigDTO> mockDtoList;
    private List<KnowledgeCategoryConfig> mockEntityList;

    @BeforeEach
    void setUp() {
        // 准备模拟数据
        mockDtoList = new ArrayList<>();
        mockEntityList = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            KnowledgeCategoryConfigDTO dto = new KnowledgeCategoryConfigDTO();
            dto.setId((long) i);
            dto.setCode("CODE_" + i);
            dto.setName("名称" + i);
            dto.setEnabled(true);
            dto.setSortOrder(i);
            mockDtoList.add(dto);

            KnowledgeCategoryConfig entity = new KnowledgeCategoryConfig();
            entity.setId((long) i);
            entity.setCode("CODE_" + i);
            entity.setName("名称" + i);
            entity.setEnabled(true);
            entity.setSortOrder(i);
            mockEntityList.add(entity);
        }
    }

    @Test
    void testQueryAll_WithPageable() {
        // 给定
        KnowledgeCategoryQueryParam query = new KnowledgeCategoryQueryParam();
        Pageable pageable = PageRequest.of(0, 10);

        IPage<KnowledgeCategoryConfig> mockPage = mock(IPage.class);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getRecords()).thenReturn(mockEntityList);

        when(knowledgeCategoryConfigMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(knowledgeCategoryConfigConverter.convertPage(mockPage)).thenReturn(new PageInfo<>());

        // 执行测试
        PageInfo<KnowledgeCategoryConfigDTO> result = knowledgeCategoryConfigService.queryAll(query, pageable);

        // 验证
        assertNotNull(result);
        verify(knowledgeCategoryConfigMapper, times(1)).selectPage(any(), any());
        verify(knowledgeCategoryConfigConverter, times(1)).convertPage(mockPage);
    }

    @Test
    void testQueryAll_WithoutPageable() {
        // 给定
        KnowledgeCategoryQueryParam query = new KnowledgeCategoryQueryParam();

        when(knowledgeCategoryConfigMapper.selectList(any())).thenReturn(mockEntityList);
        when(knowledgeCategoryConfigConverter.toDto(mockEntityList)).thenReturn(mockDtoList);

        // 执行测试
        List<KnowledgeCategoryConfigDTO> result = knowledgeCategoryConfigService.queryAll(query);

        // 验证
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(knowledgeCategoryConfigMapper, times(1)).selectList(any());
    }

    @Test
    void testGetById() {
        // 给定
        Long id = 1L;
        KnowledgeCategoryConfig mockEntity = mockEntityList.get(0);
        KnowledgeCategoryConfigDTO mockDto = mockDtoList.get(0);

        when(knowledgeCategoryConfigMapper.selectById(id)).thenReturn(mockEntity);
        when(knowledgeCategoryConfigConverter.toDto(mockEntity)).thenReturn(mockDto);

        // 执行测试
        KnowledgeCategoryConfigDTO result = knowledgeCategoryConfigService.getById(id);

        // 验证
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(knowledgeCategoryConfigMapper, times(1)).selectById(id);
    }

    @Test
    void testGetEnabledCategories() {

        when(knowledgeCategoryConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockEntityList);
        when(knowledgeCategoryConfigConverter.toDto(mockEntityList)).thenReturn(mockDtoList);

        // 执行测试
        List<KnowledgeCategoryConfigDTO> result = knowledgeCategoryConfigService.getByType("business");

        // 验证
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(knowledgeCategoryConfigMapper, times(1)).selectList(any(LambdaQueryWrapper.class));

        // 验证查询条件包含了 type 和 enabled 过滤
        ArgumentCaptor<LambdaQueryWrapper<KnowledgeCategoryConfig>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeCategoryConfigMapper).selectList(wrapperCaptor.capture());

        // 由于无法直接验证 LambdaQueryWrapper 的内容，至少验证了方法被调用
    }

    @Test
    void testInsert() {
        // 给定
        KnowledgeCategoryConfigDTO dto = mockDtoList.get(0);
        KnowledgeCategoryConfig entity = mockEntityList.get(0);

        when(knowledgeCategoryConfigConverter.toEntity(dto)).thenReturn(entity);
        when(knowledgeCategoryConfigMapper.insert(entity)).thenReturn(1);

        // 执行测试
        int result = knowledgeCategoryConfigService.insert(dto);

        // 验证
        assertEquals(1, result);
        verify(knowledgeCategoryConfigConverter, times(1)).toEntity(dto);
        verify(knowledgeCategoryConfigMapper, times(1)).insert(entity);
    }

    @Test
    void testUpdateById() {
        // 给定
        KnowledgeCategoryConfigDTO dto = mockDtoList.get(0);
        KnowledgeCategoryConfig entity = mockEntityList.get(0);

        when(knowledgeCategoryConfigConverter.toEntity(dto)).thenReturn(entity);
        when(knowledgeCategoryConfigMapper.updateById(entity)).thenReturn(1);

        // 执行测试
        int result = knowledgeCategoryConfigService.updateById(dto);

        // 验证
        assertEquals(1, result);
        verify(knowledgeCategoryConfigConverter, times(1)).toEntity(dto);
        verify(knowledgeCategoryConfigMapper, times(1)).updateById(entity);
    }

    @Test
    void testUpdateById_WithNullId() {
        // 给定
        KnowledgeCategoryConfigDTO dto = new KnowledgeCategoryConfigDTO();
        dto.setId(null);

        // 执行测试
        int result = knowledgeCategoryConfigService.updateById(dto);

        // 验证
        assertEquals(0, result);
        verify(knowledgeCategoryConfigMapper, never()).updateById((KnowledgeCategoryConfig) any());
    }

    @Test
    void testRemoveByIds() {
        // 给定
        List<Long> ids = CollUtil.newArrayList(1L, 2L, 3L);

        when(knowledgeCategoryConfigMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);

        // 执行测试
        int result = knowledgeCategoryConfigService.removeByIds(ids);

        // 验证
        assertEquals(3, result);
        verify(knowledgeCategoryConfigMapper, times(1)).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void testRemoveByIds_EmptyIds() {
        // 给定
        List<Long> ids = CollUtil.newArrayList();

        // 执行测试
        int result = knowledgeCategoryConfigService.removeByIds(ids);

        // 验证
        assertEquals(0, result);
        verify(knowledgeCategoryConfigMapper, never()).delete(any());
    }

    @Test
    void testUpdateEnabledStatus() {
        // 给定
        List<Long> ids = CollUtil.newArrayList(1L, 2L);
        Boolean enabled = false;

        when(knowledgeCategoryConfigMapper.update(null, any(LambdaUpdateWrapper.class))).thenReturn(2);

        // 执行测试
        knowledgeCategoryConfigService.updateEnabledStatus(ids, enabled);

        // 验证
        verify(knowledgeCategoryConfigMapper, times(1)).update(null, any(LambdaUpdateWrapper.class));
    }
}
