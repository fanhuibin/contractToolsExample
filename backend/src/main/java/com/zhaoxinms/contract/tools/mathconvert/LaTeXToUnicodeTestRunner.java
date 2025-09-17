package com.zhaoxinms.contract.tools.mathconvert;

/**
 * LaTeX到Unicode转换器测试运行器
 * 用于快速测试和验证转换功能
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class LaTeXToUnicodeTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== LaTeX到Unicode转换器测试 ===\n");
        
        // 测试用户提供的实际案例
        testUserTestCase();
        
        // 测试基本转换功能
        testBasicConversions();
        
        // 测试下标上标转换
        testSubscriptSuperscript();
        
        // 测试复杂表达式
        testComplexExpressions();
    }
    
    private static void testUserTestCase() {
        System.out.println("1. 用户实际测试案例:");
        
        String userTestData = "100 多页文档   3个幻觉问题。    $\\Delta P$  公式问题：ΔP  $P_0$    P₀     $B_1$; $B_2$; $B_3$......$B_n$     B₁; B₂; B₃……Bₙ    $F_{t1}$; $F_{t2}$; $F_{t3}$......$F_{tn}$     Ft1; Ft2; Ft3;...; Ftn     $F_{01}$; $F_{02}$; $F_{03}$;...; $F_{0n}$     F01; F02; F03......F0n       年月日 丢失 月  。";
        
        System.out.println("原始文本:");
        System.out.println(userTestData);
        System.out.println();
        
        String converted = LaTeXToUnicodeConverter.convertToUnicode(userTestData);
        System.out.println("转换后文本:");
        System.out.println(converted);
        System.out.println();
        
        // 显示关键转换对比
        System.out.println("关键转换对比:");
        String[][] comparisons = {
            {"$\\Delta P$", "ΔP"},
            {"$P_0$", "P₀"},
            {"$B_1$", "B₁"},
            {"$B_2$", "B₂"},
            {"$B_3$", "B₃"},
            {"$B_n$", "Bₙ"},
            {"$F_{01}$", "F₀₁"},
            {"$F_{02}$", "F₀₂"},
            {"$F_{03}$", "F₀₃"},
            {"$F_{0n}$", "F₀ₙ"}
        };
        
        for (String[] comparison : comparisons) {
            String input = comparison[0];
            String expected = comparison[1];
            String actual = LaTeXToUnicodeConverter.convertToUnicode(input);
            System.out.printf("   %-15s → %-8s (期望: %s)%n", input, actual, expected);
        }
        System.out.println();
    }
    
    private static void testBasicConversions() {
        System.out.println("2. 基本转换测试:");
        
        String[] testCases = {
            "$\\alpha$",
            "$\\beta$", 
            "$\\gamma$",
            "$\\Delta$",
            "$\\pi$",
            "$\\infty$",
            "$\\pm$",
            "$\\times$",
            "$\\div$",
            "$\\leq$",
            "$\\geq$",
            "$\\neq$"
        };
        
        for (String testCase : testCases) {
            String result = LaTeXToUnicodeConverter.convertToUnicode(testCase);
            System.out.printf("   %-15s → %s%n", testCase, result);
        }
        System.out.println();
    }
    
    private static void testSubscriptSuperscript() {
        System.out.println("3. 下标上标转换测试:");
        
        String[] testCases = {
            "$x^2$",
            "$y^3$", 
            "$H_2O$",
            "$CO_2$",
            "$a_1$",
            "$a_n$",
            "$x_{max}$",
            "$F^{(n)}$",
            "$P_0$",
            "$B_1$",
            "$B_2$",
            "$B_3$",
            "$F_{01}$",
            "$F_{02}$",
            "$F_{03}$",
            "$F_{0n}$"
        };
        
        for (String testCase : testCases) {
            String result = LaTeXToUnicodeConverter.convertToUnicode(testCase);
            System.out.printf("   %-15s → %s%n", testCase, result);
        }
        System.out.println();
    }
    
    private static void testComplexExpressions() {
        System.out.println("4. 复杂表达式测试:");
        
        String[] testCases = {
            "$E = mc^2$",
            "$\\alpha + \\beta \\geq \\gamma$",
            "$\\forall x \\in \\mathbb{R}, x^2 \\geq 0$",
            "$\\sum_{i=1}^{n} x_i^2$",
            "$\\int_0^\\infty e^{-x^2} dx$",
            "$A \\cup B \\cap C$"
        };
        
        for (String testCase : testCases) {
            String result = LaTeXToUnicodeConverter.convertToUnicode(testCase);
            System.out.printf("   %-40s → %s%n", testCase, result);
        }
        System.out.println();
    }
}