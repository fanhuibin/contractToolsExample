<template>
  <div class="gpu-ocr-compare-page">
    <!-- 1:1 复刻 Compare 页的页眉卡片 -->
    <el-card class="page-header-card mb12">
      <div class="page-header">
        <div class="header-content">
          <h2><el-icon class="header-icon"><Search /></el-icon>GPU OCR合同比对</h2>
          <p>通过视觉大模型进行ocr比对，支持pdf格式文档。</p>
        </div>
        <div class="header-decoration"></div>
      </div>
    </el-card>

    <el-card class="mb12">
      <template #header>
        <div class="card-header">
          <span>GPU OCR文档比对</span>
          <el-tag type="success" size="small">专业版</el-tag>
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

        <el-button type="info" @click="settingsOpen = true" icon="Setting">比对设置</el-button>

        <el-button type="warning" @click="debugDialogVisible = true">调试模式</el-button>
      </el-form>

      <el-alert
        title="肇新合同比对专业版，基于先进的AI模型进行文字识别和比对。已广泛应用于合同管理、招投标管理、合同风险控制等场景。"
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
            {{ currentTask.statusDescription }}
          </el-tag>
        </div>
      </template>

      <div class="progress-content">
        <div class="progress-info">
          <div class="task-id">任务ID: {{ currentTask.taskId }}</div>
          <div class="current-step">比对中</div>
        </div>

        <el-progress
          :percentage="displayProgress"
          :status="getProgressStatus(currentTask.status)"
          :stroke-width="20"
        />

        <div class="progress-details">
          <span>比对中...{{ displayProgress.toFixed(1) }}%</span>
          <span>{{ currentTask.startTime ? '开始时间: ' + formatTime(currentTask.startTime) : (currentTask.createdTime ? '创建时间: ' + formatTime(currentTask.createdTime) : '') }}</span>
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

      <el-table 
        :data="taskHistory" 
        style="width: 100%" 
        empty-text="暂无历史任务" 
        class="task-history-table"
        table-layout="auto"
      >
        <el-table-column prop="taskId" label="任务ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="oldFileName" label="原文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="newFileName" label="新文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="差异总数" min-width="120" align="center">
          <template #default="scope">
            <span>{{ getDifferencesCount(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="任务开始时间" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="任务完成时间" min-width="180">
          <template #default="scope">
            {{ scope.row.endTime ? formatTime(scope.row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="分析时长" min-width="120" align="center">
          <template #default="scope">
            {{ getProcessingDuration(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                v-if="scope.row.resultUrl"
                size="small"
                type="primary"
                @click="goToResult(scope.row.taskId)"
              >
                比对结果
              </el-button>
              <el-button
                v-if="scope.row.resultUrl"
                size="small"
                type="success"
                :icon="DownloadOutlined"
                @click="downloadResult(scope.row.taskId)"
                title="下载结果"
              />
              <el-button
                size="small"
                type="danger"
                :icon="DeleteOutlined"
                @click="deleteTask(scope.row.taskId)"
                title="删除任务"
              />
            </div>
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
        <el-form-item label="去除水印">
          <el-switch v-model="settings.removeWatermark" />
          <div class="setting-hint">自动去除图片中的水印，提高OCR识别准确度</div>
        </el-form-item>
        <el-form-item v-if="settings.removeWatermark" label="去水印强度">
          <el-select v-model="settings.watermarkRemovalStrength" style="width: 100%">
            <el-option
              v-for="option in watermarkStrengthOptions"
              :key="option.value"
              :label="option.label + (option.recommended ? ' (推荐)' : '')"
              :value="option.value"
            >
              <div>
                <div style="display: flex; align-items: center;">
                  <span>{{ option.label }}</span>
                  <el-tag v-if="option.recommended" type="success" size="small" style="margin-left: 8px">推荐</el-tag>
                </div>
                <div style="font-size: 12px; color: #999; margin-top: 4px;">{{ option.description }}</div>
              </div>
            </el-option>
          </el-select>
          <div class="setting-hint">
            <strong>智能模式(推荐)</strong>：自动尝试多种强度，获得最佳效果<br>
            <strong>默认强度</strong>：适合常见浅色水印<br>
            <strong>扩展强度</strong>：适合半透明水印<br>
            <strong>宽松强度</strong>：可能误删文字，慎用
          </div>
        </el-form-item>
        <el-alert
          title="说明：这些设置影响GPU OCR识别结果的比对过滤，页眉页脚设置影响OCR识别区域。GPU加速提供更快的处理速度和更高的准确率。"
          type="info"
          show-icon
          :closable="false"
        />
      </el-form>
    </el-drawer>

    <!-- 导出比对报告对话框 -->
    <el-dialog
      v-model="showExportDialogVisible"
      title="导出比对报告"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="export-options">
        <div class="export-section">
          <h4 class="section-title">选择导出文档格式类型</h4>
          <el-checkbox-group v-model="exportFormats" class="checkbox-group">
            <el-checkbox label="doc" class="checkbox-item">
              <span class="checkbox-text">.doc</span>
            </el-checkbox>
            <el-checkbox label="html" class="checkbox-item">
              <span class="checkbox-text">.html</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelExport">取消</el-button>
          <el-button type="primary" @click="confirmExport" :disabled="exportFormats.length === 0">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, EditPen } from '@element-plus/icons-vue'
import { DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import {
  uploadGPUOCRCompare,
  getGPUOCRCompareTaskStatus,
  getAllGPUOCRCompareTasks,
  deleteGPUOCRCompareTask,
  debugGPUCompareWithExistingOCR,
  debugGPUCompareLegacy,
  exportCompareReport,
  type GPUOCRCompareTaskStatus,
  type WatermarkStrengthOption
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

// 平滑进度相关
const displayProgress = ref(0) // 用于显示的平滑进度
const targetProgress = ref(0)  // 目标进度
const smoothTimer = ref<number | null>(null)

// 阶段信息（用于基于时间的平滑增长）
const currentStageInfo = ref({
  minProgress: 0,
  maxProgress: 100,
  estimatedTime: 0,
  elapsedTime: 0,
  startTime: Date.now()
})

// 调试相关
const debugDialogVisible = ref(false)
const debugLoading = ref(false)
const debugForm = reactive({
  taskId: ''
})

// 导出功能状态管理
const showExportDialogVisible = ref(false)
const exportFormats = ref(['doc']) // ['doc', 'html']
const currentExportTaskId = ref('')

// 去水印强度选项
const watermarkStrengthOptions: WatermarkStrengthOption[] = [
  {
    value: 'default',
    label: '默认强度',
    description: '检测浅灰色到白色水印(RGB 160-255)，适合常见水印'
  },
  {
    value: 'extended',
    label: '扩展强度',
    description: '检测中等灰度水印(RGB 120-255)，适合半透明水印'
  },
  {
    value: 'loose',
    label: '宽松强度',
    description: '检测深色水印(RGB 80-255)，可能误删文字，慎用'
  },
  {
    value: 'smart',
    label: '智能模式',
    description: '自动尝试多种强度，推荐使用',
    recommended: true
  }
]

// 设置相关
const settingsOpen = ref(false)
const settings = reactive({
  ignoreHeaderFooter: true,
  headerHeightPercent: 12,
  footerHeightPercent: 12,
  ignoreCase: true,
  ignoredSymbols: '_＿',
  ignoreSpaces: false,
  ignoreSeals: true,
  removeWatermark: false,
  watermarkRemovalStrength: 'smart' as 'default' | 'extended' | 'loose' | 'smart'
})

// 生命周期
onMounted(() => {
  refreshTasks()
})

onUnmounted(() => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
  }
  if (smoothTimer.value) {
    clearInterval(smoothTimer.value)
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
  formData.append('removeWatermark', String(settings.removeWatermark))
  formData.append('watermarkRemovalStrength', settings.watermarkRemovalStrength)

  uploading.value = true
  
  // 初始化平滑进度
  displayProgress.value = 0
  targetProgress.value = 0

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

// 判断当前是否是OCR步骤
const isOCRStep = () => {
  const stepDesc = currentTask.value?.currentStepDesc || ''
  return stepDesc.includes('OCR识别原文档') || stepDesc.includes('OCR识别新文档')
}

// 基于页面进度计算OCR步骤的进度
const calculateOCRPageProgress = () => {
  const stageInfo = currentStageInfo.value
  const task = currentTask.value
  
  if (!task) return stageInfo.minProgress
  
  let currentDocPages = 0
  let completedPages = 0
  
  // 判断当前处理的是哪个文档
  if (task.currentStepDesc?.includes('原文档')) {
    currentDocPages = task.oldDocPages || 0
    completedPages = task.completedPagesOld || 0
  } else if (task.currentStepDesc?.includes('新文档')) {
    currentDocPages = task.newDocPages || 0
    completedPages = task.completedPagesNew || 0
  }
  
  if (currentDocPages <= 0) {
    // 如果没有页面信息，回退到时间基础计算
    let stageProgressRatio = Math.min(1.0, stageInfo.elapsedTime / stageInfo.estimatedTime)
    const stageRange = stageInfo.maxProgress - stageInfo.minProgress
    return stageInfo.minProgress + (stageRange * stageProgressRatio)
  }
  
  // 计算页面进度比例
  let pageProgressRatio = completedPages / currentDocPages
  
  // 限制在0-1范围内
  pageProgressRatio = Math.min(1.0, Math.max(0.0, pageProgressRatio))
  
  // 在阶段范围内插值
  const stageRange = stageInfo.maxProgress - stageInfo.minProgress
  return stageInfo.minProgress + (stageRange * pageProgressRatio)
}

// 基于后端预估时间和页面进度的精确进度计算
const updateSmoothProgress = () => {
  const stageInfo = currentStageInfo.value
  
  // 如果没有有效的阶段信息，使用缓慢增长保持进度条活跃
  if (stageInfo.estimatedTime <= 0 || stageInfo.elapsedTime < 0) {
    // 缓慢增长，每次增长0.01%，但不超过5%
    if (displayProgress.value < 5.0) {
      displayProgress.value = Math.min(displayProgress.value + 0.01, 5.0)
    }
    return
  }
  
  let calculatedProgress = 0
  
  // 检查是否是OCR步骤，如果是，优先使用页面进度
  if (isOCRStep()) {
    calculatedProgress = calculateOCRPageProgress()
  } else {
    // 非OCR步骤使用时间基础的进度计算
    let stageProgressRatio = Math.min(1.0, stageInfo.elapsedTime / stageInfo.estimatedTime)
    const stageRange = stageInfo.maxProgress - stageInfo.minProgress
    calculatedProgress = stageInfo.minProgress + (stageRange * stageProgressRatio)
  }
  
  // 平滑过渡到计算出的进度
  const diff = calculatedProgress - displayProgress.value
  
  if (Math.abs(diff) < 0.1) {
    displayProgress.value = calculatedProgress
  } else {
    // 每次更新移动差值的15%，稍快一些但仍然平滑
    const step = diff * 0.15
    displayProgress.value += step
  }
  
  // 确保进度在阶段范围内，并且不超过基于时间计算的进度
  displayProgress.value = Math.max(stageInfo.minProgress, 
    Math.min(displayProgress.value, calculatedProgress, stageInfo.maxProgress - 0.5))
  
  // 调试日志
  if (Math.random() < 0.1) { // 10%概率输出日志，避免日志过多
    const stageProgressRatio = stageInfo.estimatedTime > 0 ? Math.min(1.0, stageInfo.elapsedTime / stageInfo.estimatedTime) : 0
    console.log(`📊 进度更新: 阶段${stageInfo.minProgress}%-${stageInfo.maxProgress}%, 已用时间${stageInfo.elapsedTime}ms/${stageInfo.estimatedTime}ms (${(stageProgressRatio*100).toFixed(1)}%), 显示进度${displayProgress.value.toFixed(1)}%`)
  }
}


// 更新阶段信息并启动平滑进度
const updateStageInfoAndStartProgress = (taskData: any) => {
  // 启动平滑进度定时器（如果还没启动）
  if (!smoothTimer.value) {
    smoothTimer.value = setInterval(updateSmoothProgress, 300) // 每300ms更新一次，稍慢一些更平滑
    console.log('🚀 启动基于后端时间的平滑进度更新')
  }
  
  // 检查是否有完整的阶段信息
  if (taskData.stageMinProgress === undefined || taskData.stageMaxProgress === undefined || 
      taskData.stageEstimatedTime === undefined || taskData.stageElapsedTime === undefined) {
    console.warn('⚠️ 后端返回的阶段信息不完整，使用缓慢增长模式', taskData)
    return
  }
  
  const newStageInfo = {
    minProgress: taskData.stageMinProgress,
    maxProgress: taskData.stageMaxProgress,
    estimatedTime: taskData.stageEstimatedTime,
    elapsedTime: taskData.stageElapsedTime,
    startTime: Date.now() // 这个字段在新逻辑中不再使用，但保留兼容性
  }
  
  // 检查是否进入了新阶段
  const isNewStage = newStageInfo.minProgress !== currentStageInfo.value.minProgress || 
                     newStageInfo.maxProgress !== currentStageInfo.value.maxProgress
  
  if (isNewStage) {
    console.log(`🎯 进入新阶段: ${newStageInfo.minProgress}% - ${newStageInfo.maxProgress}%, 预估时间: ${(newStageInfo.estimatedTime/1000).toFixed(1)}秒`)
  }
  
  currentStageInfo.value = newStageInfo
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

    // 更新阶段信息和平滑进度
    updateStageInfoAndStartProgress(res.data)

    // 如果任务完成，停止监控
    if (res.data.status === 'COMPLETED' || res.data.status === 'FAILED' || res.data.status === 'TIMEOUT') {
      if (progressTimer.value) {
        clearInterval(progressTimer.value)
        progressTimer.value = null
      }
      
      // 停止平滑进度动画
      if (smoothTimer.value) {
        clearInterval(smoothTimer.value)
        smoothTimer.value = null
      }

      // 刷新任务历史
      refreshTasks()

      if (res.data.status === 'COMPLETED') {
        // 设置进度为100%
        displayProgress.value = 100
        targetProgress.value = 100
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
    taskHistory.value = ((res.data || []) as any[]).sort(
      (a: any, b: any) =>
        new Date(b.startTime || 0).getTime() - new Date(a.startTime || 0).getTime()
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
        ignoreSeals: settings.ignoreSeals,
        removeWatermark: settings.removeWatermark,
        watermarkRemovalStrength: settings.watermarkRemovalStrength
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
const formatTime = (timeStr: string | undefined) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString()
}

// 获取差异总数
const getDifferencesCount = (task: any) => {
  // 直接使用后端返回的 differenceCount 字段
  if (task.differenceCount !== null && task.differenceCount !== undefined) {
    return task.differenceCount.toString()
  }
  
  // 如果没有差异数据，显示 "-"
  return '-'
}

// 计算分析时长
const getProcessingDuration = (task: any) => {
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

// 下载结果
const downloadResult = async (taskId: string) => {
  currentExportTaskId.value = taskId
  showExportDialogVisible.value = true
}

// 导出功能函数
const cancelExport = () => {
  showExportDialogVisible.value = false
  currentExportTaskId.value = ''
}

const confirmExport = async () => {
  if (exportFormats.value.length === 0) {
    ElMessage.warning('请选择至少一种导出格式')
    return
  }
  
  try {
    const exportData = {
      taskId: currentExportTaskId.value,
      formats: exportFormats.value,
      includeIgnored: false, // 是否包含已忽略的差异
      includeRemarks: true // 是否包含备注
    }
    
    ElMessage.info('正在生成导出文件，请稍候...')
    
    // 调用后端导出API
    const response = await exportCompareReport(exportData)
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 确定文件名
    const format = exportFormats.value[0]
    let filename = `比对报告_${currentExportTaskId.value}`
    if (exportFormats.value.length === 1) {
      filename += format === 'html' ? '.zip' : '.docx'
    } else {
      filename += '.zip'
    }
    
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功！文件已开始下载')
    showExportDialogVisible.value = false
    currentExportTaskId.value = ''
    
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error?.message || '导出失败，请重试')
  }
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

/* 任务历史表格样式优化 */
.task-history-table {
  /* 表格布局优化 */
  width: 100% !important;
  table-layout: auto;
  
  /* 表格容器 */
  :deep(.el-table__header-wrapper),
  :deep(.el-table__body-wrapper) {
    width: 100% !important;
  }
  
  /* 表格主体 */
  :deep(.el-table__header),
  :deep(.el-table__body) {
    width: 100% !important;
    table-layout: auto;
  }
  
  /* 增加表格行高 */
  :deep(.el-table__row) {
    height: 60px;
  }
  
  /* 表格单元格内边距 */
  :deep(.el-table td) {
    padding: 12px 8px;
  }
  
  /* 表头样式 */
  :deep(.el-table th) {
    padding: 12px 8px;
    background-color: #f8f9fa;
  }
  
  /* 表格单元格内容换行处理 */
  :deep(.cell) {
    word-break: break-word;
    white-space: normal;
    line-height: 1.4;
    overflow: visible;
  }
  
  /* 让表格列自动分配剩余空间 */
  :deep(.el-table colgroup col) {
    min-width: auto;
  }
  
  /* 确保表格占满容器宽度 */
  :deep(.el-table) {
    width: 100% !important;
  }
}

/* 操作按钮容器样式 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: nowrap;
  
  .el-button {
    margin: 0;
    white-space: nowrap;
  }
  
  /* 确保按钮不换行 */
  .el-button + .el-button {
    margin-left: 0;
  }
}

/* 导出对话框样式 */
.export-options {
  padding: 20px 0;
}

.export-section {
  margin-bottom: 24px;
}

.export-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  padding: 0;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.radio-item {
  margin: 0 !important;
  height: auto;
}

.radio-text {
  font-size: 14px;
  color: #606266;
  margin-left: 8px;
}

.checkbox-group {
  display: flex;
  gap: 20px;
}

.checkbox-item {
  margin: 0 !important;
  height: auto;
}

.checkbox-text {
  font-size: 14px;
  color: #606266;
  margin-left: 8px;
}

/* 对话框底部按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
