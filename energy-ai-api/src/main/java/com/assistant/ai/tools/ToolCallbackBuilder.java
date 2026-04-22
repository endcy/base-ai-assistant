package com.assistant.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.Function;

/**
 * 工具回调构建器
 * 提供简洁的 API 将工具名、描述和执行函数组装为 Spring AI 标准的 ToolCallback
 *
 * @author endcy
 * @date 2026/04/22
 */
public class ToolCallbackBuilder {

    private String name;
    private String description;
    private Function<String, String> function;

    private ToolCallbackBuilder() {
    }

    public static ToolCallbackBuilder builder() {
        return new ToolCallbackBuilder();
    }

    /**
     * 快速构建 JSON Schema 参数定义
     */
    public static String buildSchema(ObjectNode properties, String... requiredFields) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        if (requiredFields.length > 0) {
            ArrayNode required = schema.putArray("required");
            for (String field : requiredFields) {
                required.add(field);
            }
        }
        return schema.toString();
    }

    /**
     * 快速构建单个属性的 JSON Schema
     */
    public static ObjectNode stringProperty(String description, boolean required) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode prop = mapper.createObjectNode();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    public static ObjectNode numberProperty(String description, boolean required) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode prop = mapper.createObjectNode();
        prop.put("type", "number");
        prop.put("description", description);
        return prop;
    }

    public ToolCallbackBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ToolCallbackBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ToolCallbackBuilder function(Function<String, String> function) {
        this.function = function;
        return this;
    }

    public ToolCallback build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Tool name must not be null");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Tool description must not be null");
        }
        if (function == null) {
            throw new IllegalArgumentException("Tool function must not be null");
        }

        return FunctionToolCallback.builder(name, function)
                                   .description(description)
                                   .build();
    }
}
