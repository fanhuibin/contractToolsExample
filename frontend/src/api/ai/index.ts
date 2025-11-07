import axios from 'axios'

/**
 * AI合同相关API
 */
const aiContract = {
  /**
   * 获取合同类型列表
   */
  async getContractTypes() {
    try {
      const response = await axios.get('/api/ai/contract/types')
      return response.data
    } catch (error) {
      console.error('获取合同类型失败:', error)
      return { data: {} }
    }
  },

  /**
   * 根据ID获取模板
   * @param id 模板ID
   */
  async getTemplateById(id: number) {
    try {
      const response = await axios.get(`/api/ai/contract/template/${id}`)
      return response.data
    } catch (error) {
      console.error('获取模板失败:', error)
      throw error
    }
  }
}

/**
 * 导出AI相关API集合
 */
export default {
  aiContract
}

