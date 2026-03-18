package com.assistant.ai.controller;

import com.assistant.ai.repository.domain.dto.TokenUsageDTO;
import com.assistant.ai.repository.domain.request.TokenUsageQueryParam;
import com.assistant.ai.repository.service.TokenUsageService;
import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Token 用量统计管理
 *
 * @author endcy
 * @since 2026/03/18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token-usage")
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    @GetMapping
    @LogRecord("查询 Token 用量")
    public PageInfo<TokenUsageDTO> query(TokenUsageQueryParam query, Pageable pageable) {
        return tokenUsageService.queryAll(query, pageable);
    }

    @GetMapping("/stats")
    @LogRecord("查询 Token 用量统计")
    public TokenUsageService.TokenUsageStatsDTO stats(TokenUsageQueryParam query) {
        return tokenUsageService.getStats(query);
    }

    @GetMapping("/stats/daily")
    @LogRecord("查询 Token 用量按日统计")
    public List<TokenUsageService.TokenUsageDailyStatsDTO> dailyStats(TokenUsageQueryParam query) {
        return tokenUsageService.getDailyStats(query);
    }

    @GetMapping("/stats/user")
    @LogRecord("查询 Token 用量按用户统计")
    public List<TokenUsageService.TokenUsageUserStatsDTO> userStats(TokenUsageQueryParam query) {
        return tokenUsageService.getUserStats(query);
    }

    @GetMapping("/{id}")
    @LogRecord("查询 Token 用量详情")
    public TokenUsageDTO detail(@PathVariable Long id) {
        return tokenUsageService.getById(id);
    }

    @GetMapping("/chat/{chatId}")
    @LogRecord("查询对话 Token 用量")
    public List<TokenUsageDTO> getByChatId(@PathVariable Long chatId) {
        return tokenUsageService.getByChatId(chatId);
    }

    @GetMapping("/user/{userId}")
    @LogRecord("查询用户 Token 用量")
    public List<TokenUsageDTO> getByUserId(@PathVariable Long userId, @RequestParam(required = false) Integer userType, @RequestParam(required = false) Long chatId) {
        return tokenUsageService.getByUserId(userId, userType, chatId);
    }
}
