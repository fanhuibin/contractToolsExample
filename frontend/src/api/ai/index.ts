import axios from 'axios';

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    return Promise.reject(error);
  }
);

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
    return request.post('/ai/chat/send', params, {
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
    return request.get('/ai/chat/sessions');
  },

  /**
   * 删除会话
   * @param sessionId 会话ID
   * @returns 响应结果
   */
  deleteSession(sessionId: string) {
    return request.delete(`/ai/chat/session/${sessionId}`);
  }
};

/**
 * PDF抽取相关API
 * @deprecated 请使用 aiContract 替代
 */
export const aiPdf = {
  /**
   * 抽取PDF文本
   * @param file PDF文件
   * @returns 响应结果
   */
  extractText(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/ai/pdf/extract', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  /**
   * 获取抽取任务状态
   * @param taskId 任务ID
   * @returns 任务状态
   */
  getTaskStatus(taskId: string) {
    return request.get(`/ai/pdf/status/${taskId}`);
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
   * @returns 响应结果
   */
  extractInfo(file: File, prompt?: string) {
    const formData = new FormData();
    formData.append('file', file);
    if (prompt) {
      formData.append('prompt', prompt);
    }
    return request.post('/ai/contract/extract', formData, {
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
    return request.get(`/ai/contract/status/${taskId}`);
  }
};

export default {
  aiChat,
  aiPdf,
  aiContract
};