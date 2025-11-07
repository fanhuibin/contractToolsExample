import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { v4 as uuidv4 } from 'uuid'

// 创建axios实例（将超时时间调大，以适配AI审核长耗时与大文件上传）
const request = axios.create({
  baseURL: '/api',
  timeout: 990000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 生成请求追踪ID
    const traceId = uuidv4()
    config.headers['X-Trace-Id'] = traceId
    
    // 可以在这里添加token等认证信息
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers['Authorization'] = `Bearer ${token}`
    // }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// API响应接口
interface ApiResponse<T = any> {
  code: number
  message: string
  data?: T
  traceId?: string
  timestamp?: string
  errorDetail?: string
  metadata?: any
}

// 扩展axios配置，支持静默模式
declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 是否跳过错误通知（静默模式） */
    skipErrorNotification?: boolean
  }
}

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data, config } = response

    // 如果是blob响应（如文件下载），直接返回response
    if (config.responseType === 'blob') {
      return response
    }

    // 统一响应格式：{code: 200, message: "...", data: ...}
    const apiResponse: ApiResponse = data
    
    if (apiResponse.code === 200) {
      // 成功响应，返回完整的 AxiosResponse 对象
      // 前端期待: response.data.code / response.data.data
      response.data = apiResponse
      return response
    } else {
      // 业务错误
      if (!config.skipErrorNotification) {
        handleBusinessError(apiResponse)
      } else {
        // 静默模式：只打印到控制台
        console.warn(`业务错误 [Code: ${apiResponse.code}]: ${apiResponse.message}`)
      }
      return Promise.reject(new Error(apiResponse.message || '请求失败'))
    }
  },
  (error: AxiosError<ApiResponse>) => {
    // 处理HTTP错误
    if (error.response) {
      const { status, data } = error.response
      const traceId = error.response.headers['x-trace-id']
      
      // 打印TraceID，方便排查问题
      if (traceId) {
        console.error(`请求失败 [TraceID: ${traceId}]`)
      }
      
      // 401 认证错误
      if (status === 401) {
        ElMessage({
          type: 'error',
          message: '🔒 认证失败，请重新登录',
          duration: 3000
        })
        // 可以跳转到登录页
        // router.push('/login')
        return Promise.reject(new Error('认证失败'))
      }
      
      // 403 权限错误（含授权）
      if (status === 403) {
        const message = data?.message || '权限不足，无法访问该功能'
        ElMessage({
          type: 'error',
          message: `⚠️ ${message}`,
          duration: 5000,
          showClose: true
        })
        return Promise.reject(new Error(message))
      }
      
      // 404 资源不存在
      if (status === 404) {
        const message = data?.message || '请求的资源不存在'
        if (!error.config?.skipErrorNotification) {
          ElMessage.error(message)
        } else {
          console.warn(`资源不存在 [404]: ${message}`)
        }
        return Promise.reject(new Error(message))
      }
      
      // 429 请求频率过高
      if (status === 429) {
        ElMessage.warning('请求过于频繁，请稍后再试')
        return Promise.reject(new Error('请求频率过高'))
      }
      
      // 500 服务器错误
      if (status === 500) {
        const message = data?.message || '服务器内部错误'
        if (!error.config?.skipErrorNotification) {
          ElMessage({
            type: 'error',
            message: `❌ ${message}`,
            duration: 5000,
            showClose: true
          })
        } else {
          console.warn(`服务器错误 [500]: ${message}`)
        }
        
        // 开发环境显示详细错误
        if (data?.errorDetail && import.meta.env.DEV) {
          console.error('错误详情:', data.errorDetail)
        }
        
        return Promise.reject(new Error(message))
      }
      
      // 503 服务不可用
      if (status === 503) {
        ElMessage.error('服务暂时不可用，请稍后重试')
        return Promise.reject(new Error('服务不可用'))
      }
      
      // 其他HTTP错误
      const errorMessage = data?.message || error.message || '网络错误'
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
    
    // 网络错误或请求超时
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
      return Promise.reject(new Error('请求超时'))
    }
    
    // 网络断开
    if (!window.navigator.onLine) {
      ElMessage.error('网络连接已断开，请检查网络')
      return Promise.reject(new Error('网络断开'))
    }
    
    // 其他错误
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 处理业务错误
 */
function handleBusinessError(response: ApiResponse) {
  const { code, message, traceId } = response
  
  // 根据业务错误码显示不同类型的提示
  if (code >= 18000 && code < 19000) {
    // 授权相关错误（特殊处理）
    ElMessage({
      type: 'warning',
      message: `🔐 ${message}`,
      duration: 5000,
      showClose: true
    })
  } else if (code >= 17000 && code < 18000) {
    // 文件相关错误
    ElMessage({
      type: 'warning',
      message: `📁 ${message}`,
      duration: 4000
    })
  } else if (code >= 10000 && code < 17000) {
    // 其他业务错误
    ElMessage({
      type: 'error',
      message: message,
      duration: 4000
    })
  } else {
    // 通用错误
    ElMessage.error(message)
  }
  
  // 打印TraceID
  if (traceId) {
    console.error(`业务错误 [Code: ${code}] [TraceID: ${traceId}]: ${message}`)
  }
}

export default request 