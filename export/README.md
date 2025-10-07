# GPU OCR 离线比对查看器使用指南

## 项目简介

这是一个离线的GPU OCR合同比对结果查看器，支持通过file://协议直接在浏览器中打开HTML文件查看比对结果，无需部署Web服务器。

## 功能特点

- ✅ **离线查看**：支持file://协议，无需Web服务器
- ✅ **完整功能**：差异列表、统计信息、页面导航
- ✅ **JSON内嵌**：避免CORS问题，数据直接嵌入HTML
- ✅ **动态数据**：可随时更换JSON文件和图片查看不同任务
- ✅ **相对路径**：图片使用相对路径，便于文件管理

## 目录结构

```
export/                     # 独立的导出项目
├── embed-json-data.cjs     # 🆕 JSON数据内嵌脚本（CommonJS格式，兼容ES模块项目）
├── src/                    # Vue源代码
├── public/
│   └── data/
│       └── current/        # 当前任务数据目录
│           ├── task-status.json      # 任务状态数据
│           ├── compare-result.json   # 比对结果数据  
│           └── images/
│               ├── old/              # 原文档图片
│               │   ├── page-1.png
│               │   ├── page-2.png
│               │   └── ...
│               └── new/              # 新文档图片
│                   ├── page-1.png
│                   ├── page-2.png
│                   └── ...
├── dist/                   # 构建输出目录
│   ├── index.html         # 构建后的HTML文件
│   └── data/              # 备用数据目录（支持从后端导出）
│       └── current/
├── package.json
├── vite.config.ts
└── README.md
```

## 完整使用流程

### 1. 准备数据文件

将您的比对任务数据放置到指定目录：

```bash
# 复制JSON文件
cp /path/to/your/task-status.json export/public/data/current/
cp /path/to/your/compare-result.json export/public/data/current/

# 复制图片文件
cp /path/to/old/images/* export/public/data/current/images/old/
cp /path/to/new/images/* export/public/data/current/images/new/
```

### 2. 构建前端项目

```bash
# 进入 export 项目目录
cd export

# 安装依赖（首次运行）
npm install

# 构建项目
npm run build
```

### 3. 内嵌JSON数据

在 export 项目目录下，运行数据内嵌脚本：

```bash
# 🆕 在 export 目录下直接运行（推荐）
node embed-json-data.cjs

# 或查看帮助信息
node embed-json-data.cjs --help
```

**内嵌脚本功能**：
- ✅ **智能路径检测**：自动检测 `public/` 和 `dist/` 目录下的数据文件
- ✅ **备用路径支持**：支持从后端导出的数据结构
- ✅ **统一管理**：脚本位于 export 项目内，便于维护
- ✅ **详细日志**：显示文件路径和数据统计信息
- ✅ **错误处理**：友好的错误提示和解决建议

**脚本处理流程**：
1. 读取 `./dist/index.html`（构建后的HTML文件）
2. 读取 `./public/data/current/task-status.json`（主数据源）
3. 读取 `./public/data/current/compare-result.json`（主数据源）
4. 如果主数据源不存在，尝试 `./dist/data/current/` 备用路径
5. 将JSON数据作为全局变量嵌入HTML中
6. 生成最终的可离线使用的HTML文件

### 4. 打开查看结果

直接用浏览器打开构建后的HTML文件：

```
file:///D:/git/zhaoxin-contract-tool-set/export/dist/index.html#/gpu-ocr-canvas-compare-result
```

**或者**：
1. 用文件管理器导航到 `export/dist/` 目录
2. 双击 `index.html` 文件
3. 浏览器会自动跳转到比对结果页面

## 快速命令总览

```bash
# 🆕 完整流程（在 export 目录执行）
cd export
npm run build
node embed-json-data.cjs

# 然后打开文件
# file:///D:/git/zhaoxin-contract-tool-set/export/dist/index.html#/gpu-ocr-canvas-compare-result
```

## 🆕 高级功能

### 从后端导出数据集成

如果您有后端导出的ZIP文件，可以直接集成：

```bash
# 方法1: 手动解压后端ZIP到 dist 目录
unzip backend-export.zip -d ./dist/

# 方法2: 使用脚本自动处理（需要安装 adm-zip）
npm install adm-zip
node -e "require('./embed-json-data.cjs').copyFromBackendExport('path/to/backend-export.zip')"
```

### 自定义配置

```bash
# 自定义HTML文件路径
node embed-json-data.cjs --htmlPath ./custom/index.html

# 自定义数据目录
node embed-json-data.cjs --dataDir custom/data/path

# 查看所有选项
node embed-json-data.cjs --help
```

## 更换任务数据

要查看不同的比对任务结果：

1. **替换JSON文件**：
   ```bash
   # 在 export 目录下执行
   cp new-task-status.json ./public/data/current/task-status.json
   cp new-compare-result.json ./public/data/current/compare-result.json
   ```

2. **替换图片文件**：
   ```bash
   # 清理旧图片
   rm ./public/data/current/images/old/*
   rm ./public/data/current/images/new/*
   
   # 复制新图片
   cp /path/to/new/old/images/* ./public/data/current/images/old/
   cp /path/to/new/new/images/* ./public/data/current/images/new/
   ```

3. **重新内嵌数据**：
   ```bash
   # 🆕 在 export 目录下执行
   node embed-json-data.cjs
   ```

4. **刷新浏览器**查看新结果

### 🆕 批量处理多个任务

```bash
# 创建处理脚本
cat > process-task.sh << 'EOF'
#!/bin/bash
TASK_DIR=$1
if [ -z "$TASK_DIR" ]; then
  echo "用法: ./process-task.sh <任务目录>"
  exit 1
fi

echo "处理任务: $TASK_DIR"
cp "$TASK_DIR/task-status.json" ./public/data/current/
cp "$TASK_DIR/compare-result.json" ./public/data/current/
cp "$TASK_DIR/images/old/"* ./public/data/current/images/old/
cp "$TASK_DIR/images/new/"* ./public/data/current/images/new/
node embed-json-data.cjs
echo "任务处理完成，可以打开 dist/index.html 查看结果"
EOF

chmod +x process-task.sh

# 使用脚本处理任务
./process-task.sh /path/to/task/data
```

## 数据格式要求

### task-status.json 格式
```json
{
  "currentPageOld": 14,
  "totalSteps": 8,
  "oldFileName": "原文档.pdf",
  "newFileName": "新文档.pdf",
  "remainingTime": "0秒",
  "currentPageNew": 14
}
```

### compare-result.json 格式
```json
{
  "failedPages": [],
  "failedPagesCount": 0,
  "differences": [...],
  "oldImageInfo": {
    "totalPages": 14,
    "pages": [...]
  },
  "newImageInfo": {
    "totalPages": 14,
    "pages": [...]
  }
}
```

### 图片命名格式
```
old/page-1.png, old/page-2.png, ...
new/page-1.png, new/page-2.png, ...
```

## 技术细节

### 解决的问题
- **CORS限制**：通过JSON数据内嵌避免file://协议的跨域问题
- **路径问题**：使用相对路径确保文件可移植性
- **模块加载**：使用单文件构建避免ES模块加载问题

### 构建配置
- 使用 `vite-plugin-singlefile` 插件将所有资源打包到单个HTML文件
- 设置 `base: './'` 确保相对路径
- 使用 Vue Hash Router 兼容file://协议

### 数据加载机制
1. 构建时：Vue代码编译到HTML中
2. 运行时：读取内嵌的全局变量 `window.TASK_STATUS_DATA` 和 `window.COMPARE_RESULT_DATA`
3. 图片：通过相对路径动态加载

## 常见问题

### Q: 为什么要运行embed-json-data.js？
A: 因为file://协议不允许动态加载JSON文件，所以需要将JSON数据直接嵌入HTML中作为全局变量。

### Q: 🆕 embed-json-data.js 移动到 export 项目有什么好处？
A: 
- ✅ **统一管理**：脚本和相关文件在同一项目中，便于维护
- ✅ **路径简化**：使用相对路径，不需要复杂的路径计算
- ✅ **独立部署**：export 项目可以独立使用，不依赖外部脚本
- ✅ **版本控制**：脚本版本与前端代码版本保持一致

### Q: 可以直接修改JSON文件而不重新构建吗？
A: 可以，只需要替换 `public/data/current/` 目录下的JSON和图片文件，然后重新运行 `node embed-json-data.cjs` 即可。

### Q: 图片不显示怎么办？
A: 
1. 检查图片文件是否存在于 `public/data/current/images/old/` 和 `public/data/current/images/new/` 目录中
2. 确认图片命名格式为 `page-1.png`, `page-2.png` 等
3. 运行 `node embed-json-data.cjs --help` 查看路径配置
4. 检查控制台是否有路径相关的错误信息

### Q: 🆕 如何处理后端导出的数据？
A: 
```bash
# 方法1: 手动解压到 dist 目录
unzip backend-export.zip -d ./dist/

# 方法2: 复制到 public 目录后重新嵌入
cp backend-data/* ./public/data/current/
node embed-json-data.cjs
```

### Q: 如何查看控制台错误信息？
A: 在浏览器中按F12打开开发者工具，查看Console标签页的错误信息。

### Q: 🆕 脚本运行失败怎么办？
A: 
1. 确保在 export 目录下运行脚本
2. 检查 `dist/index.html` 文件是否存在（需要先运行 `npm run build`）
3. 运行 `node embed-json-data.cjs --help` 查看使用说明
4. 查看脚本输出的详细错误信息和路径提示

## 版本信息

- Vue 3
- Vite 5
- Element Plus
- 支持现代浏览器（Chrome, Firefox, Edge, Safari）

---

## 🚀 Java后端增强：智能任务状态生成

### 📋 改进概述

Java后端现在能够根据实际的比对结果智能生成完整的 `task-status.json` 数据，确保状态信息与实际情况完全一致。

### 🔧 技术改进

#### **Java 后端优化**
- **新增 `generateTaskStatusJsonFromCompareResult()` 方法**：根据实际比对结果生成任务状态
- **智能页面统计**：从比对结果中自动提取真实的页面数量信息
- **数据一致性保证**：任务状态与比对结果完全同步
- **完整状态数据**：生成包含所有必需字段的标准任务状态

### 📊 智能生成逻辑

```java
// Java后端自动生成完整的任务状态数据
private String generateTaskStatusJsonFromCompareResult(CompareResult result, ExportRequest request, String compareResultJson) {
    // 1. 解析比对结果获取实际页面信息
    JsonNode compareData = mapper.readTree(compareResultJson);
    int oldPages = compareData.path("oldImageInfo").path("totalPages").asInt(0);
    int newPages = compareData.path("newImageInfo").path("totalPages").asInt(0);
    int totalPages = Math.max(oldPages, newPages);
    
    // 2. 生成完整的任务状态
    Map<String, Object> taskStatus = new HashMap<>();
    taskStatus.put("taskId", request.getTaskId());
    taskStatus.put("status", "COMPLETED");
    taskStatus.put("progress", 100);
    taskStatus.put("oldFileName", result.getOldFileName());
    taskStatus.put("newFileName", result.getNewFileName());
    taskStatus.put("totalPages", totalPages);        // 真实页面数量
    taskStatus.put("oldDocPages", oldPages);         // 原文档页数
    taskStatus.put("newDocPages", newPages);         // 新文档页数
    // ... 其他完整状态字段
    
    return mapper.writeValueAsString(taskStatus);
}
```

### 💡 主要优势

1. **数据准确性**：页面数量等信息来源于实际的比对结果，不再使用固定默认值
2. **自动化程度**：后端自动生成完整的任务状态，无需手动维护
3. **一致性保证**：任务状态与比对结果数据完全同步
4. **完整性**：生成包含所有必需字段的标准JSON格式

### 🎯 实际效果

**之前的固定数据**：
```json
{
  "totalPages": 14,        // 固定默认值
  "oldDocPages": 14,       // 固定默认值
  "newDocPages": 14,       // 固定默认值
  "currentPageOld": 14,    // 固定默认值
  "currentPageNew": 14     // 固定默认值
}
```

**现在的智能生成**：
```json
{
  "totalPages": 6,         // 从实际比对结果获取
  "oldDocPages": 6,        // 原文档真实页数
  "newDocPages": 6,        // 新文档真实页数
  "currentPageOld": 6,     // 基于实际页数
  "currentPageNew": 6      // 基于实际页数
}
```

---

**注意**：此查看器专为离线使用设计，所有数据和图片都需要本地存储。如需在Web服务器环境中使用，请参考原始的前端项目配置。
