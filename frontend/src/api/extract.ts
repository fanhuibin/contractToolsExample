import request from '@/utils/request'

/**
 * 信息提取API
 */

// 从文件提取信息
export function extractFromFile(formData: FormData) {
  return request({
    url: '/extract/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取任务状态
export function getTaskStatus(taskId: string) {
  return request({
    url: `/extract/status/${taskId}`,
    method: 'get'
  })
}

// 获取提取结果
export function getExtractResult(taskId: string) {
  return request({
    url: `/extract/result/${taskId}`,
    method: 'get'
  })
}

// 获取可视化HTML（返回URL，由iframe直接访问）
export function getVisualizationUrl(taskId: string) {
  return `/api/extract/visualization/${taskId}`
}

// 获取支持的提取模式
export function getSupportedSchemas() {
  return request({
    url: '/extract/schemas',
    method: 'get'
  })
}

// 取消任务
export function cancelTask(taskId: string) {
  return request({
    url: `/extract/cancel/${taskId}`,
    method: 'post'
  })
}

// 数据类型定义
export interface ExtractTask {
  taskId: string
  status: 'initializing' | 'saving_file' | 'ocr_processing' | 'creating_document' | 
          'loading_schema' | 'configuring_llm' | 'extracting' | 'generating_visualization' |
          'finalizing' | 'completed' | 'failed' | 'cancelled'
  message: string
  progress: number
  description: string
  fileName?: string
  createdAt: string
  lastUpdated: string
}

export interface ExtractResult {
  taskId: string
  document: {
    id: string
    type: string
    contentLength: number
    ocrProvider: string
    ocrConfidence: number
  }
  extractions: {
    count: number
    items: Array<{
      field: string
      value: any
      confidence?: number
      charInterval?: {
        startPos: number
        endPos: number
      }
      metadata?: Record<string, any>
    }>
    schema: string
  }
  statistics: {
    totalFields: number
    positionedFields: number
    highConfidenceFields: number
    averageConfidence: number
    positionAccuracy: number
  }
  metadata: Record<string, any>
}

export interface SchemaInfo {
  schemas: string[]
  defaultSchema: string
  descriptions: Record<string, string>
}

// 增强OCR结果接口
export interface EnhancedOCRResult {
  content: string
  provider: string
  charBoxes: CharBox[]
  imagesPath?: string
  totalPages: number
}

export interface CharBox {
  page: number
  ch: string
  bbox: number[] // [x1, y1, x2, y2]
  category: string
}

export interface BboxMapping {
  interval: {
    id: string
    start: number
    end: number
  }
  text: string
  bboxes: BboxInfo[]
  pages: number[]
}

export interface BboxInfo {
  page: number
  bbox: number[]
  category: string
  character: string
}

// 增强提取结果接口
export interface EnhancedExtractResult {
  extractResult: ExtractResult
  ocrResult: EnhancedOCRResult
  ocrMetadata: {
    provider: string
    textLength: number
    totalPages: number
    charBoxCount: number
    processedAt: string
    imagesPath?: string
  }
  bboxMappings: BboxMapping[]
  hasPositionInfo: boolean
}

/**
 * 上传文件进行增强信息提取（包含位置信息）
 */
export const uploadFileEnhanced = (formData: FormData): Promise<ApiResponse<{ taskId: string }>> => {
  return request({
    url: '/extract/enhanced/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取增强提取结果
 */
export const getEnhancedTaskResult = (taskId: string): Promise<ApiResponse<EnhancedExtractResult>> => {
  return request({
    url: `/extract/enhanced/result/${taskId}`,
    method: 'get'
  })
}

/**
 * 获取任务的图片文件
 */
export const getTaskImage = (taskId: string, page: number): string => {
  return `/api/extract/files/tasks/${taskId}/images/page-${page}.png`
}

/**
 * 获取任务的CharBox数据
 */
export const getTaskCharBoxes = (taskId: string): Promise<ApiResponse<CharBox[]>> => {
  return request({
    url: `/extract/charboxes/${taskId}`,
    method: 'get'
  })
}

/**
 * 获取任务的位置映射数据
 */
export const getTaskBboxMappings = (taskId: string): Promise<ApiResponse<BboxMapping[]>> => {
  return request({
    url: `/extract/bbox-mappings/${taskId}`,
    method: 'get'
  })
}
