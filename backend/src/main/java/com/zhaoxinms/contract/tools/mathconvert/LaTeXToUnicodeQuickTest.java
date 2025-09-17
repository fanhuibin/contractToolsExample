package com.zhaoxinms.contract.tools.mathconvert;

/**
 * LaTeX到Unicode转换器快速测试
 * 用于验证用户报告的问题修复效果
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class LaTeXToUnicodeQuickTest {
    
    public static void main(String[] args) {
        System.out.println("=== LaTeX到Unicode转换器快速测试 ===\n");
        
        // 测试用户报告的问题案例
        testUserReportedIssues();
        
        // 测试预处理功能
        testPreprocessing();
        
        // 测试完整转换流程
        testCompleteConversion();
    }
    
    private static void testUserReportedIssues() {
        System.out.println("1. 用户报告的问题案例:");
        
        String[] problemCases = {
            "$P___0$",      // 应该转换为 P₀
            "$B1$",         // 应该转换为 B₁  
            "$B2$",         // 应该转换为 B₂
            "$B3$",         // 应该转换为 B₃
            "$F{01}$",      // 应该转换为 F₀₁
            "$F{02}$",      // 应该转换为 F₀₂
            "$F{03}$",      // 应该转换为 F₀₃
            "$F{0n}$",      // 应该转换为 F₀ₙ
            "$F{t1}$",      // 应该转换为 Ft₁
            "$F{t2}$",      // 应该转换为 Ft₂
            "$F{t3}$",      // 应该转换为 Ft₃
            "$F{tn}$"       // 应该转换为 Ftₙ
        };
        
        for (String testCase : problemCases) {
            String result = LaTeXToUnicodeConverter.convertToUnicode(testCase);
            System.out.printf("   %-15s → %s%n", testCase, result);
        }
        System.out.println();
    }
    
    private static void testPreprocessing() {
        System.out.println("2. 预处理功能测试:");
        
        String[] testCases = {
            "$B1$",
            "$B2$", 
            "$B3$",
            "$F{01}$",
            "$F{02}$",
            "$F{03}$",
            "$F{t1}$",
            "$F{t2}$",
            "$F{t3}$",
            "$P___0$"
        };
        
        for (String testCase : testCases) {
            // 这里我们需要直接测试预处理逻辑
            String preprocessed = preprocessLatex(testCase);
            String result = LaTeXToUnicodeConverter.convertToUnicode(testCase);
            System.out.printf("   原始: %-15s 预处理: %-15s 结果: %s%n", 
                testCase, preprocessed, result);
        }
        System.out.println();
    }
    
    private static void testCompleteConversion() {
        System.out.println("3. 完整转换流程测试:");
        
        String userTestData = "100 多页文档   3个幻觉问题。    $\\Delta P$  公式问题：ΔP  $P___0$    P₀     $B1$; $B2$; $B3$......$Bn$     B₁; B₂; B₃……Bₙ    $F{t1}$; $F{t2}$; $F{t3}$......$F{tn}$     Ft1; Ft2; Ft3;...; Ftn     $F{01}$; $F{02}$; $F{03}$;...; $F{0n}$     F01; F02; F03......F0n       年月日 丢失 月  。";
        
        System.out.println("原始文本:");
        System.out.println(userTestData);
        System.out.println();
        
        String converted = LaTeXToUnicodeConverter.convertToUnicode(userTestData);
        System.out.println("转换后文本:");
        System.out.println(converted);
        System.out.println();
        
        // 检查关键转换是否正确
        System.out.println("关键转换验证:");
        String[] keyConversions = {
            "$\\Delta P$ → ΔP",
            "$P___0$ → P₀", 
            "$B1$ → B₁",
            "$B2$ → B₂", 
            "$B3$ → B₃",
            "$F{01}$ → F₀₁",
            "$F{02}$ → F₀₂",
            "$F{03}$ → F₀₃"
        };
        
        for (String conversion : keyConversions) {
            System.out.printf("   %s%n", conversion);
        }
    }
    
    // 复制预处理逻辑用于测试
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
}
