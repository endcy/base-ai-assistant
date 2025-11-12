package com.assistant.ai.repository.domain.vector;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.assistant.ai.repository.pgsql.config.PgVectorTypeHandler;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serial;
import java.io.Serializable;

/**
 * 向量数据原型
 * 使用默认推荐属性，其他属性作为元数据存档，使用灵活通用的元数据过滤方式
 *
 * @author endcy
 * @date 2025/8/6 20:51:42
 */
@Data
@TableName(value = "vector_store", autoResultMap = true)
public class VectorDocument implements Serializable {
    @Serial
    private static final long serialVersionUID = 4778001783389769562L;

    /**
     * 文档UUID主键
     */
    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 原始文本内容
     */
    private String content;

    /**
     * JSON格式元数据（如title、source等）
     */
    private String metadata;

    /**
     * 向量数据对象 自定义类型处理器
     */
    @TableField(typeHandler = PgVectorTypeHandler.class)
    private Object embedding;

}
