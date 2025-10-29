import request from '@/utils/request'

// 获取模板设计字段
export function fetchTemplateFields() {
  return request({
    url: '/template/fields',
    method: 'get'
  })
}

// 发起模板设计会话
export function startTemplateDesign(data: {
  templateId: string
  callbackUrl: string
  backendUrl: string
}) {
  return request({
    url: '/template/design/start',
    method: 'post',
    data
  })
}

// 保存模板元素内容（旧版）
export function saveTemplateDesign(data: {
  id?: string
  templateId?: string
  templateCode?: string
  templateName?: string
  description?: string
  version?: string
  status?: string
  fileId: string
  elements?: Array<{ key: string; tag: string; type: string; meta?: any }>
  elementsJson?: string
}) {
  // 后端保存的是 TemplateDesignRecord，elements 以 JSON 字符串入库
  const payload: any = {
    id: data.id,
    templateCode: data.templateCode,
    templateName: data.templateName,
    description: data.description,
    version: data.version,
    status: data.status,
    fileId: data.fileId,
  }
  
  // 兼容旧版templateId字段
  if (data.templateId) {
    payload.templateId = data.templateId
  }
  
  // 处理elementsJson
  if (data.elementsJson) {
    payload.elementsJson = data.elementsJson
  } else if (data.elements) {
    payload.elementsJson = JSON.stringify({ elements: data.elements })
  }
  
  return request({
    url: '/template/design/save',
    method: 'post',
    data: payload
  })
}

// 查询模板设计明细
export function getTemplateDesignDetail(id: string) {
  return request({
    url: `/template/design/detail/${id}`,
    method: 'get'
  })
}

// 通过模板ID查询模板设计明细
export function getTemplateDesignByTemplateId(templateId: string) {
  return request({
    url: `/template/design/byTemplate/${templateId}`,
    method: 'get'
  })
}

// 上传模板（仅支持docx）
export function uploadTemplateDocx(data: { templateId: string; file: File }) {
  const form = new FormData()
  form.append('templateId', data.templateId)
  form.append('file', data.file)
  return request({
    url: '/template/design/upload',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 模板设计记录列表
export function listTemplateDesigns() {
  return request({
    url: '/template/design/list',
    method: 'get'
  })
}

// 删除模板设计记录（软删除）
export function deleteTemplateDesign(id: string) {
  return request({
    url: `/template/design/detail/${id}`,
    method: 'delete'
  })
}

// 创建新版本（基于现有版本复制）
export function createNewVersion(sourceId: string, newVersion: string) {
  return request({
    url: '/template/design/version/create',
    method: 'post',
    params: { sourceId, newVersion }
  })
}

// 发布版本
export function publishVersion(id: string) {
  return request({
    url: `/template/design/version/publish/${id}`,
    method: 'post'
  })
}

// 更新状态
export function updateTemplateStatus(id: string, status: string) {
  return request({
    url: '/template/design/status/update',
    method: 'post',
    params: { id, status }
  })
}

// 获取所有版本
export function getTemplateVersions(templateCode: string) {
  return request({
    url: `/template/design/versions/${templateCode}`,
    method: 'get'
  })
}

// 获取最新版本
export function getLatestVersion(templateCode: string) {
  return request({
    url: `/template/design/version/latest/${templateCode}`,
    method: 'get'
  })
}

// 获取已发布版本
export function getPublishedVersion(templateCode: string) {
  return request({
    url: `/template/design/version/published/${templateCode}`,
    method: 'get'
  })
}


