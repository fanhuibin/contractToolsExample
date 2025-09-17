package com.zhaoxinms.contract.tools.mathconvert;

/**
 * Unicode到LaTeX转换器使用示例
 * 
 * @author zhaoxin
 * @version 1.0
 */
public class UnicodeToLatexExample {
    
    public static void main(String[] args) {
        System.out.println("=== Unicode到LaTeX转换器示例 ===\n");
        
        // 示例1: 基本数学表达式
        demonstrateBasicMath();
        
        // 示例2: 希腊字母
        demonstrateGreekLetters();
        
        // 示例3: 上标下标
        demonstrateSuperscriptSubscript();
        
        // 示例4: 复杂数学公式
        demonstrateComplexFormulas();
        
        // 示例5: 集合论和逻辑
        demonstrateSetTheoryAndLogic();
        
        // 示例6: 实际应用场景
        demonstrateRealWorldUsage();
    }
    
    private static void demonstrateBasicMath() {
        System.out.println("1. 基本数学符号转换:");
        
        String[] examples = {
            "a ± b",
            "x × y ÷ z",
            "p ≤ q ≥ r",
            "m ≠ n ≈ o",
            "f(x) → ∞"
        };
        
        for (String example : examples) {
            String latex = UnicodeToLatexConverter.convertToLatex(example);
            System.out.printf("   %s  →  %s%n", example, latex);
        }
        System.out.println();
    }
    
    private static void demonstrateGreekLetters() {
        System.out.println("2. 希腊字母转换:");
        
        String[] examples = {
            "α + β = γ",
            "Δx → 0",
            "θ = π/4",
            "Ω = 2π",
            "λ函数"
        };
        
        for (String example : examples) {
            String latex = UnicodeToLatexConverter.convertToLatex(example);
            System.out.printf("   %s  →  %s%n", example, latex);
        }
        System.out.println();
    }
    
    private static void demonstrateSuperscriptSubscript() {
        System.out.println("3. 上标下标转换:");
        
        String[] examples = {
            "x² + y³ = z⁴",
            "H₂O + CO₂",
            "a₁ + a₂ = a₃",
            "E = mc²",
            "10⁶ = 1000000"
        };
        
        for (String example : examples) {
            String latex = UnicodeToLatexConverter.convertToLatex(example);
            System.out.printf("   %s  →  %s%n", example, latex);
        }
        System.out.println();
    }
    
    private static void demonstrateComplexFormulas() {
        System.out.println("4. 复杂数学公式:");
        
        String[] examples = {
            "∑(i=1 to n) i² = n(n+1)(2n+1)/6",
            "∫₀^∞ e^(-x²) dx = √π/2",
            "lim(x→0) sin(x)/x = 1",
            "∇²φ = ∂²φ/∂x² + ∂²φ/∂y²",
            "√(a² + b²) ≥ (a + b)/√2"
        };
        
        for (String example : examples) {
            String latex = UnicodeToLatexConverter.convertToLatex(example);
            System.out.printf("   %s%n   →  %s%n%n", example, latex);
        }
    }
    
    private static void demonstrateSetTheoryAndLogic() {
        System.out.println("5. 集合论和逻辑符号:");
        
        String[] examples = {
            "A ∪ B ∩ C",
            "x ∈ ℝ, x ∉ ∅",
            "∀x ∃y (x ≤ y)",
            "P ∧ Q ∨ ¬R",
            "A ⊆ B ⊇ C"
        };
        
        for (String example : examples) {
            String latex = UnicodeToLatexConverter.convertToLatex(example);
            System.out.printf("   %s  →  %s%n", example, latex);
        }
        System.out.println();
    }
    
    private static void demonstrateRealWorldUsage() {
        System.out.println("6. 实际应用场景:");
        
        // 物理公式
        String physics = "E = mc², F = ma, P = ρgh";
        System.out.printf("   物理公式: %s%n", physics);
        System.out.printf("   LaTeX:   %s%n%n", UnicodeToLatexConverter.convertToLatex(physics));
        
        // 化学方程式
        String chemistry = "2H₂ + O₂ → 2H₂O";
        System.out.printf("   化学方程式: %s%n", chemistry);
        System.out.printf("   LaTeX:     %s%n%n", UnicodeToLatexConverter.convertToLatex(chemistry));
        
        // 数学定理
        String theorem = "∀ε>0, ∃δ>0: |x-a|<δ ⇒ |f(x)-f(a)|<ε";
        System.out.printf("   数学定理: %s%n", theorem);
        System.out.printf("   LaTeX:   %s%n%n", UnicodeToLatexConverter.convertToLatex(theorem));
        
        // 统计学
        String statistics = "μ = Σxi/n, σ² = Σ(xi-μ)²/n";
        System.out.printf("   统计学公式: %s%n", statistics);
        System.out.printf("   LaTeX:     %s%n%n", UnicodeToLatexConverter.convertToLatex(statistics));
        
        // 检查是否包含Unicode符号
        System.out.println("7. Unicode符号检测:");
        String[] testTexts = {
            "普通文本",
            "包含α符号的文本",
            "E = mc²",
            "Hello World"
        };
        
        for (String text : testTexts) {
            boolean hasUnicode = UnicodeToLatexConverter.containsUnicodeSymbols(text);
            System.out.printf("   \"%s\" 包含Unicode符号: %s%n", text, hasUnicode);
        }
        
        // 显示支持的符号总数
        int totalSymbols = UnicodeToLatexConverter.getSupportedSymbols().size();
        System.out.printf("%n   总共支持 %d 个Unicode符号%n", totalSymbols);
    }
}
