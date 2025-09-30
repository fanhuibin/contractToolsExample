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
frontend/export/
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
│   └── index.html         # 构建后的HTML文件
├── package.json
├── vite.config.ts
└── README.md
```

## 完整使用流程

### 1. 准备数据文件

将您的比对任务数据放置到指定目录：

```bash
# 复制JSON文件
cp /path/to/your/task-status.json frontend/export/public/data/current/
cp /path/to/your/compare-result.json frontend/export/public/data/current/

# 复制图片文件
cp /path/to/old/images/* frontend/export/public/data/current/images/old/
cp /path/to/new/images/* frontend/export/public/data/current/images/new/
```

### 2. 构建前端项目

```bash
# 进入前端目录
cd frontend/export

# 安装依赖（首次运行）
npm install

# 构建项目
npm run build
```

### 3. 内嵌JSON数据

回到项目根目录，运行数据内嵌脚本：

```bash
# 回到项目根目录
cd ../..

# 运行数据内嵌脚本
node embed-json-data.js
```

**内嵌脚本作用**：
- 读取 `frontend/export/dist/index.html`
- 读取 `frontend/export/public/data/current/task-status.json`
- 读取 `frontend/export/public/data/current/compare-result.json`
- 将JSON数据作为全局变量嵌入HTML中
- 覆盖原HTML文件

### 4. 打开查看结果

直接用浏览器打开构建后的HTML文件：

```
file:///D:/git/zhaoxin-contract-tool-set/frontend/export/dist/index.html#/gpu-ocr-canvas-compare-result
```

**或者**：
1. 用文件管理器导航到 `frontend/export/dist/` 目录
2. 双击 `index.html` 文件
3. 浏览器会自动跳转到比对结果页面

## 快速命令总览

```bash
# 完整流程（在项目根目录执行）
cd frontend/export && npm run build && cd ../.. && node embed-json-data.js

# 然后打开文件
# file:///D:/git/zhaoxin-contract-tool-set/frontend/export/dist/index.html#/gpu-ocr-canvas-compare-result
```

## 更换任务数据

要查看不同的比对任务结果：

1. **替换JSON文件**：
   ```bash
   cp new-task-status.json frontend/export/public/data/current/task-status.json
   cp new-compare-result.json frontend/export/public/data/current/compare-result.json
   ```

2. **替换图片文件**：
   ```bash
   rm frontend/export/public/data/current/images/old/*
   rm frontend/export/public/data/current/images/new/*
   cp /path/to/new/old/images/* frontend/export/public/data/current/images/old/
   cp /path/to/new/new/images/* frontend/export/public/data/current/images/new/
   ```

3. **重新内嵌数据**：
   ```bash
   node embed-json-data.js
   ```

4. **刷新浏览器**查看新结果

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

### Q: 可以直接修改JSON文件而不重新构建吗？
A: 可以，只需要替换 `public/data/current/` 目录下的JSON和图片文件，然后重新运行 `node embed-json-data.js` 即可。

### Q: 图片不显示怎么办？
A: 检查图片文件是否存在于 `public/data/current/images/old/` 和 `public/data/current/images/new/` 目录中，且命名格式为 `page-1.png`, `page-2.png` 等。

### Q: 如何查看控制台错误信息？
A: 在浏览器中按F12打开开发者工具，查看Console标签页的错误信息。

## 版本信息

- Vue 3
- Vite 5
- Element Plus
- 支持现代浏览器（Chrome, Firefox, Edge, Safari）

---

**注意**：此查看器专为离线使用设计，所有数据和图片都需要本地存储。如需在Web服务器环境中使用，请参考原始的前端项目配置。
