# 肇新合同比对 - Demo 演示项目

这是一个展示如何集成肇新合同比对服务的 Demo 项目，包含完整的前端和后端示例代码。

## 📦 项目结构

```
ContractComparisonDemo/
├── backend/              # Spring Boot 后端（端口 8090）
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/zhaoxin/demo/
│   │       │       ├── controller/    # 控制器
│   │       │       ├── service/       # 服务层
│   │       │       ├── model/         # 数据模型
│   │       │       └── config/        # 配置类
│   │       └── resources/
│   │           └── application.yml    # 配置文件
│   ├── pom.xml
│   └── start.bat                      # 启动脚本
│
├── frontend-vue/         # Vue 3 前端（端口 3002）
│   ├── src/
│   │   ├── views/                     # 页面组件
│   │   │   ├── Compare.vue            # 比对页面
│   │   │   └── Result.vue             # 结果页面（iframe）
│   │   ├── api/                       # API 客户端
│   │   ├── utils/                     # 工具函数
│   │   └── config.js                  # 配置文件
│   ├── package.json
│   └── start.bat                      # 启动脚本
│
└── docs/                 # 文档目录
    ├── 前端集成指南.md
    ├── API对接说明.md
    └── 快速开始.md
```

## 🚀 快速开始

### 前置条件

1. **肇新服务已启动**
   - 后端：`http://localhost:8080`
   - 前端：`http://localhost:3000`

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

访问：`http://localhost:8090`

#### 2. 启动 Demo 前端

```bash
cd frontend-vue
npm install   # 首次运行
start.bat
```

访问：`http://localhost:3002`

## ✨ 核心功能

### 1. 文件上传
- 支持 PDF、Word 格式
- 最大 50MB
- UUID 命名避免冲突
- URL 编码支持中文

### 2. 比对任务
- 实时进度显示
- 轮询任务状态
- 自动刷新任务列表

### 3. 结果展示
- iframe 嵌套肇新前端结果页
- 嵌入模式（隐藏导航栏）
- 新窗口打开

### 4. 任务管理
- 任务历史列表
- 查看/下载/删除
- 显示原始文件名

## 📚 文档

- [快速开始](docs/快速开始.md) - 5分钟上手指南
- [前端集成指南](docs/前端集成指南.md) - 详细的前端集成说明
- [API对接说明](docs/API对接说明.md) - 后端 API 接口文档

## 🔧 技术栈

### 后端
- Spring Boot 3.2.1
- RestTemplate（HTTP 客户端）
- Lombok（简化代码）

### 前端
- Vue 3（组合式 API）
- Vite（构建工具）
- Axios（HTTP 客户端）
- Font Awesome（图标）

## 🎯 设计原则

1. **不修改源代码** - 仅通过 API 和 iframe 集成
2. **配置化** - 所有 URL 集中在配置文件
3. **简洁明了** - 代码结构清晰，易于理解
4. **生产就绪** - 包含错误处理、日志、CORS 配置

## 📝 配置说明

### 后端配置（`backend/src/main/resources/application.yml`）

```yaml
server:
  port: 8090

zhaoxin:
  api:
    base-url: http://localhost:8080      # 肇新后端地址
  frontend:
    url: http://localhost:3000            # 肇新前端地址
```

### 前端配置（`frontend-vue/src/config.js`）

```javascript
export const ZHAOXIN_CONFIG = {
  frontendUrl: 'http://localhost:3000',   // 肇新前端地址
  backendUrl: 'http://localhost:8080'     // 肇新后端地址
}
```

## 🐛 常见问题

### 1. 端口冲突

确保以下端口未被占用：
- `8080` - 肇新后端
- `3000` - 肇新前端
- `8090` - Demo 后端
- `3002` - Demo 前端

### 2. 文件上传失败

检查：
- 文件格式是否支持（PDF/Word）
- 文件大小是否超过 50MB
- uploads 文件夹是否有写权限

### 3. iframe 显示不全

确保：
- 肇新前端已添加嵌入模式支持
- URL 包含 `?embed=true` 参数

## 📄 开源协议

MIT License

## 👥 支持

如有问题，请联系肇新技术支持团队。

