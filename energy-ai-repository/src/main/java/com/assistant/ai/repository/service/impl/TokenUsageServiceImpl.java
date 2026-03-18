package com.assistant.ai.repository.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.assistant.ai.repository.domain.dto.TokenUsageDTO;
import com.assistant.ai.repository.domain.entity.TokenUsage;
import com.assistant.ai.repository.domain.request.TokenUsageQueryParam;
import com.assistant.ai.repository.service.TokenUsageService;
import com.assistant.ai.repository.service.convert.TokenUsageConverter;
import com.assistant.ai.repository.trans.mapper.TokenUsageMapper;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.utils.PageUtil;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Token 用量服务实现类
 *
 * @author endcy
 * @since 2026/03/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "tokenUsageCache")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TokenUsageServiceImpl implements TokenUsageService {

    private final TokenUsageMapper tokenUsageMapper;
    private final TokenUsageConverter tokenUsageConverter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<TokenUsageDTO> getByChatId(Long chatId) {
        LambdaQueryWrapper<TokenUsage> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TokenUsage::getChatId, chatId);
        return tokenUsageConverter.toDto(tokenUsageMapper.selectList(queryWrapper));
    }

    @Override
    public List<TokenUsageDTO> getByUserId(Long userId, Integer userType, Long chatId) {
        LambdaQueryWrapper<TokenUsage> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TokenUsage::getUserId, userId)
                    .eq(userType != null, TokenUsage::getUserType, userType)
                    .eq(chatId != null, TokenUsage::getChatId, chatId);
        return tokenUsageConverter.toDto(tokenUsageMapper.selectList(queryWrapper));
    }

    @Override
    public PageInfo<TokenUsageDTO> queryAll(TokenUsageQueryParam query, Pageable pageable) {
        IPage<TokenUsage> queryPage = PageUtil.toMybatisPage(pageable);
        IPage<TokenUsage> page = tokenUsageMapper.selectPage(queryPage, QueryHelpMybatisPlus.getPredicateSimple(query));
        return tokenUsageConverter.convertPage(page);
    }

    @Override
    public List<TokenUsageDTO> queryAll(TokenUsageQueryParam query) {
        return tokenUsageConverter.toDto(tokenUsageMapper.selectList(QueryHelpMybatisPlus.getPredicateSimple(query)));
    }

    @Override
    public TokenUsageDTO getById(Long id) {
        return tokenUsageConverter.toDto(tokenUsageMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(TokenUsageDTO dto) {
        TokenUsage entity = tokenUsageConverter.toEntity(dto);
        int ret = tokenUsageMapper.insert(entity);
        if (ret > 0) {
            dto.setId(entity.getId());
        }
        return ret;
    }

    @Override
    public TokenUsageStatsDTO getStats(TokenUsageQueryParam query) {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                COUNT(*) as total_request_count,
                COALESCE(SUM(input_tokens), 0) as total_input_tokens,
                COALESCE(SUM(output_tokens), 0) as total_output_tokens,
                COALESCE(SUM(total_tokens), 0) as total_tokens,
                COALESCE(AVG(request_cost_ms), 0) as avg_request_cost_ms
            FROM ai_token_usage WHERE 1=1
        """);

        List<Object> params = new java.util.ArrayList<>();

        if (query.getChatId() != null) {
            sqlBuilder.append(" AND chat_id = ?");
            params.add(query.getChatId());
        }
        if (query.getUserId() != null) {
            sqlBuilder.append(" AND user_id = ?");
            params.add(query.getUserId());
        }
        if (query.getUserType() != null) {
            sqlBuilder.append(" AND user_type = ?");
            params.add(query.getUserType());
        }
        if (query.getGroupId() != null) {
            sqlBuilder.append(" AND group_id = ?");
            params.add(query.getGroupId());
        }
        if (StrUtil.isNotBlank(query.getScopeType())) {
            sqlBuilder.append(" AND scope_type = ?");
            params.add(query.getScopeType());
        }
        if (StrUtil.isNotBlank(query.getBusinessType())) {
            sqlBuilder.append(" AND business_type = ?");
            params.add(query.getBusinessType());
        }
        if (StrUtil.isNotBlank(query.getModelName())) {
            sqlBuilder.append(" AND model_name = ?");
            params.add(query.getModelName());
        }
        if (query.getStartTime() != null) {
            sqlBuilder.append(" AND request_date >= ?");
            params.add(query.getStartTime());
        }
        if (query.getEndTime() != null) {
            sqlBuilder.append(" AND request_date <= ?");
            params.add(query.getEndTime());
        }
        if (query.getStatus() != null) {
            sqlBuilder.append(" AND status = ?");
            params.add(query.getStatus());
        }

        Map<String, Object> result = jdbcTemplate.queryForMap(sqlBuilder.toString(), params.toArray());

        return new TokenUsageStatsDTO() {
            @Override
            public Long getTotalRequestCount() {
                return ((Number) result.get("total_request_count")).longValue();
            }
            @Override
            public Integer getTotalInputTokens() {
                return ((Number) result.get("total_input_tokens")).intValue();
            }
            @Override
            public Integer getTotalOutputTokens() {
                return ((Number) result.get("total_output_tokens")).intValue();
            }
            @Override
            public Integer getTotalTokens() {
                return ((Number) result.get("total_tokens")).intValue();
            }
            @Override
            public Double getAvgRequestCostMs() {
                return ((Number) result.get("avg_request_cost_ms")).doubleValue();
            }
        };
    }

    @Override
    public List<TokenUsageDailyStatsDTO> getDailyStats(TokenUsageQueryParam query) {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                request_date,
                model_name,
                scope_type,
                business_type,
                COUNT(*) as request_count,
                SUM(input_tokens) as total_input_tokens,
                SUM(output_tokens) as total_output_tokens,
                SUM(total_tokens) as total_tokens,
                AVG(request_cost_ms) as avg_request_cost_ms
            FROM ai_token_usage WHERE 1=1
        """);

        List<Object> params = new java.util.ArrayList<>();

        if (query.getUserId() != null) {
            sqlBuilder.append(" AND user_id = ?");
            params.add(query.getUserId());
        }
        if (query.getGroupId() != null) {
            sqlBuilder.append(" AND group_id = ?");
            params.add(query.getGroupId());
        }
        if (StrUtil.isNotBlank(query.getScopeType())) {
            sqlBuilder.append(" AND scope_type = ?");
            params.add(query.getScopeType());
        }
        if (StrUtil.isNotBlank(query.getModelName())) {
            sqlBuilder.append(" AND model_name = ?");
            params.add(query.getModelName());
        }
        if (query.getStartTime() != null) {
            sqlBuilder.append(" AND request_date >= ?");
            params.add(query.getStartTime());
        }
        if (query.getEndTime() != null) {
            sqlBuilder.append(" AND request_date <= ?");
            params.add(query.getEndTime());
        }

        sqlBuilder.append(" GROUP BY request_date, model_name, scope_type, business_type ORDER BY request_date DESC");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

        return results.stream()
                .map(row -> new TokenUsageDailyStatsDTO() {
                    @Override
                    public String getRequestDate() {
                        return row.get("request_date").toString();
                    }
                    @Override
                    public String getModelName() {
                        Object val = row.get("model_name");
                        return val != null ? val.toString() : null;
                    }
                    @Override
                    public String getScopeType() {
                        Object val = row.get("scope_type");
                        return val != null ? val.toString() : null;
                    }
                    @Override
                    public Long getRequestCount() {
                        return ((Number) row.get("request_count")).longValue();
                    }
                    @Override
                    public Integer getTotalInputTokens() {
                        return ((Number) row.get("total_input_tokens")).intValue();
                    }
                    @Override
                    public Integer getTotalOutputTokens() {
                        return ((Number) row.get("total_output_tokens")).intValue();
                    }
                    @Override
                    public Integer getTotalTokens() {
                        return ((Number) row.get("total_tokens")).intValue();
                    }
                    @Override
                    public Double getAvgRequestCostMs() {
                        Object val = row.get("avg_request_cost_ms");
                        return val != null ? ((Number) val).doubleValue() : 0.0;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<TokenUsageUserStatsDTO> getUserStats(TokenUsageQueryParam query) {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                user_id,
                user_type,
                group_id,
                COUNT(*) as request_count,
                SUM(total_tokens) as total_tokens,
                MAX(create_time) as last_request_time
            FROM ai_token_usage WHERE 1=1
        """);

        List<Object> params = new java.util.ArrayList<>();

        if (query.getGroupId() != null) {
            sqlBuilder.append(" AND group_id = ?");
            params.add(query.getGroupId());
        }
        if (query.getStartTime() != null) {
            sqlBuilder.append(" AND request_date >= ?");
            params.add(query.getStartTime());
        }
        if (query.getEndTime() != null) {
            sqlBuilder.append(" AND request_date <= ?");
            params.add(query.getEndTime());
        }

        sqlBuilder.append(" GROUP BY user_id, user_type, group_id ORDER BY total_tokens DESC");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());

        return results.stream()
                .map(row -> new TokenUsageUserStatsDTO() {
                    @Override
                    public Long getUserId() {
                        return ((Number) row.get("user_id")).longValue();
                    }
                    @Override
                    public Integer getUserType() {
                        return ((Number) row.get("user_type")).intValue();
                    }
                    @Override
                    public Long getGroupId() {
                        Object val = row.get("group_id");
                        return val != null ? ((Number) val).longValue() : null;
                    }
                    @Override
                    public Long getRequestCount() {
                        return ((Number) row.get("request_count")).longValue();
                    }
                    @Override
                    public Integer getTotalTokens() {
                        return ((Number) row.get("total_tokens")).intValue();
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
