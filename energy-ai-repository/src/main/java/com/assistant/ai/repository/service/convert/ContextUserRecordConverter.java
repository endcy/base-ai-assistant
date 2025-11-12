package com.assistant.ai.repository.service.convert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.domain.entity.ContextUserRecord;
import com.assistant.service.common.base.PageInfo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContextUserRecordConverter {
    /**
     * DTO转Entity
     *
     * @param dto /
     * @return /
     */
    ContextUserRecord toEntity(ContextUserRecordDTO dto);

    /**
     * Entity转DTO
     *
     * @param entity /
     * @return /
     */
    ContextUserRecordDTO toDto(ContextUserRecord entity);

    /**
     * DTO集合转Entity集合
     *
     * @param dtoList /
     * @return /
     */
    List<ContextUserRecord> toEntity(List<ContextUserRecordDTO> dtoList);

    /**
     * Entity集合转DTO集合
     *
     * @param entityList /
     * @return /
     */
    List<ContextUserRecordDTO> toDto(List<ContextUserRecord> entityList);

    default PageInfo<ContextUserRecordDTO> convertPage(IPage<ContextUserRecord> page) {
        if (page == null) {
            return null;
        }
        PageInfo<ContextUserRecordDTO> pageInfo = new PageInfo<>();
        pageInfo.setTotalElements(page.getTotal());
        pageInfo.setContent(toDto(page.getRecords()));
        return pageInfo;
    }
}
