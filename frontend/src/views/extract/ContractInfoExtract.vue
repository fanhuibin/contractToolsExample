<template>
  <div class="contract-info-extract">
    <!-- 头部区域 -->
    <div class="header-section">
      <h1 class="page-title">
        <el-icon><Search /></el-icon>
        智能信息提取
      </h1>
      <p class="page-description">
        基于OCR方案的精准合同信息提取功能，从PDF文档中智能提取关键信息，支持字符级精确定位和可视化分析
      </p>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧：上传和配置区域 -->
      <div class="left-panel">
        <el-card class="upload-card">
          <template #header>文档上传</template>
          <el-upload
            drag
            v-model:file-list="fileList"
            :multiple="false"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept=".pdf"
            :on-change="handleFileChange"
            :auto-upload="false"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">点击或拖拽文件到此区域上传</div>
            <div class="el-upload__tip">仅支持PDF格式，文件大小不超过100MB</div>
          </el-upload>
          
          <!-- 已选择的文件 -->
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
        </el-card>


        <!-- 提取配置 -->
        <el-card class="config-card">
          <template #header>提取配置</template>
          <el-form :model="extractConfig" label-position="top">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="文档类型">
                  <el-select v-model="extractConfig.schemaType" style="width: 100%">
                    <el-option value="contract" label="合同文档" />
                    <el-option value="invoice" label="发票" />
                    <el-option value="resume" label="简历" />
                    <el-option value="news" label="新闻" />
                    <el-option value="general" label="通用" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="提取轮数">
                  <el-select v-model="extractConfig.extractionPasses" style="width: 100%">
                    <el-option :value="1" label="1轮（快速）" />
                    <el-option :value="3" label="3轮（推荐）" />
                    <el-option :value="5" label="5轮（精确）" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item>
                  <el-checkbox v-model="extractConfig.enableChunking">
                    启用分块处理
                  </el-checkbox>
                  <el-tooltip content="将大文档分割成小块处理，可以解决大模型上下文不足问题，但是会导致处理时间增加和Token消耗大幅增加，一般不建议使用">
                    <el-icon style="margin-left: 4px; color: #999;"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <!-- 可视化报告选项已移除，现在由前端负责渲染 -->
              </el-col>
            </el-row>

            <el-form-item>
              <el-button 
                type="primary" 
                size="large" 
                style="width: 100%"
                :loading="isExtracting"
                :disabled="!canStartExtraction"
                @click="startExtraction"
              >
                <el-icon><VideoPlay /></el-icon>
                开始提取信息
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- 右侧：结果展示区域 -->
      <div class="right-panel">
        <!-- 任务状态 -->
        <el-card v-if="currentTask" class="status-card">
          <template #header>提取进度</template>
          <div class="task-status">
            <el-progress 
              :percentage="currentTask.progress" 
              :status="getProgressStatus(currentTask.status)"
              :color="getProgressColor(currentTask.status)"
            />
            <div class="status-info">
              <p class="status-message">{{ currentTask.message }}</p>
              <p class="task-info">
                任务ID: {{ currentTask.taskId }} | 
                创建时间: {{ formatTime(currentTask.createdAt) }}
              </p>
            </div>
          </div>
        </el-card>

        <!-- 提取结果 -->
        <el-card v-if="extractResult" class="result-card">
          <template #header>提取结果</template>
          <!-- 统计信息 -->
          <div class="statistics">
            <el-row :gutter="16">
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
          </div>

          <!-- 提取的字段 -->
          <el-divider />
          <div class="extracted-fields">
            <h3>提取的信息字段</h3>
            <el-table 
              :data="extractResult.extractions.items" 
              size="small"
              row-key="field"
            >
              <el-table-column prop="field" label="字段名" />
              <el-table-column prop="value" label="提取值" :show-overflow-tooltip="true">
                <template #default="{ row }">
                  <el-tooltip :content="row.value">
                    <span class="value-text">{{ row.value }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="confidence" label="置信度" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="Math.round((row.confidence || 0) * 100)"
                    :stroke-width="16"
                    :color="getConfidenceColor(row.confidence)"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="position" label="字符位置" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.charInterval" type="primary">
                    {{ row.charInterval.startPos }}-{{ row.charInterval.endPos }}
                  </el-tag>
                  <el-tag v-else type="info">未定位</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 操作按钮 -->
          <el-divider />
          <div class="result-actions" style="display: flex; gap: 12px;">
            <el-button type="primary" @click="openVisualization">
              <el-icon><View /></el-icon>
              查看可视化报告
            </el-button>
            <el-button @click="downloadResults">
              <el-icon><Download /></el-icon>
              下载结果
            </el-button>
            <el-button @click="copyResults">
              <el-icon><CopyDocument /></el-icon>
              复制结果
            </el-button>
          </div>
        </el-card>

        <!-- 历史记录 -->
        <el-card class="history-card">
          <template #header>最近提取</template>
          <div v-for="item in recentTasks" :key="item.taskId" class="history-item">
            <div class="history-item-content">
              <el-avatar :style="getStatusColor(item.status)">
                {{ getStatusIcon(item.status) }}
              </el-avatar>
              <div class="history-item-info">
                <div class="history-item-title">{{ item.fileName || '文本输入' }}</div>
                <div class="history-item-desc">{{ getSchemaTypeLabel(item.schemaType) }} | {{ formatTime(item.createdAt) }}</div>
              </div>
            </div>
            <el-button size="small" text type="primary" @click="loadTask(item.taskId)">
              查看
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 可视化功能已移至独立页面 -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
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
import { extractFromFile, getTaskStatus, getExtractResult } from '@/api/extract'

// 数据定义
const fileList = ref<any[]>([])
const selectedFile = ref<File | null>(null)
const isExtracting = ref(false)
const currentTask = ref<any>(null)
const extractResult = ref<any>(null)
const recentTasks = ref<any[]>([])

// 提取配置
const extractConfig = reactive({
  schemaType: 'contract',
  extractionPasses: 1,  // 默认改为1轮
  enableChunking: false,
  llmProvider: 'auto'
})

// 表格列定义
const extractionColumns = [
  {
    title: '字段名',
    dataIndex: 'field',
    key: 'field',
    width: 150
  },
  {
    title: '提取值',
    dataIndex: 'value',
    key: 'value'
  },
  {
    title: '置信度',
    key: 'confidence',
    width: 120
  },
  {
    title: '位置',
    key: 'position',
    width: 100
  }
]

// 计算属性
const canStartExtraction = computed(() => {
  return selectedFile.value && !isExtracting.value
})

// 方法
const beforeUpload = (file: File) => {
  const isValidType = file.type === 'application/pdf'
  if (!isValidType) {
    ElMessage.error('仅支持PDF文件')
    return false
  }
  
  const isValidSize = file.size / 1024 / 1024 < 100
  if (!isValidSize) {
    ElMessage.error('文件大小不能超过100MB')
    return false
  }
  
  return false // 阻止自动上传
}

const handleFileChange = (info: any) => {
  if (info.fileList.length > 0) {
    selectedFile.value = info.fileList[0].originFileObj
  }
}

const clearFile = () => {
  selectedFile.value = null
  fileList.value = []
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
        
        // 添加到历史记录
        addToHistory(response.data)
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

const getSchemaTypeLabel = (schemaType: string): string => {
  const labelMap: Record<string, string> = {
    'contract': '合同文档',
    'invoice': '发票',
    'resume': '简历',
    'news': '新闻',
    'general': '通用',
    'unknown': '未知类型'
  }
  return labelMap[schemaType] || schemaType || '未知类型'
}

const addToHistory = (task: any) => {
  const existingIndex = recentTasks.value.findIndex((t: any) => t.taskId === task.taskId)
  if (existingIndex >= 0) {
    recentTasks.value[existingIndex] = task
  } else {
    recentTasks.value.unshift(task)
    if (recentTasks.value.length > 10) {
      recentTasks.value = recentTasks.value.slice(0, 10)
    }
  }
  // 保存到localStorage
  localStorage.setItem('extract_history', JSON.stringify(recentTasks.value))
}

const loadTask = async (taskId: string) => {
  try {
    const statusResponse = await getTaskStatus(taskId)
    if (statusResponse && statusResponse.data) {
      currentTask.value = statusResponse.data
      
      if (statusResponse.data?.status === 'completed') {
        await loadExtractResult(taskId)
      }
    }
  } catch (error) {
    ElMessage.error('加载任务失败')
  }
}

// 生命周期
onMounted(() => {
  // 加载历史记录
  const history = localStorage.getItem('extract_history')
  if (history) {
    try {
      recentTasks.value = JSON.parse(history)
    } catch (error) {
      console.error('解析历史记录失败:', error)
    }
  }
})
</script>

<style scoped>
.contract-info-extract {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.header-section {
  text-align: center;
  margin-bottom: 32px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 16px;
}

.page-description {
  font-size: 16px;
  color: #666;
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.6;
}

.main-content {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
  align-items: start;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-card,
.text-input-card,
.config-card,
.status-card,
.result-card,
.history-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.selected-file {
  margin-top: 16px;
}

.task-status {
  padding: 16px 0;
}

.status-info {
  margin-top: 16px;
}

.status-message {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.task-info {
  font-size: 12px;
  color: #666;
  margin: 0;
}

.statistics {
  padding: 16px 0;
}

.extracted-fields h3 {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
}

.value-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.result-actions {
  text-align: center;
}

/* 历史记录样式 */
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.history-item:last-child {
  border-bottom: none;
}

.history-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.history-item-info {
  flex: 1;
}

.history-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.history-item-desc {
  font-size: 12px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .left-panel {
    order: 1;
  }
  
  .right-panel {
    order: 2;
  }
}
</style>
