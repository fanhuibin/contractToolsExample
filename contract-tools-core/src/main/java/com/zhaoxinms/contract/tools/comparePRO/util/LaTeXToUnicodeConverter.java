package com.zhaoxinms.contract.tools.comparePRO.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LaTeX数学符号到Unicode转换器
 * 参考pandoc-unicode-math项目中LatexToUnicode.hs的实现思路
 * 支持将LaTeX数学命令转换为对应的Unicode符号
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class LaTeXToUnicodeConverter {
    
    private static final Map<String, String> LATEX_TO_UNICODE_MAP = new HashMap<>();
    private static final Map<String, String> SUBSCRIPT_MAP = new HashMap<>();
    private static final Map<String, String> SUPERSCRIPT_MAP = new HashMap<>();
    
    // 正则表达式模式
    private static final Pattern MATH_MODE_PATTERN = Pattern.compile("\\$([^$]+)\\$");
    private static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile("\\\\([a-zA-Z]+)");
    private static final Pattern LATEX_ESCAPED_CHAR_PATTERN = Pattern.compile("\\\\([%$&#_{}])");  // 转义字符
    private static final Pattern MATHBB_PATTERN = Pattern.compile("\\\\mathbb\\{([A-Z])\\}");
    private static final Pattern SUBSCRIPT_PATTERN = Pattern.compile("_+\\{([^}]+)\\}|_+([a-zA-Z0-9])");
    private static final Pattern SUPERSCRIPT_PATTERN = Pattern.compile("\\^\\{([^}]+)\\}|\\^([a-zA-Z0-9])");
    
    static {
        initializeBasicCommands();
        initializeGreekLetters();
        initializeMathOperators();
        initializeArrowSymbols();
        initializeSetTheorySymbols();
        initializeLogicSymbols();
        initializeSubscriptSuperscript();
        initializeMathSets();
    }
    
    /**
     * 将包含LaTeX数学命令的文本转换为Unicode格式
     * 
     * @param latexText 包含LaTeX命令的原始文本
     * @return 转换后的Unicode格式文本
     */
    public static String convertToUnicode(String latexText) {
        if (latexText == null || latexText.isEmpty()) {
            return latexText;
        }
        
        String result = latexText;
        
        // 预处理：修复常见的LaTeX格式问题
        result = preprocessLatex(result);
        
        // 处理数学模式 $...$
        result = processMathMode(result);
        
        return result;
    }
    
    /**
     * 预处理LaTeX文本，修复常见格式问题
     */
    private static String preprocessLatex(String text) {
        String result = text;
        
        // 修复 $B1$ -> $B_1$ 这样的模式
        result = result.replaceAll("\\$B(\\d+)\\$", "\\$B_$1\\$");
        result = result.replaceAll("\\$F\\{(\\d+)\\}\\$", "\\$F_{$1}\\$");
        result = result.replaceAll("\\$F\\{t(\\d+)\\}\\$", "\\$F_{t$1}\\$");
        
        // 修复 $P___0$ -> $P_0$ 这样的模式
        result = result.replaceAll("\\$P_+0\\$", "\\$P_0\\$");
        
        return result;
    }
    
    /**
     * 处理数学模式内容
     */
    private static String processMathMode(String text) {
        Matcher matcher = MATH_MODE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String mathContent = matcher.group(1);
            String converted = convertMathExpression(mathContent);
            matcher.appendReplacement(result, Matcher.quoteReplacement(converted));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 转换数学表达式
     */
    private static String convertMathExpression(String mathExpr) {
        String result = mathExpr;
        
        // 处理转义字符（如 \%、\$、\& 等）- 必须先处理，避免被其他规则影响
        result = processEscapedCharacters(result);
        
        // 处理上标
        result = processSuperscript(result);
        
        // 处理下标
        result = processSubscript(result);
        
        // 处理mathbb命令
        result = processMathbbCommands(result);
        
        // 处理LaTeX命令
        result = processLatexCommands(result);
        
        // 移除多余的花括号
        result = removeBraces(result);
        
        return result;
    }
    
    /**
     * 处理上标
     */
    private static String processSuperscript(String text) {
        Matcher matcher = SUPERSCRIPT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String superscriptContent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String converted = convertToSuperscript(superscriptContent);
            matcher.appendReplacement(result, Matcher.quoteReplacement(converted));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理下标
     */
    private static String processSubscript(String text) {
        Matcher matcher = SUBSCRIPT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String subscriptContent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String converted = convertToSubscript(subscriptContent);
            matcher.appendReplacement(result, Matcher.quoteReplacement(converted));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理mathbb命令
     */
    private static String processMathbbCommands(String text) {
        Matcher matcher = MATHBB_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String letter = matcher.group(1);
            String mathbbCommand = "\\mathbb{" + letter + "}";
            String unicode = LATEX_TO_UNICODE_MAP.get(mathbbCommand);
            if (unicode != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(unicode));
            } else {
                // 保持原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理转义字符（\%、\$、\&、\#、\_、\{、\}）
     */
    private static String processEscapedCharacters(String text) {
        Matcher matcher = LATEX_ESCAPED_CHAR_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String escapedChar = matcher.group(1);
            // 直接返回转义后的字符本身
            matcher.appendReplacement(result, Matcher.quoteReplacement(escapedChar));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理LaTeX命令
     */
    private static String processLatexCommands(String text) {
        Matcher matcher = LATEX_COMMAND_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String command = "\\" + matcher.group(1);
            String unicode = LATEX_TO_UNICODE_MAP.get(command);
            if (unicode != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(unicode));
            } else {
                // 保持原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 移除多余的花括号
     */
    private static String removeBraces(String text) {
        // 只移除简单的包围整个内容的花括号
        return text.replaceAll("\\{([^{}]+)\\}", "$1");
    }
    
    /**
     * 转换为上标Unicode
     */
    private static String convertToSuperscript(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String superscript = SUPERSCRIPT_MAP.get(String.valueOf(c));
            if (superscript != null) {
                result.append(superscript);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * 转换为下标Unicode
     */
    private static String convertToSubscript(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String subscript = SUBSCRIPT_MAP.get(String.valueOf(c));
            if (subscript != null) {
                result.append(subscript);
            } else {
                // 对于没有直接映射的字符，尝试转换为数字
                if (Character.isDigit(c)) {
                    result.append(SUBSCRIPT_MAP.get(String.valueOf(c)));
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
    
    /**
     * 初始化基本命令映射
     */
    private static void initializeBasicCommands() {
        // 基本运算符
        LATEX_TO_UNICODE_MAP.put("\\pm", "±");
        LATEX_TO_UNICODE_MAP.put("\\mp", "∓");
        LATEX_TO_UNICODE_MAP.put("\\times", "×");
        LATEX_TO_UNICODE_MAP.put("\\div", "÷");
        LATEX_TO_UNICODE_MAP.put("\\ast", "∗");
        LATEX_TO_UNICODE_MAP.put("\\star", "⋆");
        LATEX_TO_UNICODE_MAP.put("\\circ", "∘");
        LATEX_TO_UNICODE_MAP.put("\\bullet", "∙");
        
        // 关系符号
        LATEX_TO_UNICODE_MAP.put("\\leq", "≤");
        LATEX_TO_UNICODE_MAP.put("\\geq", "≥");
        LATEX_TO_UNICODE_MAP.put("\\neq", "≠");
        LATEX_TO_UNICODE_MAP.put("\\approx", "≈");
        LATEX_TO_UNICODE_MAP.put("\\equiv", "≡");
        LATEX_TO_UNICODE_MAP.put("\\sim", "∼");
        LATEX_TO_UNICODE_MAP.put("\\simeq", "≃");
        LATEX_TO_UNICODE_MAP.put("\\propto", "∝");
        
        // 特殊符号
        LATEX_TO_UNICODE_MAP.put("\\infty", "∞");
        LATEX_TO_UNICODE_MAP.put("\\partial", "∂");
        LATEX_TO_UNICODE_MAP.put("\\nabla", "∇");
        LATEX_TO_UNICODE_MAP.put("\\triangle", "△");
        LATEX_TO_UNICODE_MAP.put("\\square", "□");
        LATEX_TO_UNICODE_MAP.put("\\diamond", "◇");
        LATEX_TO_UNICODE_MAP.put("\\wp", "℘");
        LATEX_TO_UNICODE_MAP.put("\\Re", "ℜ");
        LATEX_TO_UNICODE_MAP.put("\\Im", "ℑ");
    }
    
    /**
     * 初始化希腊字母映射
     */
    private static void initializeGreekLetters() {
        // 小写希腊字母
        LATEX_TO_UNICODE_MAP.put("\\alpha", "α");
        LATEX_TO_UNICODE_MAP.put("\\beta", "β");
        LATEX_TO_UNICODE_MAP.put("\\gamma", "γ");
        LATEX_TO_UNICODE_MAP.put("\\delta", "δ");
        LATEX_TO_UNICODE_MAP.put("\\epsilon", "ε");
        LATEX_TO_UNICODE_MAP.put("\\zeta", "ζ");
        LATEX_TO_UNICODE_MAP.put("\\eta", "η");
        LATEX_TO_UNICODE_MAP.put("\\theta", "θ");
        LATEX_TO_UNICODE_MAP.put("\\iota", "ι");
        LATEX_TO_UNICODE_MAP.put("\\kappa", "κ");
        LATEX_TO_UNICODE_MAP.put("\\lambda", "λ");
        LATEX_TO_UNICODE_MAP.put("\\mu", "μ");
        LATEX_TO_UNICODE_MAP.put("\\nu", "ν");
        LATEX_TO_UNICODE_MAP.put("\\xi", "ξ");
        LATEX_TO_UNICODE_MAP.put("\\omicron", "ο");
        LATEX_TO_UNICODE_MAP.put("\\pi", "π");
        LATEX_TO_UNICODE_MAP.put("\\rho", "ρ");
        LATEX_TO_UNICODE_MAP.put("\\sigma", "σ");
        LATEX_TO_UNICODE_MAP.put("\\tau", "τ");
        LATEX_TO_UNICODE_MAP.put("\\upsilon", "υ");
        LATEX_TO_UNICODE_MAP.put("\\phi", "φ");
        LATEX_TO_UNICODE_MAP.put("\\chi", "χ");
        LATEX_TO_UNICODE_MAP.put("\\psi", "ψ");
        LATEX_TO_UNICODE_MAP.put("\\omega", "ω");
        
        // 大写希腊字母
        LATEX_TO_UNICODE_MAP.put("\\Alpha", "Α");
        LATEX_TO_UNICODE_MAP.put("\\Beta", "Β");
        LATEX_TO_UNICODE_MAP.put("\\Gamma", "Γ");
        LATEX_TO_UNICODE_MAP.put("\\Delta", "Δ");
        LATEX_TO_UNICODE_MAP.put("\\Epsilon", "Ε");
        LATEX_TO_UNICODE_MAP.put("\\Zeta", "Ζ");
        LATEX_TO_UNICODE_MAP.put("\\Eta", "Η");
        LATEX_TO_UNICODE_MAP.put("\\Theta", "Θ");
        LATEX_TO_UNICODE_MAP.put("\\Iota", "Ι");
        LATEX_TO_UNICODE_MAP.put("\\Kappa", "Κ");
        LATEX_TO_UNICODE_MAP.put("\\Lambda", "Λ");
        LATEX_TO_UNICODE_MAP.put("\\Mu", "Μ");
        LATEX_TO_UNICODE_MAP.put("\\Nu", "Ν");
        LATEX_TO_UNICODE_MAP.put("\\Xi", "Ξ");
        LATEX_TO_UNICODE_MAP.put("\\Omicron", "Ο");
        LATEX_TO_UNICODE_MAP.put("\\Pi", "Π");
        LATEX_TO_UNICODE_MAP.put("\\Rho", "Ρ");
        LATEX_TO_UNICODE_MAP.put("\\Sigma", "Σ");
        LATEX_TO_UNICODE_MAP.put("\\Tau", "Τ");
        LATEX_TO_UNICODE_MAP.put("\\Upsilon", "Υ");
        LATEX_TO_UNICODE_MAP.put("\\Phi", "Φ");
        LATEX_TO_UNICODE_MAP.put("\\Chi", "Χ");
        LATEX_TO_UNICODE_MAP.put("\\Psi", "Ψ");
        LATEX_TO_UNICODE_MAP.put("\\Omega", "Ω");
        
        // 变体希腊字母
        LATEX_TO_UNICODE_MAP.put("\\vartheta", "ϑ");
        LATEX_TO_UNICODE_MAP.put("\\varphi", "ϕ");
        LATEX_TO_UNICODE_MAP.put("\\varpi", "ϖ");
        LATEX_TO_UNICODE_MAP.put("\\varrho", "ϱ");
        LATEX_TO_UNICODE_MAP.put("\\varsigma", "ς");
        LATEX_TO_UNICODE_MAP.put("\\varepsilon", "ε");
    }
    
    /**
     * 初始化数学运算符映射
     */
    private static void initializeMathOperators() {
        LATEX_TO_UNICODE_MAP.put("\\sum", "∑");
        LATEX_TO_UNICODE_MAP.put("\\prod", "∏");
        LATEX_TO_UNICODE_MAP.put("\\coprod", "∐");
        LATEX_TO_UNICODE_MAP.put("\\int", "∫");
        LATEX_TO_UNICODE_MAP.put("\\iint", "∬");
        LATEX_TO_UNICODE_MAP.put("\\iiint", "∭");
        LATEX_TO_UNICODE_MAP.put("\\oint", "∮");
        LATEX_TO_UNICODE_MAP.put("\\sqrt", "√");
    }
    
    /**
     * 初始化箭头符号映射
     */
    private static void initializeArrowSymbols() {
        LATEX_TO_UNICODE_MAP.put("\\leftarrow", "←");
        LATEX_TO_UNICODE_MAP.put("\\rightarrow", "→");
        LATEX_TO_UNICODE_MAP.put("\\uparrow", "↑");
        LATEX_TO_UNICODE_MAP.put("\\downarrow", "↓");
        LATEX_TO_UNICODE_MAP.put("\\leftrightarrow", "↔");
        LATEX_TO_UNICODE_MAP.put("\\updownarrow", "↕");
        LATEX_TO_UNICODE_MAP.put("\\Leftarrow", "⇐");
        LATEX_TO_UNICODE_MAP.put("\\Rightarrow", "⇒");
        LATEX_TO_UNICODE_MAP.put("\\Uparrow", "⇑");
        LATEX_TO_UNICODE_MAP.put("\\Downarrow", "⇓");
        LATEX_TO_UNICODE_MAP.put("\\Leftrightarrow", "⇔");
        LATEX_TO_UNICODE_MAP.put("\\Updownarrow", "⇕");
        LATEX_TO_UNICODE_MAP.put("\\mapsto", "↦");
        LATEX_TO_UNICODE_MAP.put("\\leftharpoonup", "↼");
        LATEX_TO_UNICODE_MAP.put("\\rightharpoonup", "⇀");
    }
    
    /**
     * 初始化集合论符号映射
     */
    private static void initializeSetTheorySymbols() {
        LATEX_TO_UNICODE_MAP.put("\\in", "∈");
        LATEX_TO_UNICODE_MAP.put("\\notin", "∉");
        LATEX_TO_UNICODE_MAP.put("\\ni", "∋");
        LATEX_TO_UNICODE_MAP.put("\\subset", "⊂");
        LATEX_TO_UNICODE_MAP.put("\\supset", "⊃");
        LATEX_TO_UNICODE_MAP.put("\\subseteq", "⊆");
        LATEX_TO_UNICODE_MAP.put("\\supseteq", "⊇");
        LATEX_TO_UNICODE_MAP.put("\\cup", "∪");
        LATEX_TO_UNICODE_MAP.put("\\cap", "∩");
        LATEX_TO_UNICODE_MAP.put("\\emptyset", "∅");
        LATEX_TO_UNICODE_MAP.put("\\forall", "∀");
        LATEX_TO_UNICODE_MAP.put("\\exists", "∃");
        LATEX_TO_UNICODE_MAP.put("\\nexists", "∄");
    }
    
    /**
     * 初始化逻辑符号映射
     */
    private static void initializeLogicSymbols() {
        LATEX_TO_UNICODE_MAP.put("\\land", "∧");
        LATEX_TO_UNICODE_MAP.put("\\lor", "∨");
        LATEX_TO_UNICODE_MAP.put("\\neg", "¬");
        LATEX_TO_UNICODE_MAP.put("\\top", "⊤");
        LATEX_TO_UNICODE_MAP.put("\\bot", "⊥");
        LATEX_TO_UNICODE_MAP.put("\\vdash", "⊢");
        LATEX_TO_UNICODE_MAP.put("\\models", "⊨");
        LATEX_TO_UNICODE_MAP.put("\\therefore", "∴");
        LATEX_TO_UNICODE_MAP.put("\\because", "∵");
    }
    
    /**
     * 初始化数学集合映射
     */
    private static void initializeMathSets() {
        LATEX_TO_UNICODE_MAP.put("\\mathbb{R}", "ℝ");
        LATEX_TO_UNICODE_MAP.put("\\mathbb{C}", "ℂ");
        LATEX_TO_UNICODE_MAP.put("\\mathbb{N}", "ℕ");
        LATEX_TO_UNICODE_MAP.put("\\mathbb{Q}", "ℚ");
        LATEX_TO_UNICODE_MAP.put("\\mathbb{Z}", "ℤ");
    }
    
    /**
     * 初始化上标下标映射
     */
    private static void initializeSubscriptSuperscript() {
        // 下标数字
        SUBSCRIPT_MAP.put("0", "₀");
        SUBSCRIPT_MAP.put("1", "₁");
        SUBSCRIPT_MAP.put("2", "₂");
        SUBSCRIPT_MAP.put("3", "₃");
        SUBSCRIPT_MAP.put("4", "₄");
        SUBSCRIPT_MAP.put("5", "₅");
        SUBSCRIPT_MAP.put("6", "₆");
        SUBSCRIPT_MAP.put("7", "₇");
        SUBSCRIPT_MAP.put("8", "₈");
        SUBSCRIPT_MAP.put("9", "₉");
        SUBSCRIPT_MAP.put("+", "₊");
        SUBSCRIPT_MAP.put("-", "₋");
        SUBSCRIPT_MAP.put("=", "₌");
        SUBSCRIPT_MAP.put("(", "₍");
        SUBSCRIPT_MAP.put(")", "₎");
        
        // 下标字母
        SUBSCRIPT_MAP.put("a", "ₐ");
        SUBSCRIPT_MAP.put("e", "ₑ");
        SUBSCRIPT_MAP.put("h", "ₕ");
        SUBSCRIPT_MAP.put("i", "ᵢ");
        SUBSCRIPT_MAP.put("j", "ⱼ");
        SUBSCRIPT_MAP.put("k", "ₖ");
        SUBSCRIPT_MAP.put("l", "ₗ");
        SUBSCRIPT_MAP.put("m", "ₘ");
        SUBSCRIPT_MAP.put("n", "ₙ");
        SUBSCRIPT_MAP.put("o", "ₒ");
        SUBSCRIPT_MAP.put("p", "ₚ");
        SUBSCRIPT_MAP.put("r", "ᵣ");
        SUBSCRIPT_MAP.put("s", "ₛ");
        SUBSCRIPT_MAP.put("t", "ₜ");
        SUBSCRIPT_MAP.put("u", "ᵤ");
        SUBSCRIPT_MAP.put("v", "ᵥ");
        SUBSCRIPT_MAP.put("x", "ₓ");
        
        // 上标数字
        SUPERSCRIPT_MAP.put("0", "⁰");
        SUPERSCRIPT_MAP.put("1", "¹");
        SUPERSCRIPT_MAP.put("2", "²");
        SUPERSCRIPT_MAP.put("3", "³");
        SUPERSCRIPT_MAP.put("4", "⁴");
        SUPERSCRIPT_MAP.put("5", "⁵");
        SUPERSCRIPT_MAP.put("6", "⁶");
        SUPERSCRIPT_MAP.put("7", "⁷");
        SUPERSCRIPT_MAP.put("8", "⁸");
        SUPERSCRIPT_MAP.put("9", "⁹");
        SUPERSCRIPT_MAP.put("+", "⁺");
        SUPERSCRIPT_MAP.put("-", "⁻");
        SUPERSCRIPT_MAP.put("=", "⁼");
        SUPERSCRIPT_MAP.put("(", "⁽");
        SUPERSCRIPT_MAP.put(")", "⁾");
        
        // 上标字母
        SUPERSCRIPT_MAP.put("a", "ᵃ");
        SUPERSCRIPT_MAP.put("b", "ᵇ");
        SUPERSCRIPT_MAP.put("c", "ᶜ");
        SUPERSCRIPT_MAP.put("d", "ᵈ");
        SUPERSCRIPT_MAP.put("e", "ᵉ");
        SUPERSCRIPT_MAP.put("f", "ᶠ");
        SUPERSCRIPT_MAP.put("g", "ᵍ");
        SUPERSCRIPT_MAP.put("h", "ʰ");
        SUPERSCRIPT_MAP.put("i", "ⁱ");
        SUPERSCRIPT_MAP.put("j", "ʲ");
        SUPERSCRIPT_MAP.put("k", "ᵏ");
        SUPERSCRIPT_MAP.put("l", "ˡ");
        SUPERSCRIPT_MAP.put("m", "ᵐ");
        SUPERSCRIPT_MAP.put("n", "ⁿ");
        SUPERSCRIPT_MAP.put("o", "ᵒ");
        SUPERSCRIPT_MAP.put("p", "ᵖ");
        SUPERSCRIPT_MAP.put("r", "ʳ");
        SUPERSCRIPT_MAP.put("s", "ˢ");
        SUPERSCRIPT_MAP.put("t", "ᵗ");
        SUPERSCRIPT_MAP.put("u", "ᵘ");
        SUPERSCRIPT_MAP.put("v", "ᵛ");
        SUPERSCRIPT_MAP.put("w", "ʷ");
        SUPERSCRIPT_MAP.put("x", "ˣ");
        SUPERSCRIPT_MAP.put("y", "ʸ");
        SUPERSCRIPT_MAP.put("z", "ᶻ");
    }
    
    /**
     * 获取所有支持的LaTeX命令
     * 
     * @return 支持的LaTeX命令集合
     */
    public static Map<String, String> getSupportedCommands() {
        return new HashMap<>(LATEX_TO_UNICODE_MAP);
    }
    
    /**
     * 检查是否包含LaTeX数学命令
     * 
     * @param text 待检查的文本
     * @return 是否包含LaTeX数学命令
     */
    public static boolean containsLatexCommands(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return MATH_MODE_PATTERN.matcher(text).find() || 
               LATEX_COMMAND_PATTERN.matcher(text).find();
    }
}
