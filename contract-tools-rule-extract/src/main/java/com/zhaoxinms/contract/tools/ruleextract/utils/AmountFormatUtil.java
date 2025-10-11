package com.zhaoxinms.contract.tools.ruleextract.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 金额格式转换工具类
 * 支持多种金额格式的解析和统一输出
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
public class AmountFormatUtil {

    /**
     * 金额单位
     */
    private static final String[] UNITS = {"", "万", "亿", "万亿"};

    /**
     * 大写数字
     */
    private static final char[] CN_UPPER_NUMBER = {
        '零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'
    };

    /**
     * 大写单位
     */
    private static final char[] CN_UPPER_UNIT = {
        '元', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿'
    };

    /**
     * 小数部分大写
     */
    private static final char[] CN_UPPER_FRACTION = {'角', '分'};

    /**
     * 解析金额字符串为BigDecimal
     * 支持多种格式：123456.78, 123,456.78, 12.3万, 人民币壹拾贰万叁仟肆佰伍拾陆元柒角捌分等
     */
    public static BigDecimal parseAmount(String amountStr) {
        if (StrUtil.isBlank(amountStr)) {
            return null;
        }

        // 清理字符串
        amountStr = amountStr.trim()
            .replace("人民币", "")
            .replace("￥", "")
            .replace("$", "")
            .replace("¥", "")
            .replace(" ", "");

        try {
            // 尝试解析中文大写金额
            BigDecimal chineseAmount = parseChineseAmount(amountStr);
            if (chineseAmount != null) {
                return chineseAmount;
            }

            // 尝试解析带单位的金额（如12.3万元）
            BigDecimal unitAmount = parseAmountWithUnit(amountStr);
            if (unitAmount != null) {
                return unitAmount;
            }

            // 去除千分位分隔符
            amountStr = amountStr.replace(",", "");

            // 尝试直接解析为数字
            return new BigDecimal(amountStr);

        } catch (Exception e) {
            log.warn("无法解析金额字符串: {}", amountStr);
            return null;
        }
    }

    /**
     * 解析中文大写金额
     * 如：壹拾贰万叁仟肆佰伍拾陆元柒角捌分
     */
    private static BigDecimal parseChineseAmount(String amountStr) {
        try {
            // 移除整字
            amountStr = amountStr.replace("整", "");

            // 分离元和角分
            String[] parts;
            if (amountStr.contains("元")) {
                parts = amountStr.split("元");
            } else if (amountStr.contains("圆")) {
                parts = amountStr.split("圆");
            } else {
                return null;
            }

            // 解析整数部分
            long intPart = parseChineseInteger(parts[0]);
            if (intPart < 0) {
                return null;
            }

            // 解析小数部分
            BigDecimal result = new BigDecimal(intPart);
            if (parts.length > 1 && StrUtil.isNotBlank(parts[1])) {
                String fractionStr = parts[1];
                int jiao = 0;
                int fen = 0;

                if (fractionStr.contains("角")) {
                    int jiaoIndex = fractionStr.indexOf("角");
                    String jiaoStr = fractionStr.substring(0, jiaoIndex);
                    jiao = chineseDigitToNumber(jiaoStr.charAt(jiaoStr.length() - 1));
                }

                if (fractionStr.contains("分")) {
                    int fenIndex = fractionStr.indexOf("分");
                    String fenStr = fractionStr.substring(fractionStr.indexOf("角") + 1, fenIndex);
                    if (fenStr.length() > 0) {
                        fen = chineseDigitToNumber(fenStr.charAt(fenStr.length() - 1));
                    }
                }

                BigDecimal fraction = new BigDecimal(jiao).multiply(new BigDecimal("0.1"))
                    .add(new BigDecimal(fen).multiply(new BigDecimal("0.01")));
                result = result.add(fraction);
            }

            return result;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析中文数字的整数部分
     */
    private static long parseChineseInteger(String numStr) {
        try {
            long result = 0;
            long section = 0;
            long unit = 1;

            for (int i = numStr.length() - 1; i >= 0; i--) {
                char c = numStr.charAt(i);

                if (c == '亿') {
                    section += unit;
                    result += section * 100000000;
                    section = 0;
                    unit = 1;
                } else if (c == '万') {
                    section += unit;
                    result += section * 10000;
                    section = 0;
                    unit = 1;
                } else if (c == '仟' || c == '千') {
                    section += unit * 1000;
                    unit = 1;
                } else if (c == '佰' || c == '百') {
                    section += unit * 100;
                    unit = 1;
                } else if (c == '拾' || c == '十') {
                    section += unit * 10;
                    unit = 1;
                } else if (c == '零') {
                    // 零不处理
                } else {
                    int digit = chineseDigitToNumber(c);
                    if (digit >= 0) {
                        unit = digit;
                    }
                }
            }

            result += section + unit;
            return result;

        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 中文数字字符转阿拉伯数字
     */
    private static int chineseDigitToNumber(char c) {
        switch (c) {
            case '零': case '〇': return 0;
            case '壹': case '一': return 1;
            case '贰': case '二': return 2;
            case '叁': case '三': return 3;
            case '肆': case '四': return 4;
            case '伍': case '五': return 5;
            case '陆': case '六': return 6;
            case '柒': case '七': return 7;
            case '捌': case '八': return 8;
            case '玖': case '九': return 9;
            default: return -1;
        }
    }

    /**
     * 解析带单位的金额
     * 如：12.3万元、15.6万、200亿
     */
    private static BigDecimal parseAmountWithUnit(String amountStr) {
        try {
            // 移除元字
            amountStr = amountStr.replace("元", "");

            // 匹配数字+单位的模式
            Pattern pattern = Pattern.compile("([0-9.]+)\\s*(万|亿|万亿)?");
            Matcher matcher = pattern.matcher(amountStr);

            if (matcher.find()) {
                String numberStr = matcher.group(1);
                String unit = matcher.group(2);

                BigDecimal number = new BigDecimal(numberStr);

                if (StrUtil.isNotBlank(unit)) {
                    switch (unit) {
                        case "万":
                            number = number.multiply(new BigDecimal("10000"));
                            break;
                        case "亿":
                            number = number.multiply(new BigDecimal("100000000"));
                            break;
                        case "万亿":
                            number = number.multiply(new BigDecimal("1000000000000"));
                            break;
                    }
                }

                return number;
            }

        } catch (Exception e) {
            // 解析失败
        }
        return null;
    }

    /**
     * 格式化金额为标准格式（带千分位）
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }

    /**
     * 格式化金额为标准格式（不带千分位）
     */
    public static String formatAmountPlain(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 格式化金额为标准格式
     */
    public static String formatAmount(String amountStr) {
        BigDecimal amount = parseAmount(amountStr);
        return formatAmount(amount);
    }

    /**
     * 转换为中文大写金额
     */
    public static String toChinese(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        // 处理负数
        boolean negative = amount.compareTo(BigDecimal.ZERO) < 0;
        if (negative) {
            amount = amount.abs();
        }

        // 分离整数和小数部分
        long integerPart = amount.longValue();
        int fraction = amount.subtract(new BigDecimal(integerPart))
            .multiply(new BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

        // 转换整数部分
        String integerStr = convertIntegerToChinese(integerPart);

        // 转换小数部分
        StringBuilder result = new StringBuilder();
        if (negative) {
            result.append("负");
        }
        result.append(integerStr).append("元");

        if (fraction == 0) {
            result.append("整");
        } else {
            int jiao = fraction / 10;
            int fen = fraction % 10;

            if (jiao > 0) {
                result.append(CN_UPPER_NUMBER[jiao]).append("角");
            } else {
                result.append("零");
            }

            if (fen > 0) {
                result.append(CN_UPPER_NUMBER[fen]).append("分");
            }
        }

        return result.toString();
    }

    /**
     * 转换整数部分为中文大写
     */
    private static String convertIntegerToChinese(long number) {
        if (number == 0) {
            return "零";
        }

        StringBuilder result = new StringBuilder();
        boolean needZero = false;

        // 处理亿
        long yi = number / 100000000;
        if (yi > 0) {
            result.append(convertSection(yi)).append("亿");
            needZero = true;
        }

        // 处理万
        long wan = (number % 100000000) / 10000;
        if (wan > 0) {
            if (needZero && wan < 1000) {
                result.append("零");
            }
            result.append(convertSection(wan)).append("万");
            needZero = true;
        }

        // 处理个
        long ge = number % 10000;
        if (ge > 0) {
            if (needZero && ge < 1000) {
                result.append("零");
            }
            result.append(convertSection(ge));
        }

        return result.toString();
    }

    /**
     * 转换一个小节（四位数）为中文
     */
    private static String convertSection(long section) {
        StringBuilder result = new StringBuilder();
        boolean needZero = false;

        // 千位
        long qian = section / 1000;
        if (qian > 0) {
            result.append(CN_UPPER_NUMBER[(int)qian]).append("仟");
            needZero = true;
        }

        // 百位
        long bai = (section % 1000) / 100;
        if (bai > 0) {
            result.append(CN_UPPER_NUMBER[(int)bai]).append("佰");
            needZero = true;
        } else if (needZero) {
            needZero = false;
        }

        // 十位
        long shi = (section % 100) / 10;
        if (shi > 0) {
            if (!needZero && result.length() > 0) {
                result.append("零");
            }
            result.append(CN_UPPER_NUMBER[(int)shi]).append("拾");
            needZero = true;
        } else if (needZero && result.length() > 0) {
            result.append("零");
            needZero = false;
        }

        // 个位
        long ge = section % 10;
        if (ge > 0) {
            result.append(CN_UPPER_NUMBER[(int)ge]);
        }

        return result.toString();
    }

    /**
     * 转换为中文大写金额
     */
    public static String toChinese(String amountStr) {
        BigDecimal amount = parseAmount(amountStr);
        return toChinese(amount);
    }

    /**
     * 转换金额为JSON格式
     * 包含原始值、格式化值、中文大写等
     */
    public static JSONObject toJSON(String amountStr) {
        BigDecimal amount = parseAmount(amountStr);
        if (amount == null) {
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("value", amount);
        json.put("formatted", formatAmount(amount));
        json.put("plain", formatAmountPlain(amount));
        json.put("chinese", toChinese(amount));
        json.put("currency", "CNY");

        return json;
    }

    /**
     * 验证金额字符串是否有效
     */
    public static boolean isValidAmount(String amountStr) {
        return parseAmount(amountStr) != null;
    }
}
