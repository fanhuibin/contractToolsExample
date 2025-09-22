import request from '@/utils/request'

// GPU OCR比对任务状态
export interface GPUOCRCompareTaskStatus {
  taskId: string
  status: 'PENDING' | 'OCR_PROCESSING' | 'COMPARING' | 'ANNOTATING' | 'COMPLETED' | 'FAILED' | 'TIMEOUT'
  statusDesc: string
  progress: number
  totalSteps: number
  currentStep: number
  currentStepDesc: string
  createdTime: string
  updatedTime: string
  errorMessage?: string
  oldFileName: string
  newFileName: string
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

// GPU OCR比对选项
export interface GPUOCRCompareOptions {
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
    url: '/gpu-ocr-compare/submit',
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
    url: `/gpu-ocr-compare/task/${taskId}`,
    method: 'get'
  })
}


// 获取Canvas版本的GPU OCR比对结果
export function getGPUOCRCanvasCompareResult(taskId: string) {
  return request({
    url: `/gpu-ocr-compare/canvas-result/${taskId}`,
    method: 'get'
  })
}

// 获取文档图片信息
export function getDocumentImages(taskId: string, mode: 'old' | 'new') {
  return request({
    url: `/gpu-ocr-compare/images/${taskId}/${mode}`,
    method: 'get'
  })
}

// 获取所有GPU OCR比对任务
export function getAllGPUOCRCompareTasks() {
  return request({
    url: '/gpu-ocr-compare/tasks',
    method: 'get'
  })
}

// 删除GPU OCR比对任务
export function deleteGPUOCRCompareTask(taskId: string) {
  return request({
    url: `/gpu-ocr-compare/task/${taskId}`,
    method: 'delete'
  })
}

// 调试模式：使用TaskId进行比对
export function debugGPUCompareWithExistingOCR(data: {
  taskId: string
  options: GPUOCRCompareOptions
}) {
  return request({
    url: '/gpu-ocr-compare/debug-compare',
    method: 'post',
    data
  })
}

// 调试模式：使用传统OCR任务ID进行比对（向后兼容）
export function debugGPUCompareLegacy(data: {
  oldOcrTaskId: string
  newOcrTaskId: string
  options: GPUOCRCompareOptions
}) {
  return request({
    url: '/gpu-ocr-compare/debug-compare-legacy',
    method: 'post',
    data
  })
}

// 获取任务队列状态
export function getQueueStats() {
  return request({
    url: '/gpu-ocr-compare/queue/stats',
    method: 'get'
  })
}

// 检查队列是否繁忙
export function checkQueueBusy() {
  return request({
    url: '/gpu-ocr-compare/queue/busy',
    method: 'get'
  })
}

// 动态调整最大并发线程数
export function adjustMaxConcurrency(maxThreads: number) {
  return request({
    url: '/gpu-ocr-compare/queue/adjust-concurrency',
    method: 'post',
    params: { maxThreads }
  })
}

// 获取任务摘要信息（包含差异总数）
export function getGPUOCRCompareTaskSummary(taskId: string) {
  return request({
    url: `/gpu-ocr-compare/task-summary/${taskId}`,
    method: 'get'
  })
}
