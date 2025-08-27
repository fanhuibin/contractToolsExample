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

// 保存模板元素内容
export function saveTemplateDesign(data: {
  id?: string
  templateId: string
  fileId: string
  elements: Array<{ key: string; tag: string; type: string; meta?: any }>
}) {
  // 后端保存的是 TemplateDesignRecord，elements 以 JSON 字符串入库
  const payload = {
    id: data.id,
    templateId: data.templateId,
    fileId: data.fileId,
    elementsJson: JSON.stringify({ elements: data.elements })
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
    url: `/template/design/${id}`,
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

// 删除模板设计记录
export function deleteTemplateDesign(id: string) {
  return request({
    url: `/template/design/${id}`,
    method: 'delete'
  })
}


