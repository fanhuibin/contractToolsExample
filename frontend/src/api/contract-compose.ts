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
  code: number
  message: string
  data: ComposeResponseData
}> {
  return request({
    url: '/compose/sdt',
    method: 'post',
    data
  })
}

export function downloadTempFile(path: string) {
  // Use window.open to trigger download for files served from backend
  const url = `/api/download/temp?path=${encodeURIComponent(path)}`
  window.open(url, '_blank')
}


