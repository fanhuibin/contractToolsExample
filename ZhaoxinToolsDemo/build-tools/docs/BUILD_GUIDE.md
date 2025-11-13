# 肇新工具集 Demo 构建指南

## 📁 项目结构

```
ZhaoxinToolsDemo/
├── backend/                    # 后端源码 (Spring Boot)
├── frontend/                   # 前端源码 (Vue 3 + Vite)
├── docs/                       # 项目文档
├── build-tools/                # 构建和部署工具 🔧
│   ├── scripts/                # 构建脚本
│   │   ├── build.bat          # Windows 完整构建
│   │   ├── build.sh           # Linux 完整构建
│   │   └── quick-build.bat    # Windows 快速构建
│   ├── deployment/             # 部署配置
│   │   ├── nginx/             # Nginx 部署方案
│   │   └── docker/            # Docker 部署方案 (待开发)
│   └── docs/                  # 构建部署文档
├── dist/                      # 构建产物 (自动生成)
├── build.bat                  # 构建入口 (Windows)
├── build.sh                   # 构建入口 (Linux)
├── deploy.bat                 # 部署入口 (Windows)
├── deploy.sh                  # 部署入口 (Linux)
└── README.md                  # 项目说明
```

## 🚀 快速开始

### 1. 环境要求

**开发环境**:
- Java 17+
- Maven 3.6+
- Node.js 16+
- npm 8+

**部署环境**:
- Linux 服务器
- Nginx 1.18+
- Java 17+ (运行时)

### 2. 构建项目

#### Windows 环境
```bash
# 方式1: 使用入口脚本 (推荐)
./build.bat
# 选择 "1. 完整构建"

# 方式2: 直接执行构建脚本
./build-tools/scripts/build.bat

# 方式3: 快速构建 (开发测试)
./build-tools/scripts/quick-build.bat
```

#### Linux 环境
```bash
# 方式1: 使用入口脚本 (推荐)
chmod +x build.sh
./build.sh
# 选择 "1. 完整构建"

# 方式2: 直接执行构建脚本
chmod +x build-tools/scripts/build.sh
./build-tools/scripts/build.sh
```

### 3. 部署项目

#### Windows 环境
```bash
# 使用部署入口脚本
./deploy.bat
# 选择部署方式
```

#### Linux 环境
```bash
# 使用部署入口脚本
chmod +x deploy.sh
./deploy.sh
# 选择 "1. Nginx 生产部署"
```

## 📦 构建脚本说明

### 完整构建脚本

**功能特性**:
- 🔍 环境检查 (Java, Maven, Node.js)
- 🧹 自动清理旧构建文件
- 📦 Maven 后端打包
- 🎨 Vite 前端构建
- 📁 创建完整的 dist 目录结构
- 🚀 生成启动脚本 (Windows/Linux)
- 📖 自动生成部署文档

**构建产物**:
```
dist/
├── backend/                    # 后端 JAR 文件
├── frontend/                   # 前端静态文件
├── scripts/                    # 开发环境启动脚本
├── config/                     # 配置文件
├── nginx/                      # Nginx 部署文件
├── logs/                       # 日志目录
└── README.md                   # 部署说明
```

### 快速构建脚本

**适用场景**:
- 开发环境快速测试
- 单独构建前端或后端
- 清理构建缓存

**交互选项**:
1. 仅构建后端
2. 仅构建前端
3. 构建全部
4. 清理构建缓存

## 🌐 部署方案

### 1. Nginx 生产部署 ⭐ 推荐

**特性**:
- 统一 80 端口访问
- 前端静态文件服务
- 后端 API 反向代理
- 性能优化和安全配置

**部署步骤**:
```bash
# 1. 构建项目
./build.sh

# 2. 部署到服务器
./deploy.sh
# 选择 "1. Nginx 生产部署"
```

**访问地址**:
- 前端: `http://your-server-ip/`
- API: `http://your-server-ip/api/`

### 2. 开发环境部署

**特性**:
- 前后端分离运行
- 适合开发调试
- 热重载支持

**启动方式**:
```bash
# 构建后使用开发脚本
cd dist/scripts
./start-all.sh    # Linux
start-all.bat     # Windows
```

**访问地址**:
- 前端: `http://localhost:3004`
- 后端: `http://localhost:8091`

### 3. Docker 部署 (规划中)

**特性**:
- 容器化部署
- 环境隔离
- 易于扩展

## ⚙️ 配置说明

### 后端配置

**关键配置文件**: `backend/src/main/resources/application.yml`

```yaml
server:
  port: 8091                    # 后端服务端口

zhaoxin:
  api:
    base-url: http://your-zhaoxin-api-server    # 肇新API服务地址
  frontend:
    url: http://your-zhaoxin-frontend           # 肇新前端地址
  demo:
    backend-url: http://your-demo-backend:8091  # Demo后端地址
```

### 前端配置

**关键配置文件**: `frontend/vite.config.js`

```javascript
export default defineConfig({
  server: {
    port: 3004,                 # 开发服务器端口
    proxy: {
      '/api': {
        target: 'http://localhost:8091',    # 代理到后端
        changeOrigin: true,
      }
    }
  }
})
```

### Nginx 配置

**配置文件**: `build-tools/deployment/nginx/nginx.conf`

**核心配置**:
- 前端静态文件服务
- API 反向代理
- Gzip 压缩
- 静态资源缓存
- 安全头设置

## 🔧 自定义构建

### 修改构建脚本

构建脚本位于 `build-tools/scripts/` 目录：

**Windows**: `build.bat`
**Linux**: `build.sh`

### 添加构建步骤

在构建脚本中可以添加：
- 代码质量检查
- 单元测试执行
- 安全扫描
- 性能测试

### 自定义部署配置

部署配置位于 `build-tools/deployment/` 目录：

**Nginx**: `nginx/`
**Docker**: `docker/` (待开发)

## 🛠️ 故障排除

### 构建问题

**Maven 构建失败**:
```bash
# 检查 Java 版本
java -version

# 清理 Maven 缓存
cd backend
mvn clean
```

**前端构建失败**:
```bash
# 清理 node_modules
cd frontend
rm -rf node_modules
npm install
```

### 部署问题

**502 Bad Gateway**:
```bash
# 检查后端服务
sudo systemctl status zhaoxin-demo-backend
sudo netstat -tlnp | grep 8091
```

**404 Not Found**:
```bash
# 检查前端文件
ls -la /usr/share/nginx/html/zhaoxin-demo/
sudo nginx -t
```

### 权限问题

**脚本无执行权限**:
```bash
# 设置执行权限
chmod +x build.sh
chmod +x deploy.sh
chmod +x build-tools/scripts/*.sh
```

## 📚 相关文档

- **部署总结**: `build-tools/docs/DEPLOY_SUMMARY.md`
- **Nginx 部署**: `build-tools/deployment/nginx/README.md`
- **API 文档**: `docs/` 目录

## 🎯 最佳实践

### 开发环境

1. 使用快速构建进行日常开发
2. 定期执行完整构建验证
3. 使用开发脚本启动服务

### 生产环境

1. 使用完整构建生成部署包
2. 使用 Nginx 部署方案
3. 配置监控和日志
4. 定期备份配置文件

### CI/CD 集成

构建脚本支持 CI/CD 集成：

```yaml
# GitHub Actions 示例
- name: Build Project
  run: |
    chmod +x build-tools/scripts/build.sh
    ./build-tools/scripts/build.sh

- name: Deploy to Server
  run: |
    scp -r dist/ user@server:/opt/zhaoxin-demo/
    ssh user@server "cd /opt/zhaoxin-demo/nginx && sudo ./deploy-nginx.sh"
```

---

**版本**: 2.0.0  
**更新时间**: 2025-01-13  
**维护团队**: 肇新科技
