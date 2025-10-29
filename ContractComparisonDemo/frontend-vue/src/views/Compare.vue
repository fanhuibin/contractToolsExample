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
                  <p class="upload-hint">支持 PDF格式，最大 50MB</p>
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
                accept=".pdf,.doc,.docx" 
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
                  <p class="upload-hint">支持 PDF格式，最大 50MB</p>
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
                accept=".pdf,.doc,.docx" 
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
          
          <!-- 成功提示 -->
          <div v-if="success" class="alert-box alert-success">
            <div class="alert-icon">
              <i class="fas fa-check-circle"></i>
            </div>
            <div class="alert-content">
              <h4>比对完成！</h4>
              <p>文档比对已成功完成，点击下方按钮查看详细结果</p>
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
                <p>查看所有比对任务记录</p>
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
                  <th>原文档</th>
                  <th>新文档</th>
                  <th class="text-center">差异数</th>
                  <th>开始时间</th>
                  <th>完成时间</th>
                  <th class="text-center">时长</th>
                  <th class="text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="taskHistory.length === 0">
                  <td colspan="8" class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>暂无历史任务</p>
                  </td>
                </tr>
                <tr v-for="task in taskHistory" :key="task.taskId">
                  <td class="task-id" :title="task.taskId">
                    <code>{{ task.taskId.substring(0, 12) }}...</code>
                  </td>
                  <td :title="task.oldFileName">
                    {{ task.oldFileName || '-' }}
                  </td>
                  <td :title="task.newFileName">
                    {{ task.newFileName || '-' }}
                  </td>
                  <td class="text-center">
                    <span class="badge-count">{{ getDifferencesCount(task) }}</span>
                  </td>
                  <td class="time-cell">{{ formatTime(task.startTime) }}</td>
                  <td class="time-cell">{{ formatTime(task.endTime) }}</td>
                  <td class="text-center duration-cell">{{ getProcessingDuration(task) }}</td>
                  <td class="text-center action-cell">
                    <button 
                      v-if="task.resultUrl" 
                      @click="viewTaskResult(task.taskId)" 
                      class="btn-icon btn-primary" 
                      title="查看结果"
                    >
                      <i class="fas fa-eye"></i>
                    </button>
                    <button 
                      v-if="task.resultUrl" 
                      @click="downloadTaskResult(task.taskId)" 
                      class="btn-icon btn-success" 
                      title="下载结果"
                    >
                      <i class="fas fa-download"></i>
                    </button>
                    <button 
                      @click="deleteTaskItem(task.taskId)" 
                      class="btn-icon btn-danger" 
                      title="删除任务"
                    >
                      <i class="fas fa-trash-alt"></i>
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
        <p>肇新智能文档比对系统 · Demo 演示 © 2025</p>
      </footer>
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
const oldFileName = ref('') // 保存原始文件名
const newFileName = ref('') // 保存原始文件名
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

// 任务历史
const taskHistory = ref([])

// 计算属性
const canSubmit = computed(() => {
  return oldFile.value && newFile.value && !comparing.value
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
  
  // 验证文件类型
  const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!validTypes.includes(file.type)) {
    alert('只支持 PDF 格式')
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
  oldFileName.value = ''
}

const removeNewFile = () => {
  newFile.value = null
  newFileName.value = ''
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
    
    console.log('📤 开始上传文件...')
    
    // 1. 上传旧文件
    progress.value = 5
    const oldFileResult = await api.uploadFile(oldFile.value)
    const oldFileUrl = oldFileResult.data.data.fileUrl
    oldFileName.value = oldFileResult.data.data.originalName // 保存原始文件名
    console.log('✅ 旧文件上传成功:', oldFileUrl, '原始文件名:', oldFileName.value)
    
    // 2. 上传新文件
    progress.value = 15
    statusText.value = '正在上传新文件...'
    const newFileResult = await api.uploadFile(newFile.value)
    const newFileUrl = newFileResult.data.data.fileUrl
    newFileName.value = newFileResult.data.data.originalName // 保存原始文件名
    console.log('✅ 新文件上传成功:', newFileUrl, '原始文件名:', newFileName.value)
    
    // 3. 提交比对任务（传递文件URL和原始文件名）
    progress.value = 20
    statusText.value = '正在提交比对任务...'
    const result = await api.submitCompare(
      oldFileUrl, 
      newFileUrl, 
      removeWatermark.value,
      oldFileName.value,
      newFileName.value
    )
    const taskId = result.data
    currentTaskId.value = taskId
    
    console.log('✅ 任务提交成功，taskId:', taskId)
    
    // 4. 轮询任务状态
    statusText.value = '正在比对中...'
    await pollTaskStatus(taskId, (prog, status) => {
      // 进度从 20% 到 90%
      progress.value = Math.min(90, 20 + prog * 0.7)
      statusText.value = status.statusMessage || '正在比对中...'
    })
    
    // 5. 完成
    progress.value = 100
    statusText.value = '比对完成！'
    comparing.value = false
    success.value = true
    
    console.log('🎉 比对完成！')
    
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
}

// 查看结果
const viewResult = () => {
  if (currentTaskId.value) {
    // 刷新任务历史（任务已经在后端记录了）
    refreshTaskHistory()
    // 在新窗口打开结果页
    const resultUrl = `${window.location.origin}/result/${currentTaskId.value}`
    window.open(resultUrl, '_blank')
  }
}

// 查看任务结果
const viewTaskResult = (taskId) => {
  const resultUrl = `${window.location.origin}/result/${taskId}`
  window.open(resultUrl, '_blank')
}

// 加载任务历史
const loadTaskHistory = async () => {
  try {
    console.log('🔄 加载任务历史...')
    const result = await api.getAllTasks()
    console.log('📊 任务历史数据:', result)
    
    // result.data 是任务数组
    taskHistory.value = (result.data || []).sort((a, b) => {
      return new Date(b.startTime || 0).getTime() - new Date(a.startTime || 0).getTime()
    })
    
    console.log('✅ 任务历史加载成功，共', taskHistory.value.length, '条记录')
  } catch (error) {
    console.error('❌ 加载任务历史失败:', error)
    // 失败时显示空列表
    taskHistory.value = []
  }
}

// 删除任务
const deleteTaskItem = async (taskId) => {
  if (confirm('确定要删除这个任务吗？')) {
    try {
      console.log('🗑️ 删除任务:', taskId)
      await api.deleteTask(taskId)
      console.log('✅ 任务删除成功')
      
      // 重新加载任务列表
      await loadTaskHistory()
    } catch (error) {
      console.error('❌ 删除任务失败:', error)
      alert('删除任务失败: ' + error.message)
    }
  }
}

// 格式化时间
const formatTime = (timeString) => {
  if (!timeString) return '-'
  const date = new Date(timeString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取差异总数
const getDifferencesCount = (task) => {
  // 使用 differenceCount 字段（不带s）
  if (task.differenceCount !== null && task.differenceCount !== undefined) {
    return task.differenceCount.toString()
  }
  return '-'
}

// 计算分析时长
const getProcessingDuration = (task) => {
  if (!task.startTime || !task.endTime) {
    return '-'
  }
  
  const startTime = new Date(task.startTime || 0).getTime()
  const endTime = new Date(task.endTime || 0).getTime()
  const durationMs = endTime - startTime
  
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
}

// 下载任务结果
const downloadTaskResult = async (taskId) => {
  try {
    console.log('📥 下载任务结果:', taskId)
    
    // 调用导出API，同时导出 doc 和 html 格式（打包成 zip）
    const response = await api.exportReport(taskId, ['doc', 'html'])
    
    // 创建 Blob 对象
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/zip'
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `比对报告_${taskId}.zip`  // 多格式导出为 zip
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

// 完成任务后刷新任务列表（不再需要手动保存，后端会自动记录）
const refreshTaskHistory = async () => {
  try {
    await loadTaskHistory()
  } catch (error) {
    console.error('刷新任务历史失败:', error)
  }
}

// 组件挂载时加载任务历史
onMounted(() => {
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

.compare-page {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f5f5f5;
  min-height: 100vh;
  padding: 0;
}

.container-fluid {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 30px;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50px;
  color: white;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
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
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
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
  grid-template-columns: 1fr auto 1fr;
  gap: 30px;
  margin-bottom: 30px;
  align-items: center;
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
  color: #667eea;
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
  border-color: #667eea;
  background: #f8f9ff;
}

.upload-zone.drag-active {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  margin-bottom: 8px;
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

.compare-arrow {
  font-size: 32px;
  color: #667eea;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-compare:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
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
  background: #52c41a;
  color: white;
}

.btn-view-result:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(82, 196, 26, 0.4);
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
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
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
    rgba(255, 255, 255, 0.3), 
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
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.modern-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.modern-table thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.modern-table thead th {
  padding: 18px 16px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: white;
  border: none;
}

.modern-table thead th:first-child {
  border-top-left-radius: 12px;
}

.modern-table thead th:last-child {
  border-top-right-radius: 12px;
}

.modern-table tbody tr {
  transition: all 0.3s ease;
}

.modern-table tbody tr:hover {
  background: #fafafa;
}

.modern-table tbody td {
  padding: 16px;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.modern-table tbody tr:last-child td {
  border-bottom: none;
}

.modern-table tbody tr:last-child td:first-child {
  border-bottom-left-radius: 12px;
}

.modern-table tbody tr:last-child td:last-child {
  border-bottom-right-radius: 12px;
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
  padding: 4px 8px;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #667eea;
}


.badge-count {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.time-cell,
.duration-cell {
  font-size: 13px;
  color: #666;
}

.text-center {
  text-align: center;
}

.action-cell {
  white-space: nowrap;
}

.action-cell .btn-icon {
  margin: 0 4px;
}

.btn-icon {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  transform: translateY(-2px);
}

.btn-primary {
  background: #667eea;
}

.btn-primary:hover {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-success {
  background: #52c41a;
}

.btn-success:hover {
  box-shadow: 0 6px 16px rgba(82, 196, 26, 0.4);
}

.btn-danger {
  background: #ff4d4f;
}

.btn-danger:hover {
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.4);
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
  .upload-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .compare-arrow {
    display: none;
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
}
</style>
