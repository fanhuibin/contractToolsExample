# 🚀 从这里开始！

欢迎使用肇新智能工具集 Demo 项目！

## ✅ 项目状态：100% 完成

所有代码已完整创建，可以直接运行！

---

## 📋 快速启动（3步）

### 步骤1：启动后端
```bash
cd backend
start.bat
```
✅ 成功标志：看到 "肇新工具集 Demo 后端服务已启动"

### 步骤2：安装前端依赖（首次运行）
```bash
cd frontend
npm install
```
⏳ 等待 5-10 分钟安装完成

### 步骤3：启动前端
```bash
cd frontend
start.bat
```
✅ 成功标志：看到 "Local: http://localhost:3003"

### 访问应用
打开浏览器访问：**http://localhost:3003**

---

## 📚 重要文档索引

| 文档 | 说明 | 优先级 |
|------|------|--------|
| **DEPLOYMENT_GUIDE.md** | 完整部署指南（必读！） | ⭐⭐⭐ |
| README.md | 项目总体介绍 | ⭐⭐⭐ |
| IMPLEMENTATION_GUIDE.md | 详细实现指南 | ⭐⭐ |
| PROJECT_SUMMARY.md | 项目创建总结 | ⭐ |
| docs/项目架构说明.md | 技术架构文档 | ⭐⭐ |
| docs/快速开始.md | 快速开始指南 | ⭐⭐ |

---

## 🎯 核心功能

### 1. 智能文档抽取
- ✅ PDF文件上传
- ✅ 模板选择
- ✅ 实时进度显示
- ✅ 结果查看（iframe嵌套SDK页面）
- ✅ 任务历史管理

### 2. 模板管理
- ✅ 通过iframe嵌套SDK的模板管理页面
- ✅ 数据在SDK端存储

### 3. 待扩展功能
- ⏳ 合同合成（参考Extract.vue添加）
- ⏳ 智能文档解析（参考Extract.vue添加）

---

## 🔧 技术栈

### 后端
- Spring Boot 3.2.1
- RestTemplate
- Lombok

### 前端
- Vue 3
- Vite
- Axios

---

## 📁 项目结构

```
ZhaoxinToolsDemo/
├── backend/                     # 后端（Spring Boot）
│   ├── src/main/java/.../
│   │   ├── controller/          # ✅ ExtractController.java
│   │   ├── service/             # ✅ ZhaoxinApiClient.java
│   │   └── config/              # ✅ WebConfig, RestTemplateConfig
│   ├── pom.xml                  # ✅ Maven配置
│   └── start.bat                # ✅ 启动脚本
│
├── frontend/                    # 前端（Vue 3）
│   ├── src/
│   │   ├── views/               # ✅ 页面组件
│   │   │   ├── Extract.vue      # 智能文档抽取
│   │   │   ├── ExtractResult.vue # 结果页（iframe）
│   │   │   └── TemplateManage.vue # 模板管理（iframe）
│   │   ├── api/                 # ✅ API客户端
│   │   ├── utils/               # ✅ 工具函数
│   │   └── config.js            # ✅ 配置文件
│   ├── package.json             # ✅ npm配置
│   ├── vite.config.js           # ✅ Vite配置
│   └── start.bat                # ✅ 启动脚本
│
└── docs/                        # 文档目录
    ├── START_HERE.md            # 本文档
    ├── DEPLOYMENT_GUIDE.md      # ⭐ 部署指南（必读）
    ├── IMPLEMENTATION_GUIDE.md  # 实现指南
    └── ...
```

---

## 🐛 常见问题

### Q1: 后端启动失败，端口被占用？
```bash
# 修改 backend/src/main/resources/application.yml
server:
  port: 8092  # 改成其他端口
```

### Q2: 前端启动失败，依赖安装错误？
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Q3: 文件上传后报错？
检查肇新SDK是否运行：
- 后端：http://localhost:8080
- 前端：http://localhost:3000

### Q4: iframe无法显示结果？
检查 `frontend/src/config.js` 配置：
```javascript
frontendUrl: 'http://localhost:3000'  # SDK前端地址
```

---

## 📞 需要帮助？

1. 查看 **DEPLOYMENT_GUIDE.md** - 最详细的部署和问题排查指南
2. 查看 **README.md** - 项目总体说明
3. 查看控制台日志 - 后端和前端的错误信息

---

## 🎉 项目特色

- ✅ **完整代码**：前后端所有代码已完成
- ✅ **开箱即用**：按步骤启动即可使用
- ✅ **设计优雅**：参考ContractComparisonDemo成功模式
- ✅ **文档完善**：详细的部署和使用文档
- ✅ **iframe嵌套**：优雅复用SDK的UI界面
- ✅ **易于扩展**：清晰的代码结构，方便添加新功能

---

## 🔗 相关项目

- **ContractComparisonDemo** - 合同比对Demo（参考项目）
- **肇新SDK** - 底层智能文档处理服务

---

**准备好了吗？现在就开始吧！**

1. `cd backend && start.bat` - 启动后端
2. `cd frontend && npm install` - 安装依赖（首次）
3. `cd frontend && start.bat` - 启动前端
4. 访问 `http://localhost:3003` 🎉

**祝使用愉快！** 🚀

