import baseRequest from '@/utils/request'

/**
 * 上传PDF文件进行智能文档解析
 */
export const uploadPdfForOcr = (formData: FormData) => {
  return baseRequest.post('/ocr/extract/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取任务状态
 */
export const getOcrTaskStatus = (taskId: string) => {
  return baseRequest.get(`/ocr/extract/status/${taskId}`)
}

/**
 * 获取智能文档解析结果
 */
export const getOcrResult = (taskId: string) => {
  return baseRequest.get(`/ocr/extract/result/${taskId}`)
}

/**
 * 获取页面图片URL
 */
export const getPageImageUrl = (taskId: string, pageNum: number) => {
  return `/api/ocr/extract/page-image/${taskId}/${pageNum}`
}

/**
 * 获取TextBox数据
 */
export const getTextBoxes = (taskId: string) => {
  return baseRequest.get(`/ocr/extract/textboxes/${taskId}`)
}

/**
 * 获取Bbox映射数据（用于处理跨页表格等）
 */
export const getBboxMappings = (taskId: string) => {
  return baseRequest.get(`/ocr/extract/bbox-mappings/${taskId}`)
}

/**
 * 删除任务
 */
export const deleteOcrTask = (taskId: string) => {
  return baseRequest.delete(`/ocr/extract/task/${taskId}`)
}

