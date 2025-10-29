import request from '@/utils/request'

/**
 * OnlyOffice相关API
 */

/**
 * 获取文档编辑器配置
 * @param params 参数
 * @param params.fileId 文件ID
 * @param params.canEdit 是否可编辑
 * @param params.canReview 是否可审阅
 * @param params.updateOnlyofficeKey 是否更新OnlyOffice密钥
 */
export function getEditorConfig(params: {
  fileId: string | number
  canEdit?: boolean
  canReview?: boolean
  updateOnlyofficeKey?: boolean
}) {
  return request({
    url: '/onlyoffice/editor/config',
    method: 'get',
    params
  })
}

/**
 * 获取OnlyOffice服务器信息
 */
export function getServerInfo() {
  return request({
    url: '/onlyoffice/server/info',
    method: 'get'
  })
}

/**
 * 文档保存回调（一般由OnlyOffice服务器调用，这里提供给前端测试用）
 * @param data 回调数据
 */
export function saveCallback(data: any) {
  return request({
    url: '/onlyoffice/callback/save',
    method: 'post',
    data
  })
}

/**
 * 健康检查
 */
export function healthCheck() {
  return request({
    url: '/onlyoffice/callback/health',
    method: 'get'
  })
}

/**
 * 下载文件
 * @param fileId 文件ID
 */
export function downloadFile(fileId: string | number) {
  return request({
    url: `/onlyoffice/callback/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 上传文件以供OnlyOffice使用
 * @param file 文件对象
 */
export function uploadFileForOnlyOffice(file: File) {
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
 * 触发OnlyOffice强制保存
 * 通过后端的Command Service向OnlyOffice Document Server发送强制保存命令
 * @param fileId 文件ID
 */
export function forceSaveFile(fileId: string | number) {
  return request({
    url: `/files/forcesave/${fileId}`,
    method: 'post'
  })
}