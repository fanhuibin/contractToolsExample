/**
 * GPU OCR Canvas 布局计算相关函数
 */

import type { PageLayout, DocumentImageInfo, VisibleRange } from './types'
import { CANVAS_CONFIG } from './constants'

/**
 * 计算页面在容器中的位置布局
 * @param imageInfo 文档图片信息
 * @param containerWidth 容器宽度
 * @returns 页面布局数组
 */
export function calculatePageLayout(imageInfo: DocumentImageInfo, containerWidth: number): PageLayout[] {
  const layout: PageLayout[] = []
  let currentY = 0
  
  for (let i = 0; i < imageInfo.pages.length; i++) {
    const pageInfo = imageInfo.pages[i]
    if (pageInfo) {
      const scale = containerWidth / pageInfo.width
      const scaledHeight = pageInfo.height * scale
      
      layout.push({
        y: currentY,
        height: scaledHeight,
        scale: scale,
        visible: false
      })
      
      currentY += scaledHeight + CANVAS_CONFIG.PAGE_SPACING
    }
  }
  
  return layout
}

/**
 * 更新可见Canvas范围
 * @param scrollTop 滚动位置
 * @param containerHeight 容器高度
 * @param pageLayout 页面布局
 * @returns 可见范围信息
 */
export function updateVisibleCanvases(
  scrollTop: number, 
  containerHeight: number, 
  pageLayout: PageLayout[]
): VisibleRange {
  const buffer = CANVAS_CONFIG.SCROLL_BUFFER
  const startY = Math.max(0, scrollTop - buffer)
  const endY = scrollTop + containerHeight + buffer
  
  let visiblePages: number[] = []
  
  
  // 计算在可见范围内的页面
  for (let i = 0; i < pageLayout.length; i++) {
    const page = pageLayout[i]
    const pageBottom = page.y + page.height
    
    if (pageBottom >= startY && page.y <= endY) {
      visiblePages.push(i)
    }
  }
  
  
  // 确保至少渲染指定数量的页面
  const minPages = CANVAS_CONFIG.MIN_RENDERED_PAGES
  if (visiblePages.length < minPages && pageLayout.length >= minPages) {
    // 找到当前视口中心页面
    const viewportCenter = scrollTop + containerHeight / 2
    let centerPageIndex = 0
    let minDistance = Infinity
    
    // 找到最接近视口中心的页面
    for (let i = 0; i < pageLayout.length; i++) {
      const page = pageLayout[i]
      const pageCenter = page.y + page.height / 2
      const distance = Math.abs(pageCenter - viewportCenter)
      if (distance < minDistance) {
        minDistance = distance
        centerPageIndex = i
      }
    }
    
    
    // 以中心页面为基准，向前后扩展
    const halfMinPages = Math.floor(minPages / 2)
    const start = Math.max(0, centerPageIndex - halfMinPages)
    const end = Math.min(pageLayout.length - 1, start + minPages - 1)
    
    visiblePages = []
    for (let i = start; i <= end; i++) {
      visiblePages.push(i)
    }
    
  }
  
  // 限制同时显示的Canvas数量
  if (visiblePages.length > CANVAS_CONFIG.MAX_VISIBLE_CANVASES) {
    const middle = Math.floor(visiblePages.length / 2)
    const start = Math.max(0, middle - Math.floor(CANVAS_CONFIG.MAX_VISIBLE_CANVASES / 2))
    visiblePages = visiblePages.slice(start, start + CANVAS_CONFIG.MAX_VISIBLE_CANVASES)
  }
  
  return {
    start: visiblePages[0] || 0,
    end: visiblePages[visiblePages.length - 1] || 0,
    visiblePages
  }
}

/**
 * 计算容器总高度
 * @param layout 页面布局
 * @returns 容器总高度
 */
export function calculateTotalHeight(layout: PageLayout[]): number {
  const lastPage = layout[layout.length - 1]
  return lastPage ? (lastPage.y + lastPage.height + CANVAS_CONFIG.PAGE_SPACING) : 0
}

/**
 * 获取Canvas宽度
 * @param container 容器元素
 * @returns Canvas宽度
 */
export function getCanvasWidth(container: HTMLElement | null): number {
  return container ? container.clientWidth : 800 // 默认800px作为后备
}
