/**
 * GPU OCR Canvas 渲染相关函数
 */

import type { DocumentImageInfo, CanvasMode } from './types'
import type { ProcessedDifferenceItem } from './difference-processor'
import { CANVAS_CONFIG, COLORS, PAGE_LABEL_CONFIG } from './constants'
import { imageManager } from './image-manager'

/**
 * 渲染页面到Canvas（使用预处理的差异数据）
 * @param canvas Canvas元素
 * @param imageInfo 文档图片信息
 * @param pageIndex 页面索引
 * @param mode 渲染模式
 * @param differences 预处理的页面差异数据
 * @param layout 页面布局
 * @param baseUrl 图片基础URL
 * @param taskId 任务ID
 */
export async function renderPageToCanvas(
  canvas: HTMLCanvasElement,
  imageInfo: DocumentImageInfo,
  pageIndex: number,
  mode: CanvasMode,
  differences: ProcessedDifferenceItem[] = [],
  layout: any,
  baseUrl?: string,
  taskId?: string
): Promise<void> {
  if (!imageInfo.pages[pageIndex]) return
  
  const pageInfo = imageInfo.pages[pageIndex]
  const layoutItem = layout[pageIndex]
  const scale = layoutItem.scale
  const scaledHeight = layoutItem.height
  const canvasWidth = Math.round(pageInfo.width * scale)
  
  // 计算Canvas高度：页面高度 + 分隔空间（如果不是最后一页）
  const isLastPage = pageIndex === imageInfo.pages.length - 1
  const canvasHeight = isLastPage ? scaledHeight : scaledHeight + CANVAS_CONFIG.PAGE_SPACING
  
  // 设置Canvas尺寸和位置
  canvas.width = canvasWidth
  canvas.height = canvasHeight
  canvas.style.width = `${canvasWidth}px`
  canvas.style.height = `${canvasHeight}px`
  canvas.style.top = `${layoutItem.y}px`
  canvas.style.display = 'block'
  
  // 为该canvas设置渲染标识，避免异步加载完成后写入到已复用的canvas上
  const renderKey = `${mode}-${pageIndex}-${Date.now()}-${Math.random()}`
  ;(canvas as any).__renderKey = renderKey
  
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 清除Canvas
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  // 如果不是最后一页，在分隔区域绘制背景色
  if (!isLastPage) {
    ctx.fillStyle = COLORS.SEPARATOR.BACKGROUND
    ctx.fillRect(0, scaledHeight, canvasWidth, CANVAS_CONFIG.PAGE_SPACING)
  }
  
  // 加载并绘制图片
  // 优先使用后端提供的baseUrl，fallback为手动拼接
  const imageUrl = baseUrl 
    ? `${baseUrl}/page-${pageIndex + 1}.png`
    : `/api/compare-pro/files/tasks/${taskId}/images/${mode}/page-${pageIndex + 1}.png`
  
  console.log(`🔍 图片URL构建: baseUrl="${baseUrl}", taskId="${taskId}", mode="${mode}", 最终URL="${imageUrl}"`);
  
  console.log(`🎨 [渲染页面${pageIndex + 1}] ${mode}模式 - 收到${differences.length}个预处理差异项`)
  
  try {
    const image = await imageManager.loadImage(imageUrl)
    
    // 渲染期间若canvas已被复用，放弃本次绘制
    if ((canvas as any).__renderKey !== renderKey) {
      return
    }
    
    if (image) {
      ctx.drawImage(image, 0, 0, canvasWidth, scaledHeight)
      
      // 直接使用预处理过的页面差异数据进行绘制
      let drawnCount = 0
      for (const diffItem of differences) {
        if ((canvas as any).__renderKey !== renderKey) return
        
        const bbox = diffItem.singleBbox
        if (!bbox || bbox.length < 4) continue
        
        const x = bbox[0] * scale
        const y = bbox[1] * scale
        const width = (bbox[2] - bbox[0]) * scale
        const height = (bbox[3] - bbox[1]) * scale

        // 绘制差异框
        ctx.strokeStyle = mode === 'old' ? COLORS.DELETE.STROKE : COLORS.INSERT.STROKE
        ctx.lineWidth = 2
        ctx.strokeRect(x, y, width, height)

        // 填充半透明背景
        ctx.fillStyle = mode === 'old' ? COLORS.DELETE.FILL : COLORS.INSERT.FILL
        ctx.fillRect(x, y, width, height)
        
        drawnCount++
      }
      
      console.log(`🎨 [渲染页面${pageIndex + 1}] ${mode}模式 - 绘制了${drawnCount}个bbox`)
      
      // 绘制页码标识
      drawPageNumber(ctx, pageIndex + 1, 0, canvasWidth, scaledHeight, mode, imageInfo.pages.length)
    }
  } catch (error) {
    console.error('渲染Canvas失败:', error)
  }
}

/**
 * 创建Canvas池
 * @param count Canvas数量
 * @returns Canvas元素数组
 */
export function createCanvasPool(count: number): HTMLCanvasElement[] {
  const canvases: HTMLCanvasElement[] = []
  for (let i = 0; i < count; i++) {
    const canvas = document.createElement('canvas')
    canvas.style.position = 'absolute'
    canvas.style.display = 'none'
    canvases.push(canvas)
  }
  return canvases
}

/**
 * 绘制页码标识
 * @param ctx Canvas上下文
 * @param pageNum 页码
 * @param yOffset Y轴偏移
 * @param canvasWidth Canvas宽度
 * @param pageHeight 页面高度
 * @param mode 渲染模式
 * @param totalPages 总页数
 */
function drawPageNumber(
  ctx: CanvasRenderingContext2D, 
  pageNum: number, 
  yOffset: number, 
  canvasWidth: number, 
  pageHeight: number,
  mode: CanvasMode,
  totalPages: number
): void {
  const pageNumText = `${mode === 'old' ? '旧' : '新'} 第 ${pageNum} / ${totalPages} 页`
  ctx.font = PAGE_LABEL_CONFIG.FONT
  const textWidth = ctx.measureText(pageNumText).width
  const bgWidth = textWidth + PAGE_LABEL_CONFIG.PADDING_X * 2
  const bgHeight = PAGE_LABEL_CONFIG.HEIGHT
  const bgX = canvasWidth - bgWidth - PAGE_LABEL_CONFIG.MARGIN
  const bgY = yOffset + PAGE_LABEL_CONFIG.MARGIN
  
  ctx.save()
  
  // 绘制圆角背景
  const radius = PAGE_LABEL_CONFIG.RADIUS
  ctx.beginPath()
  ctx.moveTo(bgX + radius, bgY)
  ctx.lineTo(bgX + bgWidth - radius, bgY)
  ctx.quadraticCurveTo(bgX + bgWidth, bgY, bgX + bgWidth, bgY + radius)
  ctx.lineTo(bgX + bgWidth, bgY + bgHeight - radius)
  ctx.quadraticCurveTo(bgX + bgWidth, bgY + bgHeight, bgX + bgWidth - radius, bgY + bgHeight)
  ctx.lineTo(bgX + radius, bgY + bgHeight)
  ctx.quadraticCurveTo(bgX, bgY + bgHeight, bgX, bgY + bgHeight - radius)
  ctx.lineTo(bgX, bgY + radius)
  ctx.quadraticCurveTo(bgX, bgY, bgX + radius, bgY)
  ctx.closePath()
  ctx.fillStyle = COLORS.PAGE_LABEL.BACKGROUND
  ctx.fill()
  
  // 绘制文本
  ctx.fillStyle = COLORS.PAGE_LABEL.TEXT
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(pageNumText, bgX + bgWidth / 2, bgY + bgHeight / 2)
  
  ctx.restore()
}