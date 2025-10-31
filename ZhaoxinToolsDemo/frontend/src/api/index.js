import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api/extract',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

/**
 * API 接口
 */
export default {
  /**
   * 上传文件并开始抽取
   */
  uploadAndExtract(file, templateId) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('templateId', templateId)
    
    return axios.post('/api/extract/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 120000
    })
  },

  /**
   * 查询任务状态
   */
  getTaskStatus(taskId) {
    return request({
      url: `/status/${taskId}`,
      method: 'get'
    })
  },

  /**
   * 获取抽取结果
   */
  getResult(taskId) {
    return request({
      url: `/result/${taskId}`,
      method: 'get'
    })
  },

  /**
   * 取消任务
   */
  cancelTask(taskId) {
    return request({
      url: `/cancel/${taskId}`,
      method: 'post'
    })
  },

  /**
   * 获取任务列表
   */
  getAllTasks() {
    return request({
      url: '/tasks',
      method: 'get'
    })
  },

  /**
   * 获取页面图片
   */
  getPageImage(taskId, pageNumber) {
    return axios({
      url: `/api/extract/page-image/${taskId}/${pageNumber}`,
      method: 'get',
      responseType: 'blob'
    })
  },

  /**
   * 获取模板列表
   */
  getTemplates() {
    return request({
      url: '/templates',
      method: 'get'
    })
  }
}

