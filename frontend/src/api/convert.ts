import baseRequest from '@/utils/request'

/**
 * 文档格式转换API
 */
export const convertApi = {
  /**
   * 上传并转换文档为PDF
   * @param file 文档文件（Word、Excel、PPT等）
   * @returns 转换结果，包含下载链接
   */
  convertToPdf(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    
    return baseRequest.post('/convert/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 180000 // 3分钟超时
    })
  },

  /**
   * 获取PDF下载URL
   * @param fileName PDF文件名
   * @returns 完整的下载URL
   */
  getDownloadUrl(fileName: string) {
    return `/api/convert/download/${fileName}`
  }
}

export default convertApi

