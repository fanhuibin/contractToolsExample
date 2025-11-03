import axios from 'axios'

/**
 * 根据模板ID读取规则配置
 * @param templateId 模板ID
 * @returns Promise with rule data
 */
export async function readRuleByTemplateId(templateId: number) {
  try {
    const response = await axios.get(`/api/ai/rules/${templateId}`)
    return response.data
  } catch (error) {
    console.error('读取规则失败:', error)
    throw error
  }
}

/**
 * 保存规则配置
 * @param templateId 模板ID
 * @param data 规则数据
 * @returns Promise
 */
export async function saveRuleByTemplateId(templateId: number, data: any) {
  try {
    const response = await axios.post(`/api/ai/rules/${templateId}`, data)
    return response.data
  } catch (error) {
    console.error('保存规则失败:', error)
    throw error
  }
}

