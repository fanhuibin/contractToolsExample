import request from '@/utils/request'

export function extractInfo(file: File, prompt?: string, templateId?: number, taskTypes?: string[], keywords?: string[]) {
  const formData = new FormData()
  formData.append('file', file)
  if (prompt) formData.append('prompt', prompt)
  if (templateId) formData.append('templateId', templateId.toString())
  if (taskTypes && taskTypes.length) formData.append('taskTypes', JSON.stringify(taskTypes))
  if (keywords && keywords.length) formData.append('keywords', JSON.stringify(keywords))
  return request({ url: '/ai/auto-fulfillment/extract', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}

export function getTaskStatus(taskId: string) {
  return request({ url: `/ai/auto-fulfillment/status/${taskId}`, method: 'get' })
}

export function getTemplates(userId?: string) {
  const params = userId ? `?userId=${userId}` : ''
  return request({ url: `/ai/auto-fulfillment/template/list${params}`, method: 'get' })
}

export function getTemplatesByType(contractType: string, userId?: string) {
  const params = userId ? `?userId=${userId}` : ''
  return request({ url: `/ai/auto-fulfillment/template/type/${contractType}${params}`, method: 'get' })
}

export function getContractTypes() {
  return request({ url: '/ai/auto-fulfillment/template/contract-types', method: 'get' })
}

export function getTemplateById(id: number) {
  return request({ url: `/ai/auto-fulfillment/template/${id}`, method: 'get' })
}

export function createTemplate(template: any) {
  return request({ url: '/ai/auto-fulfillment/template/create', method: 'post', data: template })
}

export function updateTemplate(id: number, template: any) {
  return request({ url: `/ai/auto-fulfillment/template/${id}`, method: 'put', data: template })
}

export function deleteTemplate(id: number) {
  return request({ url: `/ai/auto-fulfillment/template/${id}`, method: 'delete' })
}

export function copyTemplate(id: number, newName: string, userId: string) {
  return request({ url: `/ai/auto-fulfillment/template/${id}/copy?newName=${encodeURIComponent(newName)}&userId=${userId}`, method: 'post' })
}

export function setDefaultTemplate(id: number, contractType: string) {
  return request({ url: `/ai/auto-fulfillment/template/${id}/set-default?contractType=${contractType}`, method: 'post' })
}

export function getHistory(userId: string) {
  return request({ url: `/ai/auto-fulfillment/history/list?userId=${userId}`, method: 'get' })
}

export default {
  extractInfo,
  getTaskStatus,
  getTemplates,
  getTemplatesByType,
  getContractTypes,
  getTemplateById,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  copyTemplate,
  setDefaultTemplate,
  getHistory,
  // dict apis
  getTaskTypesTree: () => request({ url: '/ai/auto-fulfillment/dicts/task-types', method: 'get' }),
  getKeywordsByTaskTypeIds: (ids: number[]) => request({ url: '/ai/auto-fulfillment/dicts/keywords', method: 'get', params: { taskTypeIds: ids } })
}


