# 肇新智能工具集 - Demo 演示项目

这是一个展示如何集成肇新智能工具集的综合 Demo 项目，包含多个智能文档处理功能的完整示例代码。

## ✨ 项目状态：✅ 100% 完成，可直接运行！

> **📖 新手？从这里开始 → [START_HERE.md](START_HERE.md)**  
> **🚀 立即部署？查看 → [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)**

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

访问：`http://localhost:3003`

## ✨ 核心功能

### 1. 智能文档抽取
- **功能**：基于规则引擎，从 PDF 文档中自动提取结构化信息
- **技术**：OCR + 规则引擎 + 智能定位
- **应用场景**：合同信息提取、发票数据录入、证件信息识别

### 2. 合同合成
- **功能**：基于模板自动生成合同文档
- **技术**：模板引擎 + 智能填充
- **应用场景**：批量生成合同、自动化文档生成

### 3. 智能文档解析
- **功能**：自动解析文档结构，提取关键信息
- **技术**：AI + NLP + 文档分析
- **应用场景**：文档智能分类、内容摘要、关键信息提取

## 📚 文档

- [快速开始](docs/快速开始.md) - 5分钟上手指南
- [智能文档抽取集成指南](docs/智能文档抽取集成指南.md) - 详细集成说明
- [合同合成集成指南](docs/合同合成集成指南.md) - 详细集成说明
- [智能文档解析集成指南](docs/智能文档解析集成指南.md) - 详细集成说明

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
  apiBaseUrl: 'http://localhost:8080',   # 肇新API地址
  demoBaseUrl: 'http://localhost:8091'   # Demo后端地址
}
```

## 🐛 常见问题

### 1. 端口冲突

确保以下端口未被占用：
- `8080` - 肇新后端
- `8091` - Demo 后端
- `3003` - Demo 前端

### 2. 文件上传失败

检查：
- 文件格式是否支持（PDF）
- 文件大小是否超过 50MB
- uploads 文件夹是否有写权限

## 📄 开源协议

MIT License

## 👥 支持

如有问题，请联系肇新技术支持团队。
- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)

