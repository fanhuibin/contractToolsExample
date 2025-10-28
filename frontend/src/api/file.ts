import request from '@/utils/request'

/**
 * 文件管理相关API
 */

export interface FileInfo {
  id: string
  fileName: string
  originalName: string
  fileExtension: string
  fileSize: number
  storePath: string
  onlyofficeKey: string
  status: number
  createTime?: string
  updateTime?: string
}

/**
 * 获取文件列表
 */
export function getFileList() {
  return request({
    url: '/file/list',
    method: 'get'
  })
}

/**
 * 获取文件信息
 * @param fileId 文件ID
 */
export function getFileInfo(fileId: string | number) {
  return request({
    url: `/file/${fileId}`,
    method: 'get'
  })
}

/**
 * 删除文件
 * @param fileId 文件ID
 */
export function deleteFile(fileId: string | number) {
  return request({
    url: `/file/${fileId}`,
    method: 'delete'
  })
}

/**
 * 下载文件
 * @param fileId 文件ID
 */
export function downloadFile(fileId: string | number) {
  return request({
    url: `/file/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 上传文件
 * @param file 文件对象
 */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/onlyoffice/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 触发 OnlyOffice 强制保存
 * 通过后端调用 OnlyOffice Document Server 的 Command Service
 * @param fileId 文件ID
 */
export function forceSaveFile(fileId: string | number) {
  return request({
    url: `/file/forcesave/${fileId}`,
    method: 'post'
  })
}

