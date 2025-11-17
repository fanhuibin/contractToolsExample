import axios from 'axios'
import { ZHAOXIN_CONFIG } from '@/config'

// 创建axios实例
// 使用 /api 前缀，通过 Vite 代理转发到后端
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
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
 * 查询模板列表
 */
export function listTemplates(params) {
  return request({
    url: '/rule-extract/templates',
    method: 'get',
    params
  })
}

/**
 * 上传文件并开始提取
 */
export function uploadAndExtract(formData) {
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
export function getRuleExtractTaskStatus(taskId) {
  return request({
    url: `/rule-extract/extract/status/${taskId}`,
    method: 'get'
  })
}

/**
 * 查询任务结果
 */
export function getRuleExtractTaskResult(taskId) {
  return request({
    url: `/rule-extract/extract/result/${taskId}`,
    method: 'get'
  })
}

/**
 * 取消任务
 */
export function cancelRuleExtractTask(taskId) {
  return request({
    url: `/rule-extract/extract/cancel/${taskId}`,
    method: 'post'
  })
}

/**
 * 查询任务列表
 */
export function listRuleExtractTasks(params) {
  return request({
    url: '/rule-extract/extract/tasks',
    method: 'get',
    params
  })
}

export default {
  listTemplates,
  uploadAndExtract,
  getRuleExtractTaskStatus,
  getRuleExtractTaskResult,
  cancelRuleExtractTask,
  listRuleExtractTasks
}

