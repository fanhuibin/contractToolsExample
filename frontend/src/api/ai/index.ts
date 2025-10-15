import baseRequest from '@/utils/request'

// removed: fulfillment APIs

/**
 * AI聊天相关API
 */
export const aiChat = {
  /**
   * 发送聊天消息
   * @param message 消息内容
   * @param sessionId 会话ID
   * @returns 响应结果
   */
  sendMessage(message: string, sessionId?: string) {
    const params = new URLSearchParams();
    params.append('message', message);
    if (sessionId) {
      params.append('sessionId', sessionId);
    }
    return baseRequest.post('/ai/chat/send', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
  },

  /**
   * 获取会话列表
   * @returns 会话列表
   */
  getSessions() {
    return baseRequest.get('/ai/chat/sessions');
  },

  /**
   * 删除会话
   * @param sessionId 会话ID
   * @returns 响应结果
   */
  deleteSession(sessionId: string) {
    return baseRequest.delete(`/ai/chat/session/${sessionId}`);
  }
};

/**
 * 合同信息提取相关API
 */
export const aiContract = {
  /**
   * 提取合同信息
   * @param file 合同文件（支持PDF、Word、Excel、图片等）
   * @param prompt 可选的提取提示
   * @param templateId 可选的模板ID
   * @returns 响应结果
   */
  extractInfo(file: File, prompt?: string, templateId?: number) {
    const formData = new FormData();
    formData.append('file', file);
    if (prompt) {
      formData.append('prompt', prompt);
    }
    if (templateId) {
      formData.append('templateId', templateId.toString());
    }
    return baseRequest.post('/ai/contract/extract', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  /**
   * 获取提取任务状态
   * @param taskId 任务ID
   * @returns 任务状态
   */
  getTaskStatus(taskId: string) {
    return baseRequest.get(`/ai/contract/status/${taskId}`);
  },
  
  /**
   * 获取所有合同提取模板
   * @param userId 可选的用户ID
   * @returns 模板列表
   */
  getTemplates(userId?: string) {
    const params = userId ? `?userId=${userId}` : '';
    return baseRequest.get(`/ai/contract/template/list${params}`);
  },
  
  /**
   * 根据合同类型获取模板
   * @param contractType 合同类型
   * @param userId 可选的用户ID
   * @returns 模板列表
   */
  getTemplatesByType(contractType: string, userId?: string) {
    const params = userId ? `?userId=${userId}` : '';
    return baseRequest.get(`/ai/contract/template/type/${contractType}${params}`);
  },
  
  /**
   * 获取所有合同类型
   * @returns 合同类型列表
   */
  getContractTypes() {
    return baseRequest.get('/ai/contract/template/contract-types');
  },
  
  /**
   * 获取指定合同类型的默认模板
   * @param contractType 合同类型
   * @returns 默认模板
   */
  getDefaultTemplate(contractType: string) {
    return baseRequest.get(`/ai/contract/template/default/${contractType}`);
  },
  // 新增：按ID获取模板详情（用于规则设置页）
  getTemplateById(id: number) {
    return baseRequest.get(`/ai/contract/template/${id}`);
  },
  
  /**
   * 创建新模板
   * @param template 模板信息
   * @returns 创建的模板
   */
  createTemplate(template: any) {
    return baseRequest.post('/ai/contract/template/create', template);
  },
  
  /**
   * 更新模板
   * @param id 模板ID
   * @param template 模板信息
   * @returns 更新后的模板
   */
  updateTemplate(id: number, template: any) {
    return baseRequest.put(`/ai/contract/template/${id}`, template);
  },
  
  /**
   * 删除模板
   * @param id 模板ID
   * @returns 操作结果
   */
  deleteTemplate(id: number) {
    return baseRequest.delete(`/ai/contract/template/${id}`);
  },
  
  /**
   * 复制模板
   * @param id 源模板ID
   * @param newName 新模板名称
   * @param userId 用户ID
   * @returns 复制的新模板
   */
  copyTemplate(id: number, newName: string, userId: string) {
    return baseRequest.post(`/ai/contract/template/${id}/copy?newName=${encodeURIComponent(newName)}&userId=${userId}`);
  },
  
  /**
   * 设置默认模板
   * @param id 模板ID
   * @param contractType 合同类型
   * @returns 设置为默认的模板
   */
  setDefaultTemplate(id: number, contractType: string) {
    return baseRequest.post(`/ai/contract/template/${id}/set-default?contractType=${contractType}`);
  },

  /**
   * 获取提取历史记录
   * @param userId 用户ID
   * @returns 历史记录列表
   */
  getHistory(userId: string) {
    return baseRequest.get(`/ai/contract/history/list?userId=${userId}`);
  }
};

export default {
  aiChat,
  aiContract
};