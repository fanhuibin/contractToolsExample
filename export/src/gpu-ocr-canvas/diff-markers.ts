/**
 * 差异点标记组件 - 中间Canvas的差异图标渲染和交互
 */

import type { DifferenceItem, ClickableArea } from './types'
import { calculateDiffBoxAbsoluteY, getCanvasHeaderHeight, convertPageCoordinateToViewport } from './connection-lines'

export interface DiffMarkersProps {
  canvas: HTMLCanvasElement
  filteredResults: DifferenceItem[]
  leftWrapper: HTMLElement
  oldImageInfo: any
  newImageInfo: any
  clickableAreas: Map<string, ClickableArea>
}

// 绘制中间canvas背景
export const drawMiddleCanvasBackground = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
  // 绘制背景色
  ctx.fillStyle = '#f8f9fa'
  ctx.fillRect(0, 0, width, height)
}

// 计算差异图标在中间canvas中的位置
export const calculateDiffIconYPosition = (
  diff: DifferenceItem,
  leftWrapper: HTMLElement,
  oldImageInfo: any,
  newImageInfo: any
) => {
  if (!diff || !oldImageInfo || !leftWrapper) {
    return null
  }

  try {
    // 1. 计算差异框在左侧文档的绝对位置
    const absoluteY = calculateDiffBoxAbsoluteY(diff, leftWrapper, oldImageInfo, newImageInfo)
    if (absoluteY === null) return null
    
    // 2. 获取当前滚动位置和header高度
    const scrollTop = leftWrapper.scrollTop
    const headerHeight = getCanvasHeaderHeight()
    
    // 3. 转换为可视区域坐标
    const viewportY = convertPageCoordinateToViewport(absoluteY, scrollTop, headerHeight)
    
    // 4. 检查是否在可视范围内
    const viewportHeight = leftWrapper.clientHeight
    const visible = viewportY >= (headerHeight - 50) && viewportY <= (viewportHeight + headerHeight + 50)
    
    return {
      absoluteY,
      relativeY: viewportY,
      visible
    }
  } catch (error) {
    console.error('计算差异图标位置失败:', error)
    return null
  }
}

// 绘制差异图标
export const drawDiffIcon = (ctx: CanvasRenderingContext2D, x: number, y: number, operation: string) => {
  const iconSize = 20
  const halfSize = iconSize / 2
  const radius = 4 // 圆角半径
  
  // 保存当前状态
  ctx.save()
  
  // 设置颜色
  const isInsert = operation === 'INSERT'
  const baseColor = isInsert ? '#4CAF50' : '#F44336' // 更鲜艳的绿色和红色
  const darkColor = isInsert ? '#2E7D32' : '#C62828' // 更深的颜色用于边框和渐变
  
  // 创建渐变效果
  const gradient = ctx.createLinearGradient(
    x - halfSize, y - halfSize, 
    x + halfSize, y + halfSize
  )
  gradient.addColorStop(0, baseColor)
  gradient.addColorStop(1, darkColor)
  
  // 绘制圆角矩形背景
  ctx.fillStyle = gradient
  ctx.beginPath()
  ctx.roundRect(x - halfSize, y - halfSize, iconSize, iconSize, radius)
  ctx.fill()
  
  // 绘制深色边框
  ctx.strokeStyle = darkColor
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.roundRect(x - halfSize, y - halfSize, iconSize, iconSize, radius)
  ctx.stroke()
  
  // 绘制符号
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 14px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  if (isInsert) {
    // 绘制加号
    // 水平线
    ctx.fillRect(x - 6, y - 1.5, 12, 3)
    // 垂直线
    ctx.fillRect(x - 1.5, y - 6, 3, 12)
  } else {
    // 绘制减号
    ctx.fillRect(x - 6, y - 1.5, 12, 3)
  }
  
  // 添加高光效果
  const highlightGradient = ctx.createLinearGradient(
    x - halfSize, y - halfSize,
    x - halfSize, y - halfSize + iconSize / 3
  )
  highlightGradient.addColorStop(0, 'rgba(255, 255, 255, 0.3)')
  highlightGradient.addColorStop(1, 'rgba(255, 255, 255, 0)')
  
  ctx.fillStyle = highlightGradient
  ctx.beginPath()
  ctx.roundRect(x - halfSize, y - halfSize, iconSize, iconSize / 3, [radius, radius, 0, 0])
  ctx.fill()
  
  // 恢复状态
  ctx.restore()
}

// 渲染中间canvas的差异图标
export const renderMiddleCanvasDiffIcons = (
  props: DiffMarkersProps,
  onRenderComplete?: () => void
) => {
  const { canvas, filteredResults, leftWrapper, oldImageInfo, newImageInfo, clickableAreas } = props
  
  if (!canvas || !filteredResults.length) return
  
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  const width = canvas.width
  const height = canvas.height
  const centerX = width / 2
  
  // 清除并重绘背景
  drawMiddleCanvasBackground(ctx, width, height)
  
  // 清除之前的点击区域映射
  clickableAreas.clear()
  
  // 绘制所有可见的差异图标
  let renderedCount = 0
  filteredResults.forEach((diff, index) => {
    const position = calculateDiffIconYPosition(diff, leftWrapper, oldImageInfo, newImageInfo)
    if (position && position.visible) {
      drawDiffIcon(ctx, centerX, position.relativeY, diff.operation)
      
      // 记录点击区域
      const iconSize = 20
      const halfSize = iconSize / 2
      const clickableId = `middle-${index}`
      clickableAreas.set(clickableId, {
        x: centerX - halfSize,
        y: position.relativeY - halfSize,
        width: iconSize,
        height: iconSize,
        diffIndex: index,
        operation: diff.operation,
        bbox: diff.operation === 'DELETE' ? diff.oldBbox || [] : diff.newBbox || [],
        originalDiff: diff
      })
      
      renderedCount++
    }
  })
  
  
  // 渲染完成后调用回调，通常用于触发连接线渲染
  if (onRenderComplete) {
    onRenderComplete()
  }
}

// 初始化中间Canvas
export const initMiddleCanvas = (canvas: HTMLCanvasElement, leftWrapper: HTMLElement) => {
  if (!canvas || !leftWrapper) return
  
  const container = canvas.parentElement
  if (!container) return
  
  // 设置canvas尺寸 - 与左侧canvas容器保持完全一致
  const containerWidth = 80 // 固定宽度
  const containerHeight = container.clientHeight || leftWrapper.clientHeight || 600
  
  canvas.width = containerWidth
  canvas.height = containerHeight
  canvas.style.width = `${containerWidth}px`
  canvas.style.height = `${containerHeight}px`
  
  // 获取绘图上下文
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 清除canvas
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  // 绘制背景
  drawMiddleCanvasBackground(ctx, containerWidth, containerHeight)
  
}

// 处理中间Canvas点击事件
export const handleMiddleCanvasClick = (
  event: MouseEvent,
  canvas: HTMLCanvasElement,
  clickableAreas: Map<string, ClickableArea>,
  onDiffClick: (diffIndex: number, operation: string) => void
) => {
  if (!canvas) return
  
  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 检查点击是否在任何差异图标区域内
  for (const [id, area] of clickableAreas) {
    if (x >= area.x && x <= area.x + area.width &&
        y >= area.y && y <= area.y + area.height) {
      // 点击了差异图标，跳转到对应位置
      onDiffClick(area.diffIndex, area.operation)
      return
    }
  }
}

// 处理中间Canvas鼠标移动事件
export const handleMiddleCanvasMouseMove = (
  event: MouseEvent,
  canvas: HTMLCanvasElement,
  clickableAreas: Map<string, ClickableArea>
) => {
  if (!canvas) return
  
  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 检查鼠标是否在任何差异图标区域内
  let isOverIcon = false
  for (const [id, area] of clickableAreas) {
    if (x >= area.x && x <= area.x + area.width &&
        y >= area.y && y <= area.y + area.height) {
      isOverIcon = true
      break
    }
  }
  
  // 设置鼠标样式
  canvas.style.cursor = isOverIcon ? 'pointer' : 'default'
}

// 处理中间Canvas鼠标离开事件
export const handleMiddleCanvasMouseLeave = (canvas: HTMLCanvasElement) => {
  if (!canvas) return
  canvas.style.cursor = 'default'
}
