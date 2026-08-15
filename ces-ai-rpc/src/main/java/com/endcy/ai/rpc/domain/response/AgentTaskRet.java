package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能体任务状态返回
 *
 * @author endcy
 * @since 2026/08/08
 */
@Data
public class AgentTaskRet implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;

    private String mode;

    private String status;

    private Integer currentStep;

    private Integer maxSteps;

    private String finalAnswer;

    private String errorMessage;

    private Integer totalPromptTokens;

    private Integer totalCompletionTokens;

    private Boolean terminal;

    private Date startedAt;

    private Date completedAt;
}
