# 肇新工具集 Demo

## 📖 项目简介

肇新工具集 Demo 是一个完整的前后端分离项目，展示了智能文档比对和智能文档抽取功能的集成使用。

### 技术栈

- **后端**: Spring Boot 3.2.1 + Java 17
- **前端**: Vue 3 + Vite + Element Plus  
- **部署**: Nginx + Docker (可选)

### 主要功能

- 🔍 **智能文档比对** - 基于 GPU OCR 的文档智能比对
- 📄 **智能文档抽取** - 规则化的文档信息提取
- 🎨 **现代化界面** - 基于 Element Plus 的响应式界面
- 🚀 **一键部署** - 支持多种部署方案

## 🚀 快速开始

### 1. 环境要求

**开发环境**:
- Java 17+
- Maven 3.6+
- Node.js 16+
- npm 8+

**部署环境**:
- Linux 服务器
- Nginx 1.18+ (推荐) 或 Docker 20.10+

### 2. 构建项目

#### Windows
```bash
# 一键构建
./build.bat
```

#### Linux
```bash
# 一键构建
chmod +x build.sh
./build.sh
```

### 3. 部署项目

#### Windows
```bash
# 选择部署方式
./deploy.bat
```

#### Linux
```bash
# 选择部署方式
chmod +x deploy.sh
./deploy.sh
```

## 📁 项目结构

```
ZhaoxinToolsDemo/
├── backend/                    # 后端源码
│   ├── src/main/java/         # Java 源码
│   ├── src/main/resources/    # 配置文件
│   └── pom.xml                # Maven 配置
├── frontend/                   # 前端源码
│   ├── src/                   # Vue 源码
│   ├── public/                # 静态资源
│   ├── package.json           # npm 配置
│   └── vite.config.js         # Vite 配置
├── docs/                      # 项目文档
├── build-tools/               # 构建和部署工具 🔧
│   ├── scripts/               # 构建脚本
│   ├── deployment/            # 部署配置
│   │   ├── nginx/            # Nginx 部署
│   │   └── docker/           # Docker 部署
│   └── docs/                 # 构建文档
├── dist/                     # 构建产物 (自动生成)
├── build.bat/.sh             # 构建入口
├── deploy.bat/.sh            # 部署入口
└── README.md                 # 本文件
```

## 🌐 部署方案

### 1. Nginx 生产部署 ⭐ 推荐

**特性**:
- 统一 80 端口访问
- 高性能静态文件服务
- API 反向代理
- 生产级配置

**部署命令**:
```bash
./build.sh && ./deploy.sh
```

**访问地址**:
- 前端: `http://your-server-ip/`
- API: `http://your-server-ip/api/`

### 2. Docker 容器部署

**特性**:
- 容器化隔离
- 易于扩展
- 环境一致性

**部署命令**:
```bash
cd build-tools/deployment/docker
docker-compose up -d --build
```

### 3. 开发环境部署

**特性**:
- 前后端分离
- 适合开发调试
- 热重载支持

**启动命令**:
```bash
cd dist/scripts
./start-all.sh    # Linux
start-all.bat     # Windows
```

## ⚙️ 配置说明

### 后端配置

**文件位置**: `backend/src/main/resources/application.yml`

**关键配置**:
```yaml
zhaoxin:
  api:
    base-url: http://your-zhaoxin-api-server  # 肇新API服务地址
  frontend:
    url: http://your-zhaoxin-frontend         # 肇新前端地址
  demo:
    backend-url: http://your-demo-backend:8091 # Demo后端地址
```

### 前端配置

**文件位置**: `frontend/vite.config.js`

**关键配置**:
```javascript
server: {
  port: 3004,
  proxy: {
    '/api': {
      target: 'http://localhost:8091',
      changeOrigin: true,
    }
  }
}
```

## 🛠️ 开发指南

### 本地开发

1. **启动后端**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **启动前端**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. **访问应用**:
   - 前端: http://localhost:3004
   - 后端: http://localhost:8091

### 构建测试

```bash
# 快速构建测试
./build-tools/scripts/quick-build.bat  # Windows
```

### 代码规范

- **后端**: 遵循 Spring Boot 最佳实践
- **前端**: 遵循 Vue 3 Composition API 规范
- **提交**: 使用语义化提交信息

## 📚 文档

- **构建指南**: `build-tools/docs/BUILD_GUIDE.md`
- **部署总结**: `build-tools/docs/DEPLOY_SUMMARY.md`
- **Nginx 部署**: `build-tools/deployment/nginx/README.md`
- **Docker 部署**: `build-tools/deployment/docker/README.md`
- **API 文档**: `docs/` 目录

## 🔧 故障排除

### 构建问题

**Maven 构建失败**:
```bash
# 检查 Java 版本
java -version
# 清理重新构建
cd backend && mvn clean package
```

**前端构建失败**:
```bash
# 清理重新安装
cd frontend && rm -rf node_modules && npm install
```

### 部署问题

**502 Bad Gateway**:
```bash
# 检查后端服务状态
sudo systemctl status zhaoxin-demo-backend
```

**404 Not Found**:
```bash
# 检查前端文件
ls -la /usr/share/nginx/html/zhaoxin-demo/
```

## 🎯 功能特性

### 智能文档比对

- 支持 PDF 文档比对
- GPU OCR 文字识别
- 可视化差异展示
- 导出比对报告

### 智能文档抽取

- 规则化信息提取
- 模板配置管理
- 批量处理支持
- 结果数据导出

### 系统特性

- 响应式设计
- 多语言支持
- 权限管理
- 操作日志

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🆘 技术支持

- **官网**: https://www.zhaoxinms.com
- **邮箱**: develop@zhaoxinms.com
- **文档**: 查看 `docs/` 目录
- **问题**: 提交 GitHub Issues

---

**版本**: 1.0.0  
**更新时间**: 2025-01-13  
**维护团队**: 肇新科技

**🎉 开始使用肇新工具集 Demo，体验智能文档处理的强大功能！**
