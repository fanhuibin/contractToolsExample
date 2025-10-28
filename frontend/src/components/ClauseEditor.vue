<template>
  <div class="clause-editor">
    <!-- 空状态提示 -->
    <div v-if="!modelValue || parsedSegments.length === 0" class="clause-empty-state">
      <el-empty description="条款内容为空">
        <template #description>
          <p style="color:#909399;font-size:14px;">
            该条款暂无内容，请在模板设计时添加条款文本
          </p>
        </template>
      </el-empty>
    </div>
    
    <!-- 最终的完整条款显示 -->
    <div v-else class="clause-final-display">
      <template v-for="(segment, index) in parsedSegments" :key="index">
        <!-- 固定文本 -->
        <span v-if="segment.type === 'text'" class="fixed-text">{{ segment.content }}</span>
        
        <!-- 变量（显示值或占位符） -->
        <el-popover
          v-else-if="segment.type === 'variable'"
          :width="350"
          trigger="click"
          placement="top"
        >
          <template #reference>
            <span 
              class="variable-value" 
              :class="{ 
                'has-value': !!variableValues[segment.name!],
                'empty-value': !variableValues[segment.name!],
                'linked': isLinkedVariable(segment.name!)
              }"
              :title="`点击编辑变量：${segment.name!}`"
            >
              {{ variableValues[segment.name!] || `[${segment.name!}]` }}
            </span>
          </template>
          
          <!-- 变量编辑弹窗 -->
          <div class="variable-editor-popover">
            <div class="popover-header">
              <div class="var-info">
                <el-icon class="var-icon"><Edit /></el-icon>
                <span class="var-name">{{ segment.name! }}</span>
              </div>
              <el-tag 
                size="small" 
                :type="isLinkedVariable(segment.name!) ? 'success' : 'warning'"
              >
                {{ isLinkedVariable(segment.name!) ? '已关联字段' : '独立变量' }}
              </el-tag>
            </div>
            
            <el-divider style="margin: 12px 0;" />
            
            <div class="popover-body">
              <el-input
                v-model="variableValues[segment.name!]"
                type="textarea"
                :rows="3"
                :placeholder="`请输入${segment.name!}的值`"
                clearable
                @input="onVariableChange(segment.name!)"
              />
            </div>
            
            <div class="popover-hint" v-if="isLinkedVariable(segment.name!)">
              <el-icon><InfoFilled /></el-icon>
              <span>该变量关联了表单字段，修改此处会同步更新表单</span>
            </div>
          </div>
        </el-popover>
      </template>
    </div>
    
    <!-- 变量列表（调试用，可选） -->
    <div class="variables-panel" v-if="showVariablesPanel">
      <el-divider>变量列表</el-divider>
      <div class="variables-list">
        <div 
          v-for="varName in allVariables" 
          :key="varName"
          class="variable-item"
        >
          <div class="variable-name">
            <el-tag size="small" :type="isLinkedVariable(varName) ? 'success' : 'warning'">
              {{ varName }}
            </el-tag>
            <span class="variable-status">
              {{ isLinkedVariable(varName) ? '已关联' : '独立变量' }}
            </span>
          </div>
          <el-input
            v-model="variableValues[varName]"
            size="small"
            :placeholder="`请输入${varName}`"
            @input="onVariableChange(varName)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Edit, InfoFilled } from '@element-plus/icons-vue'

interface ClauseSegment {
  type: 'text' | 'variable'
  content?: string  // 对于 text 类型
  name?: string     // 对于 variable 类型
}

interface Props {
  // 条款模板文本（包含变量，如："甲方{{partyA}}与乙方{{partyB}}签订合同"）
  modelValue: string
  // 已存在的表单字段（tag -> value 映射）
  existingFields?: Record<string, any>
  // 是否显示变量面板（调试用）
  showVariablesPanel?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'update:variables', variables: Record<string, string>): void
}

const props = withDefaults(defineProps<Props>(), {
  existingFields: () => ({}),
  showVariablesPanel: false
})

const emit = defineEmits<Emits>()

// 变量值存储
const variableValues = ref<Record<string, string>>({})

// 解析条款文本，提取变量和文本片段
// 支持两种格式：{{variableName}} 和 ${variableName}
const parsedSegments = computed<ClauseSegment[]>(() => {
  if (!props.modelValue) return []
  
  const segments: ClauseSegment[] = []
  const text = props.modelValue
  
  // 匹配两种格式的变量：
  // 1. {{variableName}} - 花括号格式
  // 2. ${variableName} - 美元符号格式
  // 变量名支持：字母、数字、下划线
  const variableRegex = /(\{\{([\w]+)\}\})|(\$\{([\w]+)\})/g
  
  let lastIndex = 0
  let match: RegExpExecArray | null
  
  while ((match = variableRegex.exec(text)) !== null) {
    // 添加变量前的文本
    if (match.index > lastIndex) {
      segments.push({
        type: 'text',
        content: text.substring(lastIndex, match.index)
      })
    }
    
    // 提取变量名（从匹配的捕获组中获取）
    // match[2] 是 {{var}} 格式的变量名
    // match[4] 是 ${var} 格式的变量名
    const varName = match[2] || match[4]
    
    segments.push({
      type: 'variable',
      name: varName
    })
    
    lastIndex = variableRegex.lastIndex
  }
  
  // 添加最后的文本
  if (lastIndex < text.length) {
    segments.push({
      type: 'text',
      content: text.substring(lastIndex)
    })
  }
  
  return segments
})

// 所有变量名列表
const allVariables = computed<string[]>(() => {
  const vars = parsedSegments.value
    .filter(seg => seg.type === 'variable')
    .map(seg => seg.name!)
  
  
  return vars
})

// 判断变量是否关联了表单字段
function isLinkedVariable(varName: string): boolean {
  return varName in props.existingFields
}

// 获取变量显示文本（避免在模板中使用包含 {{ }} 的模板字面量）
function getVariableDisplay(varName: string): string {
  const value = variableValues.value[varName]
  if (value) {
    return value
  }
  // 返回 {{varName}} 格式
  return '{{' + varName + '}}'
}

// 变量值变化时触发
function onVariableChange(varName: string) {
  // 如果是关联字段，同步更新表单字段（通过 emit）
  emit('update:variables', { ...variableValues.value })
  
  // 重新生成完整的条款文本
  updateClauseText()
}

// 根据当前变量值重新生成条款文本
function updateClauseText() {
  let result = props.modelValue
  
  // 替换所有变量
  Object.keys(variableValues.value).forEach(varName => {
    const value = variableValues.value[varName]
    if (value) {
      const regex = new RegExp(`\\{\\{${varName}\\}\\}`, 'g')
      result = result.replace(regex, value)
    }
  })
  
  // emit('update:modelValue', result)
}

// 初始化变量值（从已有字段或清空）
function initializeVariables() {
  allVariables.value.forEach(varName => {
    if (isLinkedVariable(varName)) {
      // 如果是关联字段，使用字段值
      variableValues.value[varName] = props.existingFields[varName] || ''
    } else {
      // 如果是独立变量，保持空或原值
      if (!variableValues.value[varName]) {
        variableValues.value[varName] = ''
      }
    }
  })
}

// 监听条款文本变化
watch(() => props.modelValue, () => {
  initializeVariables()
}, { immediate: true })

// 监听已有字段变化
watch(() => props.existingFields, (newFields) => {
  // 同步关联字段的值
  Object.keys(newFields).forEach(fieldName => {
    if (allVariables.value.includes(fieldName)) {
      variableValues.value[fieldName] = newFields[fieldName]
    }
  })
}, { deep: true })

onMounted(() => {
  initializeVariables()
})

// 暴露方法供父组件调用
defineExpose({
  getVariableValues: () => variableValues.value,
  getAllVariables: () => allVariables.value,
  isLinkedVariable,
  getVariableDisplay
})
</script>

<style scoped>
.clause-editor {
  width: 100%;
}

/* 空状态 */
.clause-empty-state {
  padding: 32px 16px;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  text-align: center;
}

/* 最终条款显示区域 */
.clause-final-display {
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  line-height: 1.8;
  font-size: 15px;
  color: #303133;
  min-height: 80px;
  white-space: pre-wrap;
  word-break: break-word;
  position: relative;
}

/* 添加使用提示 */
.clause-final-display::before {
  content: '💡 点击蓝色/红色变量可编辑';
  position: absolute;
  top: -28px;
  right: 0;
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 4px 10px;
  border-radius: 4px;
}

/* 固定文本（只读，不可修改） */
.fixed-text {
  color: #303133;
  user-select: text;
}

/* 变量值显示（可点击编辑） */
.variable-value {
  display: inline;
  padding: 2px 6px;
  margin: 0 2px;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  user-select: text;
}

/* 变量有值时的样式 */
.variable-value.has-value {
  background: #e8f4ff;
  color: #1677ff;
  border-bottom: 2px solid #91caff;
}

.variable-value.has-value:hover {
  background: #bae0ff;
  border-bottom-color: #1677ff;
}

/* 变量为空时的样式（占位符） */
.variable-value.empty-value {
  background: #fff1f0;
  color: #ff4d4f;
  border-bottom: 2px dashed #ffccc7;
}

.variable-value.empty-value:hover {
  background: #ffccc7;
  border-bottom-color: #ff4d4f;
}

/* 关联字段的特殊标识 */
.variable-value.linked.has-value {
  background: #f6ffed;
  color: #52c41a;
  border-bottom-color: #95de64;
}

.variable-value.linked.has-value:hover {
  background: #d9f7be;
  border-bottom-color: #52c41a;
}

/* 变量编辑弹窗 */
.variable-editor-popover {
  padding: 4px;
}

.popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.var-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.var-icon {
  color: #409eff;
  font-size: 16px;
}

.var-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.popover-body {
  padding: 8px 0;
}

.popover-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: #e6f7ff;
  border-left: 3px solid #1677ff;
  border-radius: 4px;
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
  margin-top: 8px;
}

.popover-hint .el-icon {
  color: #1677ff;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 2px;
}

.popover-hint span {
  flex: 1;
}

/* 变量面板 */
.variables-panel {
  margin-top: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.variables-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.variable-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.variable-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.variable-status {
  font-size: 13px;
  color: #909399;
}
</style>

