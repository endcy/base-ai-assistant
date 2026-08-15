package com.endcy.ai.rpc.domain.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体任务提交参数
 *
 * @author endcy
 * @since 2026/08/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentTaskParam extends BaseAiRequest {

    private static final long serialVersionUID = 1L;

    private String mode;

    private String question;

    private Integer maxSteps;

    private String userRole;
}
