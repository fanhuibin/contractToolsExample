<template>
  <div id="home" class="compare-page">
    <div class="container-fluid">
      <!-- 顶部导航 -->
      <div class="top-navbar">
        <div class="navbar-content">
          <div class="logo-section">
            <div class="logo-icon">
              <i class="fas fa-file-contract"></i>
            </div>
            <div class="logo-text">
              <h1>肇新智能比对</h1>
              <p>Intelligent Document Comparison</p>
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
                <h2>文件上传</h2>
                <p>上传两份文档进行智能比对分析</p>
              </div>
            </div>
          </div>
          
          <div class="upload-grid">
            <!-- 旧文件上传 -->
            <div class="upload-item">
              <div class="upload-label">
                <i class="fas fa-file-alt"></i>
                <span>基准文件（旧版本）</span>
              </div>
              <div 
                class="upload-zone" 
                :class="{ 'drag-active': dragOverOld, 'has-file': oldFile }"
                @dragover.prevent="dragOverOld = true"
                @dragleave.prevent="dragOverOld = false"
                @drop.prevent="handleDropOld"
                @click="$refs.oldFileInput.click()"
              >
                <div v-if="!oldFile" class="upload-empty">
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
                    <p class="file-name">{{ oldFile.name }}</p>
                    <p class="file-size">{{ formatFileSize(oldFile.size) }}</p>
                  </div>
                  <button @click.stop="removeOldFile" class="remove-file">
                    <i class="fas fa-times"></i>
                  </button>
                </div>
              </div>
              <input 
                ref="oldFileInput" 
                type="file" 
                accept=".pdf" 
                @change="handleOldFileChange"
                style="display: none"
              />
            </div>
            
            <!-- 比对箭头 -->
            <div class="compare-arrow">
              <i class="fas fa-exchange-alt"></i>
            </div>
            
            <!-- 新文件上传 -->
            <div class="upload-item">
              <div class="upload-label">
                <i class="fas fa-file-alt"></i>
                <span>比对文件（新版本）</span>
              </div>
              <div 
                class="upload-zone"
                :class="{ 'drag-active': dragOverNew, 'has-file': newFile }"
                @dragover.prevent="dragOverNew = true"
                @dragleave.prevent="dragOverNew = false"
                @drop.prevent="handleDropNew"
                @click="$refs.newFileInput.click()"
              >
                <div v-if="!newFile" class="upload-empty">
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
                    <p class="file-name">{{ newFile.name }}</p>
                    <p class="file-size">{{ formatFileSize(newFile.size) }}</p>
                  </div>
                  <button @click.stop="removeNewFile" class="remove-file">
                    <i class="fas fa-times"></i>
                  </button>
                </div>
              </div>
              <input 
                ref="newFileInput" 
                type="file" 
                accept=".pdf" 
                @change="handleNewFileChange"
                style="display: none"
              />
            </div>
          </div>
          
          <!-- 操作区域 -->
          <div class="action-bar">
            <div class="action-left">
              <label class="custom-checkbox">
                <input type="checkbox" v-model="removeWatermark" />
                <span class="checkmark"></span>
                <span class="checkbox-text">去除水印</span>
              </label>
            </div>
            <div class="action-right">
              <button 
                class="btn-reset" 
                @click="resetForm"
                :disabled="comparing"
              >
                <i class="fas fa-redo"></i>
                <span>重置</span>
              </button>
              <button 
                class="btn-compare" 
                :disabled="!canSubmit || comparing"
                @click="handleSubmit"
              >
                <i class="fas fa-play-circle"></i>
                <span v-if="!comparing">开始比对</span>
                <span v-else>比对中...</span>
              </button>
            </div>
          </div>
          
          <!-- 进度条 -->
          <div v-if="comparing" class="progress-container">
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
              <h4>比对失败</h4>
              <p>{{ errorMessage }}</p>
            </div>
            <button @click="error = false" class="alert-close">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- 成功提示及任务统计 -->
          <div v-if="success" class="comparison-stats-card">
            <div class="stats-grid-compact">
              <!-- 比对时长卡片 -->
              <div class="stat-card">
                <div class="stat-icon">
                  <i class="fas fa-clock"></i>
                </div>
                <div class="stat-text">比对时长：{{ taskDuration }}</div>
              </div>
              
              <!-- 差异数量卡片 -->
              <div class="stat-card">
                <div class="stat-icon">
                  <i class="fas fa-exchange-alt"></i>
                </div>
                <div class="stat-text">差异数量：{{ taskDifferenceCount }} 处</div>
              </div>
              
              <!-- 查看详细结果卡片 -->
              <div class="stat-card stat-card-action" @click="viewResult">
                <div class="stat-icon">
                  <i class="fas fa-eye"></i>
                </div>
                <div class="stat-text">查看详细结果</div>
              </div>
              
              <!-- 下载比对结果卡片 -->
              <div class="stat-card stat-card-action" @click="downloadComparisonResult">
                <div class="stat-icon">
                  <i class="fas fa-download"></i>
                </div>
                <div class="stat-text">下载比对结果</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { pollTaskStatus } from '@/utils/compareHelper'

const router = useRouter()

// 文件状态
const oldFile = ref(null)
const newFile = ref(null)
const dragOverOld = ref(false)
const dragOverNew = ref(false)

// 选项
const removeWatermark = ref(false)

// 比对状态
const comparing = ref(false)
const progress = ref(0)
const statusText = ref('')
const currentTaskId = ref(null)

// 结果状态
const success = ref(false)
const error = ref(false)
const errorMessage = ref('')

// 任务统计数据（使用 API 返回的真实数据）
const taskStartTime = ref(null)  // ISO 时间字符串
const taskEndTime = ref(null)    // ISO 时间字符串
const taskDifferenceCount = ref(0)

// 计算属性
const canSubmit = computed(() => {
  return oldFile.value && newFile.value && !comparing.value
})

// 计算任务时长（使用 API 返回的时间）
const taskDuration = computed(() => {
  if (!taskStartTime.value || !taskEndTime.value) return '-'
  
  // 将 ISO 时间字符串转为时间戳
  const startMs = new Date(taskStartTime.value).getTime()
  const endMs = new Date(taskEndTime.value).getTime()
  const durationMs = endMs - startMs
  
  if (durationMs < 1000) {
    return '<1秒'
  } else if (durationMs < 60000) {
    return `${Math.round(durationMs / 1000)}秒`
  } else if (durationMs < 3600000) {
    const minutes = Math.floor(durationMs / 60000)
    const seconds = Math.round((durationMs % 60000) / 1000)
    return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分钟`
  } else {
    const hours = Math.floor(durationMs / 3600000)
    const minutes = Math.round((durationMs % 3600000) / 60000)
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  }
})

// 处理旧文件选择
const handleOldFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    validateAndSetFile(file, 'old')
  }
}

// 处理新文件选择
const handleNewFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    validateAndSetFile(file, 'new')
  }
}

// 处理拖拽上传 - 旧文件
const handleDropOld = (event) => {
  dragOverOld.value = false
  const file = event.dataTransfer.files[0]
  if (file) {
    validateAndSetFile(file, 'old')
  }
}

// 处理拖拽上传 - 新文件
const handleDropNew = (event) => {
  dragOverNew.value = false
  const file = event.dataTransfer.files[0]
  if (file) {
    validateAndSetFile(file, 'new')
  }
}

// 验证并设置文件
const validateAndSetFile = (file, type) => {
  // 验证文件大小 (50MB)
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    alert('文件大小不能超过 50MB')
    return
  }
  
  // 验证文件类型 - 仅支持 PDF
  const validTypes = ['application/pdf']
  const validExtensions = ['.pdf']
  const fileName = file.name.toLowerCase()
  const hasValidExtension = validExtensions.some(ext => fileName.endsWith(ext))
  
  if (!validTypes.includes(file.type) && !hasValidExtension) {
    alert('仅支持 PDF 格式')
    return
  }
  
  if (type === 'old') {
    oldFile.value = file
  } else {
    newFile.value = file
  }
}

// 移除文件
const removeOldFile = () => {
  oldFile.value = null
}

const removeNewFile = () => {
  newFile.value = null
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 提交比对
const handleSubmit = async () => {
  if (!canSubmit.value) return
  
  try {
    // 重置状态
    comparing.value = true
    progress.value = 0
    statusText.value = '正在上传文件...'
    error.value = false
    success.value = false
    currentTaskId.value = null
    taskStartTime.value = null
    taskEndTime.value = null
    taskDifferenceCount.value = 0
    
    console.log('📤 开始上传文件...')
    
    // 保存原始文件名（从File对象直接获取）
    const originalOldFileName = oldFile.value.name
    const originalNewFileName = newFile.value.name
    
    // 1. 上传旧文件
    progress.value = 5
    const oldFileResult = await api.uploadFile(oldFile.value)
    const oldFileUrl = oldFileResult.data.data.fileUrl
    console.log('✅ 旧文件上传成功:', oldFileUrl, '原始文件名:', originalOldFileName)
    
    // 2. 上传新文件
    progress.value = 15
    statusText.value = '正在上传新文件...'
    const newFileResult = await api.uploadFile(newFile.value)
    const newFileUrl = newFileResult.data.data.fileUrl
    console.log('✅ 新文件上传成功:', newFileUrl, '原始文件名:', originalNewFileName)
    
    // 3. 提交比对任务（传递文件URL和原始文件名到Demo后端）
    progress.value = 20
    statusText.value = '正在提交比对任务...'
    const result = await api.submitCompare(
      oldFileUrl, 
      newFileUrl, 
      removeWatermark.value,
      originalOldFileName,
      originalNewFileName
    )
    const taskId = result.data
    currentTaskId.value = taskId
    
    console.log('✅ 任务提交成功，taskId:', taskId)
    console.log('📝 原始文件名已发送到后端: oldFileName={}, newFileName={}', originalOldFileName, originalNewFileName)
    
    // 4. 轮询任务状态
    statusText.value = '正在比对中...'
    await pollTaskStatus(taskId, (prog, status) => {
      // 进度从 20% 到 90%
      progress.value = Math.min(90, 20 + prog * 0.7)
      statusText.value = status.statusMessage || '正在比对中...'
    })
    
    // 5. 获取任务统计信息（时间和差异数量）
    progress.value = 95
    statusText.value = '正在获取结果统计...'
    
    try {
      // 5.1 从任务状态获取时间信息
      const taskStatus = await api.getTaskStatus(taskId)
      if (taskStatus.data) {
        taskStartTime.value = taskStatus.data.startTime
        taskEndTime.value = taskStatus.data.endTime
        console.log('✅ 获取时间信息:', {
          startTime: taskStartTime.value,
          endTime: taskEndTime.value
        })
      }
      
      // 5.2 从 Canvas 结果获取差异数量
      const canvasResult = await api.getResult(taskId)
      if (canvasResult.data && canvasResult.data.differences) {
        taskDifferenceCount.value = canvasResult.data.differences.length
        console.log('✅ 获取差异数量:', taskDifferenceCount.value)
      }
    } catch (err) {
      console.warn('⚠️ 获取任务统计失败:', err)
      // 不影响主流程，继续执行
    }
    
    // 6. 完成
    progress.value = 100
    statusText.value = '比对完成！'
    comparing.value = false
    success.value = true
    
    console.log('🎉 比对完成！', {
      startTime: taskStartTime.value,
      endTime: taskEndTime.value,
      duration: taskDuration.value,
      differences: taskDifferenceCount.value
    })
    
  } catch (err) {
    console.error('❌ 比对失败:', err)
    comparing.value = false
    error.value = true
    errorMessage.value = err.message || '未知错误'
  }
}

// 重置表单
const resetForm = () => {
  oldFile.value = null
  newFile.value = null
  removeWatermark.value = false
  comparing.value = false
  progress.value = 0
  statusText.value = ''
  error.value = false
  success.value = false
  currentTaskId.value = null
  taskStartTime.value = null
  taskEndTime.value = null
  taskDifferenceCount.value = 0
}

// 查看结果
const viewResult = () => {
  if (currentTaskId.value) {
    // 在新窗口打开结果页
    const resultUrl = `${window.location.origin}/result/${currentTaskId.value}`
    window.open(resultUrl, '_blank')
  }
}

// 下载比对结果
const downloadComparisonResult = async () => {
  if (!currentTaskId.value) return
  
  try {
    console.log('📥 下载比对结果:', currentTaskId.value)
    
    // 调用导出API，同时导出 doc 和 html 格式（打包成 zip）
    const response = await api.exportReport(currentTaskId.value, ['doc', 'html'])
    
    // 创建 Blob 对象
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/zip'
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `comparison-result-${currentTaskId.value}.zip`
    document.body.appendChild(link)
    link.click()
    
    // 清理
    window.URL.revokeObjectURL(url)
    document.body.removeChild(link)
    
    console.log('✅ 下载成功')
  } catch (error) {
    console.error('❌ 下载失败:', error)
    alert('下载失败: ' + error.message)
  }
}
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.compare-page {
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
  width: 100%;
}

/* 卡片通用样式 */
.upload-card,
.history-card {
  background: white;
  border-radius: 24px;
  padding: 48px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  width: 100%;
  max-width: 100%;
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

/* 上传区域 */
.upload-grid {
  display: grid;
  grid-template-columns: minmax(300px, 450px) auto minmax(300px, 450px);
  gap: 30px;
  margin-bottom: 30px;
  align-items: center;
  justify-content: center;
}

.upload-item {
  display: flex;
  flex-direction: column;
  max-width: 450px;
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
  border-color: #1890ff;
  background: #fafafa;
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
  color: #1890ff;
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

.compare-arrow {
  font-size: 32px;
  color: #667eea;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 24px 0;
  border-top: 2px solid #f0f0f0;
}

.action-left,
.action-right {
  display: flex;
  gap: 16px;
  align-items: center;
}

/* 自定义复选框 */
.custom-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  padding-left: 35px;
  user-select: none;
}

.custom-checkbox input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
}

.checkmark {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  height: 22px;
  width: 22px;
  background-color: #fff;
  border: 2px solid #d9d9d9;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.custom-checkbox:hover .checkmark {
  border-color: #667eea;
}

.custom-checkbox input:checked ~ .checkmark {
  background-color: #667eea;
  border-color: #667eea;
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 6px;
  top: 2px;
  width: 6px;
  height: 11px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.custom-checkbox input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-text {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
}

/* 按钮样式 */
.btn-reset,
.btn-compare,
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

.btn-compare {
  background: #1890ff;
  color: white;
}

.btn-compare:hover:not(:disabled) {
  background: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.35);
}

.btn-compare:disabled,
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
  background: #1890ff;
  color: white;
}

.btn-view-result:hover {
  background: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(24, 144, 255, 0.4);
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
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.alert-icon {
  font-size: 32px;
}

.alert-error .alert-icon {
  color: #ff4d4f;
}

.alert-success .alert-icon {
  color: #1890ff;
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
  color: #1890ff;
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

/* 比对统计卡片 */
.comparison-stats-card {
  margin-top: 24px;
}

.stats-grid-compact {
  display: grid;
  grid-template-columns: repeat(4, 163px);
  gap: 16px;
  justify-content: center;
}

.stat-card {
  width: 163px;
  height: 100px;
  background: white;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #1890ff;
}

.stat-card .stat-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}

.stat-card .stat-text {
  font-size: 14px;
  color: #1a1a1a;
  text-align: center;
  font-weight: 500;
  line-height: 1.5;
}


/* 响应式设计 */
@media (max-width: 1024px) {
  .upload-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .compare-arrow {
    display: none;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
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
  
  /* 统计卡片移动端样式 */
  .stats-grid-compact {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .stat-card {
    width: 100%;
    height: 100px;
  }
}
</style>
