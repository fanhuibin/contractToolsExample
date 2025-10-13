# 跨页表格关联功能说明

## 功能概述

在 MinerU 表格解析过程中，自动识别和关联跨页表格。当合同比对发现某个差异涉及跨页表格的某一部分时，会自动将该表格在所有页面上的 bbox 都标注出来，确保完整显示表格差异。

## 实现原理

### 1. 跨页表格识别规则

在 `content_list` 中，如果一个 `type="table"` 的项满足以下所有条件，则认为是跨页表格的延续部分：

- `table_caption` 为空或不存在
- `table_footnote` 为空或不存在  
- `table_body` 为空或不存在

否则，认为是一个新的主表格。

### 2. 核心组件

#### CrossPageTableManager

跨页表格关联管理器，负责：
- 识别和分组跨页表格
- 维护表格组和 bbox 的映射关系
- 提供快速查找功能

**核心数据结构：**

```java
public static class TableGroup {
    String groupId;              // 表格组ID
    TablePart mainTable;         // 主表格（第一个完整表格）
    List<TablePart> continuationParts;  // 跨页延续部分
    Set<Integer> allPages;       // 涉及的所有页码
}

public static class TablePart {
    int contentListIndex;        // 在 content_list 中的索引
    int pageIdx;                 // 页码（0-based）
    double[] bbox;               // bbox 坐标（图片坐标系）
    boolean isMainTable;         // 是否为主表格
    String text;                 // 文本内容
}
```

#### MinerURecognitionResult

MinerU 识别结果包装类，包含：
- `PageLayout[]` 数组（与 dots.ocr 格式兼容）
- `CrossPageTableManager` 跨页表格管理器

### 3. 处理流程

```
1. MinerU OCR 识别
   ↓
2. 遍历 content_list，识别表格类型
   ↓
3. 判断是主表格还是跨页延续部分
   ↓
4. 建立表格组关联
   ↓
5. 返回识别结果（包含跨页表格管理器）
   ↓
6. 文档比对完成后
   ↓
7. 检查每个 DiffBlock 的 bbox
   ↓
8. 如果 bbox 属于某个跨页表格组
   ↓
9. 将该表格组的所有其他 bbox 补充到 DiffBlock
```

### 4. 自动补充逻辑

在 `CompareService.supplementCrossPageTableBboxes()` 方法中：

1. 遍历所有差异块（`DiffBlock`）
2. 检查每个差异块的 `oldBboxes` 和 `newBboxes`
3. 查找这些 bbox 是否属于某个跨页表格组
4. 如果属于，则补充该表格组的所有其他 bbox
5. 同步补充对应的页码（`pageA`/`pageB`）和文本信息（`allTextA`/`allTextB`）

**关键点：**
- 文本信息使用空字符串占位（用户建议可以尝试空字符串）
- 使用 bbox 唯一键去重，避免重复添加
- 支持 0-based 和 1-based 页码的自动转换

## 使用示例

### 场景示例

假设有一个跨越第 2 页和第 3 页的表格：

```
第 2 页：
  - 表格标题: "产品价格表"
  - 表格内容: 第 1-10 行

第 3 页：
  - 表格内容: 第 11-20 行（没有标题和脚注）
```

**识别结果：**
```java
TableGroup {
    groupId: "table_group_0",
    mainTable: TablePart {
        pageIdx: 1,  // 第 2 页（0-based）
        bbox: [x0, y0, x1, y1],
        isMainTable: true,
        text: "产品价格表..."
    },
    continuationParts: [
        TablePart {
            pageIdx: 2,  // 第 3 页（0-based）
            bbox: [x0', y0', x1', y1'],
            isMainTable: false,
            text: "...（延续内容）"
        }
    ]
}
```

**比对结果补充：**

如果比对发现第 2 页的表格有差异：
```java
DiffBlock {
    type: MODIFIED,
    oldBboxes: [[x0, y0, x1, y1]],        // 原文档第 2 页
    pageA: [2],
    // 自动补充第 3 页的 bbox ↓
    oldBboxes: [[x0, y0, x1, y1], [x0', y0', x1', y1']],
    pageA: [2, 3],
    allTextA: ["表格第2页内容", ""]
}
```

## 日志输出

### 识别阶段

```
📋 识别到主表格: 第2页, 组ID: table_group_0
📋 识别到跨页表格延续部分: 第3页, 组ID: table_group_0
📊 跨页表格识别统计: 表格组总数: 5, 包含跨页的组: 2, 跨页部分总数: 3
```

### 补充阶段

```
检测到跨页表格，开始补充关联 bbox...
补充原文档跨页表格 bbox: 表格组 table_group_0, 新增 1 个 bbox
✅ 跨页表格 bbox 补充完成，共处理 3 个差异块
```

## 配置说明

该功能无需额外配置，在使用 MinerU OCR 时自动启用。

## 技术细节

### bbox 坐标系

- **MinerU 归一化坐标**: 0-1000 范围（`extractBbox` 方法）
- **图片坐标系**: 实际像素坐标（`convertAndValidateBbox` 方法转换）
- **存储格式**: 图片坐标系（与前端一致）

### 页码处理

- **content_list**: 0-based（MinerU 原始格式）
- **DiffBlock**: 1-based（与前端显示一致）
- **查找时**: 自动尝试两种格式

### 去重策略

使用页码 + bbox 坐标创建唯一键：
```java
String bboxKey = String.format("%d_%.2f_%.2f_%.2f_%.2f", 
    page, bbox[0], bbox[1], bbox[2], bbox[3]);
```

保留 2 位小数以允许小的浮点误差。

## 文件结构

```
contract-tools-core/src/main/java/
├── model/
│   ├── CrossPageTableManager.java      # 跨页表格管理器
│   └── MinerURecognitionResult.java    # MinerU 识别结果包装类
└── service/
    ├── CompareService.java             # 比对服务（补充 bbox 逻辑）
    └── MinerUOCRService.java           # MinerU OCR 服务（识别逻辑）
```

## 性能影响

- **识别阶段**: 几乎无影响（单次遍历 content_list）
- **补充阶段**: 仅处理包含跨页表格的差异块
- **内存占用**: 每个表格组约 1-2 KB

## 注意事项

1. **文本内容**: 跨页部分的文本使用空字符串占位，不影响 bbox 定位
2. **坐标精度**: 使用 2 位小数精度进行 bbox 匹配
3. **页码转换**: 自动处理 0-based 和 1-based 的转换
4. **去重保护**: 避免重复添加相同的 bbox

## 未来优化方向

1. 支持更复杂的表格识别规则（如部分标题的跨页表格）
2. 添加表格文本内容的智能提取
3. 支持表格跨度超过 2 页的情况
4. 提供手动调整跨页表格关联的接口

## 版本历史

- **v1.0** (2025-10-13): 初始版本，支持基本的跨页表格识别和关联

