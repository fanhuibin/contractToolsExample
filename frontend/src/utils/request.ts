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
    
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    if (data.code === 200) {
      return data
    } else {
      // 否则的话抛出错误
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request 