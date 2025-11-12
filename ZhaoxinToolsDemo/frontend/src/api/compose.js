import axios from 'axios'

/**
 * 智能合同合成 API
 */

/**
 * 获取合成模板列表
 * @param {Object} params - 查询参数
 * @param {string} params.status - 可选的模板状态筛选：PUBLISHED（已发布）、DRAFT（草稿）、DISABLED（已禁用）、DELETED（已删除）
 * @returns {Promise}
 */
export function listComposeTemplates(params = {}) {
  return axios.get('/api/compose/templates', { params })
}

/**
 * 获取模板详情（包含 elementsJson）
 * @param {string} templateId - 模板ID
 * @returns {Promise}
 */
export function getTemplateDetailById(id) {
  return axios.get(`/api/template/design/detail/${id}`)
}

/**
 * 根据模板ID获取模板详情
 * @param {string} templateId - 模板ID
 * @returns {Promise}
 */
export function getTemplateDetailByTemplateId(templateId) {
  return axios.get(`/api/template/design/byTemplate/${templateId}`)
}

/**
 * 合成合同
 * @param {Object} data - 合成请求数据
 * @param {string} data.templateCode - 模板编号（推荐使用，支持多版本）
 * @param {string} data.templateFileId - 模板文件ID（可选，向后兼容）
 * @param {Object} data.values - 填充数据
 * @returns {Promise}
 */
export function generateContract(data) {
  return axios.post('/api/compose/generate', data)
}

/**
 * 下载合成的合同
 * @param {string} fileId - 文件ID
 * @param {string} fileName - 文件名（可选）
 * @returns {Promise}
 */
export function downloadContract(fileId, fileName = null) {
  const params = fileName ? { fileName } : {}
  return axios.get(`/api/compose/download/${fileId}`, {
    params,
    responseType: 'blob'
  })
}

