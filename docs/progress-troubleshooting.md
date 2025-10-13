# 进度预估问题排查指南

## 问题：预估用时是实际用时的两倍

### 排查步骤

#### 1. 检查配置文件是否生效

**重启后端服务**后，查看控制台日志：

```
📊 第一个文档OCR时间预估: 页数=8, 每页=1000ms, 基础时间=8000ms, 最小时间=3000ms, 缓冲系数=1.20, 最终预估=9600ms (9.6秒)
📊 第二个文档OCR时间预估: 页数=19, 每页=1000ms, 基础时间=19000ms, 最小时间=3000ms, 缓冲系数=1.20, 最终预估=22800ms (22.8秒)
```

如果看到的是 `每页=3000ms` 或其他值，说明配置没生效。

#### 2. 确认配置文件位置

配置文件：`contract-tools-sdk/src/main/resources/application-compare-progress.yml`

主配置导入：
```yaml
spring:
  config:
    import:
      - classpath:application-compare-progress.yml
```

#### 3. 检查实际API返回值

比对任务状态API返回：
```json
{
  "estimatedOcrTimeOld": 9600,    // 应该是 8页 × 1000ms × 1.2 = 9600ms
  "estimatedOcrTimeNew": 22800    // 应该是 19页 × 1000ms × 1.2 = 22800ms
}
```

如果返回值不对，检查是否：
- ✅ 重启了后端服务
- ✅ 配置文件路径正确
- ✅ YAML 格式正确（缩进、冒号后有空格）

#### 4. 验证实际OCR时间

在比对完成后，查看结果中的时间统计：
```
OCR预估时间：32.4秒 
OCR实际用时：28.5秒 
(预估准确率: 88.0%)
```

理想的预估准确率应该在 **90% - 120%** 之间。

### 配置调整建议

根据实际测试结果调整配置：

#### 如果实际用时 < 预估时间

减少 `ocr-buffer-factor` 或 `ocr-*-per-page`：

```yaml
ocr-first-doc-per-page: 800      # 从 1000 减到 800
ocr-second-doc-per-page: 800
ocr-buffer-factor: 1.1           # 从 1.2 减到 1.1
```

#### 如果实际用时 > 预估时间

增加 `ocr-buffer-factor` 或 `ocr-*-per-page`：

```yaml
ocr-first-doc-per-page: 1200     # 从 1000 增到 1200
ocr-second-doc-per-page: 1200
ocr-buffer-factor: 1.3           # 从 1.2 增到 1.3
```

### 配置参数说明

```yaml
zxcm:
  compare:
    progress:
      # 每页基础时间（毫秒）
      ocr-first-doc-per-page: 1000
      ocr-second-doc-per-page: 1000
      
      # 最小时间保障（毫秒）
      # 即使只有1页，也至少需要这么长时间
      ocr-min-time: 3000
      
      # 缓冲系数（防止预估过于乐观）
      # 1.2 表示在基础时间上增加 20%
      ocr-buffer-factor: 1.2
```

### 计算公式

```
预估时间 = max(页数 × 每页时间, 最小时间) × 缓冲系数

示例：
- 8页文档：max(8 × 1000, 3000) × 1.2 = 8000 × 1.2 = 9600ms = 9.6秒
- 1页文档：max(1 × 1000, 3000) × 1.2 = 3000 × 1.2 = 3600ms = 3.6秒
```

### 调试日志

在 `SimpleProgressConfig.java` 中已添加详细日志：

```java
📊 第一个文档OCR时间预估: 页数=8, 每页=1000ms, 基础时间=8000ms, 最小时间=3000ms, 缓冲系数=1.20, 最终预估=9600ms (9.6秒)
```

这会输出：
- 实际读取的配置值
- 计算的中间结果
- 最终预估时间

### 最佳实践

1. **初始配置**：使用保守的预估（稍长一些）
2. **收集数据**：运行多个测试任务，记录实际用时
3. **调整优化**：根据实际数据调整配置
4. **持续监控**：定期检查预估准确率

### 常见问题

**Q: 为什么预估时间一直不准？**
A: 可能是配置文件没生效，检查是否重启服务，配置文件是否被正确导入。

**Q: 不同文档预估准确率差异大？**
A: OCR时间受文档复杂度影响，可以考虑根据文档类型（表格多、图片多等）使用不同的配置。

**Q: 如何禁用缓冲？**
A: 设置 `ocr-buffer-factor: 1.0` 即可。

**Q: 进度条太快或太慢？**
A: 调整 `first-doc-complete-progress` 和 `second-doc-complete-progress` 的百分比。

