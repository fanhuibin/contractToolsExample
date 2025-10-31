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
          
          <!-- 成功提示 -->
          <div v-if="success" class="alert-box alert-success">
            <div class="alert-icon">
              <i class="fas fa-check-circle"></i>
            </div>
            <div class="alert-content">
              <h4>抽取完成！</h4>
              <p>文档信息抽取已成功完成，点击下方按钮查看详细结果</p>
            </div>
            <button @click="viewResult" class="btn-view-result">
              <i class="fas fa-eye"></i>
              查看结果
            </button>
          </div>
        </div>

        <!-- 任务历史卡片 -->
        <div class="history-card">
          <div class="card-header-section">
            <div class="header-left">
              <i class="fas fa-history header-icon"></i>
              <div class="header-text">
                <h2>任务历史</h2>
                <p>查看所有抽取任务记录</p>
              </div>
            </div>
            <button @click="loadTaskHistory" class="btn-refresh">
              <i class="fas fa-sync-alt"></i>
              <span>刷新</span>
            </button>
          </div>
          
          <div class="table-wrapper">
            <table class="modern-table">
              <thead>
                <tr>
                  <th>任务ID</th>
                  <th>文件名</th>
                  <th>模板</th>
                  <th class="text-center">状态</th>
                  <th>开始时间</th>
                  <th class="text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="taskHistory.length === 0">
                  <td colspan="6" class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>暂无历史任务</p>
                  </td>
                </tr>
                <tr v-for="task in taskHistory" :key="task.taskId">
                  <td class="task-id" :title="task.taskId">
                    <code>{{ task.taskId.substring(0, 12) }}...</code>
                  </td>
                  <td :title="task.fileName">
                    {{ task.fileName || '-' }}
                  </td>
                  <td>{{ task.templateId || '-' }}</td>
                  <td class="text-center">
                    <span class="badge-status" :class="`status-${task.status}`">
                      {{ getStatusText(task.status) }}
                    </span>
                  </td>
                  <td class="time-cell">{{ formatTime(task.createdAt) }}</td>
                  <td class="text-center action-cell">
                    <button 
                      v-if="task.status === 'completed'" 
                      @click="viewTaskResult(task.taskId)" 
                      class="btn-icon btn-primary" 
                      title="查看结果"
                    >
                      <i class="fas fa-eye"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
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

// 任务历史
const taskHistory = ref([])

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
    
    // 完成
    progress.value = 100
    statusText.value = '抽取完成！'
    extracting.value = false
    success.value = true
    
    // 刷新任务列表
    await loadTaskHistory()
    
    console.log('🎉 抽取完成！')
    
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

// 弹窗关闭回调
const onResultDialogClose = () => {
  console.log('抽取结果弹窗已关闭')
  // 可以在这里刷新任务列表或做其他清理工作
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

// 加载任务历史
const loadTaskHistory = async () => {
  try {
    console.log('🔄 加载任务历史...')
    const result = await api.getAllTasks()
    const tasks = result.data.data || result.data || []
    
    taskHistory.value = (Array.isArray(tasks) ? tasks : []).sort((a, b) => {
      return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
    })
    
    console.log('✅ 任务历史加载成功，共', taskHistory.value.length, '条记录')
  } catch (error) {
    console.error('❌ 加载任务历史失败:', error)
    taskHistory.value = []
  }
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'pending': '等待中',
    'processing': '处理中',
    'completed': '已完成',
    'failed': '失败',
    'cancelled': '已取消'
  }
  return statusMap[status] || status
}

// 组件挂载时加载数据
onMounted(() => {
  console.log('🚀 页面加载完成，开始初始化')
  loadTemplates()
  loadTaskHistory()
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
.upload-card,
.history-card {
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

.alert-success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.alert-icon {
  font-size: 32px;
}

.alert-error .alert-icon {
  color: #ff4d4f;
}

.alert-success .alert-icon {
  color: #52c41a;
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

.alert-success .alert-content h4 {
  color: #52c41a;
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

/* 表格样式 */
.table-wrapper {
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid #e8e8e8;
}

.modern-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

.modern-table thead {
  background: #fafafa;
  border-bottom: 2px solid #e0e0e0;
}

.modern-table thead th {
  padding: 24px 24px;
  text-align: left;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  border: none;
}

.modern-table thead th:first-child {
  border-top-left-radius: 16px;
  padding-left: 32px;
}

.modern-table thead th:last-child {
  border-top-right-radius: 16px;
  padding-right: 32px;
}

.modern-table thead th:nth-child(1) { width: 15%; }
.modern-table thead th:nth-child(2) { width: 25%; }
.modern-table thead th:nth-child(3) { width: 20%; }
.modern-table thead th:nth-child(4) { width: 15%; }
.modern-table thead th:nth-child(5) { width: 15%; }
.modern-table thead th:nth-child(6) { width: 10%; }

.modern-table tbody tr {
  transition: all 0.3s ease;
}

.modern-table tbody tr:hover {
  background: #f5f5f5;
}

.modern-table tbody td {
  padding: 24px;
  font-size: 15px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  line-height: 1.6;
  vertical-align: middle;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.modern-table tbody td:first-child {
  padding-left: 32px;
}

.modern-table tbody td:last-child {
  padding-right: 32px;
}

.modern-table tbody td:nth-child(2) {
  white-space: normal;
  word-break: break-word;
}

.modern-table tbody tr:last-child td {
  border-bottom: none;
}

.modern-table th.text-center,
.modern-table td.text-center {
  text-align: center !important;
  vertical-align: middle;
}

.empty-state {
  text-align: center;
  padding: 60px 20px !important;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 15px;
  margin: 0;
}

.task-id code {
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #1890ff;
  font-weight: 500;
}

.badge-status {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-processing {
  background: #e6f7ff;
  color: #1890ff;
}

.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.status-failed {
  background: #fff2f0;
  color: #ff4d4f;
}

.status-cancelled {
  background: #f5f5f5;
  color: #999;
}

.time-cell {
  font-size: 14px;
  color: #666;
  font-weight: 400;
}

.action-cell {
  white-space: nowrap !important;
  text-align: center !important;
}

.btn-icon {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  transform: translateY(-2px);
}

.btn-primary {
  background: #1890ff;
}

.btn-primary:hover {
  background: #40a9ff;
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
  
  .upload-card,
  .history-card {
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
  
  .modern-table thead th,
  .modern-table tbody td {
    padding: 16px 12px;
    font-size: 13px;
  }
  
  .modern-table thead th:first-child,
  .modern-table tbody td:first-child {
    padding-left: 16px;
  }
  
  .modern-table thead th:last-child,
  .modern-table tbody td:last-child {
    padding-right: 16px;
  }
}
</style>

