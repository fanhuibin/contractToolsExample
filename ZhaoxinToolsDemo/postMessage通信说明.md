# postMessage 通信说明

## 📡 通信机制概述

ZhaoxinToolsDemo 与肇新 SDK 前端之间通过 **postMessage API** 进行跨窗口通信，实现 iframe 嵌入页面的返回按钮正确导航。

---

## 🎯 使用场景

当用户在 Demo 项目中通过 iframe 查看 SDK 页面时（如抽取结果、模板管理、比对结果），点击 SDK 页面的"返回"按钮应该返回到 Demo 项目的首页，而不是 SDK 的上一个页面。

### 应用页面

#### ZhaoxinToolsDemo

| 页面 | 路由 | 说明 |
|------|------|------|
| **抽取结果** | `/extract/result/:taskId` | 嵌入 SDK 的抽取结果展示页面 |
| **模板管理** | `/template-manage` | 嵌入 SDK 的模板管理页面 |

#### ContractComparisonDemo

| 页面 | 路由 | 说明 |
|------|------|------|
| **比对结果** | `/result/:taskId` | 嵌入 SDK 的比对结果展示页面 |

---

## 📝 通信协议

### 消息格式

```typescript
interface NavigationMessage {
  type: 'NAVIGATE_BACK'      // 消息类型
  source: 'zhaoxin-sdk'       // 消息来源标识
  payload?: {                 // 可选的附加数据
    from: string              // 来源页面标识
  }
}
```

### 消息示例

```javascript
// 从抽取结果页面返回
{
  type: 'NAVIGATE_BACK',
  source: 'zhaoxin-sdk',
  payload: { from: 'rule-extract-result' }
}

// 从模板列表页面返回
{
  type: 'NAVIGATE_BACK',
  source: 'zhaoxin-sdk',
  payload: { from: 'template-list' }
}

// 从比对结果页面返回
{
  type: 'NAVIGATE_BACK',
  source: 'zhaoxin-sdk',
  payload: { from: 'gpu-ocr-compare-result' }
}
```

---

## 🔧 实现细节

### SDK 前端（iframe 子页面）

SDK 前端通过检测 `embed=true` URL 参数判断是否运行在嵌入模式：

#### RuleExtractResult.vue

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 检测嵌入模式
const isEmbedMode = computed(() => route.query.embed === 'true')

const handleBack = () => {
  if (isEmbedMode.value) {
    // 嵌入模式：发送消息到父页面
    console.log('🔙 [嵌入模式] 发送返回消息到父页面')
    window.parent.postMessage({
      type: 'NAVIGATE_BACK',
      source: 'zhaoxin-sdk',
      payload: { from: 'rule-extract-result' }
    }, '*')
  } else {
    // 独立模式：使用路由返回
    console.log('🔙 [独立模式] 使用路由返回')
    router.push('/rule-extract')
  }
}
</script>
```

#### TemplateList.vue

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 检测嵌入模式
const isEmbedMode = computed(() => route.query.embed === 'true')

const handleBack = () => {
  if (isEmbedMode.value) {
    // 嵌入模式：发送消息到父页面
    console.log('🔙 [嵌入模式] 发送返回消息到父页面')
    window.parent.postMessage({
      type: 'NAVIGATE_BACK',
      source: 'zhaoxin-sdk',
      payload: { from: 'template-list' }
    }, '*')
  } else {
    // 独立模式：使用路由返回
    console.log('🔙 [独立模式] 使用路由返回')
    router.push('/rule-extract')
  }
}
</script>
```

#### GPUOCRCanvasCompareResult.vue

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 检测嵌入模式
const isEmbedMode = computed(() => route.query.embed === 'true')

const goBack = () => {
  if (isEmbedMode.value) {
    // 嵌入模式：发送消息到父页面
    console.log('🔙 [嵌入模式] 发送返回消息到父页面')
    window.parent.postMessage({
      type: 'NAVIGATE_BACK',
      source: 'zhaoxin-sdk',
      payload: { from: 'gpu-ocr-compare-result' }
    }, '*')
  } else {
    // 独立模式：使用路由返回
    console.log('🔙 [独立模式] 使用路由返回')
    router.push({ name: 'GPUOCRCompare' }).catch(() => {})
  }
}
</script>
```

### Demo 前端（父页面）

Demo 前端在组件挂载时添加 message 事件监听器：

#### ZhaoxinToolsDemo/ExtractResult.vue

```vue
<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ZHAOXIN_CONFIG } from '@/config'

const route = useRoute()
const router = useRouter()

// postMessage 消息处理
const handleMessage = (event) => {
  // 验证来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
    return
  }
  
  // 处理导航消息
  if (event.data?.type === 'NAVIGATE_BACK' && 
      event.data?.source === 'zhaoxin-sdk') {
    console.log('✅ 收到返回消息，导航到首页')
    router.push('/')
  }
}

// 添加和移除事件监听器
onMounted(() => {
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>
```

#### ZhaoxinToolsDemo/TemplateManage.vue

```vue
<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ZHAOXIN_CONFIG } from '@/config'

const router = useRouter()

// postMessage 消息处理
const handleMessage = (event) => {
  // 验证来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
    return
  }
  
  // 处理导航消息
  if (event.data?.type === 'NAVIGATE_BACK' && 
      event.data?.source === 'zhaoxin-sdk') {
    console.log('✅ 收到返回消息，导航到首页')
    router.push('/')
  }
}

// 添加和移除事件监听器
onMounted(() => {
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>
```

#### ContractComparisonDemo/Result.vue

```vue
<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ZHAOXIN_CONFIG } from '@/config'

const route = useRoute()
const router = useRouter()

// postMessage 消息处理
const handleMessage = (event) => {
  // 验证来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
    return
  }
  
  // 处理导航消息
  if (event.data?.type === 'NAVIGATE_BACK' && 
      event.data?.source === 'zhaoxin-sdk') {
    console.log('✅ 收到返回消息，导航到首页')
    router.push('/')
  }
}

// 添加和移除事件监听器
onMounted(() => {
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>
```

---

## 🔒 安全性

### 1. 来源验证

Demo 前端通过验证 `event.origin` 确保消息来自可信的 SDK 前端：

```javascript
if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
  console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
  return
}
```

### 2. 消息格式验证

验证消息包含正确的 `type` 和 `source` 字段：

```javascript
if (event.data?.type === 'NAVIGATE_BACK' && 
    event.data?.source === 'zhaoxin-sdk') {
  // 处理消息
}
```

### 3. 双重验证

同时验证 `event.origin` 和消息内容，确保安全性：

- **event.origin**: 验证消息来源域名
- **event.data.source**: 验证消息发送者标识

---

## 🚀 工作流程

### 1. 用户操作流程

```
用户在 Demo 中点击"查看结果"
  ↓
Demo 打开包含 iframe 的页面 (ExtractResult.vue)
  ↓
iframe 加载 SDK 前端页面 (带 ?embed=true 参数)
  ↓
用户查看结果后点击"返回"按钮
  ↓
SDK 检测到嵌入模式，发送 postMessage
  ↓
Demo 接收消息，验证来源
  ↓
Demo 执行导航，跳转到首页 (/)
```

### 2. 技术流程图

```
┌─────────────────────────────────────────────┐
│   Demo 前端 (父页面)                         │
│   http://localhost:3003                     │
│                                             │
│   1. 添加 message 事件监听器                 │
│   2. 验证消息来源和格式                      │
│   3. 执行导航操作                            │
└──────────────┬──────────────────────────────┘
               │
               │ postMessage 通信
               │
┌──────────────▼──────────────────────────────┐
│   SDK 前端 (iframe 子页面)                   │
│   http://localhost:3000/rule-extract/result/│
│   xxx?embed=true                            │
│                                             │
│   1. 检测 embed=true 参数                   │
│   2. 返回按钮点击时发送 postMessage          │
│   3. 目标: window.parent                    │
└─────────────────────────────────────────────┘
```

---

## 📊 配置说明

### config.js

```javascript
export const ZHAOXIN_CONFIG = {
  // SDK 前端地址 (用于 iframe src 和消息来源验证)
  frontendUrl: 'http://localhost:3000',
  
  // SDK 后端 API 地址
  apiBaseUrl: 'http://localhost:8080',
  
  // Demo 后端地址
  demoBaseUrl: 'http://localhost:8091'
}
```

### 重要提示

- `frontendUrl` 必须与实际的 SDK 前端地址匹配
- 用于构建 iframe URL 和验证消息来源
- 生产环境需要更新为实际域名

---

## 🧪 测试指南

### 测试场景

#### 场景 1: SDK 独立使用（非嵌入模式）

**步骤**：
1. 直接访问 SDK 前端：`http://localhost:3000/rule-extract/result/xxx`
2. 点击"返回"按钮

**预期结果**：
- ✅ 控制台输出：`🔙 [独立模式] 使用路由返回`
- ✅ 页面通过路由返回到 `/rule-extract`
- ✅ 不发送 postMessage

#### 场景 2: Demo 嵌入模式 - 抽取结果

**步骤**：
1. 访问 Demo：`http://localhost:3003/extract/result/xxx`
2. 观察 iframe 加载 SDK 页面（带 `?embed=true`）
3. 点击 iframe 中的"返回"按钮

**预期结果**：
- ✅ SDK 控制台输出：`🔙 [嵌入模式] 发送返回消息到父页面`
- ✅ Demo 控制台输出：`✅ 收到返回消息，导航到首页`
- ✅ 页面导航到 Demo 首页 `/`

#### 场景 3: Demo 嵌入模式 - 模板管理

**步骤**：
1. 访问 Demo：`http://localhost:3003/template-manage`
2. 观察 iframe 加载 SDK 模板管理页面（带 `?embed=true`）
3. 点击 iframe 中的"返回"按钮

**预期结果**：
- ✅ SDK 控制台输出：`🔙 [嵌入模式] 发送返回消息到父页面`
- ✅ Demo 控制台输出：`✅ 收到返回消息，导航到首页`
- ✅ 页面导航到 Demo 首页 `/`

#### 场景 4: 安全性测试 - 无效来源

**步骤**：
1. 在 Demo 页面打开浏览器控制台
2. 手动发送伪造消息：
   ```javascript
   window.postMessage({
     type: 'NAVIGATE_BACK',
     source: 'zhaoxin-sdk'
   }, '*')
   ```

**预期结果**：
- ✅ 控制台输出：`⚠️ 忽略来自未知来源的消息: ...`
- ✅ 页面不发生导航
- ✅ 消息被忽略

---

## 🐛 故障排查

### 问题 1: 点击返回按钮没有反应

**可能原因**：
- iframe URL 中缺少 `?embed=true` 参数
- SDK 前端未正确检测嵌入模式

**解决方法**：
1. 检查 iframe URL：`console.log(iframeUrl.value)`
2. 确认 URL 包含 `?embed=true`
3. 在 SDK 页面控制台检查：`console.log(route.query.embed)`

### 问题 2: 消息被忽略（origin 不匹配）

**可能原因**：
- `ZHAOXIN_CONFIG.frontendUrl` 配置错误
- SDK 前端运行在不同的端口

**解决方法**：
1. 检查配置：`console.log(ZHAOXIN_CONFIG.frontendUrl)`
2. 检查实际 origin：`console.log(event.origin)`
3. 确保两者匹配：
   ```javascript
   // config.js
   frontendUrl: 'http://localhost:3000'  // 必须与实际端口一致
   ```

### 问题 3: 监听器未添加或泄漏

**可能原因**：
- 组件未正确挂载/卸载
- 监听器重复添加

**解决方法**：
1. 检查控制台日志：
   - 挂载时：`📡 已添加 postMessage 监听器`
   - 卸载时：`🔌 已移除 postMessage 监听器`
2. 确保使用 `onMounted` 和 `onUnmounted`
3. 使用同一个函数引用添加和移除监听器

### 问题 4: 在生产环境中不工作

**可能原因**：
- 配置文件仍使用 localhost

**解决方法**：
更新生产环境配置：
```javascript
// production config.js
export const ZHAOXIN_CONFIG = {
  frontendUrl: 'https://sdk.example.com',
  apiBaseUrl: 'https://api.example.com',
  demoBaseUrl: 'https://demo.example.com'
}
```

---

## 📋 日志说明

### SDK 前端日志

| 日志 | 含义 |
|------|------|
| `🔙 [嵌入模式] 发送返回消息到父页面` | 嵌入模式下，发送postMessage给父页面 |
| `🔙 [独立模式] 使用路由返回` | 独立模式下，使用Vue Router返回 |

### Demo 前端日志

| 日志 | 含义 |
|------|------|
| `📡 已添加 postMessage 监听器` | 组件挂载，添加事件监听器 |
| `🔌 已移除 postMessage 监听器` | 组件卸载，移除事件监听器 |
| `✅ 收到返回消息，导航到首页` | 接收到有效消息，执行导航 |
| `⚠️ 忽略来自未知来源的消息: ...` | 消息来源验证失败，忽略消息 |

---

## 🔄 扩展性

### 支持更多消息类型

可以扩展消息协议，支持更多操作：

```typescript
interface ZhaoxinMessage {
  type: 'NAVIGATE_BACK' | 'NAVIGATE_TO' | 'REFRESH' | 'UPDATE_STATE'
  source: 'zhaoxin-sdk'
  payload?: {
    from?: string
    to?: string
    data?: any
  }
}
```

示例：导航到指定路由

```javascript
// SDK 发送
window.parent.postMessage({
  type: 'NAVIGATE_TO',
  source: 'zhaoxin-sdk',
  payload: { to: '/extract/new' }
}, '*')

// Demo 处理
if (event.data?.type === 'NAVIGATE_TO') {
  const targetPath = event.data.payload?.to
  if (targetPath) {
    router.push(targetPath)
  }
}
```

---

## 📚 参考资料

- [MDN: Window.postMessage()](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage)
- [Vue 3: Lifecycle Hooks](https://vuejs.org/guide/essentials/lifecycle.html)
- [Vue Router: Programmatic Navigation](https://router.vuejs.org/guide/essentials/navigation.html)

---

**文档版本**: 1.0  
**更新时间**: 2025-01-29  
**适用项目**: ZhaoxinToolsDemo + 肇新 SDK 前端

