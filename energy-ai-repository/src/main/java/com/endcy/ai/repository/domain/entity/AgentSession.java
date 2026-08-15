package com.endcy.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 智能体会话表（对应 ai_agent_session）。
 *
 * @author endcy
 * @since 2026-08-08
 */
@Data
@TableName(value = "ai_agent_session", autoResultMap = true)
public class AgentSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 执行器内部 UUID
     */
    private String sessionId;

    /**
     * 业务会话 ID（关联 ai_context_user_record.chat_id）
     */
    private Long chatId;

    private String userId;

    /**
     * 分组ID（租户/商户/用户分组，group_id）
     */
    private String groupId;

    /**
     * 执行模式: SINGLE_SHOT/AGENTIC/PLAN_AND_ACT
     */
    private String mode;

    /**
     * 状态: INITIALIZED/RUNNING/WAITING_APPROVAL/COMPLETED/FAILED/TERMINATED_BY_BUDGET/TERMINATED_BY_USER
     */
    private String status;

    private String userQuestion;

    private String finalAnswer;

    private String errorMessage;

    private Integer totalPromptTokens;

    private Integer totalCompletionTokens;

    private Integer currentStep;

    private Date startedAt;

    private Date completedAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
