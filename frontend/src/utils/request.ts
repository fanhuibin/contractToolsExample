import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例（将超时时间调大，以适配AI审核长耗时与大文件上传）
const request = axios.create({
  baseURL: '/api',
  timeout: 990000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 可以在这里添加token等认证信息
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response

    // 支持统一响应格式：{code: 200, message: "...", data: ...}
    if (data.code === 200) {
      return data
    } else {
      // 否则的话抛出错误
      const errorMessage = data.message || '请求失败'
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request 