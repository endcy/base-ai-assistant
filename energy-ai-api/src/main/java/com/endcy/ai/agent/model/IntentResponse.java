package com.endcy.ai.agent.model;

import lombok.Data;

import java.util.List;

/**
 * Intent result formatted output.
 *
 * @author endcy
 * @date 2026/04/09 20:46:15
 */
@Data
public class IntentResponse {

    private String businessType;

    private List<String> scopes;

}
