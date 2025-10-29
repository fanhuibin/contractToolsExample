import api from '@/api'

/**
 * 轮询任务状态
 */
export async function pollTaskStatus(taskId, onProgress, interval = 2000) {
  return new Promise((resolve, reject) => {
    const poll = async () => {
      try {
        const result = await api.getTaskStatus(taskId)
        const status = result.data
        
        // 调用进度回调
        if (onProgress && status.progressPercentage !== undefined) {
          onProgress(status.progressPercentage, status)
        }
        
        // 检查任务状态
        if (status.status === 'COMPLETED') {
          resolve(result)
        } else if (status.status === 'FAILED' || status.status === 'TIMEOUT') {
          reject(new Error(status.errorMessage || '任务失败'))
        } else {
          // 继续轮询
          setTimeout(poll, interval)
        }
      } catch (error) {
        reject(error)
      }
    }
    
    poll()
  })
}

/**
 * 格式化时间
 */
export function formatTime(timestamp) {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

