package com.assistant.ai.repository.service.convert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.entity.KnowledgeDocument;
import com.assistant.service.common.base.PageInfo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KnowledgeDocumentConverter {
    /**
     * DTO转Entity
     *
     * @param dto /
     * @return /
     */
    KnowledgeDocument toEntity(KnowledgeDocumentDTO dto);

    /**
     * Entity转DTO
     *
     * @param entity /
     * @return /
     */
    KnowledgeDocumentDTO toDto(KnowledgeDocument entity);

    /**
     * DTO集合转Entity集合
     *
     * @param dtoList /
     * @return /
     */
    List<KnowledgeDocument> toEntity(List<KnowledgeDocumentDTO> dtoList);

    /**
     * Entity集合转DTO集合
     *
     * @param entityList /
     * @return /
     */
    List<KnowledgeDocumentDTO> toDto(List<KnowledgeDocument> entityList);

    default PageInfo<KnowledgeDocumentDTO> convertPage(IPage<KnowledgeDocument> page) {
        if (page == null) {
            return null;
        }
        PageInfo<KnowledgeDocumentDTO> pageInfo = new PageInfo<>();
        pageInfo.setTotalElements(page.getTotal());
        pageInfo.setContent(toDto(page.getRecords()));
        return pageInfo;
    }
}
