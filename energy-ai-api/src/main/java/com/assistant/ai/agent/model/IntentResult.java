package com.assistant.ai.agent.model;

import com.assistant.ai.domain.enums.PossibleSourceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 意图结果
 *
 * @author endcy
 * @date 2025/10/31 20:46:15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {

    /**
     * 知识领域类型 预留，由调用端传入
     */
    private String scopeType;

    /**
     * 业务领域类型
     */
    private String businessType;

    private Long chatId;

    private String userMessage;

    /**
     * 意图分离数据来源判断
     *
     * @see PossibleSourceTypeEnum
     */
    private List<PossibleSourceTypeEnum> dataScopeList;

    /**
     * 推荐使用的工具列表
     */
    private List<String> recommendedTools;

    /**
     * 置信度 (0-10)
     */
    private Integer confidence;

    public IntentResult(String scopeType, String businessType, Long chatId, String userMessage) {
        this.scopeType = scopeType;
        this.businessType = businessType;
        this.chatId = chatId;
        this.userMessage = userMessage;
        this.dataScopeList = new ArrayList<>();
        this.recommendedTools = new ArrayList<>();
        this.confidence = 5;
    }

    /**
     * 添加数据来源
     */
    public void addDataScope(PossibleSourceTypeEnum dataScope) {
        if (this.dataScopeList == null) {
            this.dataScopeList = new ArrayList<>();
        }
        this.dataScopeList.add(dataScope);
    }

    /**
     * 添加推荐工具
     */
    public void addRecommendedTool(String tool) {
        if (this.recommendedTools == null) {
            this.recommendedTools = new ArrayList<>();
        }
        this.recommendedTools.add(tool);
    }
}
