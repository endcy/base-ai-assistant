package com.assistant.ai.repository.pgsql.mapper;

import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.service.common.base.CommonMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ...
 *
 * @author endcy
 * @date 2025/8/6 20:58:50
 */
@Repository
public interface VectorDocumentMapper extends CommonMapper<VectorDocument> {
    List<VectorDocument> retrieveWithTsQuery(@Param("params") DocumentQueryContext params, int topK, double similarityThreshold);

}
