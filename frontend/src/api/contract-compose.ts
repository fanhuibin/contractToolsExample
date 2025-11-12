import request from '@/utils/request'

export interface ComposeRequest {
  /** 模板文件ID（向后兼容） */
  templateFileId?: string
  /** 模板编号（推荐使用，支持多版本，优先获取已发布版本） */
  templateCode?: string
  values: Record<string, string>
  /** 普通印章URL及尺寸映射：key为SDT的tag */
  stampImageUrls?: Record<string, { normal?: string; width?: number; height?: number }>
  /** 骑缝章图片URL（可选） */
  ridingStampUrl?: string
  /** 骑缝章宽度（可选，单位：点，默认80） */
  ridingStampWidth?: number
  /** 骑缝章高度（可选，单位：点，默认80） */
  ridingStampHeight?: number
  /** 需要合并的额外PDF文件URL列表 */
  extraFiles?: string[]
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

