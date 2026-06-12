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

    /**
     * 计算用户问题与给定文本内容的BM25相似度得分（不查向量库）
     *
     * @param userQuestion 用户问题
     * @param content      知识文档内容
     * @return 单行结果，仅包含score
     */
    VectorDocument computeContentScore(@Param("userQuestion") String userQuestion, @Param("content") String content);

}
