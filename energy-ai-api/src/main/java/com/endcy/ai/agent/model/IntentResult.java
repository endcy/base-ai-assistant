package com.endcy.ai.agent.model;

import com.endcy.service.domain.enums.KnowledgeBusinessTypeEnum;
import com.endcy.service.domain.enums.KnowledgeScopeTypeEnum;
import com.endcy.service.domain.enums.PossibleSourceTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * Intent result.
 *
 * @author endcy
 * @date 2025/10/31 20:46:15
 */
@Data
public class IntentResult {

    /**
     * Knowledge domain type, reserved, passed by caller.
     *
     * @see KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * Business domain type.
     *
     * @see KnowledgeBusinessTypeEnum
     */
    private String businessType;

    /**
     * Conversation ID.
     */
    private Long chatId;

    /**
     * Original user message.
     */
    private String userMessage;

    /**
     * Intent-separated data source judgment.
     *
     * @see PossibleSourceTypeEnum
     */
    private List<PossibleSourceTypeEnum> dataScopeList;
}
