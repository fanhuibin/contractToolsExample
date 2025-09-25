package com.zhaoxinms.contract.tools.comparePRO.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unicode数学符号到LaTeX公式转换器
 * 参考pandoc-unicode-math实现，支持将Unicode数学符号转换为对应的LaTeX命令
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class UnicodeToLatexConverter {
    
    private static final Map<String, String> UNICODE_TO_LATEX_MAP = new HashMap<>();
    private static final Map<String, String> SUPERSCRIPT_MAP = new HashMap<>();
    private static final Map<String, String> SUBSCRIPT_MAP = new HashMap<>();
    private static final Pattern UNICODE_PATTERN = Pattern.compile("[\u0080-\uFFFF]");
    
    static {
        initializeBasicSymbols();
        initializeGreekLetters();
        initializeMathOperators();
        initializeArrowSymbols();
        initializeSetTheorySymbols();
        initializeLogicSymbols();
        initializeSuperscriptSubscript();
    }
    
    /**
     * 将包含Unicode数学符号的文本转换为LaTeX格式
     * 
     * @param unicodeText 包含Unicode符号的原始文本
     * @return 转换后的LaTeX格式文本
     */
    public static String convertToLatex(String unicodeText) {
        if (unicodeText == null || unicodeText.isEmpty()) {
            return unicodeText;
        }
        
        String result = unicodeText;
        
        // 处理上标和下标
        result = convertSuperscriptSubscript(result);
        
        // 处理其他Unicode符号
        result = convertUnicodeSymbols(result);
        
        return result;
    }
    
    /**
     * 转换Unicode符号到LaTeX命令
     */
    private static String convertUnicodeSymbols(String text) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            String latexCommand = UNICODE_TO_LATEX_MAP.get(character);
            
            if (latexCommand != null) {
                // 检查是否需要数学模式
                if (needsMathMode(latexCommand)) {
                    result.append("$").append(latexCommand).append("$");
                } else {
                    result.append(latexCommand);
                }
            } else {
                result.append(character);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 处理上标和下标转换
     */
    private static String convertSuperscriptSubscript(String text) {
        String result = text;
        
        // 转换上标
        for (Map.Entry<String, String> entry : SUPERSCRIPT_MAP.entrySet()) {
            result = result.replace(entry.getKey(), "^{" + entry.getValue() + "}");
        }
        
        // 转换下标
        for (Map.Entry<String, String> entry : SUBSCRIPT_MAP.entrySet()) {
            result = result.replace(entry.getKey(), "_{" + entry.getValue() + "}");
        }
        
        return result;
    }
    
    /**
     * 判断LaTeX命令是否需要数学模式
     */
    private static boolean needsMathMode(String latexCommand) {
        return latexCommand.startsWith("\\") && 
               !latexCommand.startsWith("\\text") &&
               !latexCommand.equals("\\&") &&
               !latexCommand.equals("\\%") &&
               !latexCommand.equals("\\$");
    }
    
    /**
     * 初始化基本数学符号映射
     */
    private static void initializeBasicSymbols() {
        // 基本运算符
        UNICODE_TO_LATEX_MAP.put("±", "\\pm");
        UNICODE_TO_LATEX_MAP.put("∓", "\\mp");
        UNICODE_TO_LATEX_MAP.put("×", "\\times");
        UNICODE_TO_LATEX_MAP.put("÷", "\\div");
        UNICODE_TO_LATEX_MAP.put("∗", "\\ast");
        UNICODE_TO_LATEX_MAP.put("⋆", "\\star");
        UNICODE_TO_LATEX_MAP.put("∘", "\\circ");
        UNICODE_TO_LATEX_MAP.put("∙", "\\bullet");
        
        // 关系符号
        UNICODE_TO_LATEX_MAP.put("≤", "\\leq");
        UNICODE_TO_LATEX_MAP.put("≥", "\\geq");
        UNICODE_TO_LATEX_MAP.put("≠", "\\neq");
        UNICODE_TO_LATEX_MAP.put("≈", "\\approx");
        UNICODE_TO_LATEX_MAP.put("≡", "\\equiv");
        UNICODE_TO_LATEX_MAP.put("∼", "\\sim");
        UNICODE_TO_LATEX_MAP.put("≃", "\\simeq");
        UNICODE_TO_LATEX_MAP.put("∝", "\\propto");
        
        // 特殊符号
        UNICODE_TO_LATEX_MAP.put("∞", "\\infty");
        UNICODE_TO_LATEX_MAP.put("∂", "\\partial");
        UNICODE_TO_LATEX_MAP.put("∇", "\\nabla");
        UNICODE_TO_LATEX_MAP.put("△", "\\triangle");
        UNICODE_TO_LATEX_MAP.put("□", "\\square");
        UNICODE_TO_LATEX_MAP.put("◇", "\\diamond");
        UNICODE_TO_LATEX_MAP.put("℘", "\\wp");
        UNICODE_TO_LATEX_MAP.put("ℜ", "\\Re");
        UNICODE_TO_LATEX_MAP.put("ℑ", "\\Im");
        UNICODE_TO_LATEX_MAP.put("ℝ", "\\mathbb{R}");
        UNICODE_TO_LATEX_MAP.put("ℂ", "\\mathbb{C}");
        UNICODE_TO_LATEX_MAP.put("ℕ", "\\mathbb{N}");
        UNICODE_TO_LATEX_MAP.put("ℚ", "\\mathbb{Q}");
        UNICODE_TO_LATEX_MAP.put("ℤ", "\\mathbb{Z}");
    }
    
    /**
     * 初始化希腊字母映射
     */
    private static void initializeGreekLetters() {
        // 小写希腊字母
        UNICODE_TO_LATEX_MAP.put("α", "\\alpha");
        UNICODE_TO_LATEX_MAP.put("β", "\\beta");
        UNICODE_TO_LATEX_MAP.put("γ", "\\gamma");
        UNICODE_TO_LATEX_MAP.put("δ", "\\delta");
        UNICODE_TO_LATEX_MAP.put("ε", "\\epsilon");
        UNICODE_TO_LATEX_MAP.put("ζ", "\\zeta");
        UNICODE_TO_LATEX_MAP.put("η", "\\eta");
        UNICODE_TO_LATEX_MAP.put("θ", "\\theta");
        UNICODE_TO_LATEX_MAP.put("ι", "\\iota");
        UNICODE_TO_LATEX_MAP.put("κ", "\\kappa");
        UNICODE_TO_LATEX_MAP.put("λ", "\\lambda");
        UNICODE_TO_LATEX_MAP.put("μ", "\\mu");
        UNICODE_TO_LATEX_MAP.put("ν", "\\nu");
        UNICODE_TO_LATEX_MAP.put("ξ", "\\xi");
        UNICODE_TO_LATEX_MAP.put("ο", "\\omicron");
        UNICODE_TO_LATEX_MAP.put("π", "\\pi");
        UNICODE_TO_LATEX_MAP.put("ρ", "\\rho");
        UNICODE_TO_LATEX_MAP.put("σ", "\\sigma");
        UNICODE_TO_LATEX_MAP.put("τ", "\\tau");
        UNICODE_TO_LATEX_MAP.put("υ", "\\upsilon");
        UNICODE_TO_LATEX_MAP.put("φ", "\\phi");
        UNICODE_TO_LATEX_MAP.put("χ", "\\chi");
        UNICODE_TO_LATEX_MAP.put("ψ", "\\psi");
        UNICODE_TO_LATEX_MAP.put("ω", "\\omega");
        
        // 大写希腊字母
        UNICODE_TO_LATEX_MAP.put("Α", "\\Alpha");
        UNICODE_TO_LATEX_MAP.put("Β", "\\Beta");
        UNICODE_TO_LATEX_MAP.put("Γ", "\\Gamma");
        UNICODE_TO_LATEX_MAP.put("Δ", "\\Delta");
        UNICODE_TO_LATEX_MAP.put("Ε", "\\Epsilon");
        UNICODE_TO_LATEX_MAP.put("Ζ", "\\Zeta");
        UNICODE_TO_LATEX_MAP.put("Η", "\\Eta");
        UNICODE_TO_LATEX_MAP.put("Θ", "\\Theta");
        UNICODE_TO_LATEX_MAP.put("Ι", "\\Iota");
        UNICODE_TO_LATEX_MAP.put("Κ", "\\Kappa");
        UNICODE_TO_LATEX_MAP.put("Λ", "\\Lambda");
        UNICODE_TO_LATEX_MAP.put("Μ", "\\Mu");
        UNICODE_TO_LATEX_MAP.put("Ν", "\\Nu");
        UNICODE_TO_LATEX_MAP.put("Ξ", "\\Xi");
        UNICODE_TO_LATEX_MAP.put("Ο", "\\Omicron");
        UNICODE_TO_LATEX_MAP.put("Π", "\\Pi");
        UNICODE_TO_LATEX_MAP.put("Ρ", "\\Rho");
        UNICODE_TO_LATEX_MAP.put("Σ", "\\Sigma");
        UNICODE_TO_LATEX_MAP.put("Τ", "\\Tau");
        UNICODE_TO_LATEX_MAP.put("Υ", "\\Upsilon");
        UNICODE_TO_LATEX_MAP.put("Φ", "\\Phi");
        UNICODE_TO_LATEX_MAP.put("Χ", "\\Chi");
        UNICODE_TO_LATEX_MAP.put("Ψ", "\\Psi");
        UNICODE_TO_LATEX_MAP.put("Ω", "\\Omega");
        
        // 变体希腊字母
        UNICODE_TO_LATEX_MAP.put("ϑ", "\\vartheta");
        UNICODE_TO_LATEX_MAP.put("ϕ", "\\varphi");
        UNICODE_TO_LATEX_MAP.put("ϖ", "\\varpi");
        UNICODE_TO_LATEX_MAP.put("ϱ", "\\varrho");
        UNICODE_TO_LATEX_MAP.put("ς", "\\varsigma");
        UNICODE_TO_LATEX_MAP.put("ε", "\\varepsilon");
    }
    
    /**
     * 初始化数学运算符映射
     */
    private static void initializeMathOperators() {
        // 求和、积分等
        UNICODE_TO_LATEX_MAP.put("∑", "\\sum");
        UNICODE_TO_LATEX_MAP.put("∏", "\\prod");
        UNICODE_TO_LATEX_MAP.put("∐", "\\coprod");
        UNICODE_TO_LATEX_MAP.put("∫", "\\int");
        UNICODE_TO_LATEX_MAP.put("∬", "\\iint");
        UNICODE_TO_LATEX_MAP.put("∭", "\\iiint");
        UNICODE_TO_LATEX_MAP.put("∮", "\\oint");
        
        // 根号等
        UNICODE_TO_LATEX_MAP.put("√", "\\sqrt");
        UNICODE_TO_LATEX_MAP.put("∛", "\\sqrt[3]");
        UNICODE_TO_LATEX_MAP.put("∜", "\\sqrt[4]");
        
        // 函数
        UNICODE_TO_LATEX_MAP.put("sin", "\\sin");
        UNICODE_TO_LATEX_MAP.put("cos", "\\cos");
        UNICODE_TO_LATEX_MAP.put("tan", "\\tan");
        UNICODE_TO_LATEX_MAP.put("log", "\\log");
        UNICODE_TO_LATEX_MAP.put("ln", "\\ln");
        UNICODE_TO_LATEX_MAP.put("exp", "\\exp");
        UNICODE_TO_LATEX_MAP.put("max", "\\max");
        UNICODE_TO_LATEX_MAP.put("min", "\\min");
        UNICODE_TO_LATEX_MAP.put("lim", "\\lim");
    }
    
    /**
     * 初始化箭头符号映射
     */
    private static void initializeArrowSymbols() {
        UNICODE_TO_LATEX_MAP.put("←", "\\leftarrow");
        UNICODE_TO_LATEX_MAP.put("→", "\\rightarrow");
        UNICODE_TO_LATEX_MAP.put("↑", "\\uparrow");
        UNICODE_TO_LATEX_MAP.put("↓", "\\downarrow");
        UNICODE_TO_LATEX_MAP.put("↔", "\\leftrightarrow");
        UNICODE_TO_LATEX_MAP.put("↕", "\\updownarrow");
        UNICODE_TO_LATEX_MAP.put("⇐", "\\Leftarrow");
        UNICODE_TO_LATEX_MAP.put("⇒", "\\Rightarrow");
        UNICODE_TO_LATEX_MAP.put("⇑", "\\Uparrow");
        UNICODE_TO_LATEX_MAP.put("⇓", "\\Downarrow");
        UNICODE_TO_LATEX_MAP.put("⇔", "\\Leftrightarrow");
        UNICODE_TO_LATEX_MAP.put("⇕", "\\Updownarrow");
        UNICODE_TO_LATEX_MAP.put("↦", "\\mapsto");
        UNICODE_TO_LATEX_MAP.put("↼", "\\leftharpoonup");
        UNICODE_TO_LATEX_MAP.put("⇀", "\\rightharpoonup");
    }
    
    /**
     * 初始化集合论符号映射
     */
    private static void initializeSetTheorySymbols() {
        UNICODE_TO_LATEX_MAP.put("∈", "\\in");
        UNICODE_TO_LATEX_MAP.put("∉", "\\notin");
        UNICODE_TO_LATEX_MAP.put("∋", "\\ni");
        UNICODE_TO_LATEX_MAP.put("⊂", "\\subset");
        UNICODE_TO_LATEX_MAP.put("⊃", "\\supset");
        UNICODE_TO_LATEX_MAP.put("⊆", "\\subseteq");
        UNICODE_TO_LATEX_MAP.put("⊇", "\\supseteq");
        UNICODE_TO_LATEX_MAP.put("∪", "\\cup");
        UNICODE_TO_LATEX_MAP.put("∩", "\\cap");
        UNICODE_TO_LATEX_MAP.put("∅", "\\emptyset");
        UNICODE_TO_LATEX_MAP.put("∀", "\\forall");
        UNICODE_TO_LATEX_MAP.put("∃", "\\exists");
        UNICODE_TO_LATEX_MAP.put("∄", "\\nexists");
    }
    
    /**
     * 初始化逻辑符号映射
     */
    private static void initializeLogicSymbols() {
        UNICODE_TO_LATEX_MAP.put("∧", "\\land");
        UNICODE_TO_LATEX_MAP.put("∨", "\\lor");
        UNICODE_TO_LATEX_MAP.put("¬", "\\neg");
        UNICODE_TO_LATEX_MAP.put("⊤", "\\top");
        UNICODE_TO_LATEX_MAP.put("⊥", "\\bot");
        UNICODE_TO_LATEX_MAP.put("⊢", "\\vdash");
        UNICODE_TO_LATEX_MAP.put("⊨", "\\models");
        UNICODE_TO_LATEX_MAP.put("∴", "\\therefore");
        UNICODE_TO_LATEX_MAP.put("∵", "\\because");
    }
    
    /**
     * 初始化上标下标映射
     */
    private static void initializeSuperscriptSubscript() {
        // 上标数字
        SUPERSCRIPT_MAP.put("⁰", "0");
        SUPERSCRIPT_MAP.put("¹", "1");
        SUPERSCRIPT_MAP.put("²", "2");
        SUPERSCRIPT_MAP.put("³", "3");
        SUPERSCRIPT_MAP.put("⁴", "4");
        SUPERSCRIPT_MAP.put("⁵", "5");
        SUPERSCRIPT_MAP.put("⁶", "6");
        SUPERSCRIPT_MAP.put("⁷", "7");
        SUPERSCRIPT_MAP.put("⁸", "8");
        SUPERSCRIPT_MAP.put("⁹", "9");
        SUPERSCRIPT_MAP.put("⁺", "+");
        SUPERSCRIPT_MAP.put("⁻", "-");
        SUPERSCRIPT_MAP.put("⁼", "=");
        SUPERSCRIPT_MAP.put("⁽", "(");
        SUPERSCRIPT_MAP.put("⁾", ")");
        
        // 下标数字
        SUBSCRIPT_MAP.put("₀", "0");
        SUBSCRIPT_MAP.put("₁", "1");
        SUBSCRIPT_MAP.put("₂", "2");
        SUBSCRIPT_MAP.put("₃", "3");
        SUBSCRIPT_MAP.put("₄", "4");
        SUBSCRIPT_MAP.put("₅", "5");
        SUBSCRIPT_MAP.put("₆", "6");
        SUBSCRIPT_MAP.put("₇", "7");
        SUBSCRIPT_MAP.put("₈", "8");
        SUBSCRIPT_MAP.put("₉", "9");
        SUBSCRIPT_MAP.put("₊", "+");
        SUBSCRIPT_MAP.put("₋", "-");
        SUBSCRIPT_MAP.put("₌", "=");
        SUBSCRIPT_MAP.put("₍", "(");
        SUBSCRIPT_MAP.put("₎", ")");
    }
    
    /**
     * 获取所有支持的Unicode符号
     * 
     * @return 支持的Unicode符号集合
     */
    public static Map<String, String> getSupportedSymbols() {
        Map<String, String> allSymbols = new HashMap<>();
        allSymbols.putAll(UNICODE_TO_LATEX_MAP);
        allSymbols.putAll(SUPERSCRIPT_MAP);
        allSymbols.putAll(SUBSCRIPT_MAP);
        return allSymbols;
    }
    
    /**
     * 检查是否包含Unicode数学符号
     * 
     * @param text 待检查的文本
     * @return 是否包含Unicode数学符号
     */
    public static boolean containsUnicodeSymbols(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        Matcher matcher = UNICODE_PATTERN.matcher(text);
        while (matcher.find()) {
            String unicodeChar = matcher.group();
            if (UNICODE_TO_LATEX_MAP.containsKey(unicodeChar) ||
                SUPERSCRIPT_MAP.containsKey(unicodeChar) ||
                SUBSCRIPT_MAP.containsKey(unicodeChar)) {
                return true;
            }
        }
        return false;
    }
}
