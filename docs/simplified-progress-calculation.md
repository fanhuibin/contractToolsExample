# 简化的合同比对进度计算逻辑

## 版本信息
- **版本**: 2.0
- **更新日期**: 2025-10-13
- **状态**: 已实施

## 背景

之前的进度计算逻辑过于复杂，包含多个步骤的时间估算和复杂的插值计算。新的简化逻辑只关注核心的OCR处理时间，让前端更容易控制进度显示。

## 核心思想

**只关注OCR时间，简化进度控制**

1. 提交任务时，基于文档页数计算两个文档的OCR预估时间
2. 前端获取预估时间后，按照固定的里程碑进度推进
3. 进度基于时间流逝和实际完成状态的结合

## 进度里程碑

```
0%  ────►  46%  ────►  60%  ────►  96%  ────►  100%
         第一文档      缓冲区    第二文档      冲刺
        (预估完成)    (超时等待) (预估完成)   (完成)
```

### 详细说明

1. **0% - 46%**: 第一个文档OCR处理
   - 基于第一个文档的预估时间线性增长
   - 到达预估时间时进度应该在46%
   - 如果提前完成，直接跳到46%

2. **46% - 60%**: 第一个文档超时缓冲区
   - 如果到46%时第一个文档还未完成，启用缓慢增长模式
   - 增长速度为原速度的1/20 (5%)
   - 最多增长到60%，避免进度停滞
   - 一旦第一个文档完成，立即跳出缓冲区

3. **60% (或当前进度) - 96%**: 第二个文档OCR处理
   - 从第一个文档完成时的当前进度开始
   - 基于第二个文档的预估时间线性增长到96%
   - 如果提前完成，直接跳到96%

4. **96% - 100%**: 最终冲刺
   - 等待比对完成
   - 完成后0.1秒内快速增长到100%
   - 给用户流畅的完成体验

## 配置参数

### 后端配置 (application-compare-progress.yml)

```yaml
zxcm:
  compare:
    progress:
      # OCR性能参数
      ocr-first-doc-per-page: 3000      # 第一个文档每页3秒
      ocr-second-doc-per-page: 2500     # 第二个文档每页2.5秒
      ocr-min-time: 5000                # 最小5秒
      ocr-buffer-factor: 1.2            # 增加20%缓冲
      
      # 进度里程碑
      first-doc-complete-progress: 46.0  # 第一个文档: 46%
      first-doc-max-wait-progress: 60.0  # 超时等待: 60%
      second-doc-complete-progress: 96.0 # 第二个文档: 96%
      slow-growth-factor: 0.05          # 缓慢增长: 5% (1/20)
      final-sprint-time: 100            # 冲刺: 0.1秒
```

### 前端配置 (Vue组件)

前端从后端获取以下信息：
- `oldDocPages`: 原文档页数
- `newDocPages`: 新文档页数
- `estimatedOcrTimeOld`: 原文档OCR预估时间（毫秒）
- `estimatedOcrTimeNew`: 新文档OCR预估时间（毫秒）
- `completedPagesOld`: 已完成的原文档页数
- `completedPagesNew`: 已完成的新文档页数
- `currentStepDesc`: 当前步骤描述

## 前端实现逻辑

### 状态机

```javascript
state = {
  phase: 'FIRST_DOC',     // FIRST_DOC | WAITING | SECOND_DOC | FINAL
  startTime: Date.now(),
  estimatedOcrTimeOld: 0,
  estimatedOcrTimeNew: 0,
  firstDocCompleteTime: 0
}
```

### 进度计算伪代码

```javascript
function calculateProgress(taskData) {
  const now = Date.now()
  const elapsed = now - state.startTime
  
  // 第一个文档阶段
  if (taskData.currentStepDesc.includes('原文档')) {
    // 页面进度优先
    const pageProgress = taskData.completedPagesOld / taskData.oldDocPages
    const timeProgress = elapsed / taskData.estimatedOcrTimeOld
    
    const progress = Math.min(pageProgress, timeProgress) * 46.0
    
    // 如果到达46%但还未完成，进入等待模式
    if (progress >= 46.0) {
      state.phase = 'WAITING'
      return slowGrowth(46.0, 60.0, elapsed)
    }
    
    return Math.min(progress, 46.0)
  }
  
  // 第二个文档阶段
  if (taskData.currentStepDesc.includes('新文档')) {
    state.phase = 'SECOND_DOC'
    const currentProgress = Math.max(displayProgress, 46.0)
    
    const pageProgress = taskData.completedPagesNew / taskData.newDocPages
    const timeProgress = elapsed / taskData.estimatedOcrTimeNew
    
    const progressRange = 96.0 - currentProgress
    const progress = currentProgress + (Math.min(pageProgress, timeProgress) * progressRange)
    
    return Math.min(progress, 96.0)
  }
  
  // 其他步骤（比对、结果生成等）
  if (displayProgress < 96.0) {
    return 96.0  // 直接跳到96%
  }
  
  // 等待完成
  if (taskData.status === 'COMPLETED') {
    state.phase = 'FINAL'
    return quickSprint(96.0, 100.0, 100) // 0.1秒冲刺
  }
  
  return 96.0
}

function slowGrowth(current, max, elapsed) {
  const slowSpeed = normalSpeed * 0.05  // 1/20速度
  return Math.min(current + slowSpeed, max)
}

function quickSprint(from, to, duration) {
  // 0.1秒内从96%到100%
  const progress = from + ((to - from) * (elapsed / duration))
  return Math.min(progress, to)
}
```

## 优势

### 1. 简单清晰
- 只关注OCR时间，不再需要复杂的多步骤估算
- 前端逻辑一目了然，易于维护

### 2. 用户体验好
- 进度条平滑推进，不会卡顿
- 超时有缓冲机制，不会停滞
- 提前完成会直接跳转，不会拖延

### 3. 可配置性强
- 所有关键参数都可在配置文件中调整
- 便于根据实际服务器性能优化

### 4. 性能影响小
- 后端只需要简单的乘法计算
- 前端300ms更新一次，负载很低

## 与旧版本的对比

| 特性 | 旧版本 | 新版本 |
|------|--------|--------|
| 步骤数量 | 9个步骤 | 2个核心步骤（OCR） |
| 计算复杂度 | 高（多步骤插值） | 低（线性插值） |
| 配置参数 | 20+ 参数 | 9个参数 |
| 代码行数 | 500+ 行 | 200+ 行 |
| 维护难度 | 高 | 低 |
| 准确性 | 中等 | 高（聚焦核心） |

## 示例场景

### 场景1：正常流程

```
文档: 10页 + 8页
预估时间: 36秒 + 24秒

时间轴:
0s     ─── 0%
10s    ─── 13%  (10/36 * 46%)
20s    ─── 26%
30s    ─── 39%
36s    ─── 46%  ✓ 第一个文档完成
40s    ─── 54%  (4/24 * 50% + 46%)
50s    ─── 75%
60s    ─── 96%  ✓ 第二个文档完成
61s    ─── 100% ✓ 比对完成
```

### 场景2：第一个文档超时

```
文档: 20页 + 5页
预估时间: 72秒 + 15秒
实际: 第一个文档花了95秒

时间轴:
0s     ─── 0%
30s    ─── 19%
60s    ─── 38%
72s    ─── 46%  (预估完成，但实际未完成)
80s    ─── 48%  (缓慢增长模式，原速度1/20)
90s    ─── 50%
95s    ─── 52%  ✓ 第一个文档实际完成
100s   ─── 67%  (基于52%到96%的44%范围)
110s   ─── 96%  ✓ 第二个文档完成
111s   ─── 100% ✓ 比对完成
```

### 场景3：提前完成

```
文档: 5页 + 5页
预估时间: 18秒 + 15秒
实际: OCR很快

时间轴:
0s     ─── 0%
5s     ─── 13%
10s    ─── 46%  ✓ 第一个文档提前完成（直接跳到46%）
15s    ─── 65%
20s    ─── 96%  ✓ 第二个文档提前完成（直接跳到96%）
21s    ─── 100% ✓ 比对完成
```

## 测试建议

1. **小文档测试**: 1页+1页，验证最小时间限制
2. **大文档测试**: 50页+50页，验证长时间稳定性
3. **不对称测试**: 30页+5页，验证两个文档不同时长
4. **超时测试**: 人为延迟OCR，验证缓冲机制
5. **快速完成测试**: 使用缓存，验证跳转逻辑

## 未来优化

1. 根据历史任务数据动态调整每页耗时
2. 支持用户自定义进度里程碑
3. 增加机器学习预测模型
4. 支持多GPU并行的进度显示

## 版本历史

- **v2.0** (2025-10-13): 简化版本，只关注OCR时间
- **v1.0** (2025-01-14): 初始版本，包含多步骤计算

