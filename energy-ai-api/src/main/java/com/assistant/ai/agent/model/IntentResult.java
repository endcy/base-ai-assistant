package com.assistant.ai.agent.model;

import com.assistant.ai.domain.enums.PossibleSourceTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * 意图结果
 *
 * @author endcy
 * @date 2025/10/31 20:46:15
 */
@Data
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
    List<PossibleSourceTypeEnum> dataScopeList;

    public IntentResult() {
    }

    public IntentResult(String scopeType, String businessType, Long chatId, String userMessage) {
        this.scopeType = scopeType;
        this.businessType = businessType;
        this.chatId = chatId;
        this.userMessage = userMessage;
    }
}
