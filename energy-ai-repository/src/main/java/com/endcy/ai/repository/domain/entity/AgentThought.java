package com.endcy.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 智能体思考过程表（对应 ai_agent_thought）。
 *
 * @author endcy
 * @since 2026-08-08
 */
@Data
@TableName(value = "ai_agent_thought", autoResultMap = true)
public class AgentThought implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 ai_agent_session.session_id
     */
    private String sessionId;

    private Integer stepIndex;

    /**
     * 思考内容（LLM 输出）
     */
    private String thought;

    /**
     * 工具调用（JSON 数组）
     */
    private String toolCalls;

    /**
     * 工具结果（JSON 数组）
     */
    private String toolResults;

    private Long durationMs;

    private Integer promptTokens;

    private Integer completionTokens;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
