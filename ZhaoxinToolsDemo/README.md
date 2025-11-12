# 肇新智能工具集 - Demo 演示项目

这是一个展示如何集成肇新智能工具集的综合 Demo 项目，包含多个智能文档处理功能的完整示例代码。

## ✨ 项目状态：✅ 100% 完成，可直接运行！

> **📖 新手？从这里开始 → [START_HERE.md](START_HERE.md)**  
> **🚀 立即部署？查看 → [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)**

## 🎯 包含功能

本 Demo 项目集成了以下肇新智能工具：

1. **智能文档抽取** - 基于规则引擎的信息提取
2. **智能文档比对** - PDF文档智能比对分析
3. **智能合同合成** - 模板化合同生成（✨ 新增）

## 📦 项目结构

```
ZhaoxinToolsDemo/
├── backend/              # Spring Boot 后端（端口 8091）
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zhaoxin/tools/demo/
│   │       │       ├── controller/    # 控制器
│   │       │       ├── service/       # 服务层
│   │       │       ├── model/         # 数据模型
│   │       │       └── config/        # 配置类
│   │       └── resources/
│   │           └── application.yml    # 配置文件
│   ├── pom.xml
│   └── start.bat                      # 启动脚本
│
├── frontend/            # Vue 3 前端（端口 3003）
│   ├── src/
│   │   ├── views/                     # 页面组件
│   │   │   ├── Extract.vue            # 智能文档抽取
│   │   │   ├── Compose.vue            # 合同合成
│   │   │   └── Parse.vue              # 智能文档解析
│   │   ├── components/                # 公共组件
│   │   │   └── Sidebar.vue            # 侧边栏导航
│   │   ├── api/                       # API 客户端
│   │   ├── router/                    # 路由配置
│   │   └── config.js                  # 配置文件
│   ├── package.json
│   └── start.bat                      # 启动脚本
│
└── docs/                # 文档目录
    ├── 智能文档抽取集成指南.md
    ├── 合同合成集成指南.md
    ├── 智能文档解析集成指南.md
    └── 快速开始.md
```

## 🚀 快速开始

### 前置条件

1. **肇新服务已启动**
   - 后端：`http://localhost:8080`

2. **开发环境**
   - Java 17+
   - Node.js 16+
   - Maven 3.6+

### 启动步骤

#### 1. 启动 Demo 后端

```bash
cd backend
start.bat
```

访问：`http://localhost:8091`

#### 2. 启动 Demo 前端

```bash
cd frontend
npm install   # 首次运行
start.bat
```

访问：`http://localhost:3004`

## ✨ 核心功能

### 1. 智能文档抽取
- **功能**：基于规则引擎，从 PDF 文档中自动提取结构化信息
- **技术**：OCR + 规则引擎 + 智能定位
- **应用场景**：合同信息提取、发票数据录入、证件信息识别
- **页面路由**：`/extract-main`

### 2. 智能文档比对
- **功能**：快速比对两个 PDF 文档的差异，生成高亮标注的比对报告
- **技术**：GPU加速OCR + 智能去水印 + 差异分析
- **应用场景**：合同版本对比、文档变更追踪、法律文书审核
- **页面路由**：`/compare`

### 3. 智能合同合成
- **功能**：基于 ContentControl 模板，通过数据填充自动生成合同文档
- **技术**：OnlyOffice 编辑器 + ContentControl + HTML 富文本支持
- **应用场景**：批量生成合同、自动化文档生成、动态内容填充
- **页面路由**：`/compose-main`
- **特点**：
  - 可视化模板设计（iframe 嵌入）
  - 支持纯文本和 HTML 富文本
  - 支持动态表格数据
  - 后端生成 + 直接下载

## 📚 文档

- [架构说明](docs/架构说明.md) - **必读**：理解系统架构和 API 调用流程
- [智能文档抽取集成指南](docs/智能文档抽取集成指南.md) - 详细集成说明
- [智能文档比对集成指南](docs/智能文档比对集成指南.md) - 详细集成说明

## 🔧 技术栈

### 后端
- Spring Boot 3.2.1
- RestTemplate（HTTP 客户端）
- Jackson（JSON 序列化）
- Lombok（简化代码）

### 前端
- Vue 3（组合式 API）
- Vue Router（路由管理）
- Vite（构建工具）
- Axios（HTTP 客户端）
- Element Plus（UI 组件库）

## 🎯 设计原则

1. **不修改源代码** - 仅通过 API 集成
2. **配置化** - 所有 URL 集中在配置文件
3. **模块化** - 每个功能独立模块
4. **简洁明了** - 代码结构清晰，易于理解
5. **生产就绪** - 包含错误处理、日志、CORS 配置

## 📝 配置说明

### 后端配置（`backend/src/main/resources/application.yml`）

```yaml
server:
  port: 8091

zhaoxin:
  api:
    base-url: http://localhost:8080      # 肇新后端地址
    timeout: 60000
```

### 前端配置（`frontend/src/config.js`）

```javascript
export const ZHAOXIN_CONFIG = {
  frontendUrl: 'http://localhost:3000',  # 肇新前端地址（用于iframe）
  apiBaseUrl: 'http://localhost:8080',   # 肇新API地址
  demoBaseUrl: 'http://localhost:8091'   # Demo后端地址
}
```

## 🐛 常见问题

### 1. 端口冲突

确保以下端口未被占用：
- `8080` - 肇新后端
- `3000` - 肇新前端
- `8091` - Demo 后端
- `3003` - Demo 前端

### 2. 文件上传失败

检查：
- 文件格式是否支持（智能抽取支持PDF，智能比对仅支持PDF）
- 文件大小是否超过限制（默认 50MB）
- uploads 文件夹是否有写权限

### 3. 比对功能显示问题

如果比对结果页面显示不正常：
- 确保肇新前端服务（端口3000）正常运行
- 检查 `frontend/src/config.js` 中的 `frontendUrl` 配置是否正确
- 确认浏览器允许跨域 iframe 嵌套

## 📄 开源协议

MIT License

## 👥 支持

如有问题，请联系肇新技术支持团队。
- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)

