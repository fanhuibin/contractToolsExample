<template>
  <div class="match-mode-config">
    <el-form-item>
      <template #label>
        <span>匹配模式</span>
        <el-tooltip placement="top">
          <template #content>
            <div style="max-width: 300px;">
              <p><strong>指定序号</strong>和<strong>返回所有</strong>是互斥的</p>
              <p>• 指定序号：只返回第N个匹配项</p>
              <p>• 返回所有：返回所有匹配项的列表</p>
            </div>
          </template>
          <el-icon style="margin-left: 4px; cursor: help;"><QuestionFilled /></el-icon>
        </el-tooltip>
      </template>
      <el-radio-group :model-value="currentMode" @change="handleModeChange">
        <el-radio label="single">指定序号</el-radio>
        <el-radio label="all">返回所有</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item v-if="currentMode === 'single'" label="匹配序号">
      <el-input-number 
        :model-value="currentOccurrence" 
        @change="handleOccurrenceChange"
        :min="1" 
        :max="100" 
        style="width: 100%;" 
      />
      <div class="hint">提取第几个匹配项</div>
    </el-form-item>

    <el-form-item v-if="currentMode === 'all'" label="返回方式">
      <el-tag type="success">
        <el-icon><Check /></el-icon>
        将返回所有匹配项的列表
      </el-tag>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { QuestionFilled, Check } from '@element-plus/icons-vue'

interface Props {
  modelValue: {
    matchMode?: string
    occurrence?: number
    returnAll?: boolean
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: any]
}>()

// 使用 computed 读取值，避免响应式陷阱
const currentMode = computed(() => props.modelValue?.matchMode || 'single')
const currentOccurrence = computed(() => props.modelValue?.occurrence || 1)

// 事件处理：每次创建新对象
const handleModeChange = (mode: string) => {
  const newConfig = {
    ...props.modelValue,
    matchMode: mode,
    returnAll: mode === 'all',
    occurrence: mode === 'single' ? (props.modelValue?.occurrence || 1) : 1
  }
  emit('update:modelValue', newConfig)
}

const handleOccurrenceChange = (value: number) => {
  const newConfig = {
    ...props.modelValue,
    occurrence: value
  }
  emit('update:modelValue', newConfig)
}
</script>

<style scoped lang="scss">
.match-mode-config {
  .hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>

