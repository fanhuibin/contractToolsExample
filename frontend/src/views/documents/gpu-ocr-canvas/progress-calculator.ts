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
  estimatedOcrTimeOld?: number
  estimatedOcrTimeNew?: number
  startTime?: string
  status?: string
  [key: string]: any
}


export interface ProgressState {
  loadingText: string
  displayProgress: number
  currentTaskData: TaskData | null
}

// 导入简化的进度计算器
import { SimpleProgressCalculator } from '@/utils/simpleProgressCalculator'

/**
 * 创建进度计算器（已简化）
 */
export function createProgressCalculator() {
  // 进度状态
  const progressState = ref<ProgressState>({
    loadingText: '加载中...0.0%',
    displayProgress: 0,
    currentTaskData: null
  })

  // 定时器引用
  const smoothTimer = ref<number | null>(null)
  
  // 使用简化的进度计算器
  const progressCalculator = new SimpleProgressCalculator()

  /**
   * 更新平滑进度（简化版）
   */
  const updateSmoothProgress = (): void => {
    const task = progressState.value.currentTaskData
    
    if (!task) {
      // 缓慢增长保持进度条活跃
      if (progressState.value.displayProgress < 5.0) {
        progressState.value.displayProgress = Math.min(progressState.value.displayProgress + 0.01, 5.0)
      }
      progressState.value.loadingText = `加载中...${progressState.value.displayProgress.toFixed(1)}%`
      return
    }
    
    // 使用简化的进度计算器
    const newProgress = progressCalculator.calculateProgress(
      {
        oldDocPages: task.oldDocPages || 0,
        newDocPages: task.newDocPages || 0,
        completedPagesOld: task.completedPagesOld || 0,
        completedPagesNew: task.completedPagesNew || 0,
        estimatedOcrTimeOld: task.estimatedOcrTimeOld || 0,
        estimatedOcrTimeNew: task.estimatedOcrTimeNew || 0,
        currentStepDesc: task.currentStepDesc || '',
        status: task.status || '',
        startTime: task.startTime
      },
      progressState.value.displayProgress
    )
    
    // 平滑过渡
    const diff = newProgress - progressState.value.displayProgress
    
    if (Math.abs(diff) < 0.1) {
      progressState.value.displayProgress = newProgress
    } else {
      progressState.value.displayProgress += diff * 0.15
    }
    
    progressState.value.loadingText = `加载中...${progressState.value.displayProgress.toFixed(1)}%`
  }

  /**
   * 更新任务数据和阶段信息（简化版）
   */
  const updateTaskDataAndStageInfo = (taskData: TaskData): void => {
    // 保存当前任务数据
    progressState.value.currentTaskData = taskData
    
    // 启动平滑进度定时器（如果还没启动）
    if (!smoothTimer.value) {
      smoothTimer.value = window.setInterval(updateSmoothProgress, 300)
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
    progressCalculator.reset()
  }

  // 计算属性：预计时间文本
  const estimatedTimeText = computed(() => {
    const taskData = progressState.value.currentTaskData
    
    if (!taskData) {
      return '预计用时计算中，请稍候'
    }
    
    // 计算总的预估时间
    const estimatedOcrTimeOld = taskData.estimatedOcrTimeOld || 0
    const estimatedOcrTimeNew = taskData.estimatedOcrTimeNew || 0
    const totalEstimatedTime = estimatedOcrTimeOld + estimatedOcrTimeNew
    
    if (totalEstimatedTime > 0) {
      const minutes = Math.ceil(totalEstimatedTime / 60000)
      
      if (minutes <= 1) {
        return '预计用时不到1分钟，请稍候'
      } else {
        return `预计用时约${minutes}分钟，请稍候`
      }
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
    cleanup
  }
}

export type ProgressCalculator = ReturnType<typeof createProgressCalculator>
