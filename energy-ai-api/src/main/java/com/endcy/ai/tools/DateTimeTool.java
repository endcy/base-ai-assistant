package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期时间工具 —— 让 agent 有时间感知。
 *
 * <p>能力：
 * <ul>
 *   <li>获取当前时间（支持时区）</li>
 *   <li>解析相对时间表达式（"下周一"、"3天后"、"明天"等）</li>
 *   <li>日期加减、两个日期的差</li>
 *   <li>星期几查询</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
public class DateTimeTool {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = "获取当前日期时间（默认中国时区 Asia/Shanghai），返回 ISO 格式 + 星期 + Unix 毫秒时间戳。" +
            "所有涉及'今天/现在/当前'的问题都必须先调用此工具获取准确时间，不得凭记忆回答。")
    public String getCurrentDateTime(
            @ToolParam(description = "时区 ID，可选，默认 Asia/Shanghai。常见值：Asia/Shanghai, America/New_York, UTC") String zoneId) {
        ZoneId zone = parseZone(zoneId);
        LocalDateTime now = LocalDateTime.now(zone);
        return String.format("当前时间：%s %s（%s）| 日期：%s | Unix毫秒：%d",
                now.format(DATETIME_FMT),
                chineseDayOfWeek(now.getDayOfWeek()),
                zone.getId(),
                now.format(DATE_FMT),
                now.atZone(zone).toInstant().toEpochMilli());
    }

    @Tool(description = "解析相对时间表达式为具体日期。支持：'今天/明天/昨天/后天'、'N天前/N天后'、" +
            "'上周X/本周X/下周X'（X=一二三四五六日）、'N周后'、'N个月后'、'YYYY-MM-DD' 字面日期。" +
            "例：'下周一'、'3天后'、'2026-08-15'。返回 yyyy-MM-dd 格式。")
    public String parseRelativeDate(
            @ToolParam(description = "相对时间表达式，如 '下周一'、'3天后'、'明天'、'2026-08-15'") String expression) {
        if (StrUtil.isBlank(expression)) {
            return "请提供时间表达式";
        }
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate result = doParse(expression.trim(), today);
        if (result == null) {
            return "无法解析：'" + expression + "'。支持的格式：今天/明天/昨天/N天前/N天后/上周X/本周X/下周X/N周后/N个月后/yyyy-MM-dd";
        }
        return String.format("%s（%s）", result.format(DATE_FMT), chineseDayOfWeek(result.getDayOfWeek()));
    }

    @Tool(description = "计算两个日期之间相差的天数/月数/年数。日期格式 yyyy-MM-dd。" +
            "支持相对时间表达式（如'今天'、'下周一'）作为输入。")
    public String dateDiff(
            @ToolParam(description = "起始日期，yyyy-MM-dd 或相对表达式（如'今天'）") String from,
            @ToolParam(description = "结束日期，yyyy-MM-dd 或相对表达式（如'下周五'）") String to) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate fromDate = tryParseDate(from, today);
        LocalDate toDate = tryParseDate(to, today);
        if (fromDate == null || toDate == null) {
            return "日期格式错误。请使用 yyyy-MM-dd 或相对表达式（如'今天'、'下周一'）";
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate);
        long months = ChronoUnit.MONTHS.between(fromDate, toDate);
        long years = ChronoUnit.YEARS.between(fromDate, toDate);
        return String.format("%s → %s：相差 %d 天（约 %d 个月，约 %d 年）",
                fromDate, toDate, days, months, years);
    }

    @Tool(description = "在指定日期上加减 N 天/周/月/年。返回新的日期。" +
            "支持相对时间表达式（如'今天'、'下周一'）作为起始日期。")
    public String dateAdd(
            @ToolParam(description = "起始日期，yyyy-MM-dd 或相对表达式") String date,
            @ToolParam(description = "要加减的数量，正数为加，负数为减") int amount,
            @ToolParam(description = "单位：DAYS/WEEKS/MONTHS/YEARS") String unit) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate start = tryParseDate(date, today);
        if (start == null) {
            return "日期格式错误";
        }
        LocalDate result;
        String unitUp = StrUtil.isBlank(unit) ? "DAYS" : unit.toUpperCase();
        switch (unitUp) {
            case "WEEKS":
                result = start.plusWeeks(amount);
                break;
            case "MONTHS":
                result = start.plusMonths(amount);
                break;
            case "YEARS":
                result = start.plusYears(amount);
                break;
            default:
                result = start.plusDays(amount);
                break;
        }
        return String.format("%s %s%d %s = %s（%s）",
                start, amount >= 0 ? "+" : "", amount, unitUp,
                result.format(DATE_FMT), chineseDayOfWeek(result.getDayOfWeek()));
    }

    // ==================== Helpers ====================

    private ZoneId parseZone(String zoneId) {
        if (StrUtil.isBlank(zoneId))
            return DEFAULT_ZONE;
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }

    private String chineseDayOfWeek(DayOfWeek dow) {
        Map<DayOfWeek, String> map = new HashMap<>();
        map.put(DayOfWeek.MONDAY, "星期一");
        map.put(DayOfWeek.TUESDAY, "星期二");
        map.put(DayOfWeek.WEDNESDAY, "星期三");
        map.put(DayOfWeek.THURSDAY, "星期四");
        map.put(DayOfWeek.FRIDAY, "星期五");
        map.put(DayOfWeek.SATURDAY, "星期六");
        map.put(DayOfWeek.SUNDAY, "星期日");
        return map.getOrDefault(dow, "");
    }

    private LocalDate doParse(String expr, LocalDate today) {
        // 字面日期 yyyy-MM-dd
        try {
            return LocalDate.parse(expr, DATE_FMT);
        } catch (Exception ignore) {
        }

        // 简单关键词
        if (expr.equals("今天") || expr.equals("今日"))
            return today;
        if (expr.equals("明天") || expr.equals("明日"))
            return today.plusDays(1);
        if (expr.equals("后天"))
            return today.plusDays(2);
        if (expr.equals("昨天") || expr.equals("昨日"))
            return today.minusDays(1);
        if (expr.equals("前天"))
            return today.minusDays(2);
        if (expr.equals("大后天"))
            return today.plusDays(3);

        // N天前 / N天后
        Matcher m = Pattern.compile("(\\d+)天(前|后)").matcher(expr);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return "前".equals(m.group(2)) ? today.minusDays(n) : today.plusDays(n);
        }

        // N周前 / N周后 / N个星期前 / N个星期后
        m = Pattern.compile("(\\d+)(个)?(周|星期)(前|后)").matcher(expr);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return "前".equals(m.group(4)) ? today.minusWeeks(n) : today.plusWeeks(n);
        }

        // N个月前 / N个月后
        m = Pattern.compile("(\\d+)(个)?月(前|后)").matcher(expr);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return "前".equals(m.group(3)) ? today.minusMonths(n) : today.plusMonths(n);
        }

        // N年前 / N年后
        m = Pattern.compile("(\\d+)(个)?年(前|后)").matcher(expr);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return "前".equals(m.group(3)) ? today.minusYears(n) : today.plusYears(n);
        }

        // 上周X / 本周X / 下周X（X = 一二三四五六日/天）
        m = Pattern.compile("(上|本|下)(周|星期)([一二三四五六日天])").matcher(expr);
        if (m.matches()) {
            String rel = m.group(1);
            int targetDow = chineseDayToNumber(m.group(3));
            if (targetDow == 0)
                return null;
            DayOfWeek target = DayOfWeek.of(targetDow);
            int currentDow = today.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
            int diff = targetDow - currentDow;
            LocalDate base = today;
            if ("上".equals(rel))
                base = today.minusWeeks(1);
            else if ("下".equals(rel))
                base = today.plusWeeks(1);
            // base 现在在目标周，调整到目标星期
            int baseDow = base.getDayOfWeek().getValue();
            return base.plusDays(targetDow - baseDow);
        }

        // 月底 / 下月底 / 上个月底
        if (expr.equals("月底"))
            return today.withDayOfMonth(today.lengthOfMonth());
        if (expr.equals("下月底"))
            return today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth());
        if (expr.equals("上个月底") || expr.equals("上月底"))
            return today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth());

        return null;
    }

    /**
     * 中文字符转星期数字（1=周一 ... 7=周日）
     */
    private int chineseDayToNumber(String ch) {
        switch (ch) {
            case "一":
                return 1;
            case "二":
                return 2;
            case "三":
                return 3;
            case "四":
                return 4;
            case "五":
                return 5;
            case "六":
                return 6;
            case "日":
            case "天":
                return 7;
            default:
                return 0;
        }
    }

    private LocalDate tryParseDate(String expr, LocalDate today) {
        if (StrUtil.isBlank(expr))
            return null;
        try {
            return LocalDate.parse(expr.trim(), DATE_FMT);
        } catch (Exception ignore) {
        }
        return doParse(expr.trim(), today);
    }
}
