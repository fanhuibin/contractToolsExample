<template>
  <div class="field-test-panel">
    <div class="panel-header">
      <span class="panel-title">测试与预览</span>
    </div>

    <div class="test-input-section">
      <div class="input-label">测试文本</div>
      <el-input
        v-model="testText"
        type="textarea"
        :rows="6"
        placeholder="粘贴测试文本..."
      />
    </div>

    <div class="test-actions">
      <el-button type="primary" @click="handleTest" :loading="isLoading">
        <el-icon><View /></el-icon>
        开始测试
      </el-button>
      <el-button @click="clearTest">
        <el-icon><Delete /></el-icon>
        清空
      </el-button>
    </div>

    <div class="test-result-section">
      <div class="result-label">测试结果</div>
      
      <el-empty v-if="!hasResult" description="点击上方按钮开始测试" :image-size="100" />

      <div v-else class="result-content">
        <el-alert
          :type="result.success ? 'success' : 'error'"
          :title="result.success ? '✓ 提取成功' : '✗ 提取失败'"
          :closable="false"
        >
          <template v-if="result.success">
            <div class="result-value">{{ result.value }}</div>
            <div class="result-meta">
              置信度：{{ result.confidence }}%
              <span v-if="result.startPosition !== undefined">
                 | 位置：{{ result.startPosition }}-{{ result.endPosition }}
              </span>
            </div>
            
            <div v-if="result.allMatches && result.allMatches.length > 0" class="all-matches">
              <el-divider content-position="left">所有匹配项 ({{ result.allMatches.length }}个)</el-divider>
              <el-tag 
                v-for="(match, idx) in result.allMatches" 
                :key="idx" 
                style="margin: 4px;"
                type="success"
                size="small"
              >
                {{ idx + 1 }}. {{ match }}
              </el-tag>
            </div>

            <div v-if="result.tableData" class="table-data">
              <el-divider content-position="left">表格数据</el-divider>
              <el-table 
                v-if="Array.isArray(result.tableData)" 
                :data="result.tableData" 
                border 
                size="small"
                max-height="300"
                style="margin-top: 12px;"
              >
                <el-table-column
                  v-for="(value, key) in getTableColumns(result.tableData)"
                  :key="key"
                  :prop="key"
                  :label="key"
                  min-width="120"
                  show-overflow-tooltip
                />
              </el-table>
            </div>
          </template>
          <template v-else>
            <div class="error-message">{{ result.errorMessage }}</div>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { View, Delete } from '@element-plus/icons-vue'

interface Props {
  field: any
  testResult: any
}

const props = defineProps<Props>()
const emit = defineEmits<{
  test: [testText: string]
}>()

const testText = ref('')
const isLoading = ref(false)

const hasResult = computed(() => props.testResult !== null && props.testResult !== undefined)
const result = computed(() => props.testResult || {})

const handleTest = () => {
  if (!testText.value.trim()) {
    return
  }
  isLoading.value = true
  emit('test', testText.value)
  setTimeout(() => {
    isLoading.value = false
  }, 500)
}

const clearTest = () => {
  testText.value = ''
}

const getTableColumns = (tableData: any[]) => {
  if (!tableData || tableData.length === 0) return {}
  return tableData[0] || {}
}

defineExpose({
  focus: () => {
    // 可以实现聚焦逻辑
  }
})
</script>

<style scoped lang="scss">
.field-test-panel {
  display: flex;
  flex-direction: column;
  height: 100%;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 2px solid #409eff;

    .panel-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
  }

  .test-input-section {
    margin-bottom: 12px;

    .input-label {
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
    }
  }

  .test-actions {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  .test-result-section {
    flex: 1;
    overflow: auto;

    .result-label {
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .result-content {
      .result-value {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
        padding: 8px 0;
        word-break: break-all;
      }

      .result-meta {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }

      .error-message {
        color: #f56c6c;
        font-size: 14px;
      }

      .all-matches {
        margin-top: 16px;
      }

      .table-data {
        margin-top: 16px;
      }
    }
  }
}
</style>

