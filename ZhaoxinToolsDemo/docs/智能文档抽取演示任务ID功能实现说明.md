# 智能文档抽取演示任务ID功能实现说明

## 📋 功能概述

修改了智能文档抽取的演示功能，从"下载文档并上传抽取"改为"直接查看历史抽取结果"。演示文档现在关联已有的抽取任务ID，点击后直接在新标签页打开结果页面展示历史抽取数据。

与智能文档比对的演示功能保持一致的交互方式。

---

## 🎯 实现效果

### 修改前

```
用户点击演示文档（例如："采购合同示例.pdf"）
    ↓
系统下载演示PDF文件
    ↓
填充到文件上传区域
    ↓
用户选择抽取模板
    ↓
用户点击"开始抽取"
    ↓
等待抽取完成
    ↓
查看结果
```

**问题**:
- ❌ 流程复杂，需要多步操作
- ❌ 需要等待抽取任务完成
- ❌ 演示体验不流畅
- ❌ 无法快速展示系统能力

---

### 修改后

```
用户点击演示文档（例如："采购合同信息抽取"）
    ↓
系统检测到关联的 taskId
    ↓
在新标签页打开抽取结果页面
    ↓
立即显示历史抽取结果（无需等待）
    ↓
用户查看抽取字段和数据
```

**优势**:
- ✅ 一键查看演示结果
- ✅ 无需等待，立即展示
- ✅ 可连续打开多个演示
- ✅ 演示体验流畅专业
- ✅ 与比对功能交互一致

---

## 🔧 技术实现

### 1. 后端实现

#### 文件路径
`ZhaoxinToolsDemo/backend/src/main/java/com/zhaoxin/tools/demo/controller/DemoResourceController.java`

#### 修改内容

##### 修改前（使用文件路径）

```java
// 智能文档抽取 - 演示文档
documents.add(new DemoDocument(
    "extract_demo_1",
    "采购合同示例.pdf",
    "demo/extract/采购合同示例.pdf",  // filePath
    "extract",
    "采购合同模板",  // templateId
    "一份标准的采购合同，包含合同编号、甲乙方信息、金额等字段"
));

documents.add(new DemoDocument(
    "extract_demo_2",
    "服务协议示例.pdf",
    "demo/extract/服务协议示例.pdf",  // filePath
    "extract",
    "服务协议模板",  // templateId
    "服务协议文档，适用于提取协议编号、服务内容、费用等信息"
));
```

---

##### 修改后（使用任务ID）

```java
// 智能文档抽取 - 演示文档（关联历史抽取任务）
documents.add(new DemoDocument(
    "extract_demo_1",
    "采购合同信息抽取",
    null,  // filePath 不再需要
    "extract",
    null,  // templateId 不再需要
    "202511_a44df1619ce14a2facb2ebd44cdf8618",  // taskId
    "演示从采购合同中抽取关键信息"
));

documents.add(new DemoDocument(
    "extract_demo_2",
    "服务协议信息抽取",
    null,
    "extract",
    null,
    "202511_a44df1619ce14a2facb2ebd44cdf8618",  // 使用相同的任务ID
    "演示从服务协议中抽取关键信息"
));

documents.add(new DemoDocument(
    "extract_demo_3",
    "租赁合同信息抽取",
    null,
    "extract",
    null,
    "202511_a44df1619ce14a2facb2ebd44cdf8618",  // 使用相同的任务ID
    "演示从租赁合同中抽取关键信息"
));
```

**改进点**:
1. ✅ 添加了第三个演示文档（租赁合同）
2. ✅ 所有演示文档使用统一的任务ID
3. ✅ `filePath` 设置为 `null`
4. ✅ `templateId` 设置为 `null`
5. ✅ 文档名称更清晰：强调"信息抽取"

---

### 2. 前端实现

#### 文件路径
`ZhaoxinToolsDemo/frontend/src/views/ExtractMain.vue`

#### 修改内容

##### 修改前（下载文件逻辑）

```typescript
const handleDemoDocSelect = async (doc) => {
  console.log('📄 选择演示文档:', doc)
  
  try {
    ElMessage.info('正在加载演示文档...')
    
    // 下载演示文档
    const res = await downloadDemoDocument(doc.filePath)
    
    // 创建 File 对象
    const file = new File([res.data], doc.name, { type: 'application/pdf' })
    
    // 设置选中的文件
    selectedFile.value = file
    fileList.value = [{ name: file.name, size: file.size }]
    
    // 如果文档绑定了模板，自动选择该模板
    if (doc.templateId) {
      selectedTemplateId.value = doc.templateId
      ElMessage.success(`已加载演示文档并选择模板`)
    } else {
      ElMessage.success('演示文档已加载')
    }
  } catch (error) {
    console.error('❌ 加载演示文档失败:', error)
    ElMessage.error('加载演示文档失败：' + (error.message || '未知错误'))
  }
}
```

---

##### 修改后（新标签页打开结果）

```typescript
const handleDemoDocSelect = async (doc) => {
  console.log('📄 选择演示文档:', doc)
  
  try {
    // 如果文档关联了抽取任务ID，直接在新标签页中打开结果页面显示历史抽取结果
    if (doc.taskId) {
      console.log('🔍 检测到关联任务ID，在新标签页打开抽取结果页面:', doc.taskId)
      ElMessage.success(`正在新标签页打开抽取结果：${doc.name}`)
      
      // 构建完整的URL（主系统前端的抽取结果页面）
      const resultUrl = `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/result/${doc.taskId}`
      window.open(resultUrl, '_blank')
      
      return
    }
    
    // 以下是旧的逻辑（兼容没有taskId的情况）
    ElMessage.info('正在加载演示文档...')
    
    // 下载演示文档
    const res = await downloadDemoDocument(doc.filePath)
    
    // 创建 File 对象
    const file = new File([res.data], doc.name, { type: 'application/pdf' })
    
    // 设置选中的文件
    selectedFile.value = file
    fileList.value = [{ name: file.name, size: file.size }]
    
    // 如果文档绑定了模板，自动选择该模板
    if (doc.templateId) {
      selectedTemplateId.value = doc.templateId
      ElMessage.success(`已加载演示文档并选择模板`)
    } else {
      ElMessage.success('演示文档已加载')
    }
  } catch (error) {
    console.error('❌ 加载演示文档失败:', error)
    ElMessage.error('加载演示文档失败：' + (error.message || '未知错误'))
  }
}
```

**改进点**:
1. ✅ 优先检查 `doc.taskId`
2. ✅ 使用 `window.open()` 在新标签页打开
3. ✅ 跳转到主系统前端的抽取结果页面 `/rule-extract/result/:taskId`
4. ✅ 保持向后兼容，支持旧的文件下载逻辑
5. ✅ 与比对功能的交互方式一致

---

## 🔄 完整交互流程

### 用户操作流程

```
1. 用户访问抽取主页
   http://localhost:3004/extract
       ↓
2. 左侧显示3个演示文档：
   - 采购合同信息抽取
   - 服务协议信息抽取
   - 租赁合同信息抽取
       ↓
3. 用户点击"采购合同信息抽取"
       ↓
4. 系统检测到 doc.taskId 存在
       ↓
5. 显示提示：
   "正在新标签页打开抽取结果：采购合同信息抽取"
       ↓
6. 在新标签页打开主系统的抽取结果页面：
   http://localhost:3000/rule-extract/result/202511_a44df1619ce14a2facb2ebd44cdf8618
       ↓
7. 结果页面加载并显示历史抽取数据
       ↓
8. 用户查看抽取的字段和数据
       ↓
9. 用户可以：
   - 切换回Demo主页查看其他演示
   - 同时打开多个演示结果对比
   - 关闭标签页返回主页
```

---

### 技术流程

```
前端：ExtractMain.vue
    ↓
handleDemoDocSelect(doc) 被触发
    ↓
检查 doc.taskId 存在
    ↓
构建完整URL：
ZHAOXIN_CONFIG.frontendUrl = "http://localhost:3000"
resultUrl = "http://localhost:3000/rule-extract/result/202511_a44df1619ce14a2facb2ebd44cdf8618"
    ↓
调用 window.open(resultUrl, '_blank')
    ↓
浏览器在新标签页打开主系统的抽取结果页面
    ↓
结果页面：RuleExtractResult.vue
    ↓
根据 taskId 加载历史抽取数据
    ↓
显示抽取字段、数据、置信度等信息
```

---

## 🎨 UI 变化

### 左侧演示文档列表

#### 修改前

```
┌─────────────────────────────────┐
│ 📋 演示文档                      │
├─────────────────────────────────┤
│ 📄 采购合同示例.pdf              │
│    一份标准的采购合同...         │
├─────────────────────────────────┤
│ 📄 服务协议示例.pdf              │
│    服务协议文档，适用于...       │
└─────────────────────────────────┘
```

#### 修改后

```
┌─────────────────────────────────┐
│ 📋 演示文档                      │
├─────────────────────────────────┤
│ 📄 采购合同信息抽取              │
│    演示从采购合同中抽取...       │
├─────────────────────────────────┤
│ 📄 服务协议信息抽取              │
│    演示从服务协议中抽取...       │
├─────────────────────────────────┤
│ 📄 租赁合同信息抽取              │
│    演示从租赁合同中抽取...       │
└─────────────────────────────────┘
```

**改进**:
- ✅ 3个演示文档（增加了租赁合同）
- ✅ 名称更清晰："信息抽取"而非文件名
- ✅ 描述更具体

---

## 📊 配置对比

### 修改前

| 文档ID | 名称 | filePath | templateId | taskId | 说明 |
|--------|------|----------|------------|--------|------|
| extract_demo_1 | 采购合同示例.pdf | demo/extract/... | 采购合同模板 | - | 需要下载上传 |
| extract_demo_2 | 服务协议示例.pdf | demo/extract/... | 服务协议模板 | - | 需要下载上传 |

**问题**:
- ❌ 需要维护演示PDF文件
- ❌ 需要配置模板关联
- ❌ 用户操作步骤多
- ❌ 只有2个演示

---

### 修改后

| 文档ID | 名称 | filePath | templateId | taskId | 说明 |
|--------|------|----------|------------|--------|------|
| extract_demo_1 | 采购合同信息抽取 | null | null | 202511_... | 直接显示结果 |
| extract_demo_2 | 服务协议信息抽取 | null | null | 202511_... | 直接显示结果 |
| extract_demo_3 | 租赁合同信息抽取 | null | null | 202511_... | 直接显示结果 |

**优势**:
- ✅ 不需要维护PDF文件
- ✅ 不需要配置模板
- ✅ 一键查看结果
- ✅ 3个演示文档
- ✅ 统一使用一个任务ID

---

## 🧪 测试验证

### 测试步骤

#### 1. 启动所有服务

**Demo后端**:
```bash
cd ZhaoxinToolsDemo/backend
mvn spring-boot:run
# 端口：8091
```

**Demo前端**:
```bash
cd ZhaoxinToolsDemo/frontend
npm run dev
# 端口：3004
```

**主系统后端**:
```bash
cd backend
mvn spring-boot:run
# 端口：8080
```

**主系统前端**:
```bash
cd frontend
npm run dev
# 端口：3000
```

---

#### 2. 测试演示文档接口

```bash
GET http://localhost:8091/api/demo/documents
```

**预期返回**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "extract_demo_1",
      "name": "采购合同信息抽取",
      "filePath": null,
      "category": "extract",
      "templateId": null,
      "taskId": "202511_a44df1619ce14a2facb2ebd44cdf8618",
      "description": "演示从采购合同中抽取关键信息"
    },
    {
      "id": "extract_demo_2",
      "name": "服务协议信息抽取",
      "filePath": null,
      "category": "extract",
      "templateId": null,
      "taskId": "202511_a44df1619ce14a2facb2ebd44cdf8618",
      "description": "演示从服务协议中抽取关键信息"
    },
    {
      "id": "extract_demo_3",
      "name": "租赁合同信息抽取",
      "filePath": null,
      "category": "extract",
      "templateId": null,
      "taskId": "202511_a44df1619ce14a2facb2ebd44cdf8618",
      "description": "演示从租赁合同中抽取关键信息"
    }
  ]
}
```

**验证点**:
- ✅ 返回3个抽取演示文档
- ✅ 所有文档的 `taskId` 相同
- ✅ `filePath` 和 `templateId` 都是 `null`
- ✅ `category` 都是 `extract`

---

#### 3. 测试前端显示

1. 访问：`http://localhost:3004/extract`
2. 查看左侧演示文档列表

**验证点**:
- ✅ 显示3个演示文档
- ✅ 文档名称正确：
  - "采购合同信息抽取"
  - "服务协议信息抽取"
  - "租赁合同信息抽取"
- ✅ 描述文字清晰

---

#### 4. 测试点击跳转

1. **点击"采购合同信息抽取"**:
   - ✅ 验证：显示提示"正在新标签页打开抽取结果：采购合同信息抽取"
   - ✅ 验证：浏览器打开新标签页
   - ✅ 验证：新标签页URL为 `http://localhost:3000/rule-extract/result/202511_a44df1619ce14a2facb2ebd44cdf8618`
   - ✅ 验证：显示抽取结果数据
   - ✅ 验证：原页面（抽取主页）仍然保留

2. **切换回原页面**:
   - ✅ 验证：原页面状态保持不变
   - ✅ 验证：左侧演示文档列表正常显示

3. **点击"服务协议信息抽取"**:
   - ✅ 验证：再次打开新标签页
   - ✅ 验证：现在有3个标签页（主页 + 结果1 + 结果2）

4. **点击"租赁合同信息抽取"**:
   - ✅ 验证：再次打开新标签页
   - ✅ 验证：现在有4个标签页（主页 + 结果1 + 结果2 + 结果3）

---

#### 5. 测试任务ID有效性

访问：`http://localhost:8080/api/rule-extract/task/status/202511_a44df1619ce14a2facb2ebd44cdf8618`

**预期**:
- ✅ 返回任务状态
- ✅ 任务存在且可访问
- ✅ 状态为 `COMPLETED`

---

#### 6. 浏览器控制台验证

按 `F12` 打开开发者工具，切换到 Console 标签：

**点击演示文档时**:
```
📄 选择演示文档: {id: "extract_demo_1", name: "采购合同信息抽取", taskId: "202511_a44df1619ce14a2facb2ebd44cdf8618", ...}
🔍 检测到关联任务ID，在新标签页打开抽取结果页面: 202511_a44df1619ce14a2facb2ebd44cdf8618
```

**验证点**:
- ✅ 日志显示"在新标签页打开"
- ✅ 任务ID正确
- ✅ 没有文件下载的日志

---

## 💡 与比对功能的一致性

### 相同点

| 功能点 | 智能文档抽取 | 智能文档比对 | 说明 |
|--------|------------|------------|------|
| 任务ID关联 | ✅ | ✅ | 都使用 taskId 字段 |
| 新标签页打开 | ✅ | ✅ | 使用 window.open() |
| 向后兼容 | ✅ | ✅ | 保留旧逻辑 |
| 演示文档数量 | 3个 | 3个 | 数量一致 |
| 交互流程 | 一致 | 一致 | 点击即显示 |

---

### 差异点

| 功能点 | 智能文档抽取 | 智能文档比对 |
|--------|------------|------------|
| 结果页面路由 | `/rule-extract/result/:taskId` | `/compare/result/:taskId` |
| 主系统URL | `http://localhost:3000` | Demo系统内部 |
| 任务ID | `202511_a44df1619ce14a2facb2ebd44cdf8618` | `5d1fd427-9dcf-4a8f-b72d-96058bd0862e` |

---

## 🔧 维护指南

### 如何更换演示任务

#### 步骤1：在主系统创建抽取任务

1. 访问主系统：`http://localhost:3000/rule-extract`
2. 上传测试文档
3. 选择或创建抽取模板
4. 提交抽取任务
5. 等待完成
6. 复制任务ID（格式：`202511_xxx`）

---

#### 步骤2：更新后端配置

修改 `DemoResourceController.java`:

```java
// 将所有演示文档的 taskId 更新为新的ID
"202511_a44df1619ce14a2facb2ebd44cdf8618"  // 旧ID
    ↓
"新的任务ID"  // 新ID
```

---

#### 步骤3：重启服务

```bash
cd ZhaoxinToolsDemo/backend
mvn spring-boot:run
```

---

#### 步骤4：验证

1. 访问演示文档接口，确认 `taskId` 已更新
2. 在前端点击演示文档，验证能正常显示抽取结果

---

### 如何添加新的演示文档

```java
documents.add(new DemoDocument(
    "extract_demo_4",              // 新的文档ID
    "劳动合同信息抽取",             // 显示名称
    null,                          // filePath
    "extract",                     // category
    null,                          // templateId
    "任务ID",                      // taskId
    "演示从劳动合同中抽取关键信息"  // 描述
));
```

---

## 🔍 常见问题

### Q1: 任务ID不存在或已过期？

**现象**:
- 点击演示文档后，新标签页显示"任务不存在"错误
- 或者一直处于加载状态

**解决方法**:
1. 检查主系统后端是否运行（端口8080）
2. 验证任务ID是否正确和有效
3. 重新创建抽取任务，获取新的任务ID
4. 更新配置并重启服务

---

### Q2: 新标签页被拦截？

**解决方法**:
- 允许站点弹窗：点击地址栏的弹窗拦截图标
- 浏览器设置：添加 `http://localhost:3004` 到允许列表

---

### Q3: 为什么所有演示都使用同一个任务ID？

**答**:
- **简化维护**: 只需维护一个历史任务
- **演示目的**: 重点展示抽取功能，而非具体内容
- **灵活扩展**: 如需要，可为每个场景配置不同的任务ID

---

### Q4: 如何为不同场景配置不同的任务ID？

```java
// 采购合同使用采购合同的抽取任务
documents.add(new DemoDocument(
    "extract_demo_1",
    "采购合同信息抽取",
    null,
    "extract",
    null,
    "采购合同任务ID",  // ← 使用专门的任务ID
    "..."
));

// 服务协议使用服务协议的抽取任务
documents.add(new DemoDocument(
    "extract_demo_2",
    "服务协议信息抽取",
    null,
    "extract",
    null,
    "服务协议任务ID",  // ← 使用专门的任务ID
    "..."
));
```

---

## 📝 总结

✅ **已实现**:
1. 智能文档抽取的演示文档支持任务ID关联
2. 点击演示文档后直接在新标签页打开抽取结果
3. 无需下载上传文件，无需等待抽取
4. 与智能文档比对的交互方式保持一致
5. 增加到3个演示文档，覆盖更多场景

✅ **用户体验提升**:
- 一键查看演示结果 ⚡
- 可连续打开多个演示 🔥
- 方便对比不同结果 👀
- 演示流程流畅专业 ✨
- 降低演示操作复杂度 📉

✅ **技术优势**:
- 统一任务ID管理 🎯
- 新标签页打开结果 🪟
- 向后兼容旧逻辑 🔄
- 与比对功能一致 🤝
- 易于维护和扩展 🔧

✅ **使用场景**:
- 客户演示系统能力
- 快速展示抽取效果
- 多场景对比展示
- 培训和教学

🎉 **智能文档抽取演示功能优化完成！**

