# Unicode到LaTeX数学符号转换器

这是一个Java工具类，用于将Unicode数学符号转换为对应的LaTeX命令。该实现参考了pandoc-unicode-math项目的设计思路。

## 功能特性

- **Unicode数学符号转换**：支持将常用的Unicode数学符号转换为LaTeX命令
- **上标下标处理**：自动处理Unicode上标和下标符号
- **希腊字母转换**：完整支持希腊字母的大小写转换
- **数学运算符**：支持求和、积分、根号等数学运算符
- **箭头符号**：支持各种方向的箭头符号
- **集合论符号**：支持集合论和逻辑运算符号
- **数学集合**：支持常用数学集合符号（ℝ, ℂ, ℕ, ℚ, ℤ等）

## 支持的符号类别

### 1. 基本运算符
- `±` → `\pm`
- `∓` → `\mp`
- `×` → `\times`
- `÷` → `\div`
- `∗` → `\ast`
- `⋆` → `\star`
- `∘` → `\circ`
- `∙` → `\bullet`

### 2. 关系符号
- `≤` → `\leq`
- `≥` → `\geq`
- `≠` → `\neq`
- `≈` → `\approx`
- `≡` → `\equiv`
- `∼` → `\sim`
- `≃` → `\simeq`
- `∝` → `\propto`

### 3. 希腊字母
- **小写**：`α` → `\alpha`，`β` → `\beta`，`γ` → `\gamma`，等
- **大写**：`Α` → `\Alpha`，`Β` → `\Beta`，`Γ` → `\Gamma`，等
- **变体**：`ϑ` → `\vartheta`，`ϕ` → `\varphi`，等

### 4. 数学运算符
- `∑` → `\sum`
- `∏` → `\prod`
- `∫` → `\int`
- `√` → `\sqrt`
- `∂` → `\partial`
- `∇` → `\nabla`
- `∞` → `\infty`

### 5. 箭头符号
- `←` → `\leftarrow`
- `→` → `\rightarrow`
- `↑` → `\uparrow`
- `↓` → `\downarrow`
- `↔` → `\leftrightarrow`
- `⇐` → `\Leftarrow`
- `⇒` → `\Rightarrow`
- `⇔` → `\Leftrightarrow`

### 6. 集合论符号
- `∈` → `\in`
- `∉` → `\notin`
- `⊂` → `\subset`
- `⊃` → `\supset`
- `⊆` → `\subseteq`
- `⊇` → `\supseteq`
- `∪` → `\cup`
- `∩` → `\cap`
- `∅` → `\emptyset`
- `∀` → `\forall`
- `∃` → `\exists`

### 7. 逻辑符号
- `∧` → `\land`
- `∨` → `\lor`
- `¬` → `\neg`
- `⊤` → `\top`
- `⊥` → `\bot`

### 8. 数学集合
- `ℝ` → `\mathbb{R}` (实数集)
- `ℂ` → `\mathbb{C}` (复数集)
- `ℕ` → `\mathbb{N}` (自然数集)
- `ℚ` → `\mathbb{Q}` (有理数集)
- `ℤ` → `\mathbb{Z}` (整数集)

### 9. 上标下标
- **上标数字**：`¹²³⁴⁵⁶⁷⁸⁹⁰` → `^{1}^{2}^{3}...`
- **下标数字**：`₁₂₃₄₅₆₇₈₉₀` → `_{1}_{2}_{3}...`
- **上标符号**：`⁺⁻⁼⁽⁾` → `^{+}^{-}^{=}^{(}^{)}`
- **下标符号**：`₊₋₌₍₎` → `_{+}_{-}_{=}_{(}_{)}`

## 使用方法

### 基本使用

```java
import com.zhaoxinms.contract.tools.mathconvert.UnicodeToLatexConverter;

// 转换单个符号
String result1 = UnicodeToLatexConverter.convertToLatex("α");
// 结果: "$\alpha$"

// 转换复杂表达式
String result2 = UnicodeToLatexConverter.convertToLatex("∀x ∈ ℝ, x² ≥ 0");
// 结果: "$\forall$x $\in$ $\mathbb{R}$, x^{2} $\geq$ 0"

// 检查是否包含Unicode符号
boolean hasSymbols = UnicodeToLatexConverter.containsUnicodeSymbols("E = mc²");
// 结果: true

// 获取所有支持的符号
Map<String, String> symbols = UnicodeToLatexConverter.getSupportedSymbols();
```

### 实际应用示例

#### 物理公式
```java
String physics = "E = mc²";
String latex = UnicodeToLatexConverter.convertToLatex(physics);
// 结果: "E = mc^{2}"
```

#### 数学定理
```java
String theorem = "∀ε>0, ∃δ>0: |x-a|<δ ⇒ |f(x)-f(a)|<ε";
String latex = UnicodeToLatexConverter.convertToLatex(theorem);
// 结果: "$\forall$$\varepsilon$>0, $\exists$$\delta$>0: |x-a|<$\delta$ $\Rightarrow$ |f(x)-f(a)|<$\varepsilon$"
```

#### 化学方程式
```java
String chemistry = "2H₂ + O₂ → 2H₂O";
String latex = UnicodeToLatexConverter.convertToLatex(chemistry);
// 结果: "2H_{2} + O_{2} $\rightarrow$ 2H_{2}O"
```

#### 集合表示
```java
String set = "A ∪ B = {x | x ∈ A ∨ x ∈ B}";
String latex = UnicodeToLatexConverter.convertToLatex(set);
// 结果: "A $\cup$ B = {x | x $\in$ A $\lor$ x $\in$ B}"
```

## 转换规则

1. **自动数学模式**：大部分数学符号会自动包装在 `$...$` 中
2. **上标下标转换**：Unicode上标下标符号转换为 `^{...}` 和 `_{...}` 格式
3. **特殊符号处理**：某些符号（如文本符号）不会包装在数学模式中
4. **原文保留**：不能识别的字符保持原样输出

## 测试

运行测试用例验证功能：

```bash
mvn test -Dtest=UnicodeToLatexConverterTest
```

运行示例程序查看演示：

```bash
java -cp target/classes com.zhaoxinms.contract.tools.mathconvert.UnicodeToLatexExample
```

## 扩展支持

如需添加新的Unicode符号支持，可以在相应的初始化方法中添加映射：

```java
// 在initializeBasicSymbols()方法中添加
UNICODE_TO_LATEX_MAP.put("新符号", "\\latex_command");
```

## 注意事项

1. **LaTeX包依赖**：某些符号（如 `\mathbb{}`）需要相应的LaTeX包支持
2. **编码问题**：确保源文件使用UTF-8编码
3. **性能考虑**：大量文本转换时建议批量处理
4. **符号优先级**：上标下标转换优先于其他符号转换

## 参考资料

- [pandoc-unicode-math](https://github.com/marhop/pandoc-unicode-math)：参考实现
- [LaTeX数学符号表](https://oeis.org/wiki/List_of_LaTeX_mathematical_symbols)
- [Unicode数学符号](https://en.wikipedia.org/wiki/Mathematical_Alphanumeric_Symbols)

## 版本历史

- **v1.0.0**：初始版本，支持基本数学符号转换
  - 169个Unicode符号支持
  - 完整的测试覆盖
  - 使用示例和文档
