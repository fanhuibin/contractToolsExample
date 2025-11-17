# 肇新合同组件 Demo

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.3.4-brightgreen.svg)](https://vuejs.org/)
[![Demo](https://img.shields.io/badge/🚀_在线演示-101.42.11.82:81-brightgreen.svg)](http://101.42.11.82:81/compare)

基于 [肇新合同组件库](http://101.42.11.82/doc-center) 开发的完整演示应用，展示智能文档处理的强大功能。

## 🌟 在线演示

**🚀 立即体验**: [http://101.42.11.82:81/compare](http://101.42.11.82:81/compare)

## 🎯 项目简介

本项目是肇新合同组件库的官方演示应用，通过直观的 Web 界面展示了智能文档处理的核心功能。项目采用前后端分离架构，提供完整的 API 集成示例和用户界面。

### 核心功能

- **🔍 智能文档比对** - 快速比对两个文档的差异，生成高亮标注的比对报告
- **📄 智能文档抽取** - 基于规则引擎从文档中自动提取结构化信息  
- **📝 智能合同合成** - 基于模板自动生成合同文档，支持富文本和动态数据填充

### 技术特性

- **智能去水印** - GPU 加速的水印去除技术
- **OCR 识别** - 高精度文字识别和版面分析
- **规则引擎** - 灵活的信息抽取规则配置
- **可视化设计** - 直观的模板设计和数据填充界面
- **AI 辅助** - 智能化的文档处理和分析

## 🏗️ 技术架构

### 后端技术栈

- **框架**: Spring Boot 2.7.18
- **语言**: Java 11+
- **构建工具**: Maven 3.6+
- **核心依赖**:
  - Spring Web - REST API 支持
  - Lombok - 简化代码编写
  - Jackson - JSON 序列化/反序列化
  - Apache Commons IO - 文件操作工具

### 前端技术栈

- **框架**: Vue.js 3.3.4
- **构建工具**: Vite 4.4.9
- **UI 组件**: Element Plus 2.4.0
- **HTTP 客户端**: Axios 1.5.0
- **路由**: Vue Router 4.2.4
- **样式**: Sass 1.93.3

### 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端 Vue.js   │───▶│   后端 API      │───▶│  肇新组件库     │
│   (端口: 3000)  │    │   (端口: 8091)  │    │  (端口: 8080)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └─────────────▶│   文件存储      │◀─────────────┘
                        │   (./uploads)   │
                        └─────────────────┘
```

## 📦 依赖要求

### 开发环境

- **操作系统**: Windows 10+ / macOS / Linux
- **Java**: JDK 11 或更高版本
- **Node.js**: 16.0 或更高版本
- **Maven**: 3.6 或更高版本
- **Git**: 用于代码管理

### 生产环境

- **操作系统**: Linux (Ubuntu 18.04+, CentOS 7+)
- **内存**: ≥ 2GB RAM
- **磁盘空间**: ≥ 5GB
- **Docker**: ≥ 20.10 (用于容器化部署)
- **网络**: 开放端口 81, 8091

### 外部依赖

- **肇新组件库服务**: 需要部署并运行肇新合同组件库 API 服务
- **文档中心**: http://101.42.11.82/doc-center

### 🔗 肇新服务地址说明

本 Demo 需要连接肇新组件库后端服务，提供以下两个环境：

#### 演示环境（推荐新手）
- **服务地址**: `http://101.42.11.82:80`
- **特点**: 
  - ✅ 开箱即用，无需额外配置
  - ✅ 稳定的演示数据和模板
  - ⚠️ **只读环境**，数据不能修改
  - 🎯 适合快速体验和功能演示

#### 开发环境（推荐开发者）
- **服务地址**: `http://101.42.11.82:9000`
- **特点**:
  - ✅ 完整功能，支持数据修改
  - ✅ 可以创建和编辑模板
  - ✅ 支持自定义配置和测试
  - 🔧 适合深度开发和功能测试

#### 网络要求
- **双向互通**: Demo 服务器与肇新服务器需要双向网络互通
- **本地 IP**: `backend-url` 需要配置为本地对外 IP（如 `http://192.168.0.10:8091`）

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd ZhaoxinToolsDemo
```

### 2. 配置环境

#### 后端配置

编辑 `backend/src/main/resources/application.yml`:

```yaml
# 肇新 API 配置
zhaoxin:
  api:
    # 演示环境（只读，适合快速体验）
    base-url: http://101.42.11.82:80
    # 开发环境（可修改，适合深度开发）
    # base-url: http://101.42.11.82:9000
    timeout: 120000
    connect-timeout: 10000
  frontend:
    # 肇新前端服务地址（用于 iframe 嵌套）
    url: http://101.42.11.82:80
  demo:
    # ⚠️ 重要：这里需要配置本地对外 IP，确保肇新服务能够回调
    backend-url: http://192.168.0.10:8091  # 替换为您的实际 IP
  # 百度统计站点ID（可选）
  baidu-analytics: 
```

#### 🔧 配置说明

**关键配置项**：

1. **`base-url`** - 肇新后端服务地址
   - 演示环境：`http://101.42.11.82:80` （只读，稳定演示）
   - 开发环境：`http://101.42.11.82:9000` （可修改，完整功能）

2. **`backend-url`** - 本地 Demo 后端地址
   - ⚠️ **必须使用对外 IP**，不能使用 `localhost` 或 `127.0.0.1`
   - 肇新服务需要能够访问此地址进行回调
   - 示例：`http://192.168.0.10:8091`

3. **网络互通要求**：
   - Demo 服务器 → 肇新服务器：能够发起 API 请求
   - 肇新服务器 → Demo 服务器：能够进行结果回调

#### 前端配置

前端配置会自动从后端 API 加载，无需手动配置。

### 3. 开发环境启动

#### 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8091 启动

#### 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动

### 4. 访问应用

打开浏览器访问: http://localhost:3000

## 🔧 构建和部署

### 本地构建

使用提供的构建脚本进行一键构建:

```bash
# Windows 环境
.\build.bat

# 选择构建模式:
# [1] 完整构建 (清理+构建+打包)
# [2] 快速构建 (仅构建)
# [3] 清理缓存
```

构建完成后，产物位于:
- 后端 JAR: `backend/target/*.jar`
- 前端静态文件: `frontend/dist/`
- 完整部署包: `dist/`

### 生产环境部署

#### 方式一: 传统部署

1. **构建项目**
   ```bash
   .\build.bat  # 选择选项 1
   ```

2. **部署后端**
   ```bash
   java -jar backend/target/zhaoxin-tools-demo-backend-1.0.0.jar
   ```

3. **部署前端**
   将 `frontend/dist/` 目录部署到 Web 服务器 (如 Nginx)

#### 方式二: Docker 部署

使用预构建的 Linux 部署包:

```bash
# 1. 上传部署包到服务器
scp zhaoxin-demo-linux.zip user@server:/opt/

# 2. 解压并部署
cd /opt/
unzip zhaoxin-demo-linux.zip
cd zhaoxin-demo-linux/
chmod +x deploy-linux.sh
./deploy-linux.sh
```

部署完成后访问: http://server-ip:81

### 配置文件说明

#### 开发环境配置 (`application.yml`)
- 用于本地开发和测试
- 默认端口: 8091
- 日志级别: DEBUG

#### 生产环境配置 (`application-prod.yml`)
- 用于生产环境部署
- 优化的日志配置
- 性能调优参数

## 📚 API 文档

### 核心 API 端点

#### 文档比对 API
```
POST /api/compare/submit          # 提交比对任务
GET  /api/compare/task/{taskId}   # 查询任务状态
GET  /api/compare/result/{taskId} # 获取比对结果
DELETE /api/compare/task/{taskId} # 删除任务
```

#### 文档抽取 API
```
POST /api/rule-extract/extract/upload        # 上传文档并抽取
GET  /api/rule-extract/extract/status/{id}   # 查询抽取状态
GET  /api/rule-extract/extract/result/{id}   # 获取抽取结果
```

#### 合同合成 API
```
GET  /api/compose/templates       # 获取模板列表
POST /api/compose/generate        # 合成合同
GET  /api/compose/download/{id}   # 下载合同
```


## 🎨 功能特性

### 智能文档比对

- **智能去水印**: 水印识别和去除
- **差异高亮**: 直观的文档差异标注和展示
- **报告导出**: 支持 DOC, HTML 等格式的比对报告导出

### 智能文档抽取

- **规则引擎**: 灵活的信息抽取规则配置
- **模板管理**: 可视化的抽取模板设计和管理
- **OCR 识别**: 高精度的文字识别和版面分析
- **结构化输出**: JSON 格式的结构化数据输出
- **实时预览**: 抽取结果的实时预览和调试

### 智能合同合成

- **模板设计**: 可视化的合同模板设计器
- **动态填充**: 支持变量、表格、图片等动态内容
- **富文本编辑**: 完整的富文本编辑和格式化功能
- **版本管理**: 模板版本控制和历史记录
- **批量生成**: 支持批量数据的合同生成

### 开发流程

1. Fork 项目到个人仓库
2. 创建功能分支: `git checkout -b feature/new-feature`
3. 提交更改: `git commit -am 'Add new feature'`
4. 推送分支: `git push origin feature/new-feature`
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 技术支持

### 文档资源

- **肇新科技官网**: https://www.zhaoxinms.com
- **组件库文档**: http://101.42.11.82/doc-center

**让智能文档处理变得更简单！** 🚀
