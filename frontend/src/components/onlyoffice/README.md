# OnlyOffice编辑器组件

## 概述

这是一个基于Vue3 + Element Plus + OnlyOffice的文档编辑器组件，支持Word、Excel、PowerPoint等多种格式的在线编辑和预览。

## 功能特性

- ✅ **Vue3 Composition API**: 使用最新的Vue3语法，性能更优
- ✅ **Element Plus集成**: 美观的UI界面和丰富的交互组件  
- ✅ **多格式支持**: 支持doc/docx/xls/xlsx/ppt/pptx/pdf等格式
- ✅ **权限控制**: 支持只读、编辑、审阅等不同权限模式
- ✅ **实时状态**: 实时显示文档编辑状态和保存状态
- ✅ **工具栏**: 提供强制保存、添加水印、刷新等功能
- ✅ **状态栏**: 显示编辑模式、权限信息等状态
- ✅ **错误处理**: 完善的错误处理和用户提示

## 快速开始

### 1. 安装依赖

```bash
npm install element-plus @element-plus/icons-vue
```

### 2. 全局注册Element Plus（main.js）

```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

app.use(ElementPlus)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

### 3. 使用组件

```vue
<template>
  <OnlyOfficeEditor
    :file-id="fileId"
    :can-edit="true"
    :can-review="false"
    height="600px"
    @ready="handleEditorReady"
    @error="handleEditorError"
  />
</template>

<script setup>
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

const fileId = ref('123')

const handleEditorReady = () => {
  console.log('编辑器已就绪')
}

const handleEditorError = (error) => {
  console.error('编辑器错误:', error)
}
</script>
```

## 组件Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| fileId | String/Number | - | 文件ID（必填） |
| canEdit | Boolean | false | 是否可编辑 |
| canReview | Boolean | false | 是否可审阅 |
| height | String | 'calc(100vh - 200px)' | 编辑器高度 |
| width | String | '100%' | 编辑器宽度 |
| showToolbar | Boolean | true | 是否显示工具栏 |
| showStatus | Boolean | true | 是否显示状态栏 |
| watermarkText | String | '机密文档' | 水印文字 |

## 组件Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| ready | - | 编辑器就绪 |
| documentStateChange | event | 文档状态变化 |
| error | error | 编辑器错误 |
| save | event | 文档保存 |
| warning | event | 编辑器警告 |

## 组件方法

| 方法名 | 参数 | 说明 |
|--------|------|------|
| forceSave | - | 强制保存文档 |
| addWatermark | - | 添加水印 |
| refreshEditor | - | 刷新编辑器 |
| getFileInfo | - | 获取文件信息 |
| isReady | - | 是否就绪 |

## 使用示例

### 基础用法

```vue
<OnlyOfficeEditor
  file-id="123"
  :can-edit="true"
/>
```

### 完整配置

```vue
<OnlyOfficeEditor
  :file-id="selectedFileId"
  :can-edit="editMode"
  :can-review="reviewMode"
  height="800px"
  :show-toolbar="true"
  :show-status="true"
  watermark-text="公司机密"
  @ready="onEditorReady"
  @documentStateChange="onDocStateChange"
  @error="onEditorError"
  @save="onDocumentSave"
/>
```

### 获取组件实例

```vue
<template>
  <OnlyOfficeEditor ref="editorRef" file-id="123" />
  <el-button @click="saveDocument">保存</el-button>
</template>

<script setup>
const editorRef = ref(null)

const saveDocument = () => {
  if (editorRef.value?.isReady()) {
    editorRef.value.forceSave()
  }
}
</script>
```

## 后端API接口

### 1. 获取编辑器配置

```http
GET /api/onlyoffice/editor/config?fileId=123&canEdit=true&canReview=false
```

### 2. 获取服务器信息

```http
GET /api/onlyoffice/server/info
```

### 3. 文档保存回调

```http
POST /api/onlyoffice/callback/save
```

### 4. 文件下载

```http
GET /api/onlyoffice/callback/download/{fileId}
```

## 配置说明

### application.yml配置

```yaml
# OnlyOffice配置
onlyoffice:
  domain: localhost
  port: 80
  callback:
    url: http://localhost:8080/api/onlyoffice/callback
  secret: your-secret-key-here
  plugins: []
  logo: ""
  logoEmbedded: ""
  logoUrl: ""
  permissions:
    view:
      print: true
    edit:
      print: true
      download: true
      comment: true
      chat: true
      review: true
      fillForms: true
      modifyContentControl: true
      modifyFilter: true
```

## 演示页面

项目提供了完整的演示页面，可以通过以下方式访问：

1. 启动项目：`npm run dev`
2. 访问：`http://localhost:3000/onlyoffice`

演示页面包含：
- 文件选择器
- 权限设置
- 文件上传
- 实时状态显示
- 操作日志

## 注意事项

1. **OnlyOffice服务**: 需要部署OnlyOffice Document Server
2. **跨域配置**: 确保OnlyOffice服务器允许跨域访问
3. **文件格式**: 确保文件格式被OnlyOffice支持
4. **网络连接**: 客户端需要能访问OnlyOffice服务器
5. **浏览器兼容**: 建议使用现代浏览器（Chrome、Firefox、Edge等）

## 故障排除

### 1. 编辑器无法加载

- 检查OnlyOffice服务器是否运行
- 检查网络连接和跨域配置
- 查看浏览器控制台错误信息

### 2. 文档无法保存

- 检查回调URL是否正确配置
- 检查文件权限设置
- 查看后端日志

### 3. 界面显示异常

- 检查Element Plus是否正确安装
- 检查CSS样式是否冲突
- 确认Vue3版本兼容性

## 版本要求

- Vue 3.0+
- Element Plus 2.0+
- OnlyOffice Document Server 6.0+
- Node.js 16+

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本的文档编辑和预览功能
- 集成Element Plus UI组件
- 提供完整的演示页面