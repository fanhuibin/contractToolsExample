import request from '@/utils/request'

/**
 * 基于规则的合同信息抽取 API
 */

// ==================== 模板管理 ====================

/**
 * 创建模板
 */
export function createTemplate(data: any) {
  return request({
    url: '/rule-extract/templates',
    method: 'post',
    data
  })
}

/**
 * 更新模板
 */
export function updateTemplate(id: number, data: any) {
  return request({
    url: `/rule-extract/templates/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除模板
 */
export function deleteTemplate(id: number) {
  return request({
    url: `/rule-extract/templates/${id}`,
    method: 'delete'
  })
}

/**
 * 查询模板详情
 */
export function getTemplate(id: number) {
  return request({
    url: `/rule-extract/templates/${id}`,
    method: 'get'
  })
}

/**
 * 查询模板列表
 */
export function listTemplates(params?: any) {
  return request({
    url: '/rule-extract/templates',
    method: 'get',
    params
  })
}

/**
 * 分页查询模板
 */
export function pageTemplates(params: any) {
  return request({
    url: '/rule-extract/templates/page',
    method: 'get',
    params
  })
}

/**
 * 启用模板
 */
export function enableTemplate(id: number) {
  return request({
    url: `/rule-extract/templates/${id}/enable`,
    method: 'post'
  })
}

/**
 * 禁用模板
 */
export function disableTemplate(id: number) {
  return request({
    url: `/rule-extract/templates/${id}/disable`,
    method: 'post'
  })
}

/**
 * 复制模板
 */
export function copyTemplate(id: number, newName: string) {
  return request({
    url: `/rule-extract/templates/${id}/copy`,
    method: 'post',
    params: { newName }
  })
}

// ==================== 字段定义 ====================

/**
 * 创建字段
 */
export function createField(data: any) {
  return request({
    url: '/rule-extract/fields',
    method: 'post',
    data
  })
}

/**
 * 更新字段
 */
export function updateField(id: number, data: any) {
  return request({
    url: `/rule-extract/fields/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除字段
 */
export function deleteField(id: number) {
  return request({
    url: `/rule-extract/fields/${id}`,
    method: 'delete'
  })
}

/**
 * 查询字段详情
 */
export function getField(id: number) {
  return request({
    url: `/rule-extract/fields/${id}`,
    method: 'get'
  })
}

/**
 * 查询模板的所有字段
 */
export function listFieldsByTemplate(templateId: number) {
  return request({
    url: `/rule-extract/fields/template/${templateId}`,
    method: 'get'
  })
}

/**
 * 按分类查询字段
 */
export function listFieldsByCategory(templateId: number, category: string) {
  return request({
    url: `/rule-extract/fields/template/${templateId}/category/${category}`,
    method: 'get'
  })
}

/**
 * 批量创建字段
 */
export function batchCreateFields(data: any[]) {
  return request({
    url: '/rule-extract/fields/batch',
    method: 'post',
    data
  })
}

/**
 * 更新字段排序
 */
export function updateFieldOrder(id: number, sortOrder: number) {
  return request({
    url: `/rule-extract/fields/${id}/order`,
    method: 'put',
    params: { sortOrder }
  })
}

// ==================== 提取规则 ====================

/**
 * 创建规则
 */
export function createRule(data: any) {
  return request({
    url: '/rule-extract/rules',
    method: 'post',
    data
  })
}

/**
 * 更新规则
 */
export function updateRule(id: number, data: any) {
  return request({
    url: `/rule-extract/rules/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return request({
    url: `/rule-extract/rules/${id}`,
    method: 'delete'
  })
}

/**
 * 查询规则详情
 */
export function getRule(id: number) {
  return request({
    url: `/rule-extract/rules/${id}`,
    method: 'get'
  })
}

/**
 * 查询字段的所有规则
 */
export function listRulesByField(fieldId: number) {
  return request({
    url: `/rule-extract/rules/field/${fieldId}`,
    method: 'get'
  })
}

/**
 * 启用规则
 */
export function enableRule(id: number) {
  return request({
    url: `/rule-extract/rules/${id}/enable`,
    method: 'post'
  })
}

/**
 * 禁用规则
 */
export function disableRule(id: number) {
  return request({
    url: `/rule-extract/rules/${id}/disable`,
    method: 'post'
  })
}

/**
 * 更新规则优先级
 */
export function updateRulePriority(id: number, priority: number) {
  return request({
    url: `/rule-extract/rules/${id}/priority`,
    method: 'put',
    params: { priority }
  })
}

/**
 * 测试规则
 */
export function testRule(id: number, content: string) {
  return request({
    url: `/rule-extract/rules/${id}/test`,
    method: 'post',
    data: { content }
  })
}

/**
 * 批量创建规则
 */
export function batchCreateRules(data: any[]) {
  return request({
    url: '/rule-extract/rules/batch',
    method: 'post',
    data
  })
}

// ==================== 规则抽取 ====================

/**
 * 上传文件并开始抽取
 */
export function uploadAndExtract(formData: FormData) {
  return request({
    url: '/rule-extract/extract/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 查询任务状态
 */
export function getRuleExtractTaskStatus(taskId: string) {
  return request({
    url: `/rule-extract/extract/status/${taskId}`,
    method: 'get'
  })
}

/**
 * 查询任务结果
 */
export function getRuleExtractTaskResult(taskId: string) {
  return request({
    url: `/rule-extract/extract/result/${taskId}`,
    method: 'get'
  })
}

/**
 * 取消任务
 */
export function cancelRuleExtractTask(taskId: string) {
  return request({
    url: `/rule-extract/extract/cancel/${taskId}`,
    method: 'post'
  })
}

/**
 * 查询任务列表
 */
export function listRuleExtractTasks(params?: any) {
  return request({
    url: '/rule-extract/extract/tasks',
    method: 'get',
    params
  })
}

