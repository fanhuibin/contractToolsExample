/**
 * GPU OCR Canvas 渲染相关函数
 */

import type { 
  PageLayout, 
  DocumentImageInfo, 
  DifferenceItem, 
  CanvasMode,
  ClickableArea 
} from './types'
import { CANVAS_CONFIG, COLORS, PAGE_LABEL_CONFIG } from './constants'
import { imageManager } from './image-manager'

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
    canvas.style.left = '0'
    canvas.style.pointerEvents = 'none'
    canvases.push(canvas)
  }
  return canvases
}

/**
 * 渲染指定页面到指定Canvas
 * @param canvas Canvas元素
 * @param imageInfo 文档图片信息
 * @param pageIndex 页面索引
 * @param mode 渲染模式
 * @param differences 差异列表
 * @param layout 页面布局
 * @param clickableAreas 点击区域Map
 * @param imageBaseUrl 图片基础URL
 * @param taskId 任务ID
 */
export async function renderPageToCanvas(
  canvas: HTMLCanvasElement,
  imageInfo: DocumentImageInfo,
  pageIndex: number,
  mode: CanvasMode,
  differences: DifferenceItem[],
  layout: PageLayout[],
  clickableAreas: Map<string, ClickableArea>,
  imageBaseUrl: string,
  taskId: string
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
  
  // 调试信息：记录Canvas定位
  console.log(`Canvas ${mode} 第${pageIndex + 1}页定位: top=${layoutItem.y}px, height=${canvasHeight}px (页面=${scaledHeight}px + 分隔=${isLastPage ? 0 : CANVAS_CONFIG.PAGE_SPACING}px)`)
  
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
  const imageUrl = imageBaseUrl
    ? `${imageBaseUrl}/page-${pageIndex + 1}.png`
    : `/api/gpu-ocr/files/tasks/${taskId}/images/${mode}/page-${pageIndex + 1}.png`
  
  try {
    const image = await imageManager.loadImage(imageUrl)
    
    // 渲染期间若canvas已被复用，放弃本次绘制
    if ((canvas as any).__renderKey !== renderKey) {
      return
    }
    
    if (image) {
      ctx.drawImage(image, 0, 0, canvasWidth, scaledHeight)
      
      // 绘制差异标记
      const pageDifferences = differences.filter(diff => {
        const page = mode === 'old' ? diff.pageA : diff.pageB
        return page === pageIndex + 1
      })
      
      // 绘制差异标记（使用现有的绘制函数）
      for (const diff of pageDifferences) {
        if ((canvas as any).__renderKey !== renderKey) return
        const bbox = mode === 'old' ? diff.oldBbox : diff.newBbox
        if (bbox && bbox.length >= 4) {
          const x = bbox[0] * scale
          const y = bbox[1] * scale
          const width = (bbox[2] - bbox[0]) * scale
          const height = (bbox[3] - bbox[1]) * scale
          
          // 绘制差异框
          const colors = mode === 'old' ? COLORS.DELETE : COLORS.INSERT
          ctx.strokeStyle = colors.STROKE
          ctx.lineWidth = 2
          ctx.strokeRect(x, y, width, height)
          
          // 填充半透明背景
          ctx.fillStyle = colors.FILL
          ctx.fillRect(x, y, width, height)
        }
      }
    }

    // 右上角页码信息（分层Canvas每页）- 相对本页Canvas坐标，不受分隔影响
    if ((canvas as any).__renderKey !== renderKey) return
    drawPageLabel(ctx, pageIndex, imageInfo.pages.length, mode, canvasWidth)
    
  } catch (error) {
    console.error(`渲染第${pageIndex + 1}页失败:`, error)
  }
}

/**
 * 绘制页码标签
 * @param ctx Canvas上下文
 * @param pageIndex 页面索引
 * @param totalPages 总页数
 * @param mode 渲染模式
 * @param canvasWidth Canvas宽度
 */
function drawPageLabel(
  ctx: CanvasRenderingContext2D,
  pageIndex: number,
  totalPages: number,
  mode: CanvasMode,
  canvasWidth: number
): void {
  try {
    const labelText = `${mode === 'old' ? '旧' : '新'} 第 ${pageIndex + 1} / ${totalPages} 页`
    ctx.font = PAGE_LABEL_CONFIG.FONT
    const textMetrics = ctx.measureText(labelText)
    const labelWidth = Math.ceil(textMetrics.width) + PAGE_LABEL_CONFIG.PADDING_X * 2
    const labelHeight = PAGE_LABEL_CONFIG.HEIGHT
    const labelX = canvasWidth - labelWidth - PAGE_LABEL_CONFIG.MARGIN
    const labelY = PAGE_LABEL_CONFIG.MARGIN
    
    // 背景（圆角矩形）
    ctx.save()
    const radius = PAGE_LABEL_CONFIG.RADIUS
    ctx.beginPath()
    ctx.moveTo(labelX + radius, labelY)
    ctx.lineTo(labelX + labelWidth - radius, labelY)
    ctx.quadraticCurveTo(labelX + labelWidth, labelY, labelX + labelWidth, labelY + radius)
    ctx.lineTo(labelX + labelWidth, labelY + labelHeight - radius)
    ctx.quadraticCurveTo(labelX + labelWidth, labelY + labelHeight, labelX + labelWidth - radius, labelY + labelHeight)
    ctx.lineTo(labelX + radius, labelY + labelHeight)
    ctx.quadraticCurveTo(labelX, labelY + labelHeight, labelX, labelY + labelHeight - radius)
    ctx.lineTo(labelX, labelY + radius)
    ctx.quadraticCurveTo(labelX, labelY, labelX + radius, labelY)
    ctx.closePath()
    ctx.fillStyle = COLORS.PAGE_LABEL.BACKGROUND
    ctx.fill()
    
    // 文本
    ctx.fillStyle = COLORS.PAGE_LABEL.TEXT
    ctx.textBaseline = 'middle'
    ctx.fillText(labelText, labelX + PAGE_LABEL_CONFIG.PADDING_X, labelY + labelHeight / 2)
    ctx.restore()
  } catch (error) {
    console.error('绘制页码标签失败:', error)
  }
}
