package com.zhaoxinms.contract.tools.ruleextract.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期格式转换工具类
 * 支持多种日期格式的解析和统一输出
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class DateFormatUtil {

    /**
     * ISO 8601 日期格式
     */
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 中文日期格式
     */
    public static final String CHINESE_DATE_FORMAT = "yyyy年MM月dd日";

    /**
     * 支持的日期格式列表（按优先级排序）
     */
    private static final List<String> DATE_PATTERNS = Arrays.asList(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyyMMdd",
        "yyyy年MM月dd日",
        "yyyy-M-d",
        "yyyy/M/d",
        "yyyy年M月d日"
    );

    /**
     * 中文数字映射
     */
    private static final Map<Character, Integer> CHINESE_NUMBER_MAP = new HashMap<>();
    static {
        CHINESE_NUMBER_MAP.put('零', 0);
        CHINESE_NUMBER_MAP.put('〇', 0);
        CHINESE_NUMBER_MAP.put('一', 1);
        CHINESE_NUMBER_MAP.put('二', 2);
        CHINESE_NUMBER_MAP.put('三', 3);
        CHINESE_NUMBER_MAP.put('四', 4);
        CHINESE_NUMBER_MAP.put('五', 5);
        CHINESE_NUMBER_MAP.put('六', 6);
        CHINESE_NUMBER_MAP.put('七', 7);
        CHINESE_NUMBER_MAP.put('八', 8);
        CHINESE_NUMBER_MAP.put('九', 9);
        CHINESE_NUMBER_MAP.put('十', 10);
    }

    /**
     * 解析日期字符串为Date对象
     * 
     * @param dateStr 日期字符串
     * @return Date对象，解析失败返回null
     */
    public static Date parseDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }

        // 清理字符串
        dateStr = dateStr.trim();

        try {
            // 尝试解析中文日期（如：二〇二四年十月九日）
            Date chineseDate = parseChineseDate(dateStr);
            if (chineseDate != null) {
                return chineseDate;
            }

            // 尝试使用Hutool的智能日期解析
            try {
                return DateUtil.parse(dateStr);
            } catch (Exception e) {
                // 继续尝试其他方式
            }

            // 尝试各种常见格式
            for (String pattern : DATE_PATTERNS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    sdf.setLenient(false);
                    return sdf.parse(dateStr);
                } catch (ParseException e) {
                    // 继续尝试下一个格式
                }
            }

            log.warn("无法解析日期字符串: {}", dateStr);
            return null;

        } catch (Exception e) {
            log.error("解析日期失败: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 解析中文日期
     * 支持：二〇二四年十月九日、2024年10月9日等
     */
    private static Date parseChineseDate(String dateStr) {
        try {
            // 匹配中文日期模式
            Pattern pattern = Pattern.compile("([〇零一二三四五六七八九十0-9]{2,4})年([〇零一二三四五六七八九十0-9]{1,2})月([〇零一二三四五六七八九十0-9]{1,2})日");
            Matcher matcher = pattern.matcher(dateStr);

            if (matcher.find()) {
                String yearStr = matcher.group(1);
                String monthStr = matcher.group(2);
                String dayStr = matcher.group(3);

                int year = parseChineseNumber(yearStr);
                int month = parseChineseNumber(monthStr);
                int day = parseChineseNumber(dayStr);

                if (year > 0 && month > 0 && month <= 12 && day > 0 && day <= 31) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month - 1, day, 0, 0, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    return calendar.getTime();
                }
            }
        } catch (Exception e) {
            // 解析失败，返回null
        }
        return null;
    }

    /**
     * 解析中文数字
     * 支持：〇一二三四五六七八九十 和 阿拉伯数字混合
     */
    private static int parseChineseNumber(String numStr) {
        if (StrUtil.isBlank(numStr)) {
            return -1;
        }

        // 如果是纯数字，直接转换
        if (numStr.matches("\\d+")) {
            return Integer.parseInt(numStr);
        }

        // 处理中文数字
        int result = 0;
        int temp = 0;
        boolean hasTen = false;

        for (char c : numStr.toCharArray()) {
            Integer num = CHINESE_NUMBER_MAP.get(c);
            if (num == null) {
                // 如果是阿拉伯数字
                if (Character.isDigit(c)) {
                    temp = temp * 10 + (c - '0');
                }
                continue;
            }

            if (num == 10) {
                hasTen = true;
                if (temp == 0) {
                    temp = 1;
                }
                result += temp * 10;
                temp = 0;
            } else {
                temp = num;
            }
        }

        result += temp;

        // 特殊处理：如果只有一个"十"，如"十月"表示10月
        if (result == 0 && hasTen) {
            result = 10;
        }

        return result;
    }

    /**
     * 格式化日期为ISO 8601格式 (yyyy-MM-dd)
     */
    public static String formatToISO(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtil.format(date, ISO_DATE_FORMAT);
    }

    /**
     * 格式化日期为ISO 8601格式
     */
    public static String formatToISO(String dateStr) {
        Date date = parseDate(dateStr);
        return formatToISO(date);
    }

    /**
     * 格式化日期为中文格式 (yyyy年MM月dd日)
     */
    public static String formatToChinese(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtil.format(date, CHINESE_DATE_FORMAT);
    }

    /**
     * 格式化日期为中文格式
     */
    public static String formatToChinese(String dateStr) {
        Date date = parseDate(dateStr);
        return formatToChinese(date);
    }

    /**
     * 格式化日期为指定格式
     */
    public static String format(String dateStr, String pattern) {
        Date date = parseDate(dateStr);
        if (date == null) {
            return null;
        }
        return DateUtil.format(date, pattern);
    }

    /**
     * 格式化日期为指定格式
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return DateUtil.format(date, pattern);
    }

    /**
     * 验证日期字符串是否有效
     */
    public static boolean isValidDate(String dateStr) {
        return parseDate(dateStr) != null;
    }

    /**
     * 获取日期的各个部分
     */
    public static Map<String, Integer> getDateParts(String dateStr) {
        Date date = parseDate(dateStr);
        if (date == null) {
            return Collections.emptyMap();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Map<String, Integer> parts = new HashMap<>();
        parts.put("year", calendar.get(Calendar.YEAR));
        parts.put("month", calendar.get(Calendar.MONTH) + 1);
        parts.put("day", calendar.get(Calendar.DAY_OF_MONTH));

        return parts;
    }
}
