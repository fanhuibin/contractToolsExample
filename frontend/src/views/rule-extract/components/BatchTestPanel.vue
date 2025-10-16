<template>
  <div class="batch-test-panel">
    <div class="test-input">
      <div class="label">测试文本（粘贴合同内容）</div>
      <el-input
        v-model="testText"
        type="textarea"
        :rows="10"
        placeholder="粘贴完整的合同文本进行批量测试..."
      />
    </div>

    <div class="actions">
      <el-button type="primary" @click="runTest" :loading="testing">
        <el-icon><View /></el-icon>
        测试所有字段
      </el-button>
      <el-switch v-model="debugMode" inactive-text="调试模式" style="margin-left: 12px;" />
    </div>

    <div v-if="results.length > 0" class="results">
      <div class="results-summary">
        <el-statistic title="测试字段" :value="results.length" />
        <el-statistic title="成功" :value="successCount" />
        <el-statistic title="失败" :value="failCount" />
        <el-statistic title="成功率" :value="successRate" suffix="%" />
      </div>

      <el-table :data="results" border stripe style="margin-top: 16px;">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="fieldName" label="字段名称" width="150" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="提取结果" min-width="250" show-overflow-tooltip />
        <el-table-column prop="confidence" label="置信度" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.success">{{ row.confidence }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" text @click="showDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-else description="输入测试文本后，点击按钮开始批量测试" />

    <el-dialog
      v-model="detailVisible"
      title="测试详情"
      width="800px"
    >
      <div v-if="currentDetail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="字段名称">
            {{ currentDetail.fieldName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentDetail.success ? 'success' : 'danger'">
              {{ currentDetail.success ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提取结果" :span="2">
            <div class="result-value">{{ currentDetail.value || currentDetail.errorMessage }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="置信度">
            {{ currentDetail.confidence || '-' }}%
          </el-descriptions-item>
          <el-descriptions-item label="位置">
            {{ currentDetail.startPosition || '-' }} - {{ currentDetail.endPosition || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentDetail.debugInfo && currentDetail.debugInfo.length > 0" class="debug-section">
          <el-divider content-position="left">调试信息</el-divider>
          <div class="debug-info">
            <div v-for="(info, idx) in currentDetail.debugInfo" :key="idx" class="debug-item">
              {{ info }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import { testExtractRule } from '@/api/rule-test'

interface Props {
  template: any
}

const props = defineProps<Props>()

const testText = ref('')
const debugMode = ref(true)
const testing = ref(false)
const results = ref<any[]>([])
const detailVisible = ref(false)
const currentDetail = ref<any>(null)

const successCount = computed(() => results.value.filter(r => r.success).length)
const failCount = computed(() => results.value.filter(r => !r.success).length)
const successRate = computed(() => {
  if (results.value.length === 0) return 0
  return Math.round((successCount.value / results.value.length) * 100)
})

const runTest = async () => {
  if (!testText.value.trim()) {
    ElMessage.warning('请输入测试文本')
    return
  }

  testing.value = true
  results.value = []

  try {
    for (const field of props.template.fields) {
      const res: any = await testExtractRule({
        text: testText.value,
        ruleType: field.ruleType,
        config: field.ruleConfig,
        debug: debugMode.value
      })

      if (res.code === 200) {
        results.value.push({
          fieldName: field.fieldName,
          fieldCode: field.fieldCode,
          ...res.data
        })
      } else {
        results.value.push({
          fieldName: field.fieldName,
          fieldCode: field.fieldCode,
          success: false,
          errorMessage: res.message
        })
      }
    }

    ElMessage.success(`批量测试完成：成功 ${successCount.value}/${results.value.length}`)
  } catch (error: any) {
    ElMessage.error('批量测试失败：' + (error.message || '未知错误'))
  } finally {
    testing.value = false
  }
}

const showDetail = (row: any) => {
  currentDetail.value = row
  detailVisible.value = true
}
</script>

<style scoped lang="scss">
.batch-test-panel {
  .test-input {
    margin-bottom: 16px;

    .label {
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
      font-weight: 500;
    }
  }

  .actions {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
  }

  .results-summary {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
  }

  .detail-content {
    .result-value {
      padding: 8px;
      background: #f5f7fa;
      border-radius: 4px;
      font-family: monospace;
      word-break: break-all;
    }

    .debug-section {
      margin-top: 20px;

      .debug-info {
        background: #f5f7fa;
        padding: 12px;
        border-radius: 4px;
        max-height: 300px;
        overflow-y: auto;

        .debug-item {
          font-size: 12px;
          color: #606266;
          padding: 4px 0;
          font-family: monospace;
          line-height: 1.6;
        }
      }
    }
  }
}
</style>

