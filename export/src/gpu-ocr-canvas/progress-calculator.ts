/**
 * GPU OCR 进度计算器
 * 提供基于页面数和时间的智能进度计算功能
 */

import { ref, computed } from 'vue'

export interface TaskData {
  currentStepDesc?: string
  oldDocPages?: number
  newDocPages?: number
  completedPagesOld?: number
  completedPagesNew?: number
  stageMinProgress?: number
  stageMaxProgress?: number
  stageEstimatedTime?: number
  stageElapsedTime?: number
  // 后端返回的预计时间字段
  estimatedTotalTime?: string
  remainingTime?: string
  [key: string]: any
}

export interface StageInfo {
  minProgress: number
  maxProgress: number
  estimatedTime: number
  elapsedTime: number
}

export interface ProgressState {
  loadingText: string
  displayProgress: number
  currentTaskData: TaskData | null
  currentStageInfo: StageInfo
}

/**
 * 创建进度计算器
 */
export function createProgressCalculator() {
  // 进度状态
  const progressState = ref<ProgressState>({
    loadingText: '加载中...0.0%',
    displayProgress: 0,
    currentTaskData: null,
    currentStageInfo: {
      minProgress: 0,
      maxProgress: 100,
      estimatedTime: 0,
      elapsedTime: 0
    }
  })

  // 定时器引用
  const smoothTimer = ref<number | null>(null)

  /**
   * 判断当前是否是OCR步骤
   */
  const isOCRStep = (): boolean => {
    const stepDesc = progressState.value.currentTaskData?.currentStepDesc || ''
    return stepDesc.includes('OCR识别原文档') || stepDesc.includes('OCR识别新文档')
  }

  /**
   * 基于页面进度计算OCR步骤的进度
   */
  const calculateOCRPageProgress = (): number => {
    const stageInfo = progressState.value.currentStageInfo
    const task = progressState.value.currentTaskData
    
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
      const stageProgressRatio = Math.min(1.0, stageInfo.elapsedTime / stageInfo.estimatedTime)
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

  /**
   * 计算基于时间的进度
   */
  const calculateTimeBasedProgress = (): number => {
    const stageInfo = progressState.value.currentStageInfo
    const stageProgressRatio = Math.min(1.0, stageInfo.elapsedTime / stageInfo.estimatedTime)
    const stageRange = stageInfo.maxProgress - stageInfo.minProgress
    return stageInfo.minProgress + (stageRange * stageProgressRatio)
  }

  /**
   * 更新平滑进度
   */
  const updateSmoothProgress = (): void => {
    const stageInfo = progressState.value.currentStageInfo
    
    // 如果没有有效的阶段信息，使用缓慢增长保持进度条活跃
    if (stageInfo.estimatedTime <= 0 || stageInfo.elapsedTime < 0) {
      if (progressState.value.displayProgress < 5.0) {
        progressState.value.displayProgress = Math.min(progressState.value.displayProgress + 0.01, 5.0)
      }
      progressState.value.loadingText = `加载中...${progressState.value.displayProgress.toFixed(1)}%`
      return
    }
    
    let calculatedProgress = 0
    
    // 检查是否是OCR步骤，如果是，优先使用页面进度
    if (isOCRStep()) {
      calculatedProgress = calculateOCRPageProgress()
    } else {
      // 非OCR步骤使用时间基础的进度计算
      calculatedProgress = calculateTimeBasedProgress()
    }
    
    // 平滑过渡到计算出的进度
    const diff = calculatedProgress - progressState.value.displayProgress
    
    if (Math.abs(diff) < 0.1) {
      progressState.value.displayProgress = calculatedProgress
    } else {
      progressState.value.displayProgress += diff * 0.15
    }
    
    // 确保进度在合理范围内
    progressState.value.displayProgress = Math.max(stageInfo.minProgress, 
      Math.min(progressState.value.displayProgress, calculatedProgress, stageInfo.maxProgress - 0.5))
    
    progressState.value.loadingText = `加载中...${progressState.value.displayProgress.toFixed(1)}%`
    
  }

  /**
   * 更新任务数据和阶段信息
   */
  const updateTaskDataAndStageInfo = (taskData: TaskData): void => {
    // 保存当前任务数据供页面进度计算使用
    progressState.value.currentTaskData = taskData
    
    
    // 启动平滑进度定时器（如果还没启动）
    if (!smoothTimer.value) {
      smoothTimer.value = window.setInterval(updateSmoothProgress, 300)
    }
    
    // 检查是否有完整的阶段信息
    if (taskData.stageMinProgress !== undefined && taskData.stageMaxProgress !== undefined && 
        taskData.stageEstimatedTime !== undefined && taskData.stageElapsedTime !== undefined) {
      
      const newStageInfo: StageInfo = {
        minProgress: taskData.stageMinProgress,
        maxProgress: taskData.stageMaxProgress,
        estimatedTime: taskData.stageEstimatedTime,
        elapsedTime: taskData.stageElapsedTime
      }
      
      progressState.value.currentStageInfo = newStageInfo
      
      // 调试日志：显示OCR进度信息
      if (isOCRStep() && taskData.currentStepDesc) {
        const isFirstDoc = taskData.currentStepDesc.includes('原文档')
        const currentDocPages = isFirstDoc ? taskData.oldDocPages : taskData.newDocPages
        const completedPages = isFirstDoc ? taskData.completedPagesOld : taskData.completedPagesNew
        
      }
    }
  }

  /**
   * 设置进度为完成状态
   */
  const setProgressComplete = (): void => {
    stopProgressUpdates()
    progressState.value.displayProgress = 100
    progressState.value.loadingText = '加载中...100.0%'
  }

  /**
   * 停止进度更新
   */
  const stopProgressUpdates = (): void => {
    if (smoothTimer.value) {
      clearInterval(smoothTimer.value)
      smoothTimer.value = null
    }
  }

  /**
   * 重置进度状态
   */
  const resetProgress = (): void => {
    stopProgressUpdates()
    progressState.value.loadingText = '加载中...0.0%'
    progressState.value.displayProgress = 0
    progressState.value.currentTaskData = null
    progressState.value.currentStageInfo = {
      minProgress: 0,
      maxProgress: 100,
      estimatedTime: 0,
      elapsedTime: 0
    }
  }

  // 计算属性：预计时间文本
  const estimatedTimeText = computed(() => {
    const taskData = progressState.value.currentTaskData
    
    // 优先使用后端返回的准确预计时间
    if (taskData?.estimatedTotalTime) {
      return `预计用时${taskData.estimatedTotalTime}，请稍候`
    }
    
    // 如果没有后端时间，使用剩余时间
    if (taskData?.remainingTime) {
      return `${taskData.remainingTime}，请稍候`
    }
    
    // 最后才使用阶段时间估算（作为备选方案）
    const stageInfo = progressState.value.currentStageInfo
    if (stageInfo && stageInfo.estimatedTime > 0) {
      const stageRemainingTime = Math.max(0, stageInfo.estimatedTime - stageInfo.elapsedTime)
      const minutes = Math.ceil(stageRemainingTime / 60000)
      
      let timeText = ''
      if (minutes <= 1) {
        timeText = '预计用时不到1分钟'
      } else {
        timeText = `预计用时约${minutes}分钟`
      }
      
      return timeText + '，请稍候'
    }
    
    return '预计用时计算中，请稍候'
  })

  // 清理函数
  const cleanup = (): void => {
    stopProgressUpdates()
  }

  return {
    // 状态
    progressState,
    estimatedTimeText,
    
    // 方法
    updateTaskDataAndStageInfo,
    setProgressComplete,
    stopProgressUpdates,
    resetProgress,
    cleanup,
    
    // 工具方法
    isOCRStep,
    calculateOCRPageProgress,
    calculateTimeBasedProgress
  }
}

export type ProgressCalculator = ReturnType<typeof createProgressCalculator>
