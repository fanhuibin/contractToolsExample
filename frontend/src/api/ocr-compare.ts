import request from '@/utils/request'

export interface OCRCompareOptions {
  ignoreHeaderFooter?: boolean
  headerHeightMm?: number
  footerHeightMm?: number
  ignoreCase?: boolean
  ignoredSymbols?: string
  ignoreSpaces?: boolean
}

export interface OCRCompareTaskStatus {
  taskId: string
  status: string
  statusDesc: string
  progress: number
  currentStep: string
  totalSteps: number
  message?: string
  createdTime: string
  startTime?: string
  completedTime?: string
  errorMessage?: string
}

export interface OCRCompareResult {
  taskId: string
  oldPdfUrl: string
  newPdfUrl: string
  differences: Array<{
    operation: 'DELETE' | 'INSERT'
    text: string
    oldPosition?: {
      page: number
      x: number
      y: number
    }
    newPosition?: {
      page: number
      x: number
      y: number
    }
  }>
  summary: {
    totalDifferences: number
    deletions: number
    insertions: number
  }
}

export interface SupportedFormats {
  formats: string[]
  description: string
}

// 使用现有的compare/upload接口，通过useOCR参数来区分OCR比对
export function uploadOCRCompare(formData: FormData): Promise<{
  code: number
  message: string
  data: {
    id: string
    message: string
    useOCR: boolean
    [key: string]: any
  }
}> {
  // 添加useOCR参数
  formData.append('useOCR', 'true')
  
  return request({
    url: '/compare/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 其他接口保持不变，用于任务状态查询等
export function getOCRCompareTaskStatus(taskId: string): Promise<{
  code: number
  message: string
  data: OCRCompareTaskStatus
}> {
  return request({
    url: `/compare/ocr-task/${taskId}/status`,
    method: 'get'
  })
}

export function getAllOCRCompareTasks(): Promise<{
  code: number
  message: string
  data: OCRCompareTaskStatus[]
}> {
  return request({
    url: '/compare/ocr-task/list',
    method: 'get'
  })
}

export function deleteOCRCompareTask(taskId: string): Promise<{
  code: number
  message: string
  data: boolean
}> {
  return request({
    url: `/compare/ocr-task/${taskId}`,
    method: 'delete'
  })
}

export function getOCRCompareResult(taskId: string): Promise<{
  code: number
  message: string
  data: any
}> {
  return request({
    url: `/compare/ocr-task/${taskId}/result`,
    method: 'get'
  })
}

export function getSupportedFormats(): Promise<{
  code: number
  message: string
  data: SupportedFormats
}> {
  return request({
    url: '/ocr-compare/supported-formats',
    method: 'get'
  })
}

/**
 * 调试接口：使用已有的OCR结果进行比对
 * 跳过上传和OCR识别过程，直接使用已有的OCR任务ID进行比对
 */
export function debugCompareWithExistingOCR(params: {
  oldOcrTaskId: string, 
  newOcrTaskId: string, 
  options?: OCRCompareOptions
}): Promise<{
  code: number
  message: string
  data: {
    taskId: string
    message: string
  }
}> {
  return request({
    url: '/ocr-compare/debug',
    method: 'post',
    data: params
  })
}

