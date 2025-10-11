package com.zhaoxinms.contract.tools.ruleextract.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 格式转换器主类
 * 提供统一的格式转换接口
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class FormatConverter {

    /**
     * 字段类型枚举
     */
    public enum FieldType {
        TEXT,           // 文本
        DATE,           // 日期
        TIME,           // 时间
        DATETIME,       // 日期时间
        AMOUNT,         // 金额
        PHONE,          // 电话
        EMAIL,          // 邮箱
        ID_CARD,        // 身份证
        NUMBER,         // 数字
        URL             // 网址
    }

    /**
     * 转换字段值
     * 
     * @param value 原始值
     * @param fieldType 字段类型
     * @param outputFormat 输出格式（JSON字符串）
     * @return 转换后的值
     */
    public static Object convert(String value, FieldType fieldType, String outputFormat) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            JSONObject formatConfig = null;
            if (StrUtil.isNotBlank(outputFormat)) {
                formatConfig = JSON.parseObject(outputFormat);
            }

            switch (fieldType) {
                case DATE:
                    return convertDate(value, formatConfig);
                case TIME:
                    return convertTime(value, formatConfig);
                case AMOUNT:
                    return convertAmount(value, formatConfig);
                case PHONE:
                    return convertPhone(value, formatConfig);
                case NUMBER:
                    return convertNumber(value, formatConfig);
                case EMAIL:
                    return convertEmail(value, formatConfig);
                case ID_CARD:
                    return convertIdCard(value, formatConfig);
                case TEXT:
                default:
                    return value;
            }
        } catch (Exception e) {
            log.error("格式转换失败: value={}, fieldType={}, outputFormat={}", value, fieldType, outputFormat, e);
            return value; // 转换失败时返回原始值
        }
    }

    /**
     * 转换日期
     */
    private static Object convertDate(String value, JSONObject formatConfig) {
        Date date = DateFormatUtil.parseDate(value);
        if (date == null) {
            return value;
        }

        JSONObject result = new JSONObject();
        result.put("raw", value);
        result.put("date", date);
        result.put("iso", DateFormatUtil.formatToISO(date));
        result.put("chinese", DateFormatUtil.formatToChinese(date));

        // 自定义格式
        if (formatConfig != null && formatConfig.containsKey("pattern")) {
            String pattern = formatConfig.getString("pattern");
            result.put("formatted", DateFormatUtil.format(date, pattern));
        } else {
            result.put("formatted", DateFormatUtil.formatToISO(date));
        }

        // 日期部分
        Map<String, Integer> parts = DateFormatUtil.getDateParts(value);
        result.put("parts", parts);

        return result;
    }

    /**
     * 转换时间
     */
    private static Object convertTime(String value, JSONObject formatConfig) {
        String standardTime = TimeFormatUtil.parseTime(value);
        if (standardTime == null) {
            return value;
        }

        JSONObject result = new JSONObject();
        result.put("raw", value);
        result.put("standard", standardTime);
        result.put("short", TimeFormatUtil.formatToShort(value));

        // 自定义格式
        if (formatConfig != null && formatConfig.containsKey("pattern")) {
            String pattern = formatConfig.getString("pattern");
            result.put("formatted", TimeFormatUtil.format(value, pattern));
        } else {
            result.put("formatted", standardTime);
        }

        // 时间部分
        Map<String, Integer> parts = TimeFormatUtil.getTimeParts(value);
        result.put("parts", parts);

        return result;
    }

    /**
     * 转换金额
     */
    private static Object convertAmount(String value, JSONObject formatConfig) {
        BigDecimal amount = AmountFormatUtil.parseAmount(value);
        if (amount == null) {
            return value;
        }

        JSONObject result = new JSONObject();
        result.put("raw", value);
        result.put("value", amount);
        result.put("formatted", AmountFormatUtil.formatAmount(amount));
        result.put("plain", AmountFormatUtil.formatAmountPlain(amount));
        result.put("chinese", AmountFormatUtil.toChinese(amount));
        result.put("currency", "CNY");

        // 自定义格式
        if (formatConfig != null) {
            if (formatConfig.containsKey("currency")) {
                result.put("currency", formatConfig.getString("currency"));
            }
            if (formatConfig.containsKey("scale")) {
                int scale = formatConfig.getIntValue("scale");
                result.put("value", amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
            }
        }

        return result;
    }

    /**
     * 转换电话号码
     */
    private static Object convertPhone(String value, JSONObject formatConfig) {
        JSONObject phoneInfo = PhoneFormatUtil.parsePhone(value);
        if (phoneInfo == null) {
            return value;
        }

        // 是否需要脱敏
        if (formatConfig != null && formatConfig.getBooleanValue("masked", false)) {
            phoneInfo.put("display", phoneInfo.getString("masked"));
        } else {
            phoneInfo.put("display", phoneInfo.getString("formatted"));
        }

        return phoneInfo;
    }

    /**
     * 转换数字
     */
    private static Object convertNumber(String value, JSONObject formatConfig) {
        try {
            // 移除千分位分隔符
            String cleanValue = value.replace(",", "").replace("，", "");
            
            // 判断是整数还是小数
            if (cleanValue.contains(".")) {
                BigDecimal decimal = new BigDecimal(cleanValue);
                
                JSONObject result = new JSONObject();
                result.put("raw", value);
                result.put("value", decimal);
                result.put("type", "decimal");

                // 自定义精度
                if (formatConfig != null && formatConfig.containsKey("scale")) {
                    int scale = formatConfig.getIntValue("scale");
                    result.put("formatted", decimal.setScale(scale, BigDecimal.ROUND_HALF_UP).toPlainString());
                } else {
                    result.put("formatted", decimal.toPlainString());
                }

                return result;
            } else {
                Long number = Long.parseLong(cleanValue);
                
                JSONObject result = new JSONObject();
                result.put("raw", value);
                result.put("value", number);
                result.put("type", "integer");
                result.put("formatted", String.format("%,d", number));

                return result;
            }
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 转换邮箱
     */
    private static Object convertEmail(String value, JSONObject formatConfig) {
        // 简单的邮箱格式验证
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return value;
        }

        JSONObject result = new JSONObject();
        result.put("raw", value);
        result.put("email", value.toLowerCase());

        // 提取用户名和域名
        String[] parts = value.split("@");
        if (parts.length == 2) {
            result.put("username", parts[0]);
            result.put("domain", parts[1]);
        }

        // 是否需要脱敏
        if (formatConfig != null && formatConfig.getBooleanValue("masked", false)) {
            String masked = value.substring(0, Math.min(3, value.indexOf("@"))) + "***@" + parts[1];
            result.put("display", masked);
        } else {
            result.put("display", value);
        }

        return result;
    }

    /**
     * 转换身份证号
     */
    private static Object convertIdCard(String value, JSONObject formatConfig) {
        // 清理空格和连字符
        String cleanValue = value.replaceAll("[\\s-]", "");

        // 验证长度
        if (cleanValue.length() != 15 && cleanValue.length() != 18) {
            return value;
        }

        JSONObject result = new JSONObject();
        result.put("raw", value);
        result.put("idCard", cleanValue);
        result.put("length", cleanValue.length());

        // 提取信息（18位身份证）
        if (cleanValue.length() == 18) {
            // 地区代码
            result.put("areaCode", cleanValue.substring(0, 6));
            
            // 出生日期
            String birthDate = cleanValue.substring(6, 14);
            String formattedBirth = birthDate.substring(0, 4) + "-" + 
                                   birthDate.substring(4, 6) + "-" + 
                                   birthDate.substring(6, 8);
            result.put("birthDate", formattedBirth);
            
            // 性别（倒数第二位，奇数为男，偶数为女）
            int genderCode = Integer.parseInt(String.valueOf(cleanValue.charAt(16)));
            result.put("gender", genderCode % 2 == 0 ? "女" : "男");
        }

        // 脱敏显示
        String masked = cleanValue.substring(0, 6) + "********" + cleanValue.substring(cleanValue.length() - 4);
        result.put("masked", masked);

        if (formatConfig != null && formatConfig.getBooleanValue("masked", false)) {
            result.put("display", masked);
        } else {
            result.put("display", cleanValue);
        }

        return result;
    }

    /**
     * 验证字段值
     */
    public static boolean validate(String value, FieldType fieldType) {
        if (StrUtil.isBlank(value)) {
            return false;
        }

        switch (fieldType) {
            case DATE:
                return DateFormatUtil.isValidDate(value);
            case TIME:
                return TimeFormatUtil.isValidTime(value);
            case AMOUNT:
                return AmountFormatUtil.isValidAmount(value);
            case PHONE:
                return PhoneFormatUtil.isValidPhone(value);
            case EMAIL:
                return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            case NUMBER:
                try {
                    new BigDecimal(value.replace(",", ""));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            case TEXT:
            default:
                return true;
        }
    }
}
