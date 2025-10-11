# 终极修复方案 - 深克隆 + 单向标志

## 🎯 最终有效的解决方案

### 问题分析
- **computed 直接返回 props 引用** → 修改对象属性会影响 props → 循环更新
- **双向 watch** → 互相触发 → 循环更新  
- **自动清空选择** → 触发 change 事件 → 递归调用

### 最终方案：深克隆 + 单向标志

```typescript
// 1. 创建本地副本（深克隆，完全独立）
const localField = ref<any>({})

// 2. 单向标志（只防止从 props 同步时触发 emit）
let isSyncingFromProps = false

// 3. 从 props 同步到本地
const syncFromProps = () => {
  isSyncingFromProps = true
  localField.value = JSON.parse(JSON.stringify(props.modelValue))
  setTimeout(() => {
    isSyncingFromProps = false
  }, 0)
}

// 4. 同步到父组件（检查标志）
const syncToParent = () => {
  if (!isSyncingFromProps) {
    emit('update:modelValue', JSON.parse(JSON.stringify(localField.value)))
  }
}

// 5. Watch 自动同步
watch(localField, () => {
  syncToParent()
}, { deep: true })

watch(() => props.modelValue, () => {
  syncFromProps()
}, { deep: true })
```

## 🔑 关键点

### 1. 深克隆避免引用共享
```typescript
// ✅ 深克隆：完全独立的副本
localField.value = JSON.parse(JSON.stringify(props.modelValue))

// ❌ 浅拷贝：仍然共享嵌套对象
localField.value = { ...props.modelValue }

// ❌ 直接引用：修改会影响 props
localField.value = props.modelValue
```

### 2. 单向标志避免循环
```typescript
// 数据流：
// 外部变化 → syncFromProps() 设置标志 → localField 变化
// → watch 触发 → syncToParent() 检查标志 → 不 emit（避免循环）

// 内部变化 → localField 变化 → watch 触发
// → syncToParent() 标志为 false → emit 到父组件
```

### 3. setTimeout(0) 确保异步重置
```typescript
// 使用 setTimeout(0) 而不是 nextTick
// 因为 watch 可能在同一个 tick 内触发多次
setTimeout(() => {
  isSyncingFromProps = false
}, 0)
```

## 📊 数据流示意图

```
外部变化流：
props.modelValue 改变
  ↓
watch(() => props.modelValue) 触发
  ↓
syncFromProps() 
  ├─ isSyncingFromProps = true
  ├─ localField.value = 深克隆(props)
  └─ setTimeout(() => isSyncingFromProps = false, 0)
  ↓
watch(localField) 触发
  ↓
syncToParent()
  ├─ 检查: isSyncingFromProps === true
  └─ 不 emit（避免循环）
  ↓
setTimeout 执行
  └─ isSyncingFromProps = false

内部变化流：
用户操作（如选择常用正则）
  ↓
localField.value.ruleConfig.pattern 改变
  ↓
watch(localField) 触发
  ↓
syncToParent()
  ├─ 检查: isSyncingFromProps === false
  └─ emit('update:modelValue', 深克隆(localField))
  ↓
父组件接收并更新 props.modelValue
  ↓
触发外部变化流（但会被标志阻止）
```

## ✅ 优势

### 1. 完全隔离
- 本地修改不直接影响 props
- Props 变化不直接影响本地（通过深克隆）

### 2. 单向防护
- 只在必要时阻止 emit
- 不影响正常的数据更新

### 3. 自动同步
- 任何本地修改都会自动同步到父组件
- 不需要手动调用同步函数

### 4. 简单可靠
- 只有一个标志变量
- 逻辑清晰，易于理解

## 🧪 测试场景

### 场景1：选择常用正则
```
1. 用户选择"整数"
2. applyCommonRegex() 修改 localField.value.ruleConfig.pattern
3. watch(localField) 触发
4. syncToParent() 检查标志（false）
5. emit 到父组件
6. 父组件更新 props.modelValue
7. watch(() => props.modelValue) 触发
8. syncFromProps() 设置标志为 true
9. 更新 localField（深克隆）
10. watch(localField) 触发
11. syncToParent() 检查标志（true）
12. 不 emit（避免循环）
13. setTimeout 后标志重置为 false
14. 完成
```

### 场景2：手动输入
```
1. 用户在输入框手动修改正则
2. v-model 更新 localField.value.ruleConfig.pattern
3. watch(localField) 触发
4. syncToParent() 检查标志（false）
5. emit 到父组件
6. 后续同场景1
```

### 场景3：切换规则类型
```
1. 用户切换规则类型
2. onRuleTypeChange() 修改 localField.value.ruleConfig
3. watch(localField) 触发
4. syncToParent() 检查标志（false）
5. emit 到父组件
6. 后续同场景1
```

## 🔍 为什么之前的方案失败

### computed 方案失败原因
```typescript
// ❌ 问题：返回 props 的引用
const localField = computed({
  get() {
    return props.modelValue  // 返回引用！
  },
  set(value) {
    emit('update:modelValue', value)
  }
})

// 修改时：
localField.value.ruleConfig.pattern = '.*'
// 实际修改的是 props.modelValue.ruleConfig.pattern
// 触发父组件更新 → 触发 get() → 无限循环
```

### 双向 watch 失败原因
```typescript
// ❌ 问题：互相触发
watch(() => props.modelValue, (v) => {
  localField.value = v  // 触发下面的 watch
})

watch(localField, (v) => {
  emit('update:modelValue', v)  // 父组件更新 → 触发上面的 watch
})

// 形成循环
```

### nextTick 清空失败原因
```typescript
// ❌ 问题：清空选择触发 change 事件
nextTick(() => {
  selectedCommonRegex.value = ''  // 触发 @change
})
// → 再次调用 applyCommonRegex → 递归
```

## 💡 核心原则

1. **永远不要直接修改 props**
2. **使用深克隆避免引用共享**
3. **用标志避免循环，而不是禁用 watch**
4. **保持数据流单向且可预测**

## 📝 代码清单

### 核心代码（20行）
```typescript
import { ref, watch } from 'vue'

const localField = ref<any>({})
let isSyncingFromProps = false

const syncFromProps = () => {
  isSyncingFromProps = true
  localField.value = JSON.parse(JSON.stringify(props.modelValue))
  setTimeout(() => { isSyncingFromProps = false }, 0)
}

const syncToParent = () => {
  if (!isSyncingFromProps) {
    emit('update:modelValue', JSON.parse(JSON.stringify(localField.value)))
  }
}

syncFromProps()

watch(localField, () => syncToParent(), { deep: true })
watch(() => props.modelValue, () => syncFromProps(), { deep: true })
```

## ✅ 测试清单

- [ ] 连续选择不同的常用正则
- [ ] 手动输入后再选择常用正则
- [ ] 快速切换提取方法
- [ ] 切换规则类型
- [ ] 混合操作（输入 + 选择 + 切换）
- [ ] 浏览器控制台无错误
- [ ] 数据正确同步到父组件
- [ ] UI 响应流畅

---

**方案版本**：v4 - 终极版  
**状态**：✅ 应该可以工作  
**日期**：2025-10-10  
**下一步**：用户测试验证

