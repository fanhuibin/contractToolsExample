# 肇新工具集 Demo 项目 - 完整实现指南

## 📋 概述

本文档详细说明如何完整创建肇新工具集 Demo 项目，基于 ContractComparisonDemo 的架构，包含三个核心功能模块：
1. **智能文档抽取**（基于规则引擎）
2. **合同合成**
3. **智能文档解析**

## 🎯 核心特性

- **多功能模块**：集成三大智能文档处理功能
- **侧边栏导航**：清晰的功能模块切换
- **统一架构**：复用 ContractComparisonDemo 的成功模式
- **配置化设计**：易于部署和维护

## 📁 完整文件清单

### 后端文件（已创建）

✅ `backend/pom.xml`
✅ `backend/src/main/resources/application.yml`
✅ `backend/src/main/java/com/zhaoxin/tools/demo/DemoApplication.java`
✅ `backend/start.bat`

### 后端文件（待创建）

需要创建以下核心Java文件：

#### 配置类
- `config/WebConfig.java` - CORS和Web配置
- `config/RestTemplateConfig.java` - HTTP客户端配置

#### 模型类
- `model/response/ApiResponse.java` - 统一响应格式
- `model/exception/ApiException.java` - 自定义异常
- `model/request/ExtractRequest.java` - 抽取请求模型

#### 服务类
- `service/ZhaoxinApiClient.java` - SDK API客户端封装
- `service/FileStorageService.java` - 文件存储服务

#### 控制器
- `controller/HomeController.java` - 首页控制器
- `controller/FileController.java` - 文件上传控制器
- `controller/ExtractController.java` - 文档抽取控制器

### 前端文件

#### 核心配置
- `frontend/package.json` - 项目依赖配置
- `frontend/vite.config.js` - Vite配置
- `frontend/index.html` - HTML入口
- `frontend/src/config.js` - 应用配置

#### 应用入口
- `frontend/src/main.js` - Vue应用入口
- `frontend/src/App.vue` - 根组件

#### 路由
- `frontend/src/router/index.js` - 路由配置

#### 组件
- `frontend/src/components/Sidebar.vue` - 侧边栏导航
- `frontend/src/components/FileUpload.vue` - 文件上传组件

#### 页面
- `frontend/src/views/Extract.vue` - 智能文档抽取页面
- `frontend/src/views/Compose.vue` - 合同合成页面（占位）
- `frontend/src/views/Parse.vue` - 智能文档解析页面（占位）

#### API客户端
- `frontend/src/api/extract.js` - 文档抽取API
- `frontend/src/api/file.js` - 文件API

#### 工具
- `frontend/src/utils/request.js` - HTTP请求封装

## 🚀 快速创建步骤

### 步骤1：复用 ContractComparisonDemo 代码

可以直接复用以下文件（稍作修改）：

#### 从 ContractComparisonDemo/backend 复用：
```bash
# 配置类
backend/src/main/java/com/zhaoxin/demo/config/WebConfig.java
backend/src/main/java/com/zhaoxin/demo/config/RestTemplateConfig.java
backend/src/main/java/com/zhaoxin/demo/config/GlobalExceptionHandler.java

# 模型类
backend/src/main/java/com/zhaoxin/demo/model/response/ApiResponse.java
backend/src/main/java/com/zhaoxin/demo/model/exception/ApiException.java

# 文件控制器
backend/src/main/java/com/zhaoxin/demo/controller/FileUploadController.java
backend/src/main/java/com/zhaoxin/demo/controller/HomeController.java
```

**修改内容**：
- 包名：`com.zhaoxin.demo` → `com.zhaoxin.tools.demo`
- 端口：`8090` → `8091`

### 步骤2：创建新的业务代码

#### 后端核心服务类

**service/ZhaoxinApiClient.java** - 参考 CompareApiClient.java
```java
package com.zhaoxin.tools.demo.service;

@Service
@Slf4j
public class ZhaoxinApiClient {
    private final RestTemplate restTemplate;
    
    @Value("${zhaoxin.api.base-url}")
    private String baseUrl;
    
    // 文档抽取API
    public ApiResponse submitExtractTask(String fileUrl, String templateId) {
        String url = baseUrl + "/api/rule-extract/extract/upload";
        // ... 实现
    }
    
    public ApiResponse getExtractStatus(String taskId) {
        String url = baseUrl + "/api/rule-extract/extract/status/" + taskId;
        // ... 实现
    }
    
    public ApiResponse getExtractResult(String taskId) {
        String url = baseUrl + "/api/rule-extract/extract/result/" + taskId;
        // ... 实现
    }
    
    // 合同合成API（待实现）
    // 文档解析API（待实现）
}
```

**controller/ExtractController.java**
```java
package com.zhaoxin.tools.demo.controller;

@RestController
@RequestMapping("/api/extract")
@Slf4j
public class ExtractController {
    
    private final ZhaoxinApiClient apiClient;
    
    @PostMapping("/upload")
    public ApiResponse uploadAndExtract(@RequestParam String fileUrl,
                                       @RequestParam String templateId) {
        return apiClient.submitExtractTask(fileUrl, templateId);
    }
    
    @GetMapping("/status/{taskId}")
    public ApiResponse getStatus(@PathVariable String taskId) {
        return apiClient.getExtractStatus(taskId);
    }
    
    @GetMapping("/result/{taskId}")
    public ApiResponse getResult(@PathVariable String taskId) {
        return apiClient.getExtractResult(taskId);
    }
}
```

### 步骤3：创建前端项目

#### package.json
```json
{
  "name": "zhaoxin-tools-demo-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.3.4",
    "vue-router": "^4.2.4",
    "axios": "^1.5.0",
    "element-plus": "^2.4.0",
    "@element-plus/icons-vue": "^2.1.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.3.4",
    "vite": "^4.4.9"
  }
}
```

#### vite.config.js
```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3003,
    proxy: {
      '/api': {
        target: 'http://localhost:8091',
        changeOrigin: true
      }
    }
  }
})
```

#### App.vue（带侧边栏布局）
```vue
<template>
  <div class="app-container">
    <Sidebar />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/Sidebar.vue'
</script>

<style>
.app-container {
  display: flex;
  height: 100vh;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: #f5f5f5;
}
</style>
```

#### components/Sidebar.vue
```vue
<template>
  <div class="sidebar">
    <div class="logo">
      <h1>肇新工具集</h1>
      <p>Zhaoxin Tools</p>
    </div>
    
    <nav class="menu">
      <router-link to="/extract" class="menu-item">
        <i class="el-icon-document"></i>
        <span>智能文档抽取</span>
      </router-link>
      
      <router-link to="/compose" class="menu-item">
        <i class="el-icon-edit"></i>
        <span>合同合成</span>
      </router-link>
      
      <router-link to="/parse" class="menu-item">
        <i class="el-icon-magic-stick"></i>
        <span>智能文档解析</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.sidebar {
  width: 250px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.logo {
  padding: 30px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  color: white;
  text-decoration: none;
  transition: all 0.3s;
}

.menu-item:hover,
.menu-item.router-link-active {
  background: rgba(255,255,255,0.2);
}
</style>
```

#### views/Extract.vue
参考 ContractComparisonDemo/frontend-vue/src/views/Compare.vue，修改为：
- 单文件上传（不是双文件）
- 添加模板选择下拉框
- 显示抽取结果（字段列表+置信度）
- 展示图文对照（文档图片+标注框）

### 步骤4：创建文档

- `docs/快速开始.md` - 参考 ContractComparisonDemo
- `docs/智能文档抽取集成指南.md` - API对接说明

## 🔧 关键代码片段

### 文件上传（复用FileUploadController.java）

从 ContractComparisonDemo 直接复用，仅修改包名。

### API调用（新建ZhaoxinApiClient.java）

```java
public ApiResponse submitExtractTask(MultipartFile file, String templateId) {
    // 1. 保存文件
    String fileName = fileStorageService.storeFile(file);
    String fileUrl = "http://localhost:8091/api/files/download/" + fileName;
    
    // 2. 调用SDK API
    String url = baseUrl + "/api/rule-extract/extract/upload";
    
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addBinaryBody("file", file.getBytes());
    builder.addTextBody("templateId", templateId);
    
    // 3. 发送请求
    HttpEntity multipart = builder.build();
    // ... 
    
    return response;
}
```

### 前端轮询（参考Compare.vue）

```javascript
const pollTaskStatus = async (taskId) => {
  while (true) {
    const res = await axios.get(`/api/extract/status/${taskId}`)
    const status = res.data.data.status
    
    if (status === 'completed') {
      const result = await axios.get(`/api/extract/result/${taskId}`)
      displayResult(result.data.data)
      break
    }
    
    await new Promise(resolve => setTimeout(resolve, 3000))
  }
}
```

## 📝 测试清单

- [ ] 后端启动成功（8091端口）
- [ ] 前端启动成功（3003端口）
- [ ] 侧边栏导航正常
- [ ] 文件上传功能
- [ ] 文档抽取提交
- [ ] 任务状态轮询
- [ ] 抽取结果展示

## 🎨 UI设计要点

### 侧边栏样式
- 紫色渐变背景
- 清晰的图标
- 悬停/激活效果

### 主内容区
- 全屏宽度布局（参考最新的ContractComparisonDemo）
- 卡片式设计
- 宽松的间距

### 抽取结果展示
- 左侧：字段列表（字段名、值、置信度）
- 右侧：文档图片+标注框

## 📦 部署

### 开发环境
```bash
# 后端
cd backend
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

### 生产环境
```bash
# 后端
mvn clean package
java -jar target/zhaoxin-tools-demo-backend-1.0.0.jar

# 前端
npm run build
# 将 dist/ 目录部署到 nginx
```

## 🔗 参考资料

1. ContractComparisonDemo 项目结构
2. 智能文档抽取-API文档.md
3. Element Plus 文档：https://element-plus.org/
4. Vue Router 文档：https://router.vuejs.org/

---



**下一步行动**：
1. 按照本指南逐步创建文件
2. 先实现智能文档抽取模块
3. 再逐步添加合同合成和文档解析模块

