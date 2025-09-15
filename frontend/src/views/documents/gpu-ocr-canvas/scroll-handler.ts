/**
 * GPU OCR Canvas 滚动处理相关函数
 */

import type { Position, CanvasMode, ScrollSide } from './types'
import { calculatePageLayout, getCanvasWidth } from './layout'
import { MARKER_CONFIG } from './constants'

/**
 * Canvas连续滚动定位
 * @param side 滚动侧
 * @param pos 位置信息
 * @param wrapper 容器元素
 * @param imageInfo 文档图片信息
 */
export function alignCanvasViewerContinuous(
  side: CanvasMode,
  pos: Position | null,
  wrapper: HTMLElement,
  imageInfo: any
): void {
  if (!pos || !pos.page || !wrapper || !imageInfo) return

  try {
    // 使用预计算的布局，确保与渲染一致
    const containerWidth = getCanvasWidth(wrapper)
    const layout = calculatePageLayout(imageInfo, containerWidth)
    
    const pageIndex = pos.page - 1 // 转换为0-based索引
    if (pageIndex < 0 || pageIndex >= layout.length) {
      console.error(`页面索引超出范围: ${pos.page}, 总页数: ${layout.length}`)
      return
    }
    
    const pageLayout = layout[pageIndex]
    
    // 计算目标位置（图像坐标转换为显示坐标）
    const targetX = pos.x * pageLayout.scale
    const targetY = pageLayout.y + pos.y * pageLayout.scale + (pos.height || 0) * pageLayout.scale / 2 // 垂直居中

    // 计算滚动位置
    const markerY = wrapper.clientHeight * MARKER_CONFIG.RATIO + MARKER_CONFIG.VISUAL_OFFSET_PX
    const newScrollTop = Math.max(0, targetY - markerY)

    wrapper.scrollTop = newScrollTop

    console.log(`Canvas连续滚动定位完成: ${side}`, {
      页面: pos.page,
      页面布局Y: pageLayout.y,
      页面高度: pageLayout.height,
      缩放比例: pageLayout.scale,
      原始坐标: [pos.x, pos.y],
      目标坐标: [targetX, targetY],
      滚动位置: newScrollTop,
      markerY: markerY
    })

  } catch (error) {
    console.error(`Canvas连续滚动定位失败: ${side}`, error)
  }
}

/**
 * 跳转到指定页面
 * @param pageNum 页面号（1-based）
 * @param wrapper 容器元素
 * @param imageInfo 文档图片信息
 * @param actualCanvasWidth 实际Canvas宽度
 */
export function jumpToPage(
  pageNum: number,
  wrapper: HTMLElement,
  imageInfo: any,
  actualCanvasWidth: number
): void {
  if (!imageInfo || !wrapper) return
  
  // 使用记录的Canvas宽度，确保与渲染时一致
  const actualWidth = actualCanvasWidth || getCanvasWidth(wrapper)
  
  // 使用布局计算目标位置
  const layout = calculatePageLayout(imageInfo, actualWidth)
  const pageIndex = pageNum - 1
  
  if (pageIndex < 0 || pageIndex >= layout.length) {
    console.error(`页面索引超出范围: ${pageNum}, 总页数: ${layout.length}`)
    return
  }
  
  const targetY = layout[pageIndex].y
  
  console.log(`跳转到第${pageNum}页，目标Y位置: ${targetY.toFixed(2)}px`)
  
  // 滚动到目标位置
  wrapper.scrollTop = targetY
}

/**
 * 创建位置对象
 * @param bbox 边界框
 * @param page 页面号
 * @param description 描述
 * @returns 位置对象或null
 */
export function createPosition(
  bbox: number[] | undefined, 
  page: number, 
  description: string
): Position | null {
  if (!bbox || bbox.length < 4) {
    console.log(`前端跳转调试 - ${description}位置创建失败: bbox无效`, bbox)
    return null
  }
  return {
    x: bbox[0],
    y: bbox[1],
    width: bbox[2] - bbox[0],
    height: bbox[3] - bbox[1],
    page: page,
    bbox: bbox
  }
}
