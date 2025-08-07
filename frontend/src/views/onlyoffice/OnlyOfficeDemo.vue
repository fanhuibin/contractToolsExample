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
           
           <el-col :span="6">
             <el-form-item label="更新文档密钥:">
               <el-tooltip 
                 content="OnlyOffice有缓存机制，有时需要使用新的密钥来防止缓存问题。开启后会重新生成文档的唯一标识符。"
                 placement="top"
               >
                 <el-switch
                   v-model="updateOnlyofficeKey"
                   active-text="更新"
                   inactive-text="不更新"
                   @change="handleKeyUpdateChange"
                 />
               </el-tooltip>
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


      </div>
      
             <!-- 功能说明 -->
       <div style="margin-top: 20px; display: flex; align-items: center; gap: 8px;">
         <span style="color: #666; font-size: 14px;">文档密钥说明</span>
                   <el-tooltip 
            placement="top"
            :show-after="200"
            :hide-after="0"
            popper-class="custom-tooltip"
          >
            <template #content>
              <div style="max-width: 300px; line-height: 1.6;">
                <div style="font-weight: bold; margin-bottom: 8px;">OnlyOffice 文档密钥（onlyofficeKey）的作用：</div>
                <ul style="margin: 8px 0; padding-left: 16px;">
                  <li>作为文档的唯一标识符，用于区分不同的文档版本</li>
                  <li>OnlyOffice 服务器会根据这个密钥进行缓存管理</li>
                  <li>当文档内容更新但密钥相同时，可能会遇到缓存问题</li>
                  <li>开启"更新文档密钥"后，每次加载都会生成新的密钥，确保获取最新内容</li>
                </ul>
                <div style="margin-top: 8px; color: #e6a23c;">
                  <strong>使用场景：</strong>当文档内容已更新但编辑器仍显示旧内容时，建议开启此选项。
                </div>
              </div>
            </template>
           <el-icon style="color: #409eff; cursor: pointer; font-size: 16px;">
             <QuestionFilled />
           </el-icon>
         </el-tooltip>
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
         :update-onlyoffice-key="updateOnlyofficeKey"
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
         <el-descriptions-item label="密钥更新">
           <el-tag :type="updateOnlyofficeKey ? 'warning' : 'info'">
             {{ updateOnlyofficeKey ? '已开启' : '未开启' }}
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Refresh, UploadFilled, QuestionFilled } from '@element-plus/icons-vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { healthCheck } from '@/api/onlyoffice'
import { getToken } from '@/utils/auth'
import axios from 'axios'

// 响应式数据
const selectedFileId = ref('')
const canEdit = ref(true)
const canReview = ref(false)
const updateOnlyofficeKey = ref(false)
const showEditor = ref(false)
const loading = ref(false)
const healthChecking = ref(false)
const editorReady = ref(false)
const documentState = ref(null)
const logs = ref([])
const editorRef = ref(null)

// 演示文件数据
const demoFiles = ref([])

// 计算属性
const editorHeight = computed(() => 'calc(100vh - 300px)')

// 生命周期
onMounted(async () => {
  addLog('info', '页面已加载，请选择文件开始演示')
  // 动态获取文件列表
  try {
    const response = await axios.get('/api/file/list')
    if (response.data.code === 200) {
      demoFiles.value = response.data.data.map(file => ({
        id: file.id,
        name: file.originalName,
        type: getFileTypeText('.' + file.fileExtension)
      }))
    }
  } catch (error) {
    ElMessage.error('获取文件列表失败')
  }
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

const handleKeyUpdateChange = () => {
  addLog('info', `文档密钥更新设置: ${updateOnlyofficeKey.value ? '开启' : '关闭'}`)
}

const loadEditor = async () => {
  if (!selectedFileId.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  loading.value = true
  editorReady.value = false  // 重置编辑器状态
  addLog('info', '开始加载编辑器...')
  
  try {
    // 如果编辑器已存在，先销毁
    if (editorRef.value) {
      editorRef.value.destroyEditor?.()
    }
    
    // 立即显示编辑器容器，让用户看到界面
    showEditor.value = true
    
    // 等待DOM更新
    await nextTick()
    

    
    // 立即开始初始化编辑器
    if (editorRef.value) {
      editorRef.value.initEditor().catch(error => {
        ElMessage.error('加载编辑器失败: ' + error.message)
        addLog('error', `加载编辑器失败: ${error.message}`)
        showEditor.value = false
        loading.value = false
      })
    }
    
  } catch (error) {
    ElMessage.error('加载编辑器失败: ' + error.message)
    addLog('error', `加载编辑器失败: ${error.message}`)
    showEditor.value = false
    loading.value = false
  }
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
  if (!editorReady.value) {  // 只在首次ready时提示
    editorReady.value = true
    loading.value = false  // 确保加载状态被正确设置
    addLog('success', '编辑器已就绪')
    ElMessage.success('文档编辑器加载完成')
  }
}

const handleDocumentStateChange = (event) => {
  documentState.value = event.data
  addLog('info', `文档状态变更: ${getDocumentStateText(event.data)}`)
}

const handleEditorError = (error) => {
  loading.value = false  // 确保在错误时也结束加载状态
  addLog('error', `编辑器错误: ${error.message || error.data}`)
}

const handleEditorSave = (event) => {
  addLog('success', '文档保存事件触发')
}

const handleEditorWarning = (event) => {
  addLog('warning', `编辑器警告: ${event.data}`)
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

/* 自定义tooltip样式 */
:deep(.custom-tooltip) {
  max-width: 350px !important;
  word-wrap: break-word !important;
  white-space: normal !important;
}

:deep(.custom-tooltip .el-tooltip__content) {
  white-space: normal !important;
  word-wrap: break-word !important;
  line-height: 1.6 !important;
}
</style>