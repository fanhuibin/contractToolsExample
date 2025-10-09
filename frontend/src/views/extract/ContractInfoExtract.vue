<template>
  <div class="contract-info-extract">
    <PageHeader 
      title="智能信息提取" 
      description="基于OCR方案的精准合同信息提取功能，从PDF文档中智能提取关键信息，支持字符级精确定位和可视化分析"
      :icon="Search"
    />

    <!-- 上传和配置区域 -->
    <el-card class="mb12">
      <template #header>
        <div class="card-header">
          <span>文档上传与配置</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <FileUploadZone
            accept=".pdf"
            tip="仅支持PDF格式，文件大小不超过100MB"
            :max-size="100"
            @change="handleFileChange"
          />
          
          <div v-if="selectedFile" class="selected-file">
            <el-alert
              :title="`已选择文件: ${selectedFile?.name}`"
              :description="`大小: ${formatFileSize(selectedFile?.size || 0)}`"
              type="info"
              show-icon
              closable
              @close="clearFile"
            />
          </div>
        </el-col>

        <el-col :span="12">
          <el-form :model="extractConfig" label-width="100px">
            <el-form-item label="文档类型">
              <el-select v-model="extractConfig.schemaType" style="width: 100%">
                <el-option value="contract" label="合同文档" />
                <el-option value="invoice" label="发票" />
                <el-option value="resume" label="简历" />
                <el-option value="news" label="新闻" />
                <el-option value="general" label="通用" />
              </el-select>
            </el-form-item>

            <el-form-item label="提取轮数">
              <el-select v-model="extractConfig.extractionPasses" style="width: 100%">
                <el-option :value="1" label="1轮（快速）" />
                <el-option :value="3" label="3轮（推荐）" />
                <el-option :value="5" label="5轮（精确）" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="分块处理">
              <el-checkbox v-model="extractConfig.enableChunking">
                启用分块处理
              </el-checkbox>
              <el-tooltip content="将大文档分割成小块处理，可以解决大模型上下文不足问题，但是会导致处理时间增加和Token消耗大幅增加，一般不建议使用">
                <el-icon style="margin-left: 4px; color: #999;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary"
                :loading="isExtracting"
                :disabled="!canStartExtraction"
                @click="startExtraction"
              >
                <el-icon><VideoPlay /></el-icon>
                开始提取信息
              </el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务进度 -->
    <el-card v-if="currentTask" class="mb12">
      <template #header>
        <div class="card-header">
          <span>提取进度</span>
          <el-tag :type="getProgressStatus(currentTask.status) === 'success' ? 'success' : 'info'" size="small">
            {{ currentTask.message }}
          </el-tag>
        </div>
      </template>

      <div class="progress-content">
        <el-progress 
          :percentage="currentTask.progress || 0" 
          :status="getProgressStatus(currentTask.status)"
          :stroke-width="20"
        />
        <div class="progress-info">
          <span>任务ID: {{ currentTask.taskId }}</span>
          <span>创建时间: {{ formatTime(currentTask.createdAt) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 提取结果 -->
    <el-card v-if="extractResult" class="mb12">
      <template #header>
        <div class="card-header">
          <span>提取结果</span>
          <div class="header-actions">
            <el-button size="small" type="primary" @click="openVisualization">
              <el-icon><View /></el-icon>
              查看可视化
            </el-button>
            <el-button size="small" @click="downloadResults">
              <el-icon><Download /></el-icon>
              下载结果
            </el-button>
            <el-button size="small" @click="copyResults">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <el-row :gutter="16" class="statistics">
        <el-col :span="6">
          <el-statistic 
            title="提取字段" 
            :value="extractResult.statistics.totalFields" 
            suffix="个"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic 
            title="成功定位" 
            :value="Math.round(extractResult.statistics.positionAccuracy * 100)" 
            suffix="%"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic 
            title="平均置信度" 
            :value="Math.round(extractResult.statistics.averageConfidence * 100)" 
            suffix="%"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic 
            title="OCR提供者" 
            :value="extractResult.document.ocrProvider"
          />
        </el-col>
      </el-row>

      <el-divider />

      <!-- 提取的字段表格 -->
      <el-table 
        :data="extractResult.extractions.items" 
        stripe
        style="width: 100%"
      >
        <el-table-column prop="field" label="字段名" width="180" />
        <el-table-column prop="value" label="提取值" show-overflow-tooltip />
        <el-table-column label="置信度" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round((row.confidence || 0) * 100)"
              :stroke-width="16"
              :color="getConfidenceColor(row.confidence)"
            />
          </template>
        </el-table-column>
        <el-table-column label="字符位置" width="150" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.charInterval" type="primary" size="small">
              {{ row.charInterval.startPos }}-{{ row.charInterval.endPos }}
            </el-tag>
            <el-tag v-else type="info" size="small">未定位</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Upload,
  UploadFilled,
  VideoPlay,
  QuestionFilled,
  View,
  Download,
  CopyDocument
} from '@element-plus/icons-vue'
import { PageHeader, FileUploadZone } from '@/components/common'
import { extractFromFile, getTaskStatus, getExtractResult } from '@/api/extract'

// 数据定义
const selectedFile = ref<File | null>(null)
const isExtracting = ref(false)
const currentTask = ref<any>(null)
const extractResult = ref<any>(null)

// 提取配置
const extractConfig = reactive({
  schemaType: 'contract',
  extractionPasses: 1,  // 默认改为1轮
  enableChunking: false,
  llmProvider: 'auto'
})

// 计算属性
const canStartExtraction = computed(() => {
  return selectedFile.value && !isExtracting.value
})

// 方法
const handleFileChange = (file: File) => {
  // 验证文件类型
  if (file.type !== 'application/pdf') {
    ElMessage.error('仅支持PDF文件')
    return
  }
  
  // 验证文件大小
  if (file.size / 1024 / 1024 > 100) {
    ElMessage.error('文件大小不能超过100MB')
    return
  }
  
  selectedFile.value = file
}

const clearFile = () => {
  selectedFile.value = null
}

const startExtraction = async () => {
  try {
    isExtracting.value = true
    currentTask.value = null
    extractResult.value = null
    
    // PDF文件提取
    const formData = new FormData()
    if (selectedFile.value) {
      formData.append('file', selectedFile.value)
    }
    formData.append('schemaType', extractConfig.schemaType)
    formData.append('extractionPasses', extractConfig.extractionPasses.toString())
    formData.append('enableChunking', extractConfig.enableChunking.toString())
    formData.append('llmProvider', extractConfig.llmProvider)
    
    const response = await extractFromFile(formData)
    
    if (response && response.data) {
      const taskId = response.data.taskId
      ElMessage.success('提取任务已启动')
      
      // 开始轮询任务状态
      pollTaskStatus(taskId)
    } else {
      throw new Error('启动提取任务失败')
    }
  } catch (error: any) {
    console.error('启动提取失败:', error)
    ElMessage.error('启动提取失败: ' + (error?.message || '未知错误'))
    isExtracting.value = false
  }
}

const pollTaskStatus = async (taskId: string) => {
  try {
    const response = await getTaskStatus(taskId)
    if (response && response.data) {
      currentTask.value = response.data
      
      if (response.data?.status === 'completed') {
        // 任务完成，获取结果
        await loadExtractResult(taskId)
        isExtracting.value = false
        ElMessage.success('信息提取完成')
      } else if (response.data?.status === 'failed') {
        // 任务失败
        isExtracting.value = false
        ElMessage.error('提取失败: ' + (response.data?.message || ''))
      } else {
        // 任务进行中，继续轮询
        setTimeout(() => pollTaskStatus(taskId), 2000)
      }
    }
  } catch (error) {
    console.error('获取任务状态失败:', error)
    isExtracting.value = false
    ElMessage.error('获取任务状态失败')
  }
}

const loadExtractResult = async (taskId: string) => {
  try {
    const response = await getExtractResult(taskId)
    if (response && response.data) {
      extractResult.value = response.data
    }
  } catch (error) {
    console.error('获取提取结果失败:', error)
    ElMessage.error('获取提取结果失败')
  }
}

const openVisualization = () => {
  if (currentTask.value?.taskId) {
    // 跳转到增强版可视化页面（当前窗口）
    const enhancedUrl = `/info-extract-enhanced?taskId=${currentTask.value.taskId}`
    window.location.href = enhancedUrl
  }
}

const downloadResults = () => {
  if (extractResult.value) {
    const data = JSON.stringify(extractResult.value, null, 2)
    const blob = new Blob([data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `extract_result_${currentTask.value.taskId}.json`
    a.click()
    URL.revokeObjectURL(url)
  }
}

const copyResults = async () => {
  if (extractResult.value?.extractions?.items) {
    try {
      const text = extractResult.value.extractions.items
        .map((item: any) => `${item.field}: ${item.value}`)
        .join('\n')
      await navigator.clipboard.writeText(text)
      ElMessage.success('结果已复制到剪赴板')
    } catch (error) {
      ElMessage.error('复制失败')
    }
  }
}

// 辅助方法
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

const getProgressStatus = (status: string) => {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return 'active'
}

const getProgressColor = (status: string) => {
  if (status === 'completed') return '#52c41a'
  if (status === 'failed') return '#f5222d'
  return '#1890ff'
}

const getConfidenceColor = (confidence: number) => {
  if (confidence > 0.8) return '#52c41a'
  if (confidence > 0.6) return '#faad14'
  return '#f5222d'
}

const getStatusColor = (status: string) => {
  const colors = {
    completed: { backgroundColor: '#52c41a' },
    failed: { backgroundColor: '#f5222d' },
    cancelled: { backgroundColor: '#d9d9d9' }
  }
  return colors[status as keyof typeof colors] || { backgroundColor: '#1890ff' }
}

const getStatusIcon = (status: string) => {
  const icons = {
    completed: '✓',
    failed: '✗',
    cancelled: '○'
  }
  return icons[status as keyof typeof icons] || '●'
}

</script>

<style scoped>
.contract-info-extract {
  /* 与其他页面保持一致的样式 */
}

.mb12 {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.selected-file {
  margin-top: 16px;
}

.progress-content {
  padding: 20px 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
}

.statistics {
  margin-bottom: 20px;
}
</style>
