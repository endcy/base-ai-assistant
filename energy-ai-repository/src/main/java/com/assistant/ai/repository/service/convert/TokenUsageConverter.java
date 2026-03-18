package com.assistant.ai.repository.service.convert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.assistant.ai.repository.domain.dto.TokenUsageDTO;
import com.assistant.ai.repository.domain.entity.TokenUsage;
import com.assistant.service.common.base.PageInfo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Token 用量 Converter
 *
 * @author endcy
 * @since 2026/03/18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TokenUsageConverter {

    /**
     * DTO 转 Entity
     *
     * @param dto /
     * @return /
     */
    TokenUsage toEntity(TokenUsageDTO dto);

    /**
     * Entity 转 DTO
     *
     * @param entity /
     * @return /
     */
    TokenUsageDTO toDto(TokenUsage entity);

    /**
     * DTO 集合转 Entity 集合
     *
     * @param dtoList /
     * @return /
     */
    List<TokenUsage> toEntity(List<TokenUsageDTO> dtoList);

    /**
     * Entity 集合转 DTO 集合
     *
     * @param entityList /
     * @return /
     */
    List<TokenUsageDTO> toDto(List<TokenUsage> entityList);

    default PageInfo<TokenUsageDTO> convertPage(IPage<TokenUsage> page) {
        if (page == null) {
            return null;
        }
        PageInfo<TokenUsageDTO> pageInfo = new PageInfo<>();
        pageInfo.setTotalElements(page.getTotal());
        pageInfo.setContent(toDto(page.getRecords()));
        return pageInfo;
    }
}
