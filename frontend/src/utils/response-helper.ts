/**
 * API响应格式处理辅助函数
 * 用于统一处理不同的后端返回格式
 */

/**
 * 提取数组数据
 * 处理多种后端返回格式：
 * 1. { data: [...] } - 直接返回数组
 * 2. { data: { data: [...] } } - 嵌套data
 * 3. { data: { list: [...] } } - 分页格式
 * 4. { data: { code: 200, data: [...] } } - 旧格式（带code）
 */
export function extractArrayData<T = any>(response: any): T[] {
  if (!response || !response.data) {
    return []
  }

  const data = response.data

  // 直接是数组
  if (Array.isArray(data)) {
    return data
  }

  // 嵌套data（新格式）
  if (data.data && Array.isArray(data.data)) {
    return data.data
  }

  // 分页格式
  if (data.list && Array.isArray(data.list)) {
    return data.list
  }

  // 旧格式（带code）
  if ((data.code === 200 || data.code === 0) && data.data) {
    if (Array.isArray(data.data)) {
      return data.data
    }
    // 旧格式的分页
    if (data.data.list && Array.isArray(data.data.list)) {
      return data.data.list
    }
  }

  // 都不是，返回空数组
  return []
}

/**
 * 提取对象数据
 * 处理多种后端返回格式
 */
export function extractObjectData<T = any>(response: any): T | null {
  if (!response || !response.data) {
    return null
  }

  const data = response.data

  // 旧格式（带code）
  if ((data.code === 200 || data.code === 0) && data.data) {
    return data.data as T
  }

  // 新格式 - 检查是否像业务对象（有特定属性）
  // 避免误判：如果有code属性但不是200/0，说明是错误响应
  if (data.code !== undefined && data.code !== 200 && data.code !== 0) {
    return null
  }

  // 如果data本身就是对象（不包含code/message等元数据字段）
  if (typeof data === 'object' && !Array.isArray(data)) {
    // 检查是否是元数据包装（包含code/message）
    const hasMetadata = 'code' in data || 'message' in data || 'success' in data
    
    if (!hasMetadata) {
      // 直接是业务对象
      return data as T
    } else if (data.data) {
      // 嵌套data
      return data.data as T
    }
  }

  return null
}

/**
 * 检查响应是否成功
 */
export function isResponseSuccess(response: any): boolean {
  if (!response || !response.data) {
    return false
  }

  const data = response.data

  // 旧格式
  if (data.code !== undefined) {
    return data.code === 200 || data.code === 0
  }

  // 新格式 - 如果没有code字段，认为成功
  // 因为axios拦截器已经处理了HTTP错误
  return true
}

