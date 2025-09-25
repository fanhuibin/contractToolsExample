import request from '@/utils/request'

// GPU OCR比对任务状态（增强版，支持智能进度计算）
export interface GPUOCRCompareTaskStatus {
  taskId: string
  status: 'PENDING' | 'OCR_PROCESSING' | 'COMPARING' | 'ANNOTATING' | 'COMPLETED' | 'FAILED' | 'TIMEOUT'
  statusDescription: string // 后端通过Status枚举的getDescription()方法提供
  oldFileName: string
  newFileName: string
  currentStep: number
  currentStepDesc: string
  
  // 智能进度信息
  progressPercentage: number
  progressDescription: string
  currentStepDescription: string
  remainingTime: string
  estimatedTotalTime: string
  
  // 阶段进度范围信息（新增）
  stageMinProgress?: number     // 当前阶段最小进度
  stageMaxProgress?: number     // 当前阶段最大进度
  stageEstimatedTime?: number   // 当前阶段预估总时间（毫秒）
  stageElapsedTime?: number     // 当前阶段已过时间（毫秒）
  
  // 页面级别进度信息（新增）
  totalPages?: number           // 总页数（最大值）
  oldDocPages?: number          // 原文档页数
  newDocPages?: number          // 新文档页数
  currentPageOld?: number       // 当前处理的原文档页面
  currentPageNew?: number       // 当前处理的新文档页面
  completedPagesOld?: number    // 已完成的原文档页面数
  completedPagesNew?: number    // 已完成的新文档页面数
  
  // 时间统计
  startTime?: string
  endTime?: string
  totalDuration?: number
  stepDurations?: Record<string, number>
  
  // 错误和失败页面信息
  errorMessage?: string
  failedPages?: string[]
  failedPagesCount?: number
  
  // 向后兼容的字段
  progress?: number
  totalSteps?: number
  createdTime?: string
  updatedTime?: string
  oldPdfUrl?: string
  newPdfUrl?: string
  annotatedOldPdfUrl?: string
  annotatedNewPdfUrl?: string
}

// GPU OCR比对任务摘要（包含统计信息）
export interface GPUOCRCompareTaskSummary {
  taskId: string
  totalDiffCount: number
  deleteCount: number
  insertCount: number
  ignoreCount: number
  processingTimeMs: number
}

// GPU OCR比对结果
export interface GPUOCRCompareResult {
  taskId: string
  oldFileName: string
  newFileName: string
  oldPdfUrl: string
  newPdfUrl: string
  annotatedOldPdfUrl?: string
  annotatedNewPdfUrl?: string
  differences: any[]
  deleteCount: number
  insertCount: number
  ignoreCount: number
  totalDiffCount: number
  summary: string
  processingTimeMs: number
}

// 高级合同比对选项
export interface CompareOptions {
  ignoreHeaderFooter?: boolean
  headerHeightPercent?: number
  footerHeightPercent?: number
  ignoreCase?: boolean
  ignoredSymbols?: string
  ignoreSpaces?: boolean
  ignoreSeals?: boolean
  removeWatermark?: boolean
  watermarkRemovalStrength?: 'default' | 'extended' | 'loose' | 'smart'
}

// 去水印强度选项
export interface WatermarkStrengthOption {
  value: 'default' | 'extended' | 'loose' | 'smart'
  label: string
  description: string
  recommended?: boolean
}

// 任务队列统计信息
export interface TaskQueueStats {
  totalSubmitted: number
  totalCompleted: number
  totalRejected: number
  currentQueueSize: number
  activeThreads: number
  maxThreads: number
  executorCompletedTasks: number
  executorTotalTasks: number
}

// 上传GPU OCR比对任务
export function uploadGPUOCRCompare(formData: FormData) {
  return request({
    url: '/compare-pro/submit',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取GPU OCR比对任务状态
export function getGPUOCRCompareTaskStatus(taskId: string) {
  return request({
    url: `/compare-pro/task/${taskId}`,
    method: 'get'
  })
}


// 获取Canvas版本的GPU OCR比对结果
export function getGPUOCRCanvasCompareResult(taskId: string) {
  return request({
    url: `/compare-pro/canvas-result/${taskId}`,
    method: 'get'
  })
}

// 获取文档图片信息
export function getDocumentImages(taskId: string, mode: 'old' | 'new') {
  return request({
    url: `/compare-pro/images/${taskId}/${mode}`,
    method: 'get'
  })
}

// 获取所有GPU OCR比对任务
export function getAllGPUOCRCompareTasks() {
  return request({
    url: '/compare-pro/tasks',
    method: 'get'
  })
}

// 删除GPU OCR比对任务
export function deleteGPUOCRCompareTask(taskId: string) {
  return request({
    url: `/compare-pro/task/${taskId}`,
    method: 'delete'
  })
}

// 调试模式：使用TaskId进行比对
export function debugGPUCompareWithExistingOCR(data: {
  taskId: string
  options: CompareOptions
}) {
  return request({
    url: '/compare-pro/debug-compare',
    method: 'post',
    data
  })
}

// 调试模式：使用传统OCR任务ID进行比对（向后兼容）
export function debugGPUCompareLegacy(data: {
  oldOcrTaskId: string
  newOcrTaskId: string
  options: CompareOptions
}) {
  return request({
    url: '/compare-pro/debug-compare-legacy',
    method: 'post',
    data
  })
}

// 获取任务队列状态
export function getQueueStats() {
  return request({
    url: '/compare-pro/queue/stats',
    method: 'get'
  })
}

// 检查队列是否繁忙
export function checkQueueBusy() {
  return request({
    url: '/compare-pro/queue/busy',
    method: 'get'
  })
}

// 动态调整最大并发线程数
export function adjustMaxConcurrency(maxThreads: number) {
  return request({
    url: '/compare-pro/queue/adjust-concurrency',
    method: 'post',
    params: { maxThreads }
  })
}

// 获取任务摘要信息（包含差异总数）
export function getGPUOCRCompareTaskSummary(taskId: string) {
  return request({
    url: `/compare-pro/task-summary/${taskId}`,
    method: 'get'
  })
}

// 导出比对报告
export function exportCompareReport(exportData: {
  taskId: string
  formats: string[]
  includeIgnored?: boolean
  includeRemarks?: boolean
}) {
  return request({
    url: '/compare-pro/export-report',
    method: 'post',
    data: exportData,
    responseType: 'blob' // 重要：设置响应类型为blob以处理二进制数据
  })
}
