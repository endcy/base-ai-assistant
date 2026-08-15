package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数学计算工具 —— 避免 LLM 直接算数出错。
 *
 * <p>能力：表达式求值、百分比、单位换算、基础统计。</p>
 *
 * <p>表达式求值使用内置简单递归下降解析器，支持：
 * {@code +  -  *  /  ^  ()  小数  函数 sqrt/sin/cos/log/abs/round  常量 pi/e  百分号 %}。</p>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
public class MathCalculatorTool {

    @Tool(description = "计算数学表达式。支持：+ - * / ^ ( ) 小数 函数(sqrt/sin/cos/log/abs/round) 常量(pi/e) 百分号(20%=0.2)。" +
            "任何需要精确数值的计算都必须调用此工具，不得心算。" +
            "例：'100 * 1.15'、'sqrt(144) + 3^2'、'(200-50)/50*100%'")
    public String calculate(
            @ToolParam(description = "数学表达式字符串") String expression) {
        if (StrUtil.isBlank(expression)) {
            return "表达式为空";
        }
        try {
            double result = new ExprEvaluator(expression).evaluate();
            if (Double.isNaN(result))
                return "结果为 NaN（计算错误）";
            if (Double.isInfinite(result))
                return "结果为无穷大";
            // 整数结果去掉小数点
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.format("%s = %.0f", expression, result);
            }
            return String.format("%s = %.6f", expression, result).replaceAll("0+$", "").replaceAll("\\.$", "");
        } catch (Exception e) {
            return "表达式错误：" + e.getMessage() + "。输入：" + expression;
        }
    }

    @Tool(description = "百分比计算。支持：求 X 的 N%、X 占 Y 的百分比、X 比 Y 增加/减少百分之几。")
    public String percentage(
            @ToolParam(description = "计算类型：'of'（X 的 N%）、'ratio'（X 占 Y 的百分比）、'change'（X 比 Y 变化百分比）") String type,
            @ToolParam(description = "数值 X") double x,
            @ToolParam(description = "数值 Y 或百分比数值（当 type='of' 时）") double y) {
        try {
            switch (StrUtil.blankToDefault(type, "of").toLowerCase()) {
                case "of":
                    return String.format("%.2f 的 %.2f%% = %.4f", x, y, x * y / 100);
                case "ratio":
                    if (y == 0)
                        return "Y 为 0，无法计算占比";
                    return String.format("%.2f 占 %.2f 的 %.2f%%", x, y, x / y * 100);
                case "change":
                    if (y == 0)
                        return "Y 为 0，无法计算变化率";
                    double change = (x - y) / y * 100;
                    return String.format("%.2f 相对 %.2f 变化了 %+.2f%%（%s %.2f%%）",
                            x, y, change,
                            change >= 0 ? "增加" : "减少", Math.abs(change));
                default:
                    return "不支持的类型 '" + type + "'。请用 'of'、'ratio' 或 'change'";
            }
        } catch (Exception e) {
            return "计算错误：" + e.getMessage();
        }
    }

    @Tool(description = "单位换算。支持：长度（km/m/cm/mm/miles）、电量（kWh/Wh/J）、温度（C/F/K）、" +
            "时间（h/min/s）、金额（yuan/fen）。")
    public String unitConvert(
            @ToolParam(description = "数值") double value,
            @ToolParam(description = "源单位（如 km、kWh、C）") String fromUnit,
            @ToolParam(description = "目标单位（如 m、Wh、F）") String toUnit) {
        try {
            double result = doConvert(value, fromUnit.toLowerCase(), toUnit.toLowerCase());
            return String.format("%.4f %s = %.4f %s", value, fromUnit, result, toUnit);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(description = "基础统计：求一组数值的平均值、总和、最大值、最小值、中位数。" +
            "数值以逗号分隔。例：'1,2,3,4,5'")
    public String statistics(
            @ToolParam(description = "逗号分隔的数值列表，如 '1,2,3,4,5'") String numbersStr,
            @ToolParam(description = "统计类型：avg/sum/min/max/median/all") String operation) {
        if (StrUtil.isBlank(numbersStr))
            return "数值列表为空";
        String[] parts = numbersStr.split("[,，\\s]+");
        double[] nums = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                nums[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                return "第 " + (i + 1) + " 个值不是有效数字：'" + parts[i] + "'";
            }
        }
        double sum = 0, min = nums[0], max = nums[0];
        for (double n : nums) {
            sum += n;
            if (n < min)
                min = n;
            if (n > max)
                max = n;
        }
        double avg = sum / nums.length;
        java.util.Arrays.sort(nums);
        double median = nums.length % 2 == 0 ? (nums[nums.length / 2 - 1] + nums[nums.length / 2]) / 2 : nums[nums.length / 2];

        String op = StrUtil.blankToDefault(operation, "all").toLowerCase();
        switch (op) {
            case "avg":
                return String.format("平均值 = %.4f", avg);
            case "sum":
                return String.format("总和 = %.4f", sum);
            case "min":
                return String.format("最小值 = %.4f", min);
            case "max":
                return String.format("最大值 = %.4f", max);
            case "median":
                return String.format("中位数 = %.4f", median);
            default:
                return String.format("共 %d 个值\n总和 = %.4f\n平均值 = %.4f\n最小值 = %.4f\n最大值 = %.4f\n中位数 = %.4f",
                        nums.length, sum, avg, min, max, median);
        }
    }

    // ==================== 单位换算 ====================

    private double doConvert(double value, String from, String to) {
        // 同一单位
        if (from.equals(to))
            return value;

        // 长度
        Map<String, Double> lengthToMeter = Map.of(
                "m", 1.0, "km", 1000.0, "cm", 0.01, "mm", 0.001,
                "mile", 1609.344, "miles", 1609.344, "ft", 0.3048, "inch", 0.0254);
        if (lengthToMeter.containsKey(from) && lengthToMeter.containsKey(to)) {
            return value * lengthToMeter.get(from) / lengthToMeter.get(to);
        }

        // 电量
        Map<String, Double> energyToWh = Map.of("wh", 1.0, "kwh", 1000.0, "j", 1.0 / 3600, "kj", 1.0 / 3.6);
        if (energyToWh.containsKey(from) && energyToWh.containsKey(to)) {
            return value * energyToWh.get(from) / energyToWh.get(to);
        }

        // 温度
        if ((from.equals("c") || from.equals("℃")) && (to.equals("f") || to.equals("℉")))
            return value * 9 / 5 + 32;
        if ((from.equals("f") || from.equals("℉")) && (to.equals("c") || to.equals("℃")))
            return (value - 32) * 5 / 9;
        if ((from.equals("c") || from.equals("℃")) && to.equals("k"))
            return value + 273.15;
        if (from.equals("k") && (to.equals("c") || to.equals("℃")))
            return value - 273.15;

        // 时间
        Map<String, Double> timeToSecond = Map.of("s", 1.0, "min", 60.0, "h", 3600.0, "hour", 3600.0, "day", 86400.0);
        if (timeToSecond.containsKey(from) && timeToSecond.containsKey(to)) {
            return value * timeToSecond.get(from) / timeToSecond.get(to);
        }

        // 金额（人民币）
        Map<String, Double> moneyToFen = Map.of("fen", 1.0, "元", 100.0, "yuan", 100.0);
        if (moneyToFen.containsKey(from) && moneyToFen.containsKey(to)) {
            return value * moneyToFen.get(from) / moneyToFen.get(to);
        }

        throw new IllegalArgumentException("不支持的单位换算：'" + from + "' → '" + to + "'");
    }

    // ==================== 表达式求值器（递归下降解析） ====================

    /**
     * 简单递归下降表达式求值器。
     * <p>文法：
     * <pre>
     * expr   → term (('+' | '-') term)*
     * term   → factor (('*' | '/') factor)*
     * factor → unary ('^' unary)*
     * unary  → ('-' | '+')? primary
     * primary → NUMBER | '(' expr ')' | FUNCTION '(' expr ')' | CONSTANT | PERCENT
     * </pre>
     */
    static class ExprEvaluator {
        private final String input;
        private int pos = 0;

        ExprEvaluator(String input) {
            this.input = input.replaceAll("\\s+", "");
        }

        double evaluate() {
            double result = parseExpr();
            if (pos < input.length()) {
                throw new RuntimeException("未预期的字符：'" + input.charAt(pos) + "' 位置 " + pos);
            }
            return result;
        }

        private double parseExpr() {
            double result = parseTerm();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '+') {
                    pos++;
                    result += parseTerm();
                } else if (c == '-') {
                    pos++;
                    result -= parseTerm();
                } else
                    break;
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '*') {
                    pos++;
                    result *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    double divisor = parseFactor();
                    if (divisor == 0)
                        throw new ArithmeticException("除以零");
                    result /= divisor;
                } else
                    break;
            }
            return result;
        }

        private double parseFactor() {
            double result = parseUnary();
            while (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                result = Math.pow(result, parseUnary());
            }
            return result;
        }

        private double parseUnary() {
            if (pos < input.length()) {
                if (input.charAt(pos) == '-') {
                    pos++;
                    return -parsePrimary();
                }
                if (input.charAt(pos) == '+') {
                    pos++;
                    return parsePrimary();
                }
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            // 括号
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double result = parseExpr();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new RuntimeException("缺少右括号");
                }
                pos++;
                // 百分号紧跟括号，如 (1+2)%
                if (pos < input.length() && input.charAt(pos) == '%') {
                    pos++;
                    return result / 100;
                }
                return result;
            }
            // 函数或常量
            if (Character.isLetter(input.charAt(pos))) {
                StringBuilder name = new StringBuilder();
                while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
                    name.append(input.charAt(pos++));
                }
                String fn = name.toString().toLowerCase();
                // 常量
                if (fn.equals("pi"))
                    return Math.PI;
                if (fn.equals("e"))
                    return Math.E;
                // 函数
                if (pos >= input.length() || input.charAt(pos) != '(') {
                    throw new RuntimeException("未知标识符：'" + fn + "'");
                }
                pos++;
                double arg = parseExpr();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new RuntimeException("函数 '" + fn + "' 缺少右括号");
                }
                pos++;
                return applyFunction(fn, arg);
            }
            // 数字
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new RuntimeException("期望数字，位置 " + pos);
            }
            double num = Double.parseDouble(input.substring(start, pos));
            // 百分号
            if (pos < input.length() && input.charAt(pos) == '%') {
                pos++;
                return num / 100;
            }
            return num;
        }

        private double applyFunction(String fn, double arg) {
            switch (fn) {
                case "sqrt":
                    return Math.sqrt(arg);
                case "sin":
                    return Math.sin(arg);
                case "cos":
                    return Math.cos(arg);
                case "tan":
                    return Math.tan(arg);
                case "log":
                    return Math.log(arg);
                case "log10":
                    return Math.log10(arg);
                case "abs":
                    return Math.abs(arg);
                case "round":
                    return Math.round(arg);
                case "ceil":
                    return Math.ceil(arg);
                case "floor":
                    return Math.floor(arg);
                case "exp":
                    return Math.exp(arg);
                default:
                    throw new RuntimeException("未知函数：'" + fn + "'");
            }
        }
    }
}
