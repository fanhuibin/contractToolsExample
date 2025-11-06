/**
 * 日期时间格式化工具
 * 提供统一的、用户友好的时间显示格式
 */

/**
 * 格式化日期时间为友好格式
 * @param dateTime 日期时间字符串或Date对象
 * @param format 格式类型：'datetime' | 'date' | 'time' | 'relative'
 * @returns 格式化后的字符串
 */
export function formatDateTime(dateTime: string | Date | null | undefined, format: 'datetime' | 'date' | 'time' | 'relative' = 'datetime'): string {
  if (!dateTime) {
    return '-'
  }

  try {
    const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return '-'
    }

    switch (format) {
      case 'datetime':
        // 2025-11-05 17:57:12
        return formatDateTimeString(date)
      
      case 'date':
        // 2025-11-05
        return formatDateString(date)
      
      case 'time':
        // 17:57:12
        return formatTimeString(date)
      
      case 'relative':
        // "刚刚"、"5分钟前"、"2小时前"、"昨天"等
        return formatRelativeTime(date)
      
      default:
        return formatDateTimeString(date)
    }
  } catch (error) {
    console.error('日期格式化失败:', error)
    return '-'
  }
}

/**
 * 格式化为 2025-11-05 17:57:12
 */
function formatDateTimeString(date: Date): string {
  const year = date.getFullYear()
  const month = padZero(date.getMonth() + 1)
  const day = padZero(date.getDate())
  const hours = padZero(date.getHours())
  const minutes = padZero(date.getMinutes())
  const seconds = padZero(date.getSeconds())
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化为 2025-11-05
 */
function formatDateString(date: Date): string {
  const year = date.getFullYear()
  const month = padZero(date.getMonth() + 1)
  const day = padZero(date.getDate())
  
  return `${year}-${month}-${day}`
}

/**
 * 格式化为 17:57:12
 */
function formatTimeString(date: Date): string {
  const hours = padZero(date.getHours())
  const minutes = padZero(date.getMinutes())
  const seconds = padZero(date.getSeconds())
  
  return `${hours}:${minutes}:${seconds}`
}

/**
 * 格式化为相对时间（人性化时间）
 */
function formatRelativeTime(date: Date): string {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  // 刚刚（1分钟内）
  if (diffSeconds < 60) {
    return '刚刚'
  }

  // X分钟前（1小时内）
  if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  }

  // X小时前（24小时内）
  if (diffHours < 24) {
    return `${diffHours}小时前`
  }

  // 昨天
  if (diffDays === 1) {
    return `昨天 ${formatTimeString(date)}`
  }

  // 前天
  if (diffDays === 2) {
    return `前天 ${formatTimeString(date)}`
  }

  // 本周内（7天内）
  if (diffDays < 7) {
    return `${diffDays}天前`
  }

  // 超过7天，显示完整日期时间
  return formatDateTimeString(date)
}

/**
 * 补零
 */
function padZero(num: number): string {
  return num.toString().padStart(2, '0')
}

/**
 * 格式化为中文日期时间
 * 2025年11月5日 17:57:12
 */
export function formatChineseDateTime(dateTime: string | Date | null | undefined): string {
  if (!dateTime) {
    return '-'
  }

  try {
    const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime
    
    if (isNaN(date.getTime())) {
      return '-'
    }

    const year = date.getFullYear()
    const month = date.getMonth() + 1
    const day = date.getDate()
    const hours = padZero(date.getHours())
    const minutes = padZero(date.getMinutes())
    const seconds = padZero(date.getSeconds())
    
    return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
  } catch (error) {
    console.error('日期格式化失败:', error)
    return '-'
  }
}

/**
 * 格式化为简洁日期时间（不显示秒）
 * 2025-11-05 17:57
 */
export function formatShortDateTime(dateTime: string | Date | null | undefined): string {
  if (!dateTime) {
    return '-'
  }

  try {
    const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime
    
    if (isNaN(date.getTime())) {
      return '-'
    }

    const year = date.getFullYear()
    const month = padZero(date.getMonth() + 1)
    const day = padZero(date.getDate())
    const hours = padZero(date.getHours())
    const minutes = padZero(date.getMinutes())
    
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch (error) {
    console.error('日期格式化失败:', error)
    return '-'
  }
}

