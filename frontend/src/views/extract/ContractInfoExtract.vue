<template>
  <div class="contract-info-extract">
    <PageHeader 
      title="智能信息提取" 
      description="基于OCR方案的精准合同信息提取功能，从PDF文档中智能提取关键信息，支持字符级精确定位和可视化分析"
      :icon="Search"
    />

    <!-- 三列式主操作区 -->
    <el-row :gutter="16" class="main-operation-area mb16">
      <!-- 左列：文件上传 -->
      <el-col :span="8">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <el-icon><Upload /></el-icon>
              <span>文档上传</span>
            </div>
          </template>
          
          <FileUploadZone
            accept=".pdf"
            tip="支持PDF，最大100MB"
            :max-size="100"
            @change="handleFileChange"
          />
          
          <div v-if="selectedFile" class="file-info-compact">
            <div class="file-name">
              <el-icon><Document /></el-icon>
              <span class="text-ellipsis">{{ selectedFile.name }}</span>
            </div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
              <el-button 
                link 
                type="danger" 
                size="small"
                @click="clearFile"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中列：配置参数 -->
      <el-col :span="8">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <el-icon><Setting /></el-icon>
              <span>提取配置</span>
            </div>
          </template>
          
          <el-form :model="extractConfig" label-width="90px" class="compact-form">
            <el-form-item label="文档类型">
              <el-select v-model="extractConfig.schemaType" size="default">
                <el-option value="contract" label="合同文档">
                  <span>📄 合同文档</span>
                </el-option>
                <el-option value="invoice" label="发票">
                  <span>🧾 发票</span>
                </el-option>
                <el-option value="resume" label="简历">
                  <span>👤 简历</span>
                </el-option>
                <el-option value="news" label="新闻">
                  <span>📰 新闻</span>
                </el-option>
                <el-option value="general" label="通用">
                  <span>📋 通用</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="提取轮数">
              <el-select v-model="extractConfig.extractionPasses" size="default">
                <el-option :value="1">
                  <span>⚡ 1轮（快速）</span>
                </el-option>
                <el-option :value="3">
                  <span>⭐ 3轮（推荐）</span>
                </el-option>
                <el-option :value="5">
                  <span>🎯 5轮（精确）</span>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="分块处理">
              <div class="checkbox-with-tip">
                <el-checkbox v-model="extractConfig.enableChunking">
                  启用
                </el-checkbox>
                <el-tooltip 
                  content="大文档分块处理，提高兼容性但增加耗时和成本"
                  placement="top"
                >
                  <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
            </el-form-item>

            <el-button 
              type="primary"
              size="large"
              :loading="isExtracting"
              :disabled="!canStartExtraction"
              @click="startExtraction"
              class="extract-btn"
            >
              <el-icon><VideoPlay /></el-icon>
              <span>{{ isExtracting ? '提取中...' : '开始提取' }}</span>
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右列：快速统计 -->
      <el-col :span="8">
        <el-card class="stats-card">
          <template #header>
            <div class="card-header">
              <el-icon><DataAnalysis /></el-icon>
              <span>快速统计</span>
            </div>
          </template>
          
          <div class="compact-stats">
            <div class="stat-item">
              <div class="stat-icon primary">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">提取字段</div>
                <div class="stat-value">
                  {{ extractResult?.statistics?.totalFields || 0 }}
                  <span class="stat-unit">个</span>
                </div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon success">
                <el-icon><Location /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">定位准确率</div>
                <div class="stat-value">
                  {{ extractResult ? Math.round(extractResult.statistics.positionAccuracy * 100) : 0 }}
                  <span class="stat-unit">%</span>
                </div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon warning">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">平均置信度</div>
                <div class="stat-value">
                  {{ extractResult ? Math.round(extractResult.statistics.averageConfidence * 100) : 0 }}
                  <span class="stat-unit">%</span>
                </div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon info">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">OCR引擎</div>
                <div class="stat-value small">
                  {{ extractResult?.document?.ocrProvider || '-' }}
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务进度（增强版带步骤指示器） -->
    <el-card v-if="currentTask" class="progress-card mb16">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Loading /></el-icon>
            <span>提取进度</span>
          </div>
          <el-tag :type="getProgressStatus(currentTask.status) === 'success' ? 'success' : 'info'">
            {{ currentTask.message }}
          </el-tag>
        </div>
      </template>

      <!-- 步骤指示器 -->
      <div class="steps-indicator mb16">
        <el-steps :active="getStepActive(currentTask.status)" align-center>
          <el-step title="文件上传" icon="Upload" />
          <el-step title="OCR识别" icon="Document" />
          <el-step title="信息提取" icon="MagicStick" />
          <el-step title="结果生成" icon="CircleCheck" />
        </el-steps>
      </div>

      <!-- 进度条 -->
      <el-progress 
        :percentage="currentTask.progress || 0" 
        :status="getProgressStatus(currentTask.status)"
        :stroke-width="24"
      >
        <span class="progress-text">{{ currentTask.progress || 0 }}%</span>
      </el-progress>
      
      <div class="progress-meta">
        <span class="task-id">
          <el-icon><Document /></el-icon>
          任务ID: {{ currentTask.taskId }}
        </span>
        <span class="create-time">
          <el-icon><Clock /></el-icon>
          {{ formatTime(currentTask.createdAt) }}
        </span>
      </div>
    </el-card>

    <!-- 提取结果（Tabs方式展示） -->
    <el-card v-if="extractResult" class="result-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Checked /></el-icon>
            <span>提取结果</span>
            <el-tag type="success" size="small">
              {{ extractResult.extractions.items.length }} 个字段
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="openVisualization">
              <el-icon><View /></el-icon>
              可视化
            </el-button>
            <el-button size="small" @click="downloadResults">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button size="small" @click="copyResults">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </div>
      </template>

      <!-- Tabs 展示 -->
      <el-tabs v-model="activeTab" class="result-tabs">
        <!-- Tab 1: 表格视图 -->
        <el-tab-pane label="表格视图" name="table">
          <template #label>
            <span class="tab-label">
              <el-icon><Grid /></el-icon>
              表格视图
            </span>
          </template>
          
          <el-table 
            :data="extractResult.extractions.items" 
            stripe
            max-height="500"
            :header-cell-style="{ background: '#f5f7fa' }"
          >
            <el-table-column prop="field" label="字段名" width="200" fixed>
              <template #default="{ row }">
                <div class="field-name">
                  <el-icon color="#409EFF"><Key /></el-icon>
                  <span>{{ row.field }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="value" label="提取值" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="extract-value">{{ row.value }}</span>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="180">
              <template #default="{ row }">
                <el-progress 
                  :percentage="Math.round((row.confidence || 0) * 100)"
                  :stroke-width="18"
                  :color="getConfidenceColor(row.confidence)"
                >
                  <span class="progress-label">{{ Math.round((row.confidence || 0) * 100) }}%</span>
                </el-progress>
              </template>
            </el-table-column>
            <el-table-column label="字符位置" width="160" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.charInterval" type="primary" effect="light">
                  {{ row.charInterval.startPos }}-{{ row.charInterval.endPos }}
                </el-tag>
                <el-tag v-else type="info" effect="plain">未定位</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: JSON 视图 -->
        <el-tab-pane label="JSON视图" name="json">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              JSON视图
            </span>
          </template>
          
          <div class="json-viewer">
            <pre>{{ JSON.stringify(extractResult, null, 2) }}</pre>
          </div>
        </el-tab-pane>
      </el-tabs>
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
  CopyDocument,
  Document,
  Setting,
  DataAnalysis,
  Location,
  TrendCharts,
  Cpu,
  Close,
  Loading,
  Clock,
  Checked,
  Grid,
  Key,
  MagicStick,
  CircleCheck
} from '@element-plus/icons-vue'
import { PageHeader, FileUploadZone } from '@/components/common'
import { extractFromFile, getTaskStatus, getExtractResult } from '@/api/extract'

// 数据定义
const selectedFile = ref<File | null>(null)
const isExtracting = ref(false)
const currentTask = ref<any>(null)
const extractResult = ref<any>(null)
const activeTab = ref('table') // Tab 切换

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

// 获取步骤激活状态
const getStepActive = (status: string) => {
  const statusMap: Record<string, number> = {
    'uploading': 0,
    'uploaded': 1,
    'ocr_processing': 2,
    'extracting': 3,
    'completed': 4,
    'failed': 4
  }
  return statusMap[status] || 0
}

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
  padding: 0;
}

.mb12 {
  margin-bottom: 12px;
}

.mb16 {
  margin-bottom: 16px;
}

/* ========== 主操作区域 ========== */
.main-operation-area {
  margin-bottom: 16px;
}

.main-operation-area .el-card {
  height: 100%;
  min-height: 380px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.card-header .el-icon {
  font-size: 18px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* ========== 文件上传卡片 ========== */
.file-info-compact {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #303133;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #909399;
}

.file-size {
  flex: 1;
}

/* ========== 配置表单 ========== */
.compact-form {
  padding-top: 8px;
}

.compact-form .el-form-item {
  margin-bottom: 18px;
}

.compact-form .el-select {
  width: 100%;
}

.checkbox-with-tip {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip-icon {
  color: #909399;
  cursor: help;
  font-size: 16px;
}

.extract-btn {
  width: 100%;
  margin-top: 12px;
  font-size: 15px;
  height: 44px;
  font-weight: 500;
}

/* ========== 紧凑型统计卡片 ========== */
.compact-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: all 0.3s;
}

.stat-item:hover {
  background: #ebeef5;
  transform: translateX(4px);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon.success {
  background: #f0f9ff;
  color: #67c23a;
}

.stat-icon.warning {
  background: #fef6ec;
  color: #e6a23c;
}

.stat-icon.info {
  background: #f4f4f5;
  color: #909399;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-value.small {
  font-size: 18px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  color: #909399;
  margin-left: 2px;
}

/* ========== 进度卡片 ========== */
.progress-card {
  animation: fadeIn 0.3s;
}

.steps-indicator {
  margin-bottom: 24px;
}

.progress-text {
  font-size: 16px;
  font-weight: 600;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
}

.progress-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.progress-meta .el-icon {
  font-size: 14px;
  color: #909399;
}

/* ========== 结果卡片 ========== */
.result-card {
  animation: fadeIn 0.3s;
}

.result-tabs {
  margin-top: -8px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.extract-value {
  color: #303133;
}

.progress-label {
  font-size: 12px;
  font-weight: 500;
}

/* JSON 视图 */
.json-viewer {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 16px;
  max-height: 500px;
  overflow: auto;
}

.json-viewer pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}

/* ========== 动画 ========== */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 响应式设计 ========== */
@media (max-width: 1200px) {
  .main-operation-area .el-col {
    margin-bottom: 16px;
  }
  
  .main-operation-area .el-card {
    min-height: auto;
  }
}

/* ========== 滚动条美化 ========== */
.json-viewer::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.json-viewer::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 4px;
}

.json-viewer::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
