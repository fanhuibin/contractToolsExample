package com.zhaoxinms.contract.tools.mathconvert;

/**
 * LaTeX到Unicode转换器使用示例
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class LaTeXToUnicodeExample {
    
    public static void main(String[] args) {
        System.out.println("=== LaTeX到Unicode转换器示例 ===\n");
        
        // 示例1: 基本LaTeX命令转换
        demonstrateBasicCommands();
        
        // 示例2: 希腊字母转换
        demonstrateGreekLetters();
        
        // 示例3: 上标下标转换
        demonstrateSubscriptSuperscript();
        
        // 示例4: 复杂数学表达式
        demonstrateComplexExpressions();
        
        // 示例5: 实际应用场景
        demonstrateRealWorldUsage();
        
        // 示例6: 双向转换演示
        demonstrateBidirectionalConversion();
    }
    
    private static void demonstrateBasicCommands() {
        System.out.println("1. 基本LaTeX命令转换:");
        
        String[] examples = {
            "$\\pm$",
            "$\\times$",
            "$\\div$",
            "$\\leq$",
            "$\\geq$",
            "$\\neq$",
            "$\\approx$",
            "$\\infty$"
        };
        
        for (String example : examples) {
            String unicode = LaTeXToUnicodeConverter.convertToUnicode(example);
            System.out.printf("   %s  →  %s%n", example, unicode);
        }
        System.out.println();
    }
    
    private static void demonstrateGreekLetters() {
        System.out.println("2. 希腊字母转换:");
        
        String[] examples = {
            "$\\alpha$",
            "$\\beta$",
            "$\\gamma$",
            "$\\Delta$",
            "$\\pi$",
            "$\\Omega$",
            "$\\lambda$函数",
            "角度$\\theta$"
        };
        
        for (String example : examples) {
            String unicode = LaTeXToUnicodeConverter.convertToUnicode(example);
            System.out.printf("   %s  →  %s%n", example, unicode);
        }
        System.out.println();
    }
    
    private static void demonstrateSubscriptSuperscript() {
        System.out.println("3. 上标下标转换:");
        
        String[] examples = {
            "$x^2$",
            "$y^3$",
            "$H_2O$",
            "$CO_2$",
            "$a_1$",
            "$a_n$",
            "$x_{max}$",
            "$F^{(n)}$"
        };
        
        for (String example : examples) {
            String unicode = LaTeXToUnicodeConverter.convertToUnicode(example);
            System.out.printf("   %s  →  %s%n", example, unicode);
        }
        System.out.println();
    }
    
    private static void demonstrateComplexExpressions() {
        System.out.println("4. 复杂数学表达式:");
        
        String[] examples = {
            "$E = mc^2$",
            "$\\sum_{i=1}^{n} x_i^2$",
            "$\\int_0^\\infty e^{-x^2} dx$",
            "$\\forall x \\in \\mathbb{R}, x^2 \\geq 0$",
            "$\\lim_{x \\rightarrow \\infty} f(x) = 0$",
            "$A \\cup B \\cap C$"
        };
        
        for (String example : examples) {
            String unicode = LaTeXToUnicodeConverter.convertToUnicode(example);
            System.out.printf("   %s%n   →  %s%n%n", example, unicode);
        }
    }
    
    private static void demonstrateRealWorldUsage() {
        System.out.println("5. 实际应用场景:");
        
        // 物理公式
        String physics = "能量守恒定律：$E = mc^2$，其中c是光速。";
        System.out.printf("   物理公式: %s%n", physics);
        System.out.printf("   转换后:   %s%n%n", LaTeXToUnicodeConverter.convertToUnicode(physics));
        
        // 化学方程式
        String chemistry = "燃烧反应：$CH_4 + 2O_2 \\rightarrow CO_2 + 2H_2O$";
        System.out.printf("   化学方程式: %s%n", chemistry);
        System.out.printf("   转换后:     %s%n%n", LaTeXToUnicodeConverter.convertToUnicode(chemistry));
        
        // 数学定理
        String theorem = "极限定义：$\\forall \\varepsilon > 0, \\exists \\delta > 0$使得当$|x-a| < \\delta$时，$|f(x)-f(a)| < \\varepsilon$";
        System.out.printf("   数学定理: %s%n", theorem);
        System.out.printf("   转换后:   %s%n%n", LaTeXToUnicodeConverter.convertToUnicode(theorem));
        
        // 统计学
        String statistics = "样本方差：$s^2 = \\frac{1}{n-1}\\sum_{i=1}^{n}(x_i - \\bar{x})^2$";
        System.out.printf("   统计学公式: %s%n", statistics);
        System.out.printf("   转换后:     %s%n%n", LaTeXToUnicodeConverter.convertToUnicode(statistics));
    }
    
    private static void demonstrateBidirectionalConversion() {
        System.out.println("6. 双向转换演示:");
        
        String[] examples = {
            "α + β = γ",
            "E = mc²",
            "∀x ∈ ℝ, x² ≥ 0",
            "H₂O + CO₂"
        };
        
        System.out.println("   Unicode → LaTeX → Unicode:");
        for (String unicode : examples) {
            // Unicode转LaTeX
            String latex = UnicodeToLatexConverter.convertToLatex(unicode);
            // LaTeX转回Unicode
            String backToUnicode = LaTeXToUnicodeConverter.convertToUnicode(latex);
            
            System.out.printf("   %s → %s → %s%n", unicode, latex, backToUnicode);
        }
        System.out.println();
        
        String[] latexExamples = {
            "$\\alpha + \\beta = \\gamma$",
            "$E = mc^2$",
            "$\\forall x \\in \\mathbb{R}, x^2 \\geq 0$",
            "$H_2O + CO_2$"
        };
        
        System.out.println("   LaTeX → Unicode → LaTeX:");
        for (String latex : latexExamples) {
            // LaTeX转Unicode
            String unicode = LaTeXToUnicodeConverter.convertToUnicode(latex);
            // Unicode转回LaTeX
            String backToLatex = UnicodeToLatexConverter.convertToLatex(unicode);
            
            System.out.printf("   %s → %s → %s%n", latex, unicode, backToLatex);
        }
        System.out.println();
    }
    
    /**
     * 用户实际测试案例演示
     */
    public static void demonstrateUserTestCase() {
        System.out.println("=== 用户实际测试案例 ===\n");
        
        String userTestData = "100 多页文档   3个幻觉问题。    $\\Delta P$  公式问题：ΔP  $P___0$    P₀     $B1$; $B2$; $B3$......$Bn$     B₁; B₂; B₃……Bₙ    $F{t1}$; $F{t2}$; $F{t3}$......$F{tn}$     Ft1; Ft2; Ft3;...; Ftn     F₀₁; F₀₂; F₀₃;...; F₀ₙ ——各    $F{01}$; $F{02}$; $F{03}$......$F{0n}$       年月日 丢失 月  。";
        
        System.out.println("原始文本（包含LaTeX和Unicode混合）:");
        System.out.println(userTestData);
        System.out.println();
        
        String converted = LaTeXToUnicodeConverter.convertToUnicode(userTestData);
        System.out.println("LaTeX→Unicode转换后:");
        System.out.println(converted);
        System.out.println();
        
        // 显示转换效果对比
        System.out.println("关键转换对比:");
        String[][] comparisons = {
            {"$\\Delta P$", "ΔP"},
            {"$P___0$", "P₀"}, // 注意：这个需要修正为$P_0$才能正确转换
            {"$B1$", "B1"}, // 注意：这个需要修正为$B_1$才能正确转换
            {"$P_0$", "P₀"},
            {"$B_1$", "B₁"},
            {"$F_{01}$", "F₀₁"}
        };
        
        for (String[] comparison : comparisons) {
            String input = comparison[0];
            String expected = comparison[1];
            String actual = LaTeXToUnicodeConverter.convertToUnicode(input);
            System.out.printf("   %-15s → %-8s (期望: %s)%n", input, actual, expected);
        }
    }
}
