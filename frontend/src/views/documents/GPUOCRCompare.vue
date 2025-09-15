<template>
  <div class="gpu-ocr-compare-page">
    <!-- 1:1 复刻 Compare 页的页眉卡片 -->
    <el-card class="page-header-card mb12">
      <div class="page-header">
        <div class="header-content">
          <h2><el-icon class="header-icon"><Search /></el-icon>GPU OCR合同比对</h2>
          <p>通过视觉大模型进行ocr比对，支持pdf、word、excel格式文档。</p>
        </div>
        <div class="header-decoration"></div>
      </div>
    </el-card>

    <el-card class="mb12">
      <template #header>
        <div class="card-header">
          <span>GPU OCR文档比对</span>
          <el-tag type="success" size="small">基于GPU加速</el-tag>
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
          @click="doUploadGPUOCRCompare"
          :loading="uploading"
          :disabled="!oldFile || !newFile"
        >
          开始GPU OCR比对
        </el-button>

        <el-button text @click="settingsOpen = true">比对设置</el-button>

        <el-button type="warning" @click="debugDialogVisible = true">调试模式</el-button>
      </el-form>

      <el-alert
        title="注意：GPU OCR比对基于先进的AI模型进行文字识别和比对，支持PDF、Word、Excel格式文档。系统会自动转换为PDF后进行GPU加速的文字识别和智能比对。"
        type="success"
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
          <div class="current-step">{{ currentTask.currentStepDesc }}</div>
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
              v-if="scope.row.status === 'COMPLETED'"
              size="small"
              type="primary"
              @click="goToResult(scope.row.taskId)"
            >
              查看结果
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

    <!-- 调试对话框 -->
    <el-dialog v-model="debugDialogVisible" title="GPU OCR比对调试模式" width="500px">
      <el-form label-width="120px" class="mt20">
        <el-form-item label="任务ID">
          <el-input v-model="debugForm.taskId" placeholder="输入已完成的GPU OCR任务ID"></el-input>
        </el-form-item>
        <el-alert
          title="说明：调试模式将使用已有任务的结果，重新应用比对参数进行分析。请确保输入的TaskId对应的任务已经完成。"
          type="info"
          show-icon
          :closable="false"
          class="mb12"
        />
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="debugDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="startDebugCompare" :loading="debugLoading">
            开始调试比对
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 比对设置抽屉 -->
    <el-drawer v-model="settingsOpen" title="GPU OCR比对设置" size="400px">
      <el-form label-width="140px">
        <el-form-item label="忽略页眉页脚">
          <el-switch v-model="settings.ignoreHeaderFooter" />
        </el-form-item>
        <el-form-item label="页眉高度(%)">
          <el-input-number 
            v-model="settings.headerHeightPercent" 
            :min="0" 
            :max="50" 
            :precision="1"
            :step="0.5"
            placeholder="页面顶部百分比"
          />
          <div class="setting-hint">文档顶部多少百分比的区域视为页眉</div>
        </el-form-item>
        <el-form-item label="页脚高度(%)">
          <el-input-number 
            v-model="settings.footerHeightPercent" 
            :min="0" 
            :max="50" 
            :precision="1"
            :step="0.5"
            placeholder="页面底部百分比"
          />
          <div class="setting-hint">文档底部多少百分比的区域视为页脚</div>
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
        <el-form-item label="忽略印章">
          <el-switch v-model="settings.ignoreSeals" />
        </el-form-item>
        <el-alert
          title="说明：这些设置影响GPU OCR识别结果的比对过滤，页眉页脚设置影响OCR识别区域。GPU加速提供更快的处理速度和更高的准确率。"
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
import { Search } from '@element-plus/icons-vue'
import {
  uploadGPUOCRCompare,
  getGPUOCRCompareTaskStatus,
  getAllGPUOCRCompareTasks,
  deleteGPUOCRCompareTask,
  debugGPUCompareWithExistingOCR,
  debugGPUCompareLegacy,
  type GPUOCRCompareTaskStatus
} from '@/api/gpu-ocr-compare'

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
const currentTask = ref<GPUOCRCompareTaskStatus | null>(null)
const taskHistory = ref<GPUOCRCompareTaskStatus[]>([])
const progressTimer = ref<number | null>(null)

// 调试相关
const debugDialogVisible = ref(false)
const debugLoading = ref(false)
const debugForm = reactive({
  taskId: ''
})

// 设置相关
const settingsOpen = ref(false)
const settings = reactive({
  ignoreHeaderFooter: true,
  headerHeightPercent: 8.0,
  footerHeightPercent: 8.0,
  ignoreCase: true,
  ignoredSymbols: '_＿',
  ignoreSpaces: false,
  ignoreSeals: true
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

// 开始GPU OCR比对
const doUploadGPUOCRCompare = async () => {
  if (!oldFile.value || !newFile.value) {
    ElMessage.warning('请先选择两个文件')
    return
  }

  const formData = new FormData()
  formData.append('oldFile', oldFile.value)
  formData.append('newFile', newFile.value)
  formData.append('ignoreHeaderFooter', String(settings.ignoreHeaderFooter))
  formData.append('headerHeightPercent', String(settings.headerHeightPercent))
  formData.append('footerHeightPercent', String(settings.footerHeightPercent))
  formData.append('ignoreCase', String(settings.ignoreCase))
  formData.append('ignoredSymbols', settings.ignoredSymbols || '')
  formData.append('ignoreSpaces', String(settings.ignoreSpaces))
  formData.append('ignoreSeals', String(settings.ignoreSeals))

  uploading.value = true

  try {
    // 直接跳转到Canvas版本结果页面
    const routeName = 'GPUOCRCanvasCompareResult'
    
    // 先进入结果页占位，显示等待动效
    router.push({ 
      name: routeName, 
      params: { taskId: 'pending' },
      query: { 
        oldFileName: oldFile.value.name, 
        newFileName: newFile.value.name 
      }
    }).catch(() => {})

    const res = await uploadGPUOCRCompare(formData)
    console.log('GPU OCR比对响应:', res) // 添加调试日志

    // 检查响应结构，获取正确的任务ID
    let taskId = res.data?.taskId
    if (!taskId) {
      throw new Error('无法获取任务ID，响应格式异常')
    }

    console.log('获取到的任务ID:', taskId) // 添加调试日志

    // 使用 replace 替换为真实 taskId，避免历史多一条
    router.replace({ 
      name: routeName, 
      params: { taskId },
      query: {
        oldFileName: oldFile.value.name,
        newFileName: newFile.value.name
      }
    }).catch(() => {})

    ElMessage.success('GPU OCR比对任务已提交，正在处理中...')

  } catch (e: any) {
    console.error('GPU OCR比对失败:', e) // 添加调试日志
    ElMessage.error(e?.message || 'GPU OCR比对任务提交失败')
  } finally {
    uploading.value = false
  }
}

// 跳转到结果页面
const goToResult = (taskId: string) => {
  router.push({ 
    name: 'GPUOCRCanvasCompareResult', 
    params: { taskId }
  }).catch(() => {})
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
    const res = await getGPUOCRCompareTaskStatus(taskId)
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
        ElMessage.success('GPU OCR比对完成！')
      } else if (res.data.status === 'FAILED') {
        ElMessage.error('GPU OCR比对失败: ' + res.data.errorMessage)
      }
    }

  } catch (e: any) {
    console.error('查询任务状态失败:', e)
  }
}

// 刷新任务列表
const refreshTasks = async () => {
  try {
    const res = await getAllGPUOCRCompareTasks()
    // 后端返回格式：{code: 200, message: "...", data: [...]}
    taskHistory.value = ((res.data || []) as GPUOCRCompareTaskStatus[]).sort(
      (a: GPUOCRCompareTaskStatus, b: GPUOCRCompareTaskStatus) =>
        new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime()
    )
  } catch (e: any) {
    console.error('获取任务历史失败:', e)
    ElMessage.error('获取任务历史失败: ' + (e.message || '网络错误'))
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
  sessionStorage.setItem('lastGPUOCRCompareTaskId', taskId)
  router.push({ name: 'GPUOCRCanvasCompareResult', params: { taskId } }).catch(() => {})
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

    await deleteGPUOCRCompareTask(taskId)
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

// 开始调试比对
const startDebugCompare = async () => {
  if (!debugForm.taskId || !debugForm.taskId.trim()) {
    ElMessage.warning('请输入任务ID')
    return
  }

  debugLoading.value = true

  try {
    const res = await debugGPUCompareWithExistingOCR({
      taskId: debugForm.taskId,
      options: {
        ignoreHeaderFooter: settings.ignoreHeaderFooter,
        headerHeightPercent: settings.headerHeightPercent,
        footerHeightPercent: settings.footerHeightPercent,
        ignoreCase: settings.ignoreCase,
        ignoredSymbols: settings.ignoredSymbols || '',
        ignoreSpaces: settings.ignoreSpaces,
        ignoreSeals: settings.ignoreSeals
      }
    })

    console.log('调试比对响应:', res)

    // 获取任务ID
    const taskId = res.data?.taskId

    if (!taskId) {
      throw new Error('任务ID为空')
    }

    // 清空表单
    debugForm.taskId = ''

    ElMessage.success('调试比对任务已提交，正在处理中...')

    // 关闭对话框
    debugDialogVisible.value = false

    // 跳转到结果页面
    router.push({ 
      name: 'GPUOCRCanvasCompareResult', 
      params: { taskId }
    }).catch(() => {})

  } catch (e: any) {
    console.error('调试比对失败:', e)
    ElMessage.error(e?.message || '调试比对任务提交失败')
  } finally {
    debugLoading.value = false
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString()
}
</script>

<style scoped>
.gpu-ocr-compare-page {
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

/* 1:1 复刻 Compare 页的页眉样式 */
.page-header-card { 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  overflow: hidden;
  transition: all 0.3s ease;
}
.page-header-card:hover { box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); }
.page-header { 
  padding: 16px 20px; 
  position: relative; 
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
}
.header-content { position: relative; z-index: 2; }
.header-decoration { 
  position: absolute; 
  top: 0; 
  right: 0; 
  width: 150px; 
  height: 100%; 
  background: linear-gradient(135deg, transparent, var(--el-color-primary-light-5)); 
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}
.page-header h2 { 
  margin: 0; 
  font-size: 26px; 
  color: var(--el-color-primary-dark-2); 
  display: flex; 
  align-items: center;
  font-weight: 600;
}
.header-icon { 
  margin-right: 10px; 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.page-header p { 
  margin: 10px 0 0; 
  color: #606266; 
  font-size: 15px; 
  max-width: 80%;
}

.setting-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
