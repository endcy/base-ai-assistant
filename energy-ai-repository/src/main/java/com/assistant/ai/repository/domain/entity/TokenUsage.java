package com.assistant.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI Token 用量统计表
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
@TableName(value = "ai_token_usage", autoResultMap = true)
public class TokenUsage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对话 id
     */
    private Long chatId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户类型：1-普通用户 2-客户租户
     */
    private Integer userType;

    /**
     * 用户分组 id，如租户 id
     */
    private Long groupId;

    /**
     * 知识领域类型
     */
    private String scopeType;

    /**
     * 业务领域类型
     */
    private String businessType;

    /**
     * 用户问题摘要
     */
    private String question;

    /**
     * 使用的模型名称
     */
    private String modelName;

    /**
     * 输入 token 数量
     */
    private Integer inputTokens;

    /**
     * 输出 token 数量
     */
    private Integer outputTokens;

    /**
     * 总 token 数量
     */
    private Integer totalTokens;

    /**
     * API 类型：chat/embedding/rerank
     */
    private String apiType;

    /**
     * 请求耗时 (毫秒)
     */
    private Long requestCostMs;

    /**
     * 请求状态：0-失败 1-成功
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 请求日期 (用于分区查询)
     */
    @TableField(fill = FieldFill.INSERT)
    private Date requestDate;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
