package com.zhaoxinms.contract.tools.compare.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文本标准化工具类
 * 用于统一处理中英文标点符号、空格等，提高比对准确性
 */
public class TextNormalizer {
    
    /**
     * 中英文标点符号映射表
     * 将中文标点符号统一转换为英文标点符号
     */
    private static final Map<String, String> PUNCTUATION_MAP = new HashMap<>();
    
    static {
        // 括号
        PUNCTUATION_MAP.put("（", "(");
        PUNCTUATION_MAP.put("）", ")");
        PUNCTUATION_MAP.put("【", "[");
        PUNCTUATION_MAP.put("】", "]");
        PUNCTUATION_MAP.put("｛", "{");
        PUNCTUATION_MAP.put("｝", "}");
        
        // 标点符号
        PUNCTUATION_MAP.put("：", ":");
        PUNCTUATION_MAP.put("；", ";");
        PUNCTUATION_MAP.put("，", ",");
        PUNCTUATION_MAP.put("。", ".");
        PUNCTUATION_MAP.put("？", "?");
        PUNCTUATION_MAP.put("！", "!");
        
        // 引号
        PUNCTUATION_MAP.put("“", "\"");
        PUNCTUATION_MAP.put("”", "\"");
        PUNCTUATION_MAP.put("‘", "'");
        PUNCTUATION_MAP.put("’", "'");
        PUNCTUATION_MAP.put("｀", "`");
        
        // 破折号和连接符
        PUNCTUATION_MAP.put("——", "--");
        PUNCTUATION_MAP.put("—", "-");
        PUNCTUATION_MAP.put("－", "-");
        PUNCTUATION_MAP.put("～", "~");
        
        // 省略号
        PUNCTUATION_MAP.put("……", "..");
        PUNCTUATION_MAP.put("…", ".");
        
        // 其他符号
        PUNCTUATION_MAP.put("、", ".");
        PUNCTUATION_MAP.put(",", ".");
        PUNCTUATION_MAP.put("·", ".");
        PUNCTUATION_MAP.put("＊", "*");
        PUNCTUATION_MAP.put("＃", "#");
        PUNCTUATION_MAP.put("＆", "&");
        PUNCTUATION_MAP.put("％", "%");
        PUNCTUATION_MAP.put("＠", "@");
        PUNCTUATION_MAP.put("＋", "+");
        PUNCTUATION_MAP.put("＝", "=");
        PUNCTUATION_MAP.put("＜", "<");
        PUNCTUATION_MAP.put("＞", ">");
        PUNCTUATION_MAP.put("｜", "|");
        PUNCTUATION_MAP.put("＼", "\\");
        PUNCTUATION_MAP.put("／", "/");
        
        // 数字符号
        PUNCTUATION_MAP.put("０", "0");
        PUNCTUATION_MAP.put("１", "1");
        PUNCTUATION_MAP.put("２", "2");
        PUNCTUATION_MAP.put("３", "3");
        PUNCTUATION_MAP.put("４", "4");
        PUNCTUATION_MAP.put("５", "5");
        PUNCTUATION_MAP.put("６", "6");
        PUNCTUATION_MAP.put("７", "7");
        PUNCTUATION_MAP.put("８", "8");
        PUNCTUATION_MAP.put("９", "9");
        
        //金额常见识别错误
        PUNCTUATION_MAP.put("貳", "贰");
        PUNCTUATION_MAP.put("參", "叁");
        PUNCTUATION_MAP.put("陸", "陆");
        PUNCTUATION_MAP.put("陌", "佰");
        PUNCTUATION_MAP.put("萬", "万");
        PUNCTUATION_MAP.put("億", "亿");
        
        // 更多常见错误项
        PUNCTUATION_MAP.put("经营商", "经营者");
        PUNCTUATION_MAP.put("購", "购");
        PUNCTUATION_MAP.put("羔", "盖");
        PUNCTUATION_MAP.put("运营", "经营");
        PUNCTUATION_MAP.put("買", "买");
        PUNCTUATION_MAP.put("説", "说");
    }
    
    /**
     * 空格字符正则表达式
     * 包括普通空格、全角空格、制表符、换行符等
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\u00A0\\u2000-\\u200F\\u2028\\u2029\\u3000]+");
    
    /**
     * 标准化文本
     * 
     * @param text 原始文本
     * @param ignoreCase 是否忽略大小写
     * @param ignoreWhitespace 是否忽略空格
     * @param ignorePunctuation 是否忽略标点符号
     * @return 标准化后的文本
     */
    public static String normalize(String text, boolean ignoreCase, boolean ignoreWhitespace, boolean ignorePunctuation) {
        if (text == null) {
            return "";
        }
        
        String result = text;
        
        // 1. 统一标点符号（中文转英文）
        result = normalizePunctuation(result);
        
        // 2. 处理大小写
        if (ignoreCase) {
            result = result.toLowerCase();
        }
        
        // 3. 处理空格
        if (ignoreWhitespace) {
            result = normalizeWhitespace(result);
        }
        
        // 4. 移除标点符号
        if (ignorePunctuation) {
            result = removePunctuation(result);
        }
        
        return result.trim();
    }
    
    /**
     * 标准化标点符号
     * 将中文标点符号转换为对应的英文标点符号
     * 
     * @param text 原始文本
     * @return 标准化后的文本
     */
    public static String normalizePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> entry : PUNCTUATION_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 标准化空格
     * 将多个连续空格替换为单个空格，并移除首尾空格
     * 
     * @param text 原始文本
     * @return 标准化后的文本
     */
    public static String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 将所有类型的空白字符替换为单个普通空格
        return WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();
    }
    
    /**
     * 移除标点符号
     * 移除所有标点符号，只保留字母、数字和中文字符
     * 
     * @param text 原始文本
     * @return 移除标点符号后的文本
     */
    public static String removePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 只保留字母、数字、中文字符和空格
        return text.replaceAll("[^\\p{L}\\p{N}\\s\\u4e00-\\u9fff]", "");
    }
    
    /**
     * 快速标准化方法
     * 使用默认设置进行文本标准化
     * 
     * @param text 原始文本
     * @return 标准化后的文本
     */
    public static String quickNormalize(String text) {
        return normalize(text, true, true, false);
    }
    
    /**
     * 比对专用标准化方法
     * 针对文档比对场景优化的标准化处理
     * 
     * @param text 原始文本
     * @param ignoreCase 是否忽略大小写
     * @param ignoreWhitespace 是否忽略空格
     * @param ignorePunctuation 是否忽略标点符号
     * @return 标准化后的文本
     */
    public static String normalizeForComparison(String text, boolean ignoreCase, boolean ignoreWhitespace, boolean ignorePunctuation) {
        if (text == null) {
            return "";
        }
        
        String result = text;
        
        // 1. 首先统一标点符号
        result = normalizePunctuation(result);
        
        // 2. 标准化空格（即使不忽略空格，也要统一空格类型）
        result = normalizeWhitespace(result);
        
        // 3. 处理大小写
        if (ignoreCase) {
            result = result.toLowerCase();
        }
        
        // 4. 如果忽略空格，则移除所有空格
        if (ignoreWhitespace) {
            result = result.replaceAll("\\s+", "");
        }
        
        // 5. 如果忽略标点符号，则移除标点符号
        if (ignorePunctuation) {
            result = removePunctuation(result);
        }
        
        return result;
    }
    
    /**
     * 检查两个文本在标准化后是否相等
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @param ignoreCase 是否忽略大小写
     * @param ignoreWhitespace 是否忽略空格
     * @param ignorePunctuation 是否忽略标点符号
     * @return 是否相等
     */
    public static boolean isEqual(String text1, String text2, boolean ignoreCase, boolean ignoreWhitespace, boolean ignorePunctuation) {
        String normalized1 = normalizeForComparison(text1, ignoreCase, ignoreWhitespace, ignorePunctuation);
        String normalized2 = normalizeForComparison(text2, ignoreCase, ignoreWhitespace, ignorePunctuation);
        return normalized1.equals(normalized2);
    }
}