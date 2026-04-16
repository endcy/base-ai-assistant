package com.assistant.ai.handler;

import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.service.domain.constant.ObjectConvertConstant;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 名称策略处理器实现
 * 基于 NameStrategyHandler 实现名称转换
 *
 * @author endcy
 * @date 2026/04/10 14:36:49
 */
@Component
public class NameSwitchStrategyHandler implements NameStrategyHandler {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 名称展示转换逻辑
     *
     * @param name  当前字段名称
     * @param value 当前值
     */
    @Override
    public Object switchName(String name, Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        return switch (name) {
            case ObjectConvertConstant.KNOWLEDGE_TITLE -> knowledgeDocumentService.getById(Long.valueOf(value.toString())).getTitle();
            case ObjectConvertConstant.ENERGY_ANGENT_NAME -> ObjectConvertConstant.ENERGY_ANGENT_NAME;
            default -> value;
        };
    }
}
