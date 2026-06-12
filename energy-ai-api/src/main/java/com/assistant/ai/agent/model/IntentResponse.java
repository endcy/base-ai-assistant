package com.assistant.ai.agent.model;

import lombok.Data;

import java.util.List;

/**
 * 意图结果格式化输出
 *
 * @author endcy
 * @date 2026/04/09 20:46:15
 */
@Data
public class IntentResponse {

    private String businessType;

    private List<String> scopes;

}
