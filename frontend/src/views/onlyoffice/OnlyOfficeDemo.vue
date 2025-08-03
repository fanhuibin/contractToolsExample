<template>
  <div class="onlyoffice-demo">
    <el-card class="demo-header">
      <template #header>
        <div class="card-header">
          <span>OnlyOffice 文档编辑器演示</span>
          <el-space>
            <el-tooltip content="健康检查">
              <el-button 
                circle 
                :icon="Connection" 
                :loading="healthChecking"
                @click="checkHealth"
              />
            </el-tooltip>
            <el-tooltip content="刷新页面">
              <el-button 
                circle 
                :icon="Refresh" 
                @click="refreshPage"
              />
            </el-tooltip>
          </el-space>
        </div>
      </template>

      <!-- 文件选择区域 -->
      <div class="demo-controls">
        <el-row :gutter="16" align="middle">
          <el-col :span="8">
            <el-form-item label="选择文件:">
              <el-select 
                v-model="selectedFileId" 
                placeholder="请选择要编辑的文件"
                style="width: 100%"
                @change="handleFileChange"
              >
                <el-option
                  v-for="file in demoFiles"
                  :key="file.id"
                  :label="file.name"
                  :value="file.id"
                >
                  <span>{{ file.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">
                    {{ file.type }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          
          <el-col :span="6">
            <el-form-item label="编辑权限:">
              <el-switch
                v-model="canEdit"
                active-text="可编辑"
                inactive-text="只读"
                @change="handlePermissionChange"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="6">
            <el-form-item label="审阅权限:">
              <el-switch
                v-model="canReview"
                active-text="可审阅"
                inactive-text="不可审阅"
                :disabled="!canEdit"
                @change="handlePermissionChange"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="4">
            <el-button 
              type="primary" 
              :disabled="!selectedFileId"
              :loading="loading"
              @click="loadEditor"
            >
              加载编辑器
            </el-button>
          </el-col>
        </el-row>

        <!-- 文件上传区域 -->
        <el-divider content-position="left">或上传新文件</el-divider>
        <el-upload
          class="upload-demo"
          drag
          :action="uploadUrl"
          :headers="uploadHeaders"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :before-upload="beforeUpload"
          accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 doc/docx/xls/xlsx/ppt/pptx/pdf 格式文件，且不超过100MB
            </div>
          </template>
        </el-upload>
      </div>
    </el-card>

    <!-- 编辑器区域 -->
    <el-card v-if="showEditor" class="editor-card">
      <OnlyOfficeEditor
        ref="editorRef"
        :file-id="selectedFileId"
        :can-edit="canEdit"
        :can-review="canReview"
        :height="editorHeight"
        :show-toolbar="true"
        :show-status="true"
        watermark-text="演示文档"
        @ready="handleEditorReady"
        @documentStateChange="handleDocumentStateChange"
        @error="handleEditorError"
        @save="handleEditorSave"
        @warning="handleEditorWarning"
      />
    </el-card>

    <!-- 状态信息 -->
    <el-card v-if="showEditor" class="status-card">
      <template #header>
        <span>状态信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件ID">{{ selectedFileId }}</el-descriptions-item>
        <el-descriptions-item label="编辑模式">
          <el-tag :type="canEdit ? 'success' : 'info'">
            {{ canEdit ? '编辑' : '只读' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审阅权限">
          <el-tag :type="canReview ? 'warning' : 'info'">
            {{ canReview ? '可审阅' : '不可审阅' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编辑器状态">
          <el-tag :type="editorReady ? 'success' : 'danger'">
            {{ editorReady ? '已就绪' : '未就绪' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文档状态" :span="2">
          <el-tag v-if="documentState" :type="getDocumentStateType(documentState)">
            {{ getDocumentStateText(documentState) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作日志 -->
    <el-card v-if="showEditor && logs.length > 0" class="log-card">
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-button size="small" @click="clearLogs">清空</el-button>
        </div>
      </template>
      <div class="log-container">
        <div
          v-for="(log, index) in logs"
          :key="index"
          class="log-item"
          :class="log.type"
        >
          <span class="log-time">{{ log.time }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Refresh, UploadFilled } from '@element-plus/icons-vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { healthCheck } from '@/api/onlyoffice'
import { getToken } from '@/utils/auth'

// 响应式数据
const selectedFileId = ref('')
const canEdit = ref(true)
const canReview = ref(false)
const showEditor = ref(false)
const loading = ref(false)
const healthChecking = ref(false)
const editorReady = ref(false)
const documentState = ref(null)
const logs = ref([])
const editorRef = ref(null)

// 演示文件数据
const demoFiles = ref([
  { id: '1', name: '示例文档.docx', type: 'Word文档' },
  { id: '2', name: '数据表格.xlsx', type: 'Excel表格' },
  { id: '3', name: '演示文稿.pptx', type: 'PowerPoint演示文稿' },
  { id: '4', name: '技术文档.pdf', type: 'PDF文档' }
])

// 计算属性
const editorHeight = computed(() => 'calc(100vh - 500px)')
const uploadUrl = computed(() => `${process.env.VUE_APP_BASE_API}/file/upload`)
const uploadHeaders = computed(() => ({
  'Authorization': 'Bearer ' + getToken()
}))

// 生命周期
onMounted(() => {
  addLog('info', '页面已加载，请选择文件开始演示')
})

// 方法定义
const handleFileChange = () => {
  if (selectedFileId.value) {
    const file = demoFiles.value.find(f => f.id === selectedFileId.value)
    addLog('info', `选择了文件: ${file?.name}`)
  }
}

const handlePermissionChange = () => {
  if (!canEdit.value) {
    canReview.value = false
  }
  addLog('info', `权限设置: 编辑=${canEdit.value}, 审阅=${canReview.value}`)
}

const loadEditor = () => {
  if (!selectedFileId.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  loading.value = true
  showEditor.value = true
  addLog('info', '开始加载编辑器...')
  
  // 模拟加载延迟
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const checkHealth = async () => {
  healthChecking.value = true
  try {
    await healthCheck()
    ElMessage.success('OnlyOffice 服务正常')
    addLog('success', '服务健康检查通过')
  } catch (error) {
    ElMessage.error('OnlyOffice 服务异常: ' + error.message)
    addLog('error', `服务健康检查失败: ${error.message}`)
  } finally {
    healthChecking.value = false
  }
}

const refreshPage = () => {
  location.reload()
}

// 编辑器事件处理
const handleEditorReady = () => {
  editorReady.value = true
  addLog('success', '编辑器已就绪')
  ElMessage.success('文档编辑器加载完成')
}

const handleDocumentStateChange = (event) => {
  documentState.value = event.data
  addLog('info', `文档状态变更: ${getDocumentStateText(event.data)}`)
}

const handleEditorError = (error) => {
  addLog('error', `编辑器错误: ${error.message || error.data}`)
}

const handleEditorSave = (event) => {
  addLog('success', '文档保存事件触发')
}

const handleEditorWarning = (event) => {
  addLog('warning', `编辑器警告: ${event.data}`)
}

// 文件上传处理
const beforeUpload = (file) => {
  const isValidType = ['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'application/pdf'].includes(file.type)
  
  if (!isValidType) {
    ElMessage.error('请上传支持的文件格式')
    return false
  }
  
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  
  addLog('info', `开始上传文件: ${file.name}`)
  return true
}

const handleUploadSuccess = (response, file) => {
  ElMessage.success('文件上传成功')
  addLog('success', `文件上传成功: ${file.name}`)
  
  // 添加到文件列表
  const newFile = {
    id: response.data.id,
    name: response.data.originalName,
    type: getFileTypeText(response.data.fileExtension)
  }
  demoFiles.value.push(newFile)
  selectedFileId.value = newFile.id
}

const handleUploadError = (error, file) => {
  ElMessage.error('文件上传失败')
  addLog('error', `文件上传失败: ${file.name} - ${error.message}`)
}

// 工具方法
const getDocumentStateType = (state) => {
  switch (state) {
    case 0: return 'info'    // 无更改
    case 1: return 'warning' // 编辑中
    case 2: return 'success' // 保存完成
    case 3: return 'danger'  // 保存错误
    default: return 'info'
  }
}

const getDocumentStateText = (state) => {
  switch (state) {
    case 0: return '无更改'
    case 1: return '编辑中'
    case 2: return '已保存'
    case 3: return '保存错误'
    default: return '未知状态'
  }
}

const getFileTypeText = (extension) => {
  const typeMap = {
    '.doc': 'Word文档',
    '.docx': 'Word文档',
    '.xls': 'Excel表格',
    '.xlsx': 'Excel表格',
    '.ppt': 'PowerPoint演示文稿',
    '.pptx': 'PowerPoint演示文稿',
    '.pdf': 'PDF文档'
  }
  return typeMap[extension] || '未知类型'
}

const addLog = (type, message) => {
  const log = {
    type,
    message,
    time: new Date().toLocaleTimeString()
  }
  logs.value.unshift(log)
  
  // 保持日志数量在50条以内
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

const clearLogs = () => {
  logs.value = []
  addLog('info', '日志已清空')
}
</script>

<style scoped>
.onlyoffice-demo {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.demo-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.demo-controls {
  margin-bottom: 20px;
}

.editor-card {
  margin-bottom: 20px;
  min-height: 600px;
}

.status-card,
.log-card {
  margin-bottom: 20px;
}

.upload-demo {
  margin-top: 10px;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 10px;
}

.log-item {
  padding: 5px 0;
  border-bottom: 1px solid #eee;
  font-family: monospace;
  font-size: 12px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #666;
  margin-right: 10px;
}

.log-message {
  color: #333;
}

.log-item.success .log-message {
  color: #67c23a;
}

.log-item.error .log-message {
  color: #f56c6c;
}

.log-item.warning .log-message {
  color: #e6a23c;
}

.log-item.info .log-message {
  color: #409eff;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>