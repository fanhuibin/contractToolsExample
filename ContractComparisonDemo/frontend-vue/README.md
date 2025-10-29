# 合同比对集成示例 - Vue 3 前端

## 🎯 项目简介

基于 Vue 3 + Vite 构建的现代化前端应用，演示如何集成肇新合同比对工具。

### ✨ 架构特点

1. **自制上传页面** - 完全自主实现的文件上传和任务提交界面
2. **API 代理转发** - Demo 后端作为代理，转发请求到肇新后端
3. **iframe 嵌套结果** - 结果页使用 iframe 嵌套肇新原有结果展示组件
4. **遵循 OpenSpec** - 不修改肇新原有代码，仅做集成演示

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3002

### 构建生产版本

```bash
npm run build
```

## 📂 项目结构

```
frontend-vue/
├── index.html              # HTML入口
├── package.json            # 项目配置
├── vite.config.js          # Vite配置
├── src/
│   ├── main.js             # 应用入口
│   ├── App.vue             # 根组件
│   ├── style.css           # 全局样式
│   ├── api/
│   │   └── index.js        # API封装
│   ├── utils/
│   │   └── compareHelper.js # 工具函数
│   └── views/
│       ├── Home.vue        # 首页
│       ├── Compare.vue     # 比对页(iframe)
│       ├── Result.vue      # 结果页(iframe)
│       └── TestApi.vue     # API测试页
└── start.bat               # Windows启动脚本
```

## 🔧 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP 客户端

## 📖 页面说明

### 1. 首页 (/)
- 项目介绍和欢迎页面
- 功能导航和快速入口
- 系统状态展示

### 2. 比对页面 (/compare) 【自制上传页面】
- ✅ 双文件拖拽上传（基准文件 + 比对文件）
- ✅ 文件格式验证（支持 PDF、Word）
- ✅ 任务提交到 Demo 后端
- ✅ 实时进度条显示
- ✅ 完成后跳转到结果页

**技术实现**：
```javascript
// 文件上传 → Demo 后端 → 肇新后端
api.submitCompare(oldFileUrl, newFileUrl)
// 轮询任务状态
pollTaskStatus(taskId, onProgress)
// 完成后跳转
router.push(`/result/${taskId}`)
```

### 3. 结果页面 (/result/:taskId) 【iframe 嵌套】
- ✅ iframe 嵌入肇新前端的结果展示组件
- ✅ 完整的 Canvas 差异标注功能
- ✅ 支持链接分享和导出报告
- ✅ 保持肇新原有的所有交互功能

**技术实现**：
```vue
<iframe 
  :src="`${ZHAOXIN_CONFIG.frontendUrl}/#/gpu-ocr-compare-result/${taskId}`"
/>
```

### 4. API测试页 (/test-api)
- 直接调用后端 API 接口
- 查看请求和响应数据
- 完整的调用日志输出

## 🔌 API 配置

Vite 已配置 API 代理，所有 `/api` 请求会自动转发到后端:

```javascript
// vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8090',
      changeOrigin: true,
    }
  }
}
```

## 🎨 样式说明

- 使用 CSS 变量定义主题色
- 响应式设计，支持移动端
- 组件样式使用 scoped 隔离

## 📝 开发指南

### 添加新页面

1. 在 `src/views/` 创建新组件
2. 在 `src/main.js` 添加路由配置
3. 在导航中添加链接

### 调用 API

```javascript
import api from '@/api'

// 提交比对任务
const result = await api.submitCompare(oldUrl, newUrl)

// 获取任务状态
const status = await api.getTaskStatus(taskId)

// 获取比对结果
const compareResult = await api.getResult(taskId)
```

### 轮询任务状态

```javascript
import { pollTaskStatus } from '@/utils/compareHelper'

await pollTaskStatus(taskId, (progress, status) => {
  console.log(`进度: ${progress}%`)
})
```

## 🌐 环境配置

### 开发环境
- Demo前端: http://localhost:3002
- Demo后端: http://localhost:8090
- 肇新后端: http://localhost:8080
- 肇新前端: http://localhost:3000

### 生产环境

修改 `vite.config.js` 和 `src/api/index.js` 中的地址配置。

## ❓ 常见问题

### 1. 端口被占用

修改 `vite.config.js` 中的端口配置：

```javascript
server: {
  port: 3001, // 改为其他端口
}
```

### 2. API 请求失败

检查后端服务是否已启动（http://localhost:8090）

### 3. iframe 无法加载

确认肇新前端服务已启动（http://localhost:5173）

## 📞 技术支持

- 邮箱: tech@zhaoxinms.com
- 网站: http://zhaoxinms.com

---

**版本**: v1.0.0  
**更新时间**: 2025-01-25

