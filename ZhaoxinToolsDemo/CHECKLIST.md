# ✅ 部署检查清单

使用此清单确保项目正确部署。

---

## 📋 前置检查

- [ ] Java 17+ 已安装 (`java -version`)
- [ ] Maven 3.6+ 已安装 (`mvn -version`)
- [ ] Node.js 16+ 已安装 (`node -version`)
- [ ] 肇新SDK前端运行中 (http://localhost:3000)
- [ ] 肇新SDK后端运行中 (http://localhost:8080)

---

## 🔧 后端部署

### 步骤1: 编译项目
- [ ] 打开终端
- [ ] `cd ZhaoxinToolsDemo/backend`
- [ ] `mvn clean package -DskipTests`
- [ ] 看到 "BUILD SUCCESS"

### 步骤2: 启动后端
- [ ] `start.bat` 或 `java -jar target/zhaoxin-tools-demo-backend-1.0.0.jar`
- [ ] 看到 "肇新工具集 Demo 后端服务已启动"
- [ ] 端口8091未被占用

### 步骤3: 验证后端
- [ ] 访问 http://localhost:8091
- [ ] 看到 "Whitelabel Error Page"（正常）

---

## 🎨 前端部署

### 步骤1: 安装依赖（首次运行）
- [ ] 打开新终端
- [ ] `cd ZhaoxinToolsDemo/frontend`
- [ ] `npm install`
- [ ] 等待5-10分钟安装完成
- [ ] 无错误提示

### 步骤2: 启动前端
- [ ] `start.bat` 或 `npm run dev`
- [ ] 看到 "Local: http://localhost:3003"
- [ ] 端口3003未被占用

### 步骤3: 验证前端
- [ ] 访问 http://localhost:3003
- [ ] 看到智能文档抽取页面
- [ ] 顶部有Logo和标题
- [ ] 有文件上传区域
- [ ] 有模板选择框

---

## 🧪 功能测试

### 测试1: 文件上传
- [ ] 拖拽或点击上传PDF文件
- [ ] 文件信息正确显示
- [ ] 文件大小正确显示
- [ ] 可以移除文件

### 测试2: 模板选择
- [ ] 模板下拉框有选项
- [ ] 可以选择模板

### 测试3: 抽取提交
- [ ] 上传文件并选择模板
- [ ] "开始抽取"按钮可点击
- [ ] 点击后显示进度条
- [ ] 进度从0%增长到100%
- [ ] 显示状态文本（上传中、抽取中）

### 测试4: 结果查看
- [ ] 抽取完成后显示成功提示
- [ ] "查看结果"按钮可点击
- [ ] 点击后在新窗口打开
- [ ] iframe显示SDK的结果页面

### 测试5: 任务历史
- [ ] 任务历史表格显示任务
- [ ] 显示任务ID、文件名、状态
- [ ] 已完成任务有"查看结果"按钮
- [ ] 点击按钮可查看结果

### 测试6: 模板管理
- [ ] 点击"模板管理"按钮
- [ ] 在新窗口打开
- [ ] iframe显示SDK的模板管理页面

---

## 🐛 常见问题检查

### 问题：后端启动失败
- [ ] 检查端口8091是否被占用
- [ ] 检查Java版本是否≥17
- [ ] 检查pom.xml是否完整

### 问题：前端启动失败
- [ ] 检查端口3003是否被占用
- [ ] 检查node_modules是否完整
- [ ] 重新运行 `npm install`

### 问题：文件上传失败
- [ ] 后端是否运行（http://localhost:8091）
- [ ] SDK后端是否运行（http://localhost:8080）
- [ ] 查看浏览器Console错误
- [ ] 查看后端控制台日志

### 问题：iframe无法显示
- [ ] SDK前端是否运行（http://localhost:3000）
- [ ] 检查 `frontend/src/config.js` 配置
- [ ] 查看浏览器Console错误

---

## 🔍 配置检查

### 后端配置 (application.yml)
- [ ] `server.port: 8091`
- [ ] `zhaoxin.api.base-url: http://localhost:8080`

### 前端配置 (config.js)
- [ ] `frontendUrl: 'http://localhost:3000'`
- [ ] `apiBaseUrl: 'http://localhost:8080'`
- [ ] `demoBaseUrl: 'http://localhost:8091'`

### Vite配置 (vite.config.js)
- [ ] `server.port: 3003`
- [ ] `proxy.target: 'http://localhost:8091'`

---

## 📊 端口占用检查

确保以下端口可用：

| 端口 | 服务 | 状态 |
|------|------|------|
| 3000 | 肇新SDK前端 | 必须运行 ✅ |
| 3003 | Demo前端 | 本项目 🔵 |
| 8080 | 肇新SDK后端 | 必须运行 ✅ |
| 8091 | Demo后端 | 本项目 🔵 |

---

## ✅ 最终验证

全部通过才算部署成功：

- [ ] 后端运行无错误
- [ ] 前端运行无错误
- [ ] 可以访问 http://localhost:3003
- [ ] 可以上传文件
- [ ] 可以选择模板
- [ ] 可以提交抽取
- [ ] 可以查看进度
- [ ] 可以查看结果（iframe）
- [ ] 可以查看任务历史
- [ ] 可以打开模板管理（iframe）

---

## 🎉 部署完成！

如果所有检查项都通过，恭喜您成功部署了肇新智能工具集Demo！

**下一步**：
1. 测试各项功能
2. 根据需求扩展新功能
3. 参考 Extract.vue 添加其他模块

**需要帮助？**
- 查看 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- 查看 [START_HERE.md](START_HERE.md)

---

**创建时间**: 2025-01-29  
**版本**: 1.0.0

