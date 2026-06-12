package com.assistant.ai.rpc.domain.base;

import com.assistant.ai.rpc.enums.MessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;

/**
 * 流式输出
 * 使用单一格式，不支持泛型类型
 *
 * @author endcy
 * @date 2025/12/13 13:49:51
 */
@Data
public class AIStreamResponse implements Serializable {
    public static final ObjectMapper MAPPER = new ObjectMapper();


    private static final long serialVersionUID = -6315820569629631857L;
    /**
     * 对话ID
     */
    private Long chatId;

    /**
     * 据类型，例如：1=回答, 2=关联文档
     */
    private MessageType type;

    /**
     * 实际的数据负载
     */
    private String data;

    /**
     * 是否停止输出
     */
    private boolean isFinal;

    public String toSSEString() {
        // 将数据转换为JSON字符串，便于前端解析
        try {
            return "data:" + MAPPER.writeValueAsString(this) + "\n\n";
        } catch (JsonProcessingException e) {
            return "data:{\"type\":\"ERROR\",\"data\":\"序列化错误\"}\n\n";
        }
    }
}
