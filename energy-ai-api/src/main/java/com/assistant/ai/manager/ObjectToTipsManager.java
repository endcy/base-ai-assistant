package com.assistant.ai.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.handler.NameStrategyHandler;
import com.assistant.service.common.annotation.Tips;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 对象到 Tips 表达转换管理器
 * 将包含@Tips 注解的对象转换为自然语言描述，支持 nameStrategy 策略转换
 *
 * @author endcy
 * @date 2026/04/10 14:36:49
 */
@Slf4j
@Component
public class ObjectToTipsManager {

    @Autowired(required = false)
    private NameStrategyHandler nameStrategyHandler;

    /**
     * 将对象转换为 Tips 表达
     *
     * @param obj 待转换的对象
     * @return 自然语言描述字符串
     */
    public String toTipsExpression(Object obj) {
        return toTipsExpression(obj, 0, 3);
    }

    /**
     * 将对象转换为 Tips 表达（支持嵌套深度控制）
     *
     * @param obj          待转换的对象
     * @param currentDepth 当前嵌套深度
     * @param maxDepth     最大嵌套深度
     * @return 自然语言描述字符串
     */
    public String toTipsExpression(Object obj, int currentDepth, int maxDepth) {
        if (obj == null) {
            return "";
        }

        if (currentDepth > maxDepth) {
            return toJsonString(obj);
        }

        // 处理数组
        if (obj.getClass().isArray()) {
            return formatArray(obj, currentDepth, maxDepth);
        }

        // 处理 Collection（List、Set 等）
        if (obj instanceof Collection) {
            return formatCollection((Collection<?>) obj, currentDepth, maxDepth);
        }

        // 处理 Map
        if (obj instanceof Map) {
            return formatMap((Map<?, ?>) obj, currentDepth, maxDepth);
        }

        Class<?> clazz = obj.getClass();
        List<String> parts = new ArrayList<>();

        // 获取所有声明的字段（包括父类）
        List<Field> allFields = getAllFields(clazz);

        for (Field field : allFields) {
            field.setAccessible(true);
            Tips tips = field.getAnnotation(Tips.class);

            if (tips != null) {
                try {
                    Object value = field.get(obj);
                    String part = processField(field, value, tips, currentDepth, maxDepth);
                    if (!part.isEmpty()) {
                        parts.add(part);
                    }
                } catch (IllegalAccessException e) {
                    log.debug("无法访问字段：{}", field.getName(), e);
                }
            }
        }

        // 如果没有@Tips 注解的字段，返回 JSON
        if (parts.isEmpty()) {
            return toJsonString(obj);
        }

        return String.join(";", parts);
    }

    /**
     * 获取类的所有字段（包括父类）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     * 判断字段值是否匹配隐藏值
     * 对于 Number 类型使用数值比较（解决 BigDecimal 精度差异问题）
     * 对于其他类型使用字符串精确匹配
     */
    private boolean isHiddenValue(Object value, String hiddenValue) {
        // Number 类型：使用 BigDecimal 做数值比较，解决 "0" vs "0.00" 等问题
        if (value instanceof Number) {
            try {
                java.math.BigDecimal decimalValue = new java.math.BigDecimal(String.valueOf(value));
                java.math.BigDecimal decimalHidden = new java.math.BigDecimal(hiddenValue);
                return decimalValue.compareTo(decimalHidden) == 0;
            } catch (NumberFormatException e) {
                // hiddenValue 不是合法数字，回退到字符串比较
                return String.valueOf(value).equals(hiddenValue);
            }
        }
        // Boolean 类型：支持 "true"/"false" 或 "0"/"1" 匹配
        if (value instanceof Boolean boolVal) {
            if ("true".equalsIgnoreCase(hiddenValue) || "1".equals(hiddenValue)) {
                return Boolean.TRUE.equals(boolVal);
            }
            if ("false".equalsIgnoreCase(hiddenValue) || "0".equals(hiddenValue)) {
                return Boolean.FALSE.equals(boolVal);
            }
            return String.valueOf(value).equals(hiddenValue);
        }
        // 其他类型：字符串精确匹配
        return String.valueOf(value).equals(hiddenValue);
    }

    /**
     * 处理单个字段
     */
    private String processField(Field field, Object value, Tips tips, int currentDepth, int maxDepth) {
        String fieldName = tips.name();

        if (value == null) {
            return "";
        }

        String hiddenValue = tips.hiddenValue();
        if (!hiddenValue.isEmpty() && isHiddenValue(value, hiddenValue)) {
            return "";
        }

        // 如果指定了 nameStrategy，使用 NameStrategyHandler 转换值
        String strategyKey = tips.nameStrategy();
        Object processedValue = value;
        if (!strategyKey.isEmpty() && nameStrategyHandler != null) {
            processedValue = nameStrategyHandler.switchName(strategyKey, value);
        }

        String fieldValue = formatValue(processedValue, field.getType(), tips, currentDepth, maxDepth);

        String result = fieldName;
        result = result + ":" + fieldValue;

        // 脱敏 中间1/3信息使用*替代
        if (tips.desensitization()) {
            result = maskMiddle(result);
        }

        // 添加额外解释
        if (!StrUtil.isBlank(tips.explain())) {
            result = result + " " + tips.explain();
        }
        return result;
    }

    public static String maskMiddle(String input) {
        if (input == null || input.length() <= 1) {
            return input;
        }
        int len = input.length();
        if (len == 2) {
            return input.charAt(0) + "***";
        }
        int keepEachSide = Math.min(len / 3, 3);
        return input.substring(0, keepEachSide) + "***" + input.substring(len - keepEachSide);
    }


    /**
     * 格式化字段值
     */
    private String formatValue(Object value, Class<?> type, Tips tips,
                               int currentDepth, int maxDepth) {
        // 处理数组
        if (value != null && type.isArray()) {
            return formatArray(value, currentDepth, maxDepth);
        }

        // 处理 Collection（List、Set 等）
        if (value instanceof Collection) {
            return formatCollection((Collection<?>) value, currentDepth, maxDepth);
        }

        // 处理 Map
        if (value instanceof Map) {
            return formatMap((Map<?, ?>) value, currentDepth, maxDepth);
        }

        // 处理枚举
        if (type.isEnum()) {
            return formatEnum(value, tips);
        }

        // 处理日期
        if (value instanceof Date) {
            return formatDate((Date) value, tips);
        }

        // 处理数值类型（带单位）
        if (value instanceof Number) {
            return formatNumber((Number) value, tips);
        }

        // 处理嵌套对象
        if (!isPrimitiveType(type) && !type.equals(String.class)) {
            return formatNestedObject(value, tips, currentDepth, maxDepth);
        }

        // 处理字符串和其他类型
        return String.valueOf(value);
    }

    /**
     * 格式化枚举值
     */
    private String formatEnum(Object enumValue, Tips tips) {
        if (tips.enumDesc().isEmpty()) {
            return enumValue.toString();
        }

        try {
            Field nameField = enumValue.getClass().getDeclaredField(tips.enumDesc());
            nameField.setAccessible(true);
            Object nameValue = nameField.get(enumValue);
            return nameValue != null ? String.valueOf(nameValue) : enumValue.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return enumValue.toString();
        }
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date, Tips tips) {
        if (tips.dateFormat().isEmpty()) {
            return date.toString();
        }

        try {
            return DateUtil.format(date, tips.dateFormat());
        } catch (Exception e) {
            return date.toString();
        }
    }

    /**
     * 格式化数值（带单位）
     */
    private String formatNumber(Number number, Tips tips) {
        if (tips.unit().isEmpty()) {
            return String.valueOf(number);
        }

        return number + tips.unit();
    }

    /**
     * 格式化嵌套对象
     */
    private String formatNestedObject(Object obj, Tips tips,
                                      int currentDepth, int maxDepth) {
        int newDepth = currentDepth + 1;
        int fieldMaxDepth = tips.maxDepth() > 0 ? tips.maxDepth() : maxDepth;

        String nested = toTipsExpression(obj, newDepth, Math.min(fieldMaxDepth, maxDepth));

        // 如果嵌套对象没有@Tips 注解，返回 JSON
        if (nested.isEmpty()) {
            return toJsonString(obj);
        }

        return "{" + nested + "}";
    }

    /**
     * 判断是否为原始类型
     */
    private boolean isPrimitiveType(Class<?> type) {
        return type.isPrimitive()
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Character.class;
    }

    /**
     * 将对象转换为 JSON 字符串（简化版）
     */
    private String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        return JSONUtil.toJsonStr(obj);
    }

    /**
     * 格式化 Collection（List、Set 等）
     * 转换为 [元素1,元素2,...] 的形式
     */
    private String formatCollection(Collection<?> collection, int currentDepth, int maxDepth) {
        if (collection.isEmpty()) {
            return "[]";
        }

        List<String> elements = new ArrayList<>();
        for (Object item : collection) {
            String itemStr = toTipsExpression(item, currentDepth + 1, maxDepth);
            // 如果元素转换后为空（null），跳过
            if (!itemStr.isEmpty()) {
                elements.add(itemStr);
            }
        }

        if (elements.isEmpty()) {
            return "[]";
        }

        return "[" + String.join(",", elements) + "]";
    }

    /**
     * 格式化数组
     * 转换为 [元素1,元素2,...] 的形式
     */
    private String formatArray(Object array, int currentDepth, int maxDepth) {
        if (array == null) {
            return "[]";
        }

        List<?> list;
        switch (array) {
            case Object[] objects -> list = Arrays.asList(objects);
            case int[] ints -> list = List.of(ints);
            case long[] longs -> list = List.of(longs);
            case double[] doubles -> list = List.of(doubles);
            case float[] floats -> list = List.of(floats);
            case boolean[] booleans -> list = List.of(booleans);
            case short[] shorts -> list = List.of(shorts);
            case byte[] bytes -> list = List.of(bytes);
            case char[] chars -> list = List.of(chars);
            default -> {
                return toJsonString(array);
            }
        }

        return formatCollection(list, currentDepth, maxDepth);
    }

    /**
     * 格式化 Map
     * 转换为 {key1: value1; key2: value2} 的形式
     * 如果 Map 的 value 是带@Tips 注解的对象，会递归转换
     */
    private String formatMap(Map<?, ?> map, int currentDepth, int maxDepth) {
        if (map.isEmpty()) {
            return "{}";
        }

        // 使用 LinkedHashMap 保持插入顺序
        Map<Object, String> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String valueStr = toTipsExpression(value, currentDepth + 1, maxDepth);
            if (valueStr.isEmpty()) {
                valueStr = value != null ? String.valueOf(value) : "null";
            }
            converted.put(key, valueStr);
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<Object, String> entry : converted.entrySet()) {
            parts.add(entry.getKey() + ":" + entry.getValue());
        }

        return "{" + String.join(",", parts) + "}";
    }

}
