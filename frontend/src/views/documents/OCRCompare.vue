<template>
  <div class="ocr-compare-page">
    <el-card class="mb12">
      <template #header>
        <div class="card-header">
          <span>OCR文档比对</span>
          <el-tag type="info" size="small">基于RapidOCR识别</el-tag>
        </div>
      </template>
      
      <el-form :inline="true" class="form-inline">
        <el-form-item label="原始文件">
          <input
            ref="oldInput"
            type="file"
            accept=".pdf,.doc,.docx,.docm,.xls,.xlsx,.xlsm,.xlsb,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            @change="onFileChange('old', $event)"
          />
          <div v-if="oldFileName" class="file-info">{{ oldFileName }}</div>
        </el-form-item>
        
        <el-form-item label="新文件">
          <input
            ref="newInput"
            type="file"
            accept=".pdf,.doc,.docx,.docm,.xls,.xlsx,.xlsm,.xlsb,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            @change="onFileChange('new', $event)"
          />
          <div v-if="newFileName" class="file-info">{{ newFileName }}</div>
        </el-form-item>
        
        <el-button 
          type="primary" 
          @click="doUploadOCRCompare" 
          :loading="uploading"
          :disabled="!oldFile || !newFile"
        >
          开始OCR比对
        </el-button>
        
        <el-button text @click="settingsOpen = true">比对设置</el-button>
      </el-form>
      
      <el-alert 
        title="注意：OCR识别需要一定时间，请耐心等待。支持PDF、Word、Excel格式文档，系统会自动转换为PDF后进行文字识别和比对。现在使用统一的比对接口，通过OCR模式进行识别。" 
        type="info" 
        show-icon 
        :closable="false"
        class="mt12"
      />
    </el-card>

    <!-- 进度显示卡片 -->
    <el-card v-if="currentTask" class="mb12">
      <template #header>
        <div class="card-header">
          <span>处理进度</span>
          <el-tag :type="getStatusTagType(currentTask.status)" size="small">
            {{ currentTask.statusDesc }}
          </el-tag>
        </div>
      </template>
      
      <div class="progress-content">
        <div class="progress-info">
          <div class="task-id">任务ID: {{ currentTask.taskId }}</div>
          <div class="current-step">{{ currentTask.currentStep }}</div>
        </div>
        
        <el-progress 
          :percentage="currentTask.progress" 
          :status="getProgressStatus(currentTask.status)"
          :stroke-width="20"
        />
        
        <div class="progress-details">
          <span>进度: {{ currentTask.progress.toFixed(1) }}%</span>
          <span>步骤: {{ currentTask.currentStep }} / {{ currentTask.totalSteps }}</span>
          <span>创建时间: {{ formatTime(currentTask.createdTime) }}</span>
        </div>
        
        <div v-if="currentTask.status === 'FAILED'" class="error-message">
          <el-alert 
            :title="currentTask.errorMessage" 
            type="error" 
            show-icon 
            :closable="false"
          />
        </div>
        
        <div v-if="currentTask.status === 'COMPLETED'" class="success-actions">
          <el-button type="success" @click="viewResult">查看比对结果</el-button>
          <el-button @click="startNewTask">开始新任务</el-button>
        </div>
      </div>
    </el-card>

    <!-- 任务历史 -->
    <el-card class="mb12">
      <template #header>
        <div class="card-header">
          <span>任务历史</span>
          <el-button size="small" @click="refreshTasks">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="taskHistory" style="width: 100%" empty-text="暂无历史任务">
        <el-table-column prop="taskId" label="任务ID" width="200" show-overflow-tooltip />
        <el-table-column prop="statusDesc" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)" size="small">
              {{ scope.row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="scope">
            {{ scope.row.progress.toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 'COMPLETED'" 
              size="small" 
              type="primary"
              @click="viewTaskResult(scope.row.taskId)"
            >
              查看结果
            </el-button>
            <el-button 
              v-if="scope.row.status === 'PROCESSING' || scope.row.status === 'OCR_PROCESSING' || scope.row.status === 'COMPARING' || scope.row.status === 'ANNOTATING'" 
              size="small"
              @click="monitorTask(scope.row.taskId)"
            >
              监控
            </el-button>
            <el-button 
              size="small" 
              type="danger"
              @click="deleteTask(scope.row.taskId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 比对设置抽屉 -->
    <el-drawer v-model="settingsOpen" title="OCR比对设置" size="400px">
      <el-form label-width="140px">
        <el-form-item label="忽略页眉页脚">
          <el-switch v-model="settings.ignoreHeaderFooter" />
        </el-form-item>
        <el-form-item label="页眉高度(mm)">
          <el-input-number v-model="settings.headerHeightMm" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="页脚高度(mm)">
          <el-input-number v-model="settings.footerHeightMm" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="忽略大小写">
          <el-switch v-model="settings.ignoreCase" />
        </el-form-item>
        <el-form-item label="忽略符号集">
          <el-input v-model="settings.ignoredSymbols" placeholder="例如：_＿-·—" />
        </el-form-item>
        <el-form-item label="忽略空格">
          <el-switch v-model="settings.ignoreSpaces" />
        </el-form-item>
        <el-alert 
          title="说明：这些设置影响OCR识别结果的比对过滤，页眉页脚设置影响OCR识别区域。" 
          type="info" 
          show-icon 
          :closable="false"
        />
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  uploadOCRCompare, 
  getOCRCompareTaskStatus, 
  getAllOCRCompareTasks,
  deleteOCRCompareTask,
  type OCRCompareTaskStatus 
} from '@/api/ocr-compare'

const router = useRouter()

// 文件相关
const oldInput = ref<HTMLInputElement | null>(null)
const newInput = ref<HTMLInputElement | null>(null)
const oldFile = ref<File | null>(null)
const newFile = ref<File | null>(null)
const oldFileName = ref('')
const newFileName = ref('')

// 状态相关
const uploading = ref(false)
const currentTask = ref<OCRCompareTaskStatus | null>(null)
const taskHistory = ref<OCRCompareTaskStatus[]>([])
const progressTimer = ref<number | null>(null)

// 设置相关
const settingsOpen = ref(false)
const settings = reactive({
  ignoreHeaderFooter: true,
  headerHeightMm: 20,
  footerHeightMm: 20,
  ignoreCase: true,
  ignoredSymbols: '_＿',
  ignoreSpaces: false
})

// 生命周期
onMounted(() => {
  refreshTasks()
})

onUnmounted(() => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
  }
})

// 文件选择处理
const onFileChange = (type: 'old' | 'new', event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (file) {
    if (type === 'old') {
      oldFile.value = file
      oldFileName.value = file.name
    } else {
      newFile.value = file
      newFileName.value = file.name
    }
  }
}

// 开始OCR比对
const doUploadOCRCompare = async () => {
  if (!oldFile.value || !newFile.value) {
    ElMessage.warning('请先选择两个文件')
    return
  }
  
  const formData = new FormData()
  formData.append('oldFile', oldFile.value)
  formData.append('newFile', newFile.value)
  formData.append('ignoreHeaderFooter', String(settings.ignoreHeaderFooter))
  formData.append('headerHeightMm', String(settings.headerHeightMm))
  formData.append('footerHeightMm', String(settings.footerHeightMm))
  formData.append('ignoreCase', String(settings.ignoreCase))
  formData.append('ignoredSymbols', settings.ignoredSymbols || '')
  formData.append('ignoreSpaces', String(settings.ignoreSpaces))
  
  uploading.value = true
  
  try {
    const res = await uploadOCRCompare(formData)
    console.log('OCR比对响应:', res) // 添加调试日志
    
    // 检查响应结构，获取正确的任务ID
    let taskId = null
    if (res.data && res.data.id) {
      taskId = res.data.id
    } else if (res.data && res.data.taskId) {
      taskId = res.data.taskId
    } else {
      throw new Error('无法获取任务ID，响应格式异常')
    }
    
    if (!taskId) {
      throw new Error('任务ID为空')
    }
    
    console.log('获取到的任务ID:', taskId) // 添加调试日志
    
    ElMessage.success('OCR比对任务已提交，正在处理中...')
    
    // 开始监控任务进度
    monitorTask(taskId)
    
  } catch (e: any) {
    console.error('OCR比对失败:', e) // 添加调试日志
    ElMessage.error(e?.message || 'OCR比对任务提交失败')
  } finally {
    uploading.value = false
  }
}

// 监控任务进度
const monitorTask = (taskId: string) => {
  // 验证taskId参数
  if (!taskId || taskId === 'undefined' || taskId.trim() === '') {
    console.error('无效的任务ID:', taskId)
    ElMessage.error('无效的任务ID，无法监控进度')
    return
  }
  
  console.log('开始监控任务:', taskId) // 添加调试日志
  
  // 清除之前的定时器
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
  }
  
  // 立即查询一次
  updateTaskStatus(taskId)
  
  // 设置定时器每2秒查询一次
  progressTimer.value = window.setInterval(() => {
    updateTaskStatus(taskId)
  }, 2000)
}

// 更新任务状态
const updateTaskStatus = async (taskId: string) => {
  // 验证taskId参数
  if (!taskId || taskId === 'undefined' || taskId.trim() === '') {
    console.error('无效的任务ID:', taskId)
    return
  }
  
  try {
    console.log('查询任务状态:', taskId) // 添加调试日志
    const res = await getOCRCompareTaskStatus(taskId)
    currentTask.value = res.data
    
    // 如果任务完成，停止监控
    if (res.data.status === 'COMPLETED' || res.data.status === 'FAILED' || res.data.status === 'TIMEOUT') {
      if (progressTimer.value) {
        clearInterval(progressTimer.value)
        progressTimer.value = null
      }
      
      // 刷新任务历史
      refreshTasks()
      
      if (res.data.status === 'COMPLETED') {
        ElMessage.success('OCR比对完成！')
      } else if (res.data.status === 'FAILED') {
        ElMessage.error('OCR比对失败: ' + res.data.errorMessage)
      }
    }
    
  } catch (e: any) {
    console.error('查询任务状态失败:', e)
  }
}

// 刷新任务列表
const refreshTasks = async () => {
  try {
    const res = await getAllOCRCompareTasks()
    taskHistory.value = res.data.sort((a, b) => 
      new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime()
    )
  } catch (e: any) {
    console.error('获取任务历史失败:', e)
  }
}

// 查看结果
const viewResult = () => {
  if (currentTask.value) {
    viewTaskResult(currentTask.value.taskId)
  }
}

// 查看任务结果
const viewTaskResult = (taskId: string) => {
  sessionStorage.setItem('lastOCRCompareTaskId', taskId)
  router.push({ name: 'OCRCompareResult', params: { taskId } }).catch(() => {})
}

// 开始新任务
const startNewTask = () => {
  currentTask.value = null
  oldFile.value = null
  newFile.value = null
  oldFileName.value = ''
  newFileName.value = ''
  
  // 清空文件输入
  if (oldInput.value) oldInput.value.value = ''
  if (newInput.value) newInput.value.value = ''
  
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
    progressTimer.value = null
  }
}

// 删除任务
const deleteTask = async (taskId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteOCRCompareTask(taskId)
    ElMessage.success('删除成功')
    
    // 如果删除的是当前监控的任务，停止监控
    if (currentTask.value && currentTask.value.taskId === taskId) {
      startNewTask()
    }
    
    refreshTasks()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'TIMEOUT': return 'warning'
    case 'PENDING': return 'info'
    default: return 'primary'
  }
}

// 获取进度条状态
const getProgressStatus = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'exception'
    default: return undefined
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString()
}
</script>

<style scoped>
.ocr-compare-page { 
  padding: 16px; 
}

.mb12 { 
  margin-bottom: 12px; 
}

.mt12 {
  margin-top: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.progress-content {
  .progress-info {
    margin-bottom: 16px;
    
    .task-id {
      font-size: 12px;
      color: #666;
      margin-bottom: 4px;
    }
    
    .current-step {
      font-weight: 500;
      margin-bottom: 8px;
    }
  }
  
  .progress-details {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #666;
    margin-top: 8px;
  }
  
  .error-message {
    margin-top: 16px;
  }
  
  .success-actions {
    margin-top: 16px;
    text-align: center;
    
    .el-button + .el-button {
      margin-left: 12px;
    }
  }
}
</style>
