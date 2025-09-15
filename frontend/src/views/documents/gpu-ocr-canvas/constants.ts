/**
 * GPU OCR Canvas 比对相关的常量配置
 */

// Canvas相关常量
export const CANVAS_CONFIG = {
  // 最大同时显示的Canvas数量
  MAX_VISIBLE_CANVASES: 12,
  
  // 页面间距
  PAGE_SPACING: 20,
  
  // 虚拟滚动缓冲区高度
  SCROLL_BUFFER: 1500,
  
  // 最小渲染页面数
  MIN_RENDERED_PAGES: 8,
  
  // Canvas最大高度限制
  MAX_CANVAS_HEIGHT: 32767
} as const

// 滚动相关常量
export const SCROLL_CONFIG = {
  // 滚动结束检测延迟（毫秒）
  SCROLL_END_DELAY: 300,
  
  // 轮转活跃侧检测延迟（毫秒）
  WHEEL_TIMER_DELAY: 150,
  
  // 跳转完成后重新渲染延迟（毫秒）
  JUMP_RENDER_DELAY: 200,
  
  // 异常滚动增量阈值
  ABNORMAL_SCROLL_THRESHOLD: 500
} as const

// 参考线配置
export const MARKER_CONFIG = {
  // 参考线位置比例
  RATIO: 0.25,
  
  // 参考线视觉偏移像素
  VISUAL_OFFSET_PX: 20
} as const

// 文本相关常量
export const TEXT_CONFIG = {
  // 文本截断限制
  TRUNCATE_LIMIT: 80
} as const

// 颜色配置
export const COLORS = {
  // 删除操作颜色
  DELETE: {
    STROKE: '#f56c6c',
    FILL: 'rgba(245, 108, 108, 0.1)',
    HIGHLIGHT: 'rgba(255, 99, 99, 0.4)'
  },
  
  // 新增操作颜色
  INSERT: {
    STROKE: '#67c23a',
    FILL: 'rgba(103, 194, 58, 0.1)',
    HIGHLIGHT: 'rgba(103, 194, 58, 0.4)'
  },
  
  // 分隔符颜色
  SEPARATOR: {
    BACKGROUND: '#f5f6f8',
    WHITE_TRANSITION: '#ffffff',
    GRADIENT_TRANSITION: 'linear-gradient(180deg, #ffffff 0%, #f3f4f6 100%)'
  },
  
  // 页码标签颜色
  PAGE_LABEL: {
    BACKGROUND: 'rgba(0, 0, 0, 0.45)',
    TEXT: '#fff'
  }
} as const

// 页码标签配置
export const PAGE_LABEL_CONFIG = {
  PADDING_X: 8,
  PADDING_Y: 6,
  HEIGHT: 24,
  RADIUS: 6,
  FONT: 'bold 13px Arial, Helvetica, sans-serif',
  MARGIN: 10
} as const
