package com.assistant.ai.repository.service;

import com.assistant.ai.repository.domain.dto.TokenUsageDTO;
import com.assistant.ai.repository.domain.request.TokenUsageQueryParam;
import com.assistant.service.common.base.PageInfo;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Token 用量服务接口
 *
 * @author endcy
 * @since 2026/03/18
 */
public interface TokenUsageService {

    /**
     * 保存 Token 用量记录
     */
    int insert(TokenUsageDTO dto);

    /**
     * 根据对话 ID 查询 Token 用量
     */
    List<TokenUsageDTO> getByChatId(Long chatId);

    /**
     * 根据用户 ID 查询 Token 用量
     */
    List<TokenUsageDTO> getByUserId(Long userId, Integer userType, Long chatId);

    /**
     * 分页查询 Token 用量
     */
    PageInfo<TokenUsageDTO> queryAll(TokenUsageQueryParam query, Pageable pageable);

    /**
     * 查询 Token 用量
     */
    List<TokenUsageDTO> queryAll(TokenUsageQueryParam query);

    /**
     * 根据 ID 查询 Token 用量
     */
    TokenUsageDTO getById(Long id);

    /**
     * 统计总 Token 用量
     */
    TokenUsageStatsDTO getStats(TokenUsageQueryParam query);

    /**
     * 按日期统计 Token 用量
     */
    List<TokenUsageDailyStatsDTO> getDailyStats(TokenUsageQueryParam query);

    /**
     * 按用户统计 Token 用量
     */
    List<TokenUsageUserStatsDTO> getUserStats(TokenUsageQueryParam query);

    /**
     * Token 用量统计 DTO
     */
    interface TokenUsageStatsDTO {
        Long getTotalRequestCount();
        Integer getTotalInputTokens();
        Integer getTotalOutputTokens();
        Integer getTotalTokens();
        Double getAvgRequestCostMs();
    }

    /**
     * Token 用量按日统计 DTO
     */
    interface TokenUsageDailyStatsDTO {
        String getRequestDate();
        String getModelName();
        String getScopeType();
        Long getRequestCount();
        Integer getTotalInputTokens();
        Integer getTotalOutputTokens();
        Integer getTotalTokens();
        Double getAvgRequestCostMs();
    }

    /**
     * Token 用量按用户统计 DTO
     */
    interface TokenUsageUserStatsDTO {
        Long getUserId();
        Integer getUserType();
        Long getGroupId();
        Long getRequestCount();
        Integer getTotalTokens();
    }
}
