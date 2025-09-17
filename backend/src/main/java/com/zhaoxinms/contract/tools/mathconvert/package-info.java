/**
 * Unicode数学符号到LaTeX转换工具包
 * 
 * <p>该包提供了将Unicode数学符号转换为LaTeX格式的工具类，
 * 参考了pandoc-unicode-math项目的实现思路。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>Unicode数学符号到LaTeX命令的转换</li>
 *   <li>上标和下标符号的处理</li>
 *   <li>希腊字母的转换</li>
 *   <li>数学运算符、箭头、集合论符号的转换</li>
 *   <li>逻辑符号的转换</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基本使用
 * String latex = UnicodeToLatexConverter.convertToLatex("α + β ≥ γ");
 * // 结果: "$\\alpha$ + $\\beta$ $\\geq$ $\\gamma$"
 * 
 * // 检查是否包含Unicode符号
 * boolean hasSymbols = UnicodeToLatexConverter.containsUnicodeSymbols("E = mc²");
 * // 结果: true
 * 
 * // 获取所有支持的符号
 * Map<String, String> symbols = UnicodeToLatexConverter.getSupportedSymbols();
 * }</pre>
 * 
 * <h2>支持的符号类别</h2>
 * <ul>
 *   <li><strong>基本运算符：</strong> ±, ∓, ×, ÷, ∗, ⋆, ∘, ∙</li>
 *   <li><strong>关系符号：</strong> ≤, ≥, ≠, ≈, ≡, ∼, ≃, ∝</li>
 *   <li><strong>希腊字母：</strong> α, β, γ, δ, ε, ζ, η, θ, 等</li>
 *   <li><strong>数学运算符：</strong> ∑, ∏, ∫, √, 等</li>
 *   <li><strong>箭头符号：</strong> ←, →, ↑, ↓, ↔, ⇐, ⇒, 等</li>
 *   <li><strong>集合论符号：</strong> ∈, ∉, ⊂, ⊃, ∪, ∩, ∅, ∀, ∃</li>
 *   <li><strong>逻辑符号：</strong> ∧, ∨, ¬, ⊤, ⊥</li>
 *   <li><strong>上标下标：</strong> ⁰¹²³⁴⁵⁶⁷⁸⁹, ₀₁₂₃₄₅₆₇₈₉</li>
 * </ul>
 * 
 * @author zhaoxin
 * @version 1.0
 * @since 1.0
 */
package com.zhaoxinms.contract.tools.mathconvert;
