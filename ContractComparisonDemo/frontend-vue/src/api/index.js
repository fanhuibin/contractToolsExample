import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api/compare',
  timeout: 60000,
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
    const res = response.data
    
    // 如果返回的状态码不是200，则抛出错误
    if (res.code !== 200) {
      console.error('接口错误:', res.message)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
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
   * 上传文件
   */
  uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    return axios.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000 // 60秒超时
    })
  },

  /**
   * 提交比对任务
   */
  submitCompare(oldFileUrl, newFileUrl, removeWatermark = false, oldFileName = '', newFileName = '') {
    return request({
      url: '/submit',
      method: 'post',
      data: {
        oldFileUrl,
        newFileUrl,
        removeWatermark,
        oldFileName,
        newFileName
      }
    })
  },

  /**
   * 获取任务状态
   */
  getTaskStatus(taskId) {
    return request({
      url: `/task/${taskId}`,
      method: 'get'
    })
  },

  /**
   * 获取比对结果
   */
  getResult(taskId) {
    return request({
      url: `/result/${taskId}`,
      method: 'get'
    })
  },

  /**
   * 删除任务
   */
  deleteTask(taskId) {
    return request({
      url: `/task/${taskId}`,
      method: 'delete'
    })
  },

  /**
   * 获取任务历史列表
   */
  getAllTasks() {
    return request({
      url: '/tasks',
      method: 'get'
    })
  },

  /**
   * 导出比对报告
   * @param {string} taskId - 任务ID
   * @param {Array<string>} formats - 导出格式 ['doc', 'html']
   */
  exportReport(taskId, formats = ['doc', 'html']) {
    return axios({
      url: `/api/compare/export-report`,
      method: 'post',
      data: {
        taskId,
        formats,
        includeIgnored: false,
        includeRemarks: true
      },
      responseType: 'blob', // 重要：处理二进制文件
      timeout: 120000 // 2分钟超时
    })
  }
}

