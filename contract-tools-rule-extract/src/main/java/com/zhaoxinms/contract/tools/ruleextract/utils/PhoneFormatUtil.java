package com.zhaoxinms.contract.tools.ruleextract.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 电话号码格式转换工具类
 * 支持手机号码、固定电话的解析和格式化
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class PhoneFormatUtil {

    /**
     * 手机号码正则（中国大陆）
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile(
        "\\d{7,12}"
    );

    /**
     * 固定电话正则（带区号）
     */
    private static final Pattern LANDLINE_PATTERN = Pattern.compile(
        "(\\d{3,4})[\\s-]?(\\d{7,8})"
    );

    /**
     * 400/800电话正则
     */
    private static final Pattern SERVICE_PATTERN = Pattern.compile(
        "[48]00[\\s-]?\\d{3}[\\s-]?\\d{4}"
    );

    /**
     * 解析电话号码
     */
    public static JSONObject parsePhone(String phoneStr) {
        if (StrUtil.isBlank(phoneStr)) {
            return null;
        }

        // 清理字符串
        String cleanPhone = phoneStr.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("（", "")
            .replace("）", "");

        try {
            // 判断电话类型并解析
            if (isMobile(cleanPhone)) {
                return parseMobile(cleanPhone);
            } else if (isServiceNumber(phoneStr)) {
                return parseServiceNumber(phoneStr);
            } else if (isLandline(phoneStr)) {
                return parseLandline(phoneStr);
            }

            log.warn("无法识别的电话号码格式: {}", phoneStr);
            return null;

        } catch (Exception e) {
            log.error("解析电话号码失败: {}", phoneStr, e);
            return null;
        }
    }

    /**
     * 判断是否为手机号码
     */
    public static boolean isMobile(String phoneStr) {
        if (StrUtil.isBlank(phoneStr)) {
            return false;
        }
        
        String cleanPhone = phoneStr.replaceAll("[^0-9]", "");
        return MOBILE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * 判断是否为固定电话
     */
    public static boolean isLandline(String phoneStr) {
        if (StrUtil.isBlank(phoneStr)) {
            return false;
        }
        
        return LANDLINE_PATTERN.matcher(phoneStr).find();
    }

    /**
     * 判断是否为400/800服务电话
     */
    public static boolean isServiceNumber(String phoneStr) {
        if (StrUtil.isBlank(phoneStr)) {
            return false;
        }
        
        return SERVICE_PATTERN.matcher(phoneStr).find();
    }

    /**
     * 解析手机号码
     */
    private static JSONObject parseMobile(String phoneStr) {
        JSONObject result = new JSONObject();
        result.put("raw", phoneStr);
        result.put("type", "mobile");
        
        // 格式化为 138-1234-5678
        String formatted = formatNumericPhone(phoneStr);
        result.put("formatted", formatted);
        
        // 提取运营商信息
        String carrier = phoneStr.length() == 11 ? getCarrier(phoneStr) : "unknown";
        result.put("carrier", carrier);
        
        // 脱敏显示
        String masked = maskNumericPhone(phoneStr);
        result.put("masked", masked);
        
        return result;
    }

    /**
     * 解析固定电话
     */
    private static JSONObject parseLandline(String phoneStr) {
        Matcher matcher = LANDLINE_PATTERN.matcher(phoneStr);
        if (!matcher.find()) {
            return null;
        }

        String areaCode = matcher.group(1);
        String number = matcher.group(2);

        JSONObject result = new JSONObject();
        result.put("raw", areaCode + number);
        result.put("type", "landline");
        result.put("areaCode", areaCode);
        result.put("number", number);
        
        // 格式化为 (021)12345678 或 (0755)1234567
        String formatted = "(" + areaCode + ")" + number;
        result.put("formatted", formatted);
        
        return result;
    }

    /**
     * 解析400/800服务电话
     */
    private static JSONObject parseServiceNumber(String phoneStr) {
        String cleanPhone = phoneStr.replaceAll("[^0-9]", "");
        
        JSONObject result = new JSONObject();
        result.put("raw", cleanPhone);
        result.put("type", "service");
        
        // 格式化为 400-123-4567
        String formatted = cleanPhone.substring(0, 3) + "-" + 
                          cleanPhone.substring(3, 6) + "-" + 
                          cleanPhone.substring(6);
        result.put("formatted", formatted);
        
        return result;
    }

    /**
     * 获取运营商信息
     */
    private static String getCarrier(String mobile) {
        if (mobile.length() != 11) {
            return "unknown";
        }

        String prefix = mobile.substring(0, 3);
        
        // 中国移动
        if (prefix.matches("13[4-9]|14[7-8]|15[0-27-9]|17[2|8]|18[2-4|7-8]|19[5|8]")) {
            return "中国移动";
        }
        // 中国联通
        else if (prefix.matches("13[0-2]|14[5|6]|15[5-6]|16[6]|17[5-6]|18[5-6]|19[6]")) {
            return "中国联通";
        }
        // 中国电信
        else if (prefix.matches("13[3]|14[9]|15[3]|17[3|7]|18[0-1|9]|19[1|3|9]")) {
            return "中国电信";
        }
        // 中国广电
        else if (prefix.matches("19[2]")) {
            return "中国广电";
        }
        
        return "未知";
    }

    /**
     * 格式化电话号码
     */
    public static String formatPhone(String phoneStr) {
        JSONObject phoneInfo = parsePhone(phoneStr);
        if (phoneInfo == null) {
            return null;
        }
        
        return phoneInfo.getString("formatted");
    }

    /**
     * 脱敏电话号码
     */
    public static String maskPhone(String phoneStr) {
        if (StrUtil.isBlank(phoneStr)) {
            return phoneStr;
        }

        String cleanPhone = phoneStr.replaceAll("[^0-9]", "");
        if (StrUtil.isBlank(cleanPhone)) {
            return phoneStr;
        }

        if (cleanPhone.length() >= 7 && cleanPhone.length() <= 12) {
            return maskNumericPhone(cleanPhone);
        }

        return phoneStr;
    }

    /**
     * 验证电话号码是否有效
     */
    public static boolean isValidPhone(String phoneStr) {
        return parsePhone(phoneStr) != null;
    }

    private static String formatNumericPhone(String phoneStr) {
        int length = phoneStr.length();
        if (length <= 4) {
            return phoneStr;
        }

        if (length == 11) {
            return phoneStr.substring(0, 3) + "-" + phoneStr.substring(3, 7) + "-" + phoneStr.substring(7);
        }

        int split = Math.max(3, length - 4);
        split = Math.min(split, length - 1);
        return phoneStr.substring(0, split) + "-" + phoneStr.substring(split);
    }

    private static String maskNumericPhone(String phoneStr) {
        int length = phoneStr.length();
        if (length <= 4) {
            return phoneStr;
        }

        int prefix = Math.min(3, length - 2);
        int suffix = Math.min(2, length - prefix);
        int maskCount = Math.max(0, length - prefix - suffix);

        StringBuilder masked = new StringBuilder();
        masked.append(phoneStr, 0, prefix);
        for (int i = 0; i < maskCount; i++) {
            masked.append('*');
        }
        masked.append(phoneStr.substring(length - suffix));
        return masked.toString();
    }
}
