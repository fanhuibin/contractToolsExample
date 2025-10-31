import api from '@/api'

/**
 * 轮询任务状态直到完成
 * 
 * @param {string} taskId - 任务ID
 * @param {function} onProgress - 进度回调函数 (progress, statusData)
 * @returns {Promise} - 完成后resolve
 */
export async function pollTaskStatus(taskId, onProgress) {
  const maxAttempts = 180  // 最多3分钟（每2秒轮询一次）
  const interval = 2000    // 轮询间隔2秒
  
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const response = await api.getTaskStatus(taskId)
      const data = response.data.data || response.data
      
      const status = data.status
      const progress = data.progress || 0
      
      console.log(`[轮询] 任务状态: ${status}, 进度: ${progress}%`)
      
      // 调用进度回调
      if (onProgress) {
        onProgress(progress, data)
      }
      
      // 任务完成
      if (status === 'completed') {
        console.log('✅ 任务完成')
        return data
      }
      
      // 任务失败
      if (status === 'failed') {
        throw new Error(data.message || '任务失败')
      }
      
      // 任务取消
      if (status === 'cancelled') {
        throw new Error('任务已取消')
      }
      
      // 等待后继续轮询
      await new Promise(resolve => setTimeout(resolve, interval))
      
    } catch (error) {
      // 如果是任务失败的错误，直接抛出
      if (error.message === '任务失败' || error.message === '任务已取消') {
        throw error
      }
      
      // 其他错误，继续轮询
      console.warn('轮询出错，继续尝试:', error.message)
      await new Promise(resolve => setTimeout(resolve, interval))
    }
  }
  
  throw new Error('任务超时')
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

/**
 * 格式化时间
 */
export function formatTime(timeString) {
  if (!timeString) return '-'
  const date = new Date(timeString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

