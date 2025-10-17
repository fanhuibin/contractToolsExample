# Extract Rule - 智能文档抽取模块（重构版本）

## 🎯 概述

这是完全重新实现的智能文档抽取功能，不参考任何旧代码。使用正确的Vue 3模式避免循环更新问题。

## 📁 文件结构

```
rule-extract/
├── components/                      # 组件目录
│   ├── MatchModeConfig.vue         # 匹配模式配置组件
│   ├── FieldConfigForm.vue         # 字段配置表单组件
│   ├── FieldTestPanel.vue          # 单字段测试面板
│   └── BatchTestPanel.vue          # 批量测试面板
├── ExtractMain.vue                 # 主页面：文件上传和任务管理
├── TemplateList.vue                # 模板列表页面
├── TemplateDesigner.vue            # 模板设计页面
└── README.md                       # 本文档
```

## 🔑 核心技术要点

### 1. 避免循环更新的模式

**❌ 错误模式（会导致Maximum recursive updates）：**
```typescript
// 不要这样做
const localData = ref({ ...props.modelValue })
watch(() => props.modelValue, (newVal) => {
  localData.value = { ...newVal }
}, { deep: true })
watch(localData, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })
```

**✅ 正确模式1：computed + 事件**
```typescript
// MatchModeConfig.vue 使用此模式
const currentMode = computed(() => props.modelValue?.matchMode || 'single')
const handleModeChange = (mode: string) => {
  const newConfig = {
    ...props.modelValue,
    matchMode: mode,
    returnAll: mode === 'all'
  }
  emit('update:modelValue', newConfig)
}
```

**✅ 正确模式2：:model-value + @input/change**
```typescript
// FieldConfigForm.vue 使用此模式
<el-input 
  :model-value="fieldName" 
  @input="(val) => emitUpdate({ fieldName: val })"
/>

const emitUpdate = (updates: Partial<FieldData>) => {
  emit('update:modelValue', {
    ...props.modelValue,
    ...updates
  })
}
```

**✅ 正确模式3：普通变量 + getData()**
```typescript
// TemplateDesigner.vue 使用此模式
let templateName = ref('')
let fieldList = ref<any[]>([])

defineExpose({
  getData: () => ({
    templateName: templateName.value,
    fields: fieldList.value
  })
})
```

### 2. 数据流设计

```
用户输入
  ↓
@input/change 事件
  ↓
创建新对象（不修改原对象）
  ↓
emit('update:modelValue', newData)
  ↓
父组件接收更新
```

### 3. 关键原则

- ✅ 使用 `computed` 读取 props
- ✅ 使用 `@change/@input` 处理用户输入
- ✅ 每次更新创建新对象
- ✅ 不使用 `watch` 监听 props
- ✅ 不使用深度监听 `{ deep: true }`
- ✅ 不直接修改 props
- ✅ 使用 `defineExpose` 暴露方法

## 🔧 组件说明

### MatchModeConfig.vue

**功能**：配置匹配模式（指定序号/返回所有）

**Props**:
```typescript
interface Props {
  modelValue: {
    matchMode?: string       // 'single' | 'all'
    occurrence?: number      // 1-100
    returnAll?: boolean
  }
}
```

**Emits**: `update:modelValue`

**特点**：
- 使用 `computed` 读取值
- 使用事件处理更新
- 零 watch，零循环

### FieldConfigForm.vue

**功能**：配置字段信息和提取规则

**Props**:
```typescript
interface FieldData {
  fieldName: string
  fieldCode: string
  fieldType: string
  required: boolean
  ruleType: 'KEYWORD_ANCHOR' | 'CONTEXT_BOUNDARY' | 'REGEX_PATTERN' | 'TABLE_CELL'
  ruleConfig: any
}
```

**Expose**: `getData()` - 返回当前表单数据

**支持的规则类型**:
1. KEYWORD_ANCHOR - 关键词锚点
2. CONTEXT_BOUNDARY - 上下文边界
3. REGEX_PATTERN - 正则表达式
4. TABLE_CELL - 表格提取

**特点**：
- 所有输入使用 `:model-value` + `@input/change`
- 提供常用正则表达式快捷按钮
- 动态显示规则配置表单
- 暴露 `getData()` 方法供父组件获取数据

### FieldTestPanel.vue

**功能**：单字段测试面板

**Props**:
```typescript
interface Props {
  field: any           // 字段配置
  testResult: any      // 测试结果
}
```

**Emits**: `test(testText: string, debugMode: boolean)`

**特点**：
- 支持调试模式
- 显示测试结果（成功/失败）
- 支持表格数据展示
- 支持Markdown表格渲染

### BatchTestPanel.vue

**功能**：批量测试所有字段

**Props**:
```typescript
interface Props {
  template: any        // 模板数据（包含fields）
}
```

**特点**：
- 依次测试所有字段
- 显示统计信息（成功率等）
- 支持查看详情对话框

### TemplateList.vue

**功能**：模板列表管理

**功能点**：
- 模板列表展示
- 创建/编辑/删除模板
- 复制模板
- 启用/禁用模板
- 跳转到设计页面

**特点**：
- 使用 `onActivated` 自动刷新
- 表单验证
- 搜索过滤

### TemplateDesigner.vue

**功能**：模板设计（字段配置）

**功能点**：
- 字段列表展示
- 添加/编辑/删除字段
- 单字段测试
- 批量测试
- 保存模板

**特点**：
- 使用普通变量存储数据（非响应式）
- 编辑时深拷贝数据
- 保存时调用子组件 `getData()`
- 对话框 `destroy-on-close`

### ExtractMain.vue

**功能**：文件上传和提取任务管理

**功能点**：
- PDF文件上传
- 模板选择
- 开始提取
- 任务进度监控
- 任务历史
- 取消任务
- 查看结果

**特点**：
- 状态轮询（每2秒）
- 自动清理定时器
- 文件大小验证
- 进度展示

## 🚀 使用方法

### 访问路径

- 主页面：`/rule-extract`
- 模板管理：`/rule-extract/templates`
- 模板设计：`/rule-extract/template/:id`

### 开发流程

1. **创建模板**
   - 访问 `/rule-extract/templates`
   - 点击"新建模板"
   - 填写基本信息

2. **设计模板**
   - 点击"设计"按钮
   - 添加字段
   - 配置提取规则
   - 测试规则
   - 保存模板

3. **执行提取**
   - 访问 `/rule-extract`
   - 上传PDF文件
   - 选择模板
   - 点击"开始提取"
   - 等待完成
   - 查看结果

## 📊 数据模型

### 模板结构
```typescript
interface Template {
  id: string
  templateName: string
  templateCode: string
  description: string
  status: 'draft' | 'active' | 'inactive'
  version: string
  fields: Field[]
}
```

### 字段结构
```typescript
interface Field {
  id: string
  fieldName: string
  fieldCode: string
  fieldType: string
  required: boolean
  ruleType: RuleType
  ruleConfig: RuleConfig
}
```

## 🐛 调试建议

如果遇到循环更新错误：

1. 检查是否使用了 `watch` 监听 props
2. 检查是否直接修改了 props
3. 检查是否使用了深度监听 `{ deep: true }`
4. 检查是否使用了 `v-model` 绑定响应式对象
5. 改用 `:model-value` + `@input/change` 模式

## 📝 API 依赖

使用以下API：
- `@/api/rule-extract.ts` - 模板和提取相关API
- `@/api/rule-test.ts` - 规则测试API

## ✨ 特性

- ✅ 零循环更新错误
- ✅ 清晰的数据流
- ✅ 良好的用户体验
- ✅ 完整的功能覆盖
- ✅ 详细的调试信息
- ✅ 响应式布局

## 🔄 与旧版本的区别

| 特性 | 旧版本 (rule-extract) | 新版本 (extract-rule) |
|------|---------------------|----------------------|
| 响应式方式 | ref + watch | computed + 事件 |
| 数据绑定 | v-model | :model-value + @change |
| 数据同步 | watch 自动同步 | 事件手动同步 |
| 循环更新 | ❌ 有问题 | ✅ 无问题 |
| 代码复杂度 | 高 | 低 |
| 可维护性 | 低 | 高 |

## 📚 参考文档

- [Vue 3 组合式 API](https://cn.vuejs.org/api/composition-api-setup.html)
- [Element Plus 表单组件](https://element-plus.org/zh-CN/component/form.html)
- [FEATURE_SPECIFICATION.md](./FEATURE_SPECIFICATION.md) - 完整功能规格说明

## 🎓 学习要点

1. **避免响应式陷阱** - 理解 Vue 3 的响应式系统
2. **单向数据流** - 父到子通过 props，子到父通过事件
3. **不可变更新** - 每次创建新对象而非修改原对象
4. **明确的接口** - 使用 defineProps, defineEmits, defineExpose
5. **类型安全** - 使用 TypeScript 接口定义数据结构

---

**注意**: 此实现完全独立于旧的 `rule-extract` 模块，可以并存运行以便对比测试。

