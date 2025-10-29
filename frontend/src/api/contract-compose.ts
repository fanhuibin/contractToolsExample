import request from '@/utils/request'

export interface ComposeRequest {
  templateFileId: string
  values: Record<string, string>
  stampImageUrls?: Record<string, { normal?: string; riding?: string }>
}

export interface ComposeResponseData {
  fileId: string
  docxPath?: string
  pdfPath?: string
  stampedPdfPath?: string
  ridingStampPdfPath?: string
}

export function composeContract(data: ComposeRequest): Promise<{
  data: {
    code: number
    message: string
    data: ComposeResponseData
  }
}> {
  return request({
    url: '/compose/sdt',
    method: 'post',
    data
  })
}

export function downloadTempFile(path: string) {
  // Use window.open to trigger download for files served from backend
  const url = `/download/temp?path=${encodeURIComponent(path)}`
  window.open(url, '_blank')
}

/**
 * 基于模板创建合同（复制模板文件）
 */
export function createContractFromTemplate(templateFileId: string) {
  return request({
    url: `/file/copy/${templateFileId}`,
    method: 'post'
  })
}

