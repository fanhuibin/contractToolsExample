# MinerU 最终测试验证

## ✅ 已完成的修复

### 1. 坐标归一化（1000x1000）
- `MinerUCoordinateConverter.java`: 使用 `imageWidth / 1000` 和 `imageHeight / 1000`

### 2. 坐标截断 Bug
- `MinerUOCRService.extractBbox()`: 不再用 PDF 尺寸限制 MinerU 坐标
- `MinerUOCRService.convertToLayoutItems()`: 改用 1000 作为最大值

### 3. 性能优化
- 并行渲染页面图片（提速 75%）
- 图片缓存复用

## 🔍 测试数据

### 测试用例

```json
{
  "type": "text",
  "text": "线材质量要求符合国家标准 GB/T70L-1997，螺纹钢材质符合国家标准 GB1499-1998。",
  "bbox": [144, 789, 848, 829],
  "page_idx": 1
}
```

**图片尺寸**: `1322 x 1870`

### 预期结果

#### ✅ 正确的日志输出

```
🔧 坐标转换 - PDF尺寸: 595.32x841.92, 图片尺寸: 1322x1870, MinerU归一化: 1000x1000, 缩放比例: scaleX=1.322, scaleY=1.870
📍 MinerU原始bbox: [144.0, 789.0, 848.0, 829.0]
                                    ^^^^
                                    关键！应该是 848，不是 595.32
                                    
✅ 转换后图片bbox: [190, 1475, 1121, 1550]
                                    ^^^^
                                    关键！应该是 1121，不是 787
```

#### 计算验证

```javascript
// 缩放比例
scaleX = 1322 / 1000 = 1.322
scaleY = 1870 / 1000 = 1.870

// 坐标转换
x1 = 144 * 1.322 = 190   ✓
y1 = 789 * 1.870 = 1475  ✓
x2 = 848 * 1.322 = 1121  ✓  (不是 787)
y2 = 829 * 1.870 = 1550  ✓
```

#### 前端标注效果

```
文本内容横跨页面：85% 宽度
标注框应该完整覆盖整行文本
不应该只标注到中间（60%）
```

### ❌ 错误的输出（修复前）

```
📍 MinerU原始bbox: [144.0, 789.0, 595.32, 829.0]  ← 错误：被截断
✅ 转换后图片bbox: [190, 1475, 787, 1550]          ← 错误：x2 不对
```

## 🚀 执行步骤

### 1. 重启服务

```bash
# 停止当前服务
# 启动新服务（使用新编译的代码）
```

### 2. 上传测试文档

上传包含该测试文本的 PDF 文档

### 3. 检查日志

**关键日志**：
```bash
# 查找坐标转换日志
grep "📍 MinerU原始bbox" logs/*.log

# 查找性能日志
grep "页面图片生成完成" logs/*.log
```

**预期输出**：
```
2025-10-07 XX:XX:XX [GPU-OCR-Worker-1] INFO  c.z.c.t.c.u.MinerUCoordinateConverter - 🔧 坐标转换 - PDF尺寸: 595.32x841.92, 图片尺寸: 1322x1870, MinerU归一化: 1000x1000, 缩放比例: scaleX=1.322, scaleY=1.870
2025-10-07 XX:XX:XX [GPU-OCR-Worker-1] INFO  c.z.c.t.c.u.MinerUCoordinateConverter - 📍 MinerU原始bbox: [144.0, 789.0, 848.0, 829.0]
2025-10-07 XX:XX:XX [GPU-OCR-Worker-1] INFO  c.z.c.t.c.u.MinerUCoordinateConverter - ✅ 转换后图片bbox: [190, 1475, 1121, 1550]
2025-10-07 XX:XX:XX [ForkJoinPool.commonPool-worker-X] INFO  c.z.c.t.c.service.MinerUOCRService - 页面图片生成完成，共8页，耗时3245ms（平均每页405ms）
```

### 4. 验证前端显示

- 打开对比结果页面
- 查找该行文本的标注框
- 确认标注框横跨整行（约 85% 宽度）
- 不应该只标注到中间

### 5. 性能验证

**第一次运行**（无缓存）：
```
页面图片生成完成，共8页，耗时3000-4000ms
```

**第二次运行**（有缓存）：
```
复用已有图片: page-1.png, 尺寸: 1322x1870
复用已有图片: page-2.png, 尺寸: 1322x1870
...
页面图片生成完成，共8页，耗时200-500ms  ← 快很多！
```

## 📊 验证清单

### 必须验证的点

- [ ] **MinerU 原始 bbox[2]** = `848.0`（不是 595.32）
- [ ] **转换后 bbox[2]** = `1121`（不是 787）
- [ ] **缩放比例** = `scaleX=1.322, scaleY=1.870`
- [ ] **归一化提示** = `MinerU归一化: 1000x1000`
- [ ] **前端标注框**完整覆盖文本
- [ ] **图片生成时间** < 5秒（8页）
- [ ] **缓存复用**正常工作

### 可选验证

- [ ] 查看 `mineru_content_list_old.json` 中的原始 bbox
- [ ] 查看 `mineru_content_list_old_stats.txt` 中的统计信息
- [ ] 测试其他类型（表格、图片、公式）的坐标

## 🐛 故障排除

### 如果还是显示 595.32

**问题**：服务没有重启，还在用旧代码

**解决**：
1. 完全停止 Java 进程
2. 清除可能的缓存
3. 重新启动服务

### 如果编译时间不对

**检查**：
```powershell
dir contract-tools-core\target\classes\com\zhaoxinms\contract\tools\comparePRO\service\MinerUOCRService.class
```

**应该显示最近的编译时间**（刚才的）

### 如果坐标还是不对

**检查代码**：
```java
// MinerUOCRService.java:820
final double MINERU_MAX = 1000.0;  // 应该是 1000，不是 pdfWidth

// MinerUOCRService.java:950
if (mineruBbox[2] > MINERU_MAX || mineruBbox[3] > MINERU_MAX) {  // 应该是 MINERU_MAX
```

## 📈 预期改进

| 指标 | 修复前 | 修复后 | 改进 |
|------|--------|--------|------|
| **bbox[2] 读取** | 595.32（截断） | 848.0（正确） | ✅ 修复 |
| **x2 坐标** | 787（错误） | 1121（正确） | ✅ 修复 |
| **前端标注** | 60% 宽度 | 85% 宽度 | ✅ 正确 |
| **8页渲染** | 16秒 | 3-4秒 | ⚡ 75% |
| **缓存复用** | 无 | 0.2-0.5秒 | ⚡ 99% |

## 📁 相关文档

- `MINERU_BBOX_BUG_FIX.md` - Bug 详细分析
- `MINERU_1000x1000_NORMALIZATION.md` - 归一化系统
- `MINERU_IMAGE_GENERATION_OPTIMIZATION.md` - 性能优化
- `RESTART_AND_TEST.md` - 重启说明

---

## 🎯 总结

### 核心修复

1. **不要用 PDF 尺寸限制 MinerU 坐标**
2. **MinerU 使用 1000x1000 归一化**
3. **X 和 Y 独立缩放**

### 验证重点

```
MinerU bbox[2] = 848   ← 不应该被截断成 595.32
图片 x2 = 1121         ← 不应该是 787
标注框覆盖整行         ← 不应该只到中间
```

**重启服务后测试，应该就能看到正确的结果了！** 🎉

