package com.endcy.ai.repository.trans.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.endcy.ai.repository.domain.entity.TokenUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Token 用量 Mapper
 *
 * @author endcy
 * @since 2026/03/18
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsage> {
}
