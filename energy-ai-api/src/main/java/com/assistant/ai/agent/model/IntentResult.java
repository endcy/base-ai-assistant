package com.assistant.ai.agent.model;

import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import com.assistant.service.domain.enums.KnowledgeScopeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 意图结果
 *
 * @author endcy
 * @date 2025/10/31 20:46:15
 */
@Data
@AllArgsConstructor
public class IntentResult {

    /**
     * 知识领域类型 预留，由调用端传入
     *
     * @see KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * 业务领域类型
     *
     * @see KnowledgeBusinessTypeEnum
     */
    private String businessType;

    private Long chatId;

    private String userMessage;

}
