package com.zhaoxinms.contract.tools.ruleextract.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间格式转换工具类
 * 支持多种时间格式的解析和统一输出
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class TimeFormatUtil {

    /**
     * 标准时间格式
     */
    public static final String STANDARD_TIME_FORMAT = "HH:mm:ss";

    /**
     * 简短时间格式
     */
    public static final String SHORT_TIME_FORMAT = "HH:mm";

    /**
     * 中文时间映射
     */
    private static final Map<String, Integer> CHINESE_TIME_MAP = new HashMap<>();
    static {
        // 中文数字
        CHINESE_TIME_MAP.put("零", 0);
        CHINESE_TIME_MAP.put("一", 1);
        CHINESE_TIME_MAP.put("二", 2);
        CHINESE_TIME_MAP.put("两", 2);
        CHINESE_TIME_MAP.put("三", 3);
        CHINESE_TIME_MAP.put("四", 4);
        CHINESE_TIME_MAP.put("五", 5);
        CHINESE_TIME_MAP.put("六", 6);
        CHINESE_TIME_MAP.put("七", 7);
        CHINESE_TIME_MAP.put("八", 8);
        CHINESE_TIME_MAP.put("九", 9);
        CHINESE_TIME_MAP.put("十", 10);
    }

    /**
     * 解析时间字符串
     * 支持格式：14:30:00, 14:30, 下午2点30分, 14时30分 等
     */
    public static String parseTime(String timeStr) {
        if (StrUtil.isBlank(timeStr)) {
            return null;
        }

        timeStr = timeStr.trim();

        try {
            // 尝试解析中文时间
            String chineseTime = parseChineseTime(timeStr);
            if (chineseTime != null) {
                return chineseTime;
            }

            // 尝试解析 HH:mm:ss 格式
            if (timeStr.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                int second = Integer.parseInt(parts[2]);
                
                if (isValidTime(hour, minute, second)) {
                    return String.format("%02d:%02d:%02d", hour, minute, second);
                }
            }

            // 尝试解析 HH:mm 格式
            if (timeStr.matches("\\d{1,2}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                
                if (isValidTime(hour, minute, 0)) {
                    return String.format("%02d:%02d:00", hour, minute);
                }
            }

            // 尝试解析 HH时mm分 格式
            Pattern pattern1 = Pattern.compile("(\\d{1,2})时(\\d{1,2})分");
            Matcher matcher1 = pattern1.matcher(timeStr);
            if (matcher1.find()) {
                int hour = Integer.parseInt(matcher1.group(1));
                int minute = Integer.parseInt(matcher1.group(2));
                
                if (isValidTime(hour, minute, 0)) {
                    return String.format("%02d:%02d:00", hour, minute);
                }
            }

            // 尝试解析 HH点mm分 格式
            Pattern pattern2 = Pattern.compile("(\\d{1,2})点(\\d{1,2})分");
            Matcher matcher2 = pattern2.matcher(timeStr);
            if (matcher2.find()) {
                int hour = Integer.parseInt(matcher2.group(1));
                int minute = Integer.parseInt(matcher2.group(2));
                
                if (isValidTime(hour, minute, 0)) {
                    return String.format("%02d:%02d:00", hour, minute);
                }
            }

            log.warn("无法解析时间字符串: {}", timeStr);
            return null;

        } catch (Exception e) {
            log.error("解析时间失败: {}", timeStr, e);
            return null;
        }
    }

    /**
     * 解析中文时间
     * 支持：上午9点30分、下午2点、晚上8点半 等
     */
    private static String parseChineseTime(String timeStr) {
        try {
            int hourOffset = 0;

            // 判断上午/下午/晚上
            if (timeStr.startsWith("下午") || timeStr.startsWith("午后")) {
                hourOffset = 12;
                timeStr = timeStr.substring(2);
            } else if (timeStr.startsWith("上午") || timeStr.startsWith("早上")) {
                timeStr = timeStr.substring(2);
            } else if (timeStr.startsWith("晚上") || timeStr.startsWith("夜里")) {
                hourOffset = 12;
                timeStr = timeStr.substring(2);
            } else if (timeStr.startsWith("中午")) {
                hourOffset = 12;
                timeStr = timeStr.substring(2);
            }

            // 提取小时
            Pattern hourPattern = Pattern.compile("([零一二三四五六七八九十\\d]+)[点時时]");
            Matcher hourMatcher = hourPattern.matcher(timeStr);
            
            if (!hourMatcher.find()) {
                return null;
            }

            String hourStr = hourMatcher.group(1);
            int hour = parseChineseNumber(hourStr);
            
            // 处理12小时制
            if (hour <= 12 && hourOffset > 0) {
                hour += hourOffset;
                if (hour == 24) {
                    hour = 0;
                }
            }

            // 提取分钟
            int minute = 0;
            Pattern minutePattern = Pattern.compile("([零一二三四五六七八九十\\d]+)分");
            Matcher minuteMatcher = minutePattern.matcher(timeStr);
            
            if (minuteMatcher.find()) {
                String minuteStr = minuteMatcher.group(1);
                minute = parseChineseNumber(minuteStr);
            } else if (timeStr.contains("半")) {
                minute = 30;
            } else if (timeStr.contains("一刻")) {
                minute = 15;
            } else if (timeStr.contains("三刻")) {
                minute = 45;
            }

            if (isValidTime(hour, minute, 0)) {
                return String.format("%02d:%02d:00", hour, minute);
            }

        } catch (Exception e) {
            // 解析失败
        }
        return null;
    }

    /**
     * 解析中文数字
     */
    private static int parseChineseNumber(String numStr) {
        if (StrUtil.isBlank(numStr)) {
            return -1;
        }

        // 如果是纯数字，直接转换
        if (numStr.matches("\\d+")) {
            return Integer.parseInt(numStr);
        }

        int result = 0;
        int temp = 0;

        for (char c : numStr.toCharArray()) {
            String charStr = String.valueOf(c);
            Integer num = CHINESE_TIME_MAP.get(charStr);
            
            if (num == null) {
                continue;
            }

            if (num == 10) {
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
        return result;
    }

    /**
     * 验证时间是否有效
     */
    private static boolean isValidTime(int hour, int minute, int second) {
        return hour >= 0 && hour < 24 && minute >= 0 && minute < 60 && second >= 0 && second < 60;
    }

    /**
     * 格式化时间为标准格式 (HH:mm:ss)
     */
    public static String formatToStandard(String timeStr) {
        return parseTime(timeStr);
    }

    /**
     * 格式化时间为简短格式 (HH:mm)
     */
    public static String formatToShort(String timeStr) {
        String standardTime = parseTime(timeStr);
        if (standardTime == null) {
            return null;
        }
        
        return standardTime.substring(0, 5); // 只取 HH:mm 部分
    }

    /**
     * 格式化时间为指定格式
     */
    public static String format(String timeStr, String pattern) {
        String standardTime = parseTime(timeStr);
        if (standardTime == null) {
            return null;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(STANDARD_TIME_FORMAT);
            Date date = inputFormat.parse(standardTime);
            
            SimpleDateFormat outputFormat = new SimpleDateFormat(pattern);
            return outputFormat.format(date);
        } catch (Exception e) {
            log.error("格式化时间失败: {}", timeStr, e);
            return null;
        }
    }

    /**
     * 验证时间字符串是否有效
     */
    public static boolean isValidTime(String timeStr) {
        return parseTime(timeStr) != null;
    }

    /**
     * 获取时间的各个部分
     */
    public static Map<String, Integer> getTimeParts(String timeStr) {
        String standardTime = parseTime(timeStr);
        if (standardTime == null) {
            return new HashMap<>();
        }

        String[] parts = standardTime.split(":");
        Map<String, Integer> timeParts = new HashMap<>();
        timeParts.put("hour", Integer.parseInt(parts[0]));
        timeParts.put("minute", Integer.parseInt(parts[1]));
        timeParts.put("second", Integer.parseInt(parts[2]));

        return timeParts;
    }
}
