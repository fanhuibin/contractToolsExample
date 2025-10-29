# 📚 文档中心使用指南

## 🚀 快速开始

### 访问方式

1. **通过菜单访问**
   - 点击顶部导航栏的"文档中心"按钮

2. **直接访问URL**
   ```
   http://localhost:3000/doc-center
   ```

3. **访问特定文档**
   ```
   http://localhost:3000/doc-center?doc=api-readme
   http://localhost:3000/doc-center?doc=api-auth
   http://localhost:3000/doc-center?doc=api-convert
   ```

---

## 📖 可用文档列表

### API 接口文档

| 文档 | doc参数 | 说明 |
|------|---------|------|
| API文档总览 | `api-readme` | 系统API总览和导航 |
| API认证说明 | `api-auth` | API Key认证机制 |
| 文档格式转换API | `api-convert` | 文档格式转换接口 |
| 智能文档比对API | `api-compare` | 文档比对接口 |
| 智能文档抽取API | `api-extract` | 信息抽取接口 |
| 智能文档解析API | `api-parse` | OCR解析接口 |
| 合同合成-UI版 | `api-compose-ui` | iframe集成方式 |
| 合同合成-API版 | `api-compose-api` | API调用方式 |
| 文档合成功能支持 | `api-compose-features` | 功能支持说明 |

### 其他文档

| 文档 | doc参数 | 说明 |
|------|---------|------|
| Postman Collection | `postman` | API测试集合 |

---

## 🎨 功能特色

### ✨ 核心功能

- ✅ **Markdown 实时渲染** - 专业的文档显示效果
- ✅ **代码高亮** - 深色主题代码块
- ✅ **美化表格** - 清晰的表格样式
- ✅ **全屏阅读** - 沉浸式阅读体验
- ✅ **复制源码** - 一键复制 Markdown
- ✅ **锚点跳转** - 文档内快速导航
- ✅ **响应式布局** - 适配各种屏幕

### 🎯 使用技巧

#### 1. 快速导航

使用左侧菜单快速切换文档：
```
API 接口文档
  ├─ 文档总览
  ├─ API认证说明
  ├─ 文档格式转换
  └─ ...
```

#### 2. 全屏阅读

点击右上角"全屏阅读"按钮：
- 进入全屏模式
- 按 ESC 退出全屏
- 文档居中显示，阅读体验更佳

#### 3. 复制内容

点击"复制Markdown"按钮：
- 复制原始 Markdown 源码
- 可粘贴到其他编辑器
- 可分享给其他人

#### 4. 文档内跳转

Markdown 文档中的锚点链接可以直接点击跳转：
```markdown
[跳转到某个章节](#section-id)
```

---

## 📦 文档更新

### 如何添加新文档？

1. **添加 Markdown 文件**
   ```bash
   # 将新文档放到 public/docs/api/ 目录
   frontend/public/docs/api/your-new-doc.md
   ```

2. **更新文档映射表**
   
   编辑 `frontend/src/views/docs/DocCenter.vue`：
   
   ```typescript
   const docMap: Record<string, { title: string; path: string }> = {
     // 添加新文档
     'your-doc-key': { 
       title: '你的文档标题', 
       path: '/docs/api/your-new-doc.md' 
     },
     // ...
   }
   ```

3. **添加菜单项**
   
   在同一文件中添加菜单项：
   
   ```vue
   <el-menu-item index="your-doc-key">
     <el-icon><YourIcon /></el-icon>
     <span>你的文档标题</span>
   </el-menu-item>
   ```

### 如何更新现有文档？

直接编辑 `frontend/public/docs/api/` 目录下的 Markdown 文件，刷新页面即可看到更新。

---

## 🛠️ 技术实现

### 核心技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.x | 前端框架 |
| Element Plus | 2.x | UI 组件库 |
| marked | latest | Markdown 渲染 |
| Vue Router | 4.x | 路由管理 |

### 关键文件

```
frontend/
├── public/
│   └── docs/
│       └── api/           # 文档存放目录
│           ├── *.md       # Markdown 文档
│           └── *.json     # JSON 文件
│
└── src/
    ├── views/
    │   └── docs/
    │       └── DocCenter.vue   # 文档中心组件
    ├── router/
    │   └── index.ts            # 路由配置
    └── layout/
        └── index.vue           # 布局菜单
```

---

## 🎯 最佳实践

### Markdown 编写建议

#### 1. 使用标准 Markdown 语法

```markdown
# 一级标题
## 二级标题
### 三级标题

**粗体** *斜体* `代码`

- 列表项1
- 列表项2

1. 有序列表1
2. 有序列表2

[链接](http://example.com)
![图片](image.png)
```

#### 2. 代码块使用

````markdown
```javascript
// JavaScript 代码
const hello = 'world'
```

```python
# Python 代码
print("Hello, World!")
```

```bash
# Shell 命令
npm install
```
````

#### 3. 表格使用

```markdown
| 列1 | 列2 | 列3 |
|-----|-----|-----|
| 数据1 | 数据2 | 数据3 |
| 数据4 | 数据5 | 数据6 |
```

#### 4. 引用块

```markdown
> 这是一个引用块
> 可以多行

> **提示**: 这是一个重要提示
```

#### 5. 锚点链接

```markdown
# 章节1 {#section-1}

跳转到 [章节1](#section-1)
```

---

## 🐛 常见问题

### Q1: 文档显示为空白？

**A**: 检查以下几点：
- 文档文件是否存在于 `public/docs/api/` 目录
- 文档路径是否正确
- 浏览器控制台是否有错误信息

### Q2: 代码块没有高亮？

**A**: 代码块已经有基础样式，如果需要语法高亮，可以集成 `highlight.js` 或 `prism.js`。

### Q3: 表格样式不美观？

**A**: 表格样式已经在 `.markdown-body` 中定义，如果需要调整，修改 `DocCenter.vue` 中的样式。

### Q4: 如何导出为 PDF？

**A**: 使用浏览器的打印功能（Ctrl+P）并选择"保存为 PDF"。

### Q5: 图片无法显示？

**A**: 
- 图片需要放在 `public` 目录下
- Markdown 中使用绝对路径：`![图片](/images/example.png)`
- 或使用完整 URL

---

## 📞 技术支持

如有问题，请联系：

- 📧 官方网站：[https://zhaoxinms.com](https://zhaoxinms.com)
- 📦 产品价格：[https://zhaoxinms.com/price](https://zhaoxinms.com/price)
- ☎️ 技术支持：18306806281

---

## 🎉 享受使用！

现在您可以在系统中直接查看和管理所有技术文档了！

**快捷访问**: [http://localhost:3000/doc-center](http://localhost:3000/doc-center)

