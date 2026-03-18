package com.assistant.ai.repository.service.convert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.domain.entity.KnowledgeCategoryConfig;
import com.assistant.service.common.base.PageInfo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 知识分类配置 Converter
 *
 * @author endcy
 * @since 2026/03/18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KnowledgeCategoryConfigConverter {

    /**
     * DTO 转 Entity
     */
    KnowledgeCategoryConfig toEntity(KnowledgeCategoryConfigDTO dto);

    /**
     * Entity 转 DTO
     */
    KnowledgeCategoryConfigDTO toDto(KnowledgeCategoryConfig entity);

    /**
     * DTO 集合转 Entity 集合
     */
    List<KnowledgeCategoryConfig> toEntity(List<KnowledgeCategoryConfigDTO> dtoList);

    /**
     * Entity 集合转 DTO 集合
     */
    List<KnowledgeCategoryConfigDTO> toDto(List<KnowledgeCategoryConfig> entityList);

    /**
     * 分页转换
     */
    default PageInfo<KnowledgeCategoryConfigDTO> convertPage(IPage<KnowledgeCategoryConfig> page) {
        if (page == null) {
            return null;
        }
        PageInfo<KnowledgeCategoryConfigDTO> pageInfo = new PageInfo<>();
        pageInfo.setTotalElements(page.getTotal());
        pageInfo.setContent(toDto(page.getRecords()));
        return pageInfo;
    }
}
