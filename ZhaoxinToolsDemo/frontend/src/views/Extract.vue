<template>
  <div id="home" class="extract-page">
    <div class="container-fluid">
      <!-- 顶部导航 -->
      <div class="top-navbar">
        <div class="navbar-content">
          <div class="logo-section">
            <div class="logo-icon">
              <i class="fas fa-file-alt"></i>
            </div>
            <div class="logo-text">
              <h1>肇新智能文档抽取</h1>
              <p>Intelligent Document Extraction</p>
            </div>
          </div>
          <div class="navbar-badge">
            <span class="badge-pro">PRO</span>
          </div>
        </div>
      </div>
      
      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 文件上传卡片 -->
        <div class="upload-card">
          <div class="card-header-section">
            <div class="header-left">
              <i class="fas fa-cloud-upload-alt header-icon"></i>
              <div class="header-text">
                <h2>文档上传与抽取</h2>
                <p>上传PDF文档，选择抽取模板，自动提取关键信息</p>
              </div>
            </div>
            <button @click="openTemplateManage" class="btn-manage">
              <i class="fas fa-cog"></i>
              <span>模板管理</span>
            </button>
          </div>
          
          <!-- 文件上传区 -->
          <div class="upload-section">
            <div class="upload-item">
              <div class="upload-label">
                <i class="fas fa-file-pdf"></i>
                <span>PDF文档</span>
              </div>
              <div 
                class="upload-zone" 
                :class="{ 'drag-active': dragOver, 'has-file': file }"
                @dragover.prevent="dragOver = true"
                @dragleave.prevent="dragOver = false"
                @drop.prevent="handleDrop"
                @click="$refs.fileInput.click()"
              >
                <div v-if="!file" class="upload-empty">
                  <div class="upload-icon-circle">
                    <i class="fas fa-cloud-upload-alt"></i>
                  </div>
                  <p class="upload-text">点击或拖拽文件到此处</p>
                  <p class="upload-hint">仅支持 PDF 格式，最大 50MB</p>
                </div>
                <div v-else class="upload-filled">
                  <div class="file-icon">
                    <i class="fas fa-file-pdf"></i>
                  </div>
                  <div class="file-info">
                    <p class="file-name">{{ file.name }}</p>
                    <p class="file-size">{{ formatFileSize(file.size) }}</p>
                  </div>
                  <button @click.stop="removeFile" class="remove-file">
                    <i class="fas fa-times"></i>
                  </button>
                </div>
              </div>
              <input 
                ref="fileInput" 
                type="file" 
                accept=".pdf" 
                @change="handleFileChange"
                style="display: none"
              />
            </div>
            
            <!-- 模板选择 -->
            <div class="template-section">
              <div class="template-label">
                <i class="fas fa-layer-group"></i>
                <span>抽取模板</span>
              </div>
              <select v-model="selectedTemplate" class="template-select" :disabled="loadingTemplates">
                <option value="">{{ loadingTemplates ? '加载中...' : '请选择抽取模板' }}</option>
                <option 
                  v-for="template in templates" 
                  :key="template.id" 
                  :value="template.id"
                >
                  {{ template.templateName }} ({{ template.templateCode }})
                </option>
              </select>
            </div>
          </div>
          
          <!-- 操作区域 -->
          <div class="action-bar">
            <div class="action-left">
              <button 
                class="btn-reset" 
                @click="resetForm"
                :disabled="extracting"
              >
                <i class="fas fa-redo"></i>
                <span>重置</span>
              </button>
            </div>
            <div class="action-right">
              <button 
                class="btn-extract" 
                :disabled="!canSubmit || extracting"
                @click="handleSubmit"
              >
                <i class="fas fa-play-circle"></i>
                <span v-if="!extracting">开始抽取</span>
                <span v-else>抽取中...</span>
              </button>
            </div>
          </div>
          
          <!-- 进度条 -->
          <div v-if="extracting" class="progress-container">
            <div class="progress-bar-wrapper">
              <div class="progress-bar-fill" :style="{ width: progress + '%' }">
                <span class="progress-text">{{ progress }}%</span>
              </div>
            </div>
            <p class="progress-status">{{ statusText }}</p>
          </div>
          
          <!-- 错误提示 -->
          <div v-if="error" class="alert-box alert-error">
            <div class="alert-icon">
              <i class="fas fa-exclamation-circle"></i>
            </div>
            <div class="alert-content">
              <h4>抽取失败</h4>
              <p>{{ errorMessage }}</p>
            </div>
            <button @click="error = false" class="alert-close">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- 成功提示及任务统计 -->
          <div v-if="success" class="result-stats-card">
            <div class="stats-header">
              <div class="header-icon-success">
                <i class="fas fa-check-circle"></i>
              </div>
              <div class="header-content">
                <h3>抽取完成！</h3>
                <p>文档信息已成功提取</p>
              </div>
            </div>
            
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-icon">
                  <i class="fas fa-clock"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-label">抽取时长</div>
                  <div class="stat-value">{{ taskDuration }}</div>
                </div>
              </div>
              
              <div class="stat-item">
                <div class="stat-icon">
                  <i class="fas fa-list-alt"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-label">提取字段</div>
                  <div class="stat-value">{{ extractedFields }} 个</div>
                </div>
              </div>
              
              <div class="stat-item">
                <div class="stat-icon">
                  <i class="fas fa-check-double"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-label">成功率</div>
                  <div class="stat-value">{{ successRate }}%</div>
                </div>
              </div>
            </div>
            
            <div class="result-actions">
              <button @click="viewResult" class="btn-view-result">
                <i class="fas fa-eye"></i>
                查看详细结果
              </button>
              <button @click="downloadResult" class="btn-download-result">
                <i class="fas fa-download"></i>
                下载结果
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 页脚 -->
      <footer class="page-footer">
        <p>肇新智能文档抽取系统 · Demo 演示 © 2025</p>
      </footer>
    </div>
    
    <!-- 抽取结果弹窗 -->
    <IframeDialog
      v-model="resultDialogVisible"
      :url="resultUrl"
      title="抽取结果"
      :fullscreen="true"
      @close="onResultDialogClose"
    />
    
    <!-- 模板管理弹窗 -->
    <IframeDialog
      v-model="templateDialogVisible"
      :url="templateManageUrl"
      title="模板管理"
      :fullscreen="true"
      @close="onTemplateDialogClose"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { pollTaskStatus, formatFileSize, formatTime } from '@/utils/extractHelper'
import IframeDialog from '@/components/IframeDialog.vue'
import { ZHAOXIN_CONFIG } from '@/config'

const router = useRouter()

// 文件状态
const file = ref(null)
const dragOver = ref(false)

// 模板选择
const selectedTemplate = ref('')
const templates = ref([])
const loadingTemplates = ref(false)

// 抽取状态
const extracting = ref(false)
const progress = ref(0)
const statusText = ref('')
const currentTaskId = ref(null)

// 结果状态
const success = ref(false)
const error = ref(false)
const errorMessage = ref('')

// 任务结果数据
const taskResult = ref(null)
const taskStartTime = ref(null)
const taskEndTime = ref(null)

// 弹窗状态
const resultDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const currentViewTaskId = ref('')

// 计算属性
const canSubmit = computed(() => {
  return file.value && selectedTemplate.value && !extracting.value
})

// iframe URL构建
const resultUrl = computed(() => {
  if (!currentViewTaskId.value) return ''
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/result/${currentViewTaskId.value}`
})

const templateManageUrl = computed(() => {
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/templates`
})

// 任务统计计算属性
const taskDuration = computed(() => {
  if (!taskStartTime.value || !taskEndTime.value) return '0秒'
  const duration = Math.round((taskEndTime.value - taskStartTime.value) / 1000)
  if (duration < 60) return `${duration}秒`
  const minutes = Math.floor(duration / 60)
  const seconds = duration % 60
  return `${minutes}分${seconds}秒`
})

const extractedFields = computed(() => {
  if (!taskResult.value) return 0
  return taskResult.value.extractedCount || 0
})

const successRate = computed(() => {
  if (!taskResult.value || !taskResult.value.totalCount) return 0
  const rate = (taskResult.value.successCount / taskResult.value.totalCount) * 100
  return Math.round(rate)
})

// 处理文件选择
const handleFileChange = (event) => {
  const selectedFile = event.target.files[0]
  if (selectedFile) {
    validateAndSetFile(selectedFile)
  }
}

// 处理拖拽上传
const handleDrop = (event) => {
  dragOver.value = false
  const droppedFile = event.dataTransfer.files[0]
  if (droppedFile) {
    validateAndSetFile(droppedFile)
  }
}

// 验证并设置文件
const validateAndSetFile = (selectedFile) => {
  // 验证文件大小 (50MB)
  const maxSize = 50 * 1024 * 1024
  if (selectedFile.size > maxSize) {
    alert('文件大小不能超过 50MB')
    return
  }
  
  // 验证文件类型 - 仅支持 PDF
  const validTypes = ['application/pdf']
  const validExtensions = ['.pdf']
  const fileName = selectedFile.name.toLowerCase()
  const hasValidExtension = validExtensions.some(ext => fileName.endsWith(ext))
  
  if (!validTypes.includes(selectedFile.type) && !hasValidExtension) {
    alert('仅支持 PDF 格式')
    return
  }
  
  file.value = selectedFile
}

// 移除文件
const removeFile = () => {
  file.value = null
}

// 提交抽取
const handleSubmit = async () => {
  if (!canSubmit.value) return
  
  try {
    // 重置状态
    extracting.value = true
    progress.value = 0
    statusText.value = '正在上传文件...'
    error.value = false
    success.value = false
    currentTaskId.value = null
    taskResult.value = null
    taskStartTime.value = Date.now()
    
    console.log('📤 开始上传文件并抽取...')
    
    // 上传文件并开始抽取
    progress.value = 10
    const result = await api.uploadAndExtract(file.value, selectedTemplate.value)
    const taskId = result.data.data.taskId || result.data.taskId
    currentTaskId.value = taskId
    
    console.log('✅ 任务提交成功，taskId:', taskId)
    
    // 轮询任务状态
    statusText.value = '正在抽取中...'
    await pollTaskStatus(taskId, (prog, status) => {
      // 进度从 10% 到 90%
      progress.value = Math.min(90, 10 + prog * 0.8)
      statusText.value = status.message || '正在抽取中...'
    })
    
    // 获取结果统计
    const taskResultData = await api.getTaskResult(taskId)
    if (taskResultData.data && taskResultData.data.data) {
      const resultInfo = taskResultData.data.data
      taskResult.value = {
        extractedCount: resultInfo.extractResults?.length || 0,
        totalCount: resultInfo.extractResults?.length || 0,
        successCount: resultInfo.extractResults?.filter(r => r.success !== false).length || 0
      }
    }
    
    // 完成
    taskEndTime.value = Date.now()
    progress.value = 100
    statusText.value = '抽取完成！'
    extracting.value = false
    success.value = true
    
    console.log('🎉 抽取完成！', taskResult.value)
    
  } catch (err) {
    console.error('❌ 抽取失败:', err)
    extracting.value = false
    error.value = true
    errorMessage.value = err.message || '未知错误'
  }
}

// 重置表单
const resetForm = () => {
  file.value = null
  selectedTemplate.value = ''
  extracting.value = false
  progress.value = 0
  statusText.value = ''
  error.value = false
  success.value = false
  currentTaskId.value = null
}

// 查看结果
const viewResult = () => {
  if (currentTaskId.value) {
    currentViewTaskId.value = currentTaskId.value
    resultDialogVisible.value = true
  }
}

// 查看任务结果
const viewTaskResult = (taskId) => {
  currentViewTaskId.value = taskId
  resultDialogVisible.value = true
}

// 打开模板管理
const openTemplateManage = () => {
  templateDialogVisible.value = true
}

// 下载结果
const downloadResult = async () => {
  if (!currentTaskId.value) return
  
  try {
    console.log('📥 下载任务结果:', currentTaskId.value)
    const response = await api.getTaskResult(currentTaskId.value)
    
    if (response.data && response.data.data) {
      const result = response.data.data
      const jsonContent = JSON.stringify(result, null, 2)
      const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `抽取结果_${currentTaskId.value.substring(0, 8)}_${Date.now()}.json`
      link.click()
      URL.revokeObjectURL(link.href)
      console.log('✅ 下载成功')
    }
  } catch (error) {
    console.error('❌ 下载失败:', error)
    alert('下载失败，请重试')
  }
}

// 弹窗关闭回调
const onResultDialogClose = () => {
  console.log('抽取结果弹窗已关闭')
}

const onTemplateDialogClose = () => {
  console.log('模板管理弹窗已关闭')
  // 刷新模板列表
  loadTemplates()
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    loadingTemplates.value = true
    console.log('📋 正在加载模板列表...')
    
    const response = await api.getTemplates()
    
    if (response.data.code === 200) {
      // 只显示已启用的模板
      const allTemplates = response.data.data || []
      templates.value = allTemplates.filter(t => 
        t.status === 'active' || t.status === 'enabled'
      )
      console.log('✅ 模板列表加载成功，共', templates.value.length, '个模板')
    } else {
      console.error('❌ 加载模板列表失败:', response.data.message)
      alert('加载模板列表失败: ' + response.data.message)
      templates.value = []
    }
  } catch (error) {
    console.error('❌ 加载模板列表异常:', error)
    alert('加载模板列表失败，请检查后端服务是否正常运行')
    templates.value = []
  } finally {
    loadingTemplates.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  console.log('🚀 页面加载完成，开始初始化')
  loadTemplates()
})
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.extract-page {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f5f5f5;
  min-height: 100vh;
  padding: 0;
}

.container-fluid {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 40px;
}

/* 顶部导航栏 */
.top-navbar {
  padding: 30px 0;
  margin-bottom: 30px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.navbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo-icon {
  width: 60px;
  height: 60px;
  background: #1890ff;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.logo-text h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.5px;
}

.logo-text p {
  font-size: 13px;
  color: #666;
  margin: 4px 0 0 0;
  letter-spacing: 0.5px;
}

.navbar-badge .badge-pro {
  display: inline-block;
  padding: 8px 20px;
  background: #1890ff;
  border-radius: 50px;
  color: white;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3);
}

/* 主内容区 */
.main-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
  margin-bottom: 30px;
}

/* 卡片通用样式 */
.upload-card {
  background: white;
  border-radius: 24px;
  padding: 48px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.card-header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 50px;
  height: 50px;
  background: #1890ff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.2);
}

.header-text h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.btn-manage {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f5f5f5;
  color: #666;
}

.btn-manage:hover {
  background: #e8e8e8;
}

/* 上传区域 */
.upload-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 30px;
  margin-bottom: 30px;
}

.upload-item {
  display: flex;
  flex-direction: column;
}

.upload-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.upload-label i {
  color: #1890ff;
}

.upload-zone {
  border: 2px dashed #e0e0e0;
  border-radius: 16px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.upload-zone:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.upload-zone.drag-active {
  border-color: #1890ff;
  background: rgba(24, 144, 255, 0.08);
  border-style: solid;
}

.upload-zone.has-file {
  border-color: #52c41a;
  background: #f6ffed;
}

.upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon-circle {
  width: 80px;
  height: 80px;
  background: #1890ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  margin-bottom: 8px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.upload-text {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.upload-hint {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.upload-filled {
  display: flex;
  align-items: center;
  gap: 16px;
}

.file-icon {
  font-size: 48px;
  color: #52c41a;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 6px 0;
  word-break: break-all;
}

.file-size {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.remove-file {
  width: 36px;
  height: 36px;
  background: #ff4d4f;
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file:hover {
  background: #ff7875;
  transform: scale(1.1);
}

/* 模板选择 */
.template-section {
  display: flex;
  flex-direction: column;
}

.template-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.template-label i {
  color: #1890ff;
}

.template-select {
  width: 100%;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
  font-family: inherit;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.template-select:hover,
.template-select:focus {
  border-color: #1890ff;
  outline: none;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0;
  border-top: 2px solid #f0f0f0;
}

.action-left,
.action-right {
  display: flex;
  gap: 16px;
  align-items: center;
}

.btn-reset,
.btn-extract,
.btn-refresh,
.btn-view-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-reset {
  background: #f5f5f5;
  color: #666;
}

.btn-reset:hover:not(:disabled) {
  background: #e8e8e8;
}

.btn-extract {
  background: #1890ff;
  color: white;
}

.btn-extract:hover:not(:disabled) {
  background: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.35);
}

.btn-extract:disabled,
.btn-reset:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-refresh {
  background: #f0f0f0;
  color: #666;
  padding: 10px 20px;
  font-size: 14px;
}

.btn-refresh:hover {
  background: #e0e0e0;
}

.btn-view-result {
  background: #52c41a;
  color: white;
}

.btn-view-result:hover {
  background: #73d13d;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(82, 196, 26, 0.35);
}

/* 进度条 */
.progress-container {
  margin-top: 30px;
  padding-top: 24px;
  border-top: 2px solid #f0f0f0;
}

.progress-bar-wrapper {
  height: 40px;
  background: #f0f0f0;
  border-radius: 20px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: #1890ff;
  border-radius: 20px;
  transition: width 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 16px;
  position: relative;
  overflow: hidden;
}

.progress-bar-fill::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, 
    transparent, 
    rgba(255, 255, 255, 0.25), 
    transparent
  );
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-text {
  color: white;
  font-weight: 700;
  font-size: 16px;
  position: relative;
  z-index: 1;
}

.progress-status {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: #666;
}

/* 警告框 */
.alert-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  margin-top: 24px;
}

.alert-error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
}

.alert-icon {
  font-size: 32px;
}

.alert-error .alert-icon {
  color: #ff4d4f;
}

.alert-content {
  flex: 1;
}

.alert-content h4 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px 0;
}

.alert-error .alert-content h4 {
  color: #ff4d4f;
}

.alert-content p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.alert-close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  color: #999;
  font-size: 18px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.alert-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

/* 任务统计卡片 */
.result-stats-card {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
  border: 2px solid #52c41a;
  border-radius: 16px;
  padding: 32px;
  margin-top: 24px;
}

.stats-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid rgba(82, 196, 26, 0.2);
}

.header-icon-success {
  width: 64px;
  height: 64px;
  background: #52c41a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  box-shadow: 0 8px 20px rgba(82, 196, 26, 0.3);
}

.header-content h3 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
  color: #52c41a;
}

.header-content p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-item {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1890ff 0%, #52c41a 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.result-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.btn-download-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #1890ff;
  color: white;
}

.btn-download-result:hover {
  background: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.35);
}


/* 页脚 */
.page-footer {
  text-align: center;
  padding: 30px 0;
  color: #999;
  font-size: 14px;
  background: white;
  margin-top: 30px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .upload-section {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .container-fluid {
    padding: 0 20px;
  }
  
  .upload-card {
    padding: 24px;
  }
  
  .card-header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .action-bar {
    flex-direction: column;
    gap: 16px;
  }
  
  .action-left,
  .action-right {
    width: 100%;
    justify-content: center;
  }
  
  .logo-text h1 {
    font-size: 22px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .result-actions {
    flex-direction: column;
  }
  
  .btn-view-result,
  .btn-download-result {
    width: 100%;
    justify-content: center;
  }
}
</style>

