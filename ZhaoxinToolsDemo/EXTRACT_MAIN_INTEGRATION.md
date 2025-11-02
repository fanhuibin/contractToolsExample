# ExtractMain 页面集成说明

本文档说明如何将 frontend 的 ExtractMain.vue 页面完全复用到 ZhaoxinToolsDemo 项目中。

## 已完成的工作

### 1. 组件复制
- ✅ **PageHeader.vue** - 页面头部组件
  - 位置: `frontend/src/components/PageHeader.vue`
  - 功能: 显示页面标题、描述和操作按钮
  - 适配: 使用 Font Awesome 图标替代 Element Plus 图标

### 2. 页面复制  
- ✅ **ExtractMain.vue** - 主要的信息提取页面
  - 位置: `frontend/src/views/ExtractMain.vue`
  - 功能: 
    - 文件上传（拖拽支持）
    - 模板选择
    - 页眉页脚设置
    - 进度跟踪
    - 结果展示
  - 适配: 
    - 使用原生HTML元素替代Element Plus组件（保持样式一致）
    - 使用 IframeDialog 嵌入主 frontend 的模板管理和结果展示页面

### 3. API 和工具函数
- ✅ **ruleExtract.js** - API调用函数
  - 位置: `frontend/src/api/ruleExtract.js`
  - 功能: 封装所有规则抽取相关的API调用
  
- ✅ **responseHelper.js** - 响应处理工具
  - 位置: `frontend/src/utils/responseHelper.js`
  - 功能: 
    - extractArrayData - 提取数组数据
    - extractObjectData - 提取对象数据
    - formatFileSize - 格式化文件大小
    - formatTime - 格式化时间

### 4. 路由配置
- ✅ 更新 `main.js`
  - 添加 ExtractMain 路由: `/extract-main`
  - 设置为默认首页
  - 保留原有的 Extract 页面: `/extract`

## 文件结构

```
ZhaoxinToolsDemo/frontend/
├── src/
│   ├── api/
│   │   ├── index.js (原有)
│   │   └── ruleExtract.js (新增)
│   ├── components/
│   │   ├── IframeDialog.vue (原有)
│   │   └── PageHeader.vue (新增)
│   ├── utils/
│   │   ├── extractHelper.js (原有)
│   │   └── responseHelper.js (新增)
│   ├── views/
│   │   ├── Extract.vue (原有)
│   │   └── ExtractMain.vue (新增)
│   ├── App.vue
│   ├── main.js (已更新)
│   ├── config.js
│   └── style.css
├── package.json
└── vite.config.js
```

## 配置说明

### API 配置
确保 `config.js` 中正确配置了后端API地址和前端URL：

```javascript
export const ZHAOXIN_CONFIG = {
  // 后端API基础URL
  apiBaseUrl: 'http://localhost:8080',
  
  // 主前端URL（用于嵌入iframe）
  frontendUrl: 'http://localhost:5173'
}
```

### 依赖项
项目已包含所需的所有依赖：
- ✅ Vue 3.3.4
- ✅ Vue Router 4.2.4
- ✅ Element Plus 2.11.5 (虽然ExtractMain.vue未直接使用，但保留兼容性)
- ✅ Axios 1.5.0

## 使用方法

### 1. 启动后端服务
```bash
cd ZhaoxinToolsDemo/backend
# 使用 start.bat 或直接运行
```

### 2. 启动主前端（用于iframe嵌入）
```bash
cd frontend
npm run dev
```

### 3. 启动Demo前端
```bash
cd ZhaoxinToolsDemo/frontend
npm run dev
# 或使用 start.bat
```

### 4. 访问
打开浏览器访问: `http://localhost:5174`（或配置的端口）

## 页面功能

### 文件上传
- 支持点击上传和拖拽上传
- 仅支持 PDF 格式
- 最大文件大小: 100MB
- 上传成功后显示文件信息

### 模板选择
- 下拉列表显示所有可用模板
- 自动加载启用状态的模板
- 显示模板名称和代码

### 页眉页脚设置
- 可选择是否忽略页眉页脚
- 可调整页眉高度百分比（0-50%）
- 可调整页脚高度百分比（0-50%）
- 默认值：启用，各6%

### 进度跟踪
- 实时显示任务状态
- 进度条可视化
- 状态时间线展示
- 支持取消任务

### 结果展示
- 通过 IframeDialog 嵌入主前端的结果页面
- 全屏模式查看
- 支持继续提取新文件

### 模板管理
- 通过 IframeDialog 嵌入主前端的模板管理页面
- 全屏模式操作
- 关闭后自动刷新模板列表

## 技术特点

### 1. 完全复用主前端功能
- 使用 IframeDialog 嵌入主前端的完整功能页面
- 避免重复开发
- 保持功能一致性

### 2. 独立的UI风格
- ExtractMain.vue 使用独立的现代化UI设计
- 渐变色背景
- 圆角卡片设计
- 流畅的动画效果

### 3. 响应式设计
- 支持桌面和移动端
- 自适应布局
- 触摸友好的交互

### 4. 错误处理
- 友好的错误提示
- 文件类型和大小验证
- API调用异常处理

## 与原 Extract.vue 的区别

### ExtractMain.vue (新页面)
- ✅ 现代化的UI设计
- ✅ 完整的步骤指示器
- ✅ 更好的视觉反馈
- ✅ 页眉页脚设置集成在主界面
- ✅ 使用 IframeDialog 嵌入完整功能
- ✅ 更强的响应式支持

### Extract.vue (原页面)
- 传统的表单式布局
- 简洁的任务历史表格
- 直接的API调用展示
- 适合开发调试

## 后续扩展

如果需要进一步定制，可以考虑：

1. **添加更多提取选项**
   - OCR提供商选择
   - 语言设置
   - 提取精度控制

2. **增强进度显示**
   - WebSocket实时推送
   - 更详细的进度信息
   - 日志查看

3. **结果导出**
   - 支持多种格式导出
   - 批量导出
   - 导出模板定制

4. **批量处理**
   - 支持上传多个文件
   - 队列管理
   - 并发控制

## 故障排查

### 问题1: 页面加载失败
- 检查路由配置是否正确
- 检查所有文件是否已复制到正确位置

### 问题2: API调用失败
- 检查 config.js 中的 apiBaseUrl 配置
- 确认后端服务已启动
- 检查浏览器控制台的网络请求

### 问题3: Iframe 内容无法显示
- 检查 config.js 中的 frontendUrl 配置
- 确认主前端服务已启动
- 检查浏览器跨域策略

### 问题4: 模板列表为空
- 确认后端数据库中有模板数据
- 检查模板状态是否为 'active'
- 查看浏览器控制台错误信息

## 联系支持

如有问题，请查看：
- 项目文档: `ZhaoxinToolsDemo/README.md`
- 快速开始: `ZhaoxinToolsDemo/START_HERE.md`
- API对接: `ZhaoxinToolsDemo/docs/`

