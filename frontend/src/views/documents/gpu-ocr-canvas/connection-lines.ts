/**
 * 连接线组件 - 处理SVG连接线的绘制和管理
 */

import type { DifferenceItem } from './types'
import { calculatePageLayout, getCanvasWidth } from './layout'

export interface ConnectionLineProps {
  svg: SVGElement
  leftWrapper: HTMLElement
  rightWrapper: HTMLElement
  middleArea: HTMLElement
  oldImageInfo: any
  newImageInfo: any
  selectedDiffIndex: number | null
  filteredResults: DifferenceItem[]
}

// 根据操作类型获取颜色
export const getOperationColor = (operation: string): string => {
  if (operation === 'DELETE') {
    return 'rgba(255, 99, 99, 0.8)' // 删除：红色，与差异框颜色一致但更不透明
  } else if (operation === 'INSERT') {
    return 'rgba(103, 194, 58, 0.8)' // 新增：绿色，与差异框颜色一致但更不透明
  }
  return 'rgba(128, 128, 128, 0.8)' // 默认：灰色
}

// 创建单条连接线
export const createConnectionLine = (
  svg: SVGElement,
  x1: number, 
  y1: number, 
  x2: number, 
  y2: number, 
  color: string
) => {
  if (!svg) {
    console.error('SVG元素不存在')
    return
  }
  
  const line = document.createElementNS('http://www.w3.org/2000/svg', 'line')
  line.setAttribute('x1', Math.round(x1).toString())
  line.setAttribute('y1', Math.round(y1).toString())
  line.setAttribute('x2', Math.round(x2).toString())
  line.setAttribute('y2', Math.round(y2).toString())
  line.setAttribute('stroke', color)
  line.setAttribute('stroke-width', '1') // 调整线条宽度为1px
  line.setAttribute('opacity', '1')
  line.setAttribute('stroke-linecap', 'round')
  
  svg.appendChild(line)
}

// 获取canvas header的高度
export const getCanvasHeaderHeight = (): number => {
  // 查找canvas-header元素
  const canvasHeader = document.querySelector('.canvas-header')
  if (canvasHeader) {
    return canvasHeader.getBoundingClientRect().height
  }
  // 如果找不到，使用默认高度（根据CSS估算）
  return 40 // 默认header高度
}

// 将页面绝对坐标转换为可视区域相对坐标
export const convertPageCoordinateToViewport = (
  absoluteY: number, 
  scrollTop: number, 
  headerHeight: number
): number => {
  // 计算相对于可视区域的位置
  const relativeY = absoluteY - scrollTop
  
  // 由于中间区域有header，需要加上header高度的偏移
  const viewportY = relativeY + headerHeight
  
  return viewportY
}

// 计算差异框在左侧的绝对位置（使用与差异框绘制完全相同的逻辑）
export const calculateDiffBoxAbsoluteY = (
  diff: DifferenceItem,
  leftWrapper: HTMLElement,
  oldImageInfo: any,
  newImageInfo: any
): number | null => {
  if (!diff || !leftWrapper) {
    return null
  }

  try {
    let bbox: number[] | undefined
    let pageNum: number
    let targetImageInfo: any

    if (diff.operation === 'DELETE') {
      // 删除项：直接使用左侧文档数据
      bbox = diff.oldBbox
      pageNum = diff.pageA || diff.page || 1
      targetImageInfo = oldImageInfo
    } else if (diff.operation === 'INSERT') {
      // 新增项：优先使用prevOldBbox，如果没有则映射newBbox
      if (diff.prevOldBbox) {
        bbox = diff.prevOldBbox
        pageNum = diff.pageA || diff.page || 1
        targetImageInfo = oldImageInfo
      } else if (diff.newBbox && newImageInfo && leftWrapper) {
        // 将右侧坐标映射到左侧坐标系统
        return mapRightCoordinateToLeftAbsolute(diff, leftWrapper, oldImageInfo, newImageInfo)
      } else {
        return null
      }
    } else {
      return null
    }

    if (!bbox || bbox.length < 4 || pageNum < 1 || !targetImageInfo) {
      return null
    }

    // 使用左侧canvas的布局计算绝对位置
    const containerWidth = getCanvasWidth(leftWrapper)
    const layout = calculatePageLayout(targetImageInfo, containerWidth)
    
    const pageIndex = pageNum - 1
    if (pageIndex >= layout.length) {
      return null
    }

    const pageLayout = layout[pageIndex]
    
    // 使用与差异框绘制完全相同的计算逻辑
    const scale = pageLayout.scale
    const y = bbox[1] * scale
    const height = (bbox[3] - bbox[1]) * scale
    const centerY = y + height / 2
    
    // 计算在整个左侧文档容器中的绝对位置
    const absoluteY = pageLayout.y + centerY
    
    return absoluteY
  } catch (error) {
    console.error('计算差异框绝对位置失败:', error)
    return null
  }
}

// 将右侧坐标映射到左侧绝对坐标
export const mapRightCoordinateToLeftAbsolute = (
  diff: DifferenceItem,
  leftWrapper: HTMLElement,
  oldImageInfo: any,
  newImageInfo: any
): number | null => {
  if (!diff.newBbox || !newImageInfo || !oldImageInfo || !leftWrapper) {
    return null
  }

  try {
    const rightBbox = diff.newBbox
    const rightPageNum = diff.pageB || diff.page || 1
    
    // 计算在右侧文档中的位置
    const rightContainerWidth = getCanvasWidth(leftWrapper) // 假设两侧宽度相同
    const rightLayout = calculatePageLayout(newImageInfo, rightContainerWidth)
    const rightPageIndex = rightPageNum - 1
    
    if (rightPageIndex >= rightLayout.length) return null
    
    const rightPageLayout = rightLayout[rightPageIndex]
    const rightScale = rightPageLayout.scale
    const rightY = rightBbox[1] * rightScale
    const rightHeight = (rightBbox[3] - rightBbox[1]) * rightScale
    const rightCenterY = rightY + rightHeight / 2
    
    // 计算在右侧页面中的相对位置（0-1比例）
    const rightPageProgress = rightCenterY / rightPageLayout.height
    
    // 映射到左侧对应页面
    const leftContainerWidth = getCanvasWidth(leftWrapper)
    const leftLayout = calculatePageLayout(oldImageInfo, leftContainerWidth)
    const leftPageIndex = Math.min(rightPageIndex, leftLayout.length - 1)
    
    if (leftPageIndex < 0) return null
    
    const leftPageLayout = leftLayout[leftPageIndex]
    const leftMappedCenterY = rightPageProgress * leftPageLayout.height
    const leftMappedAbsoluteY = leftPageLayout.y + leftMappedCenterY
    
    return leftMappedAbsoluteY
  } catch (error) {
    console.error('右侧坐标映射到左侧失败:', error)
    return null
  }
}

// 计算差异框在容器中的相对Y位置
export const calculateDiffBoxRelativeY = (
  diff: DifferenceItem, 
  side: 'left' | 'right',
  leftWrapper: HTMLElement | null,
  rightWrapper: HTMLElement | null,
  oldImageInfo: any,
  newImageInfo: any
): number | null => {
  try {
    let bbox: number[] | undefined
    let pageNum: number = 1
    let canvasWrapper: HTMLElement | null = null
    let imageInfo: any = null
    
    if (side === 'left') {
      if (diff.operation === 'DELETE') {
        bbox = diff.oldBbox
        pageNum = diff.pageA || diff.page || 1
      } else if (diff.operation === 'INSERT' && diff.prevOldBbox) {
        // INSERT操作的左侧连接：使用prevOldBbox（旧文档不同处）
        bbox = diff.prevOldBbox
        pageNum = diff.pageA || diff.page || 1
      }
      canvasWrapper = leftWrapper
      imageInfo = oldImageInfo
    } else if (side === 'right') {
      if (diff.operation === 'INSERT') {
        bbox = diff.newBbox
        pageNum = diff.pageB || diff.page || 1
      } else if (diff.operation === 'DELETE' && diff.prevNewBbox) {
        // DELETE操作的右侧连接：使用prevNewBbox（新文档不同处）
        bbox = diff.prevNewBbox
        pageNum = diff.pageB || diff.page || 1
      }
      canvasWrapper = rightWrapper
      imageInfo = newImageInfo
    }
    
    if (!bbox || bbox.length < 4 || !canvasWrapper || !imageInfo) return null
    
    const containerWidth = getCanvasWidth(canvasWrapper)
    const layout = calculatePageLayout(imageInfo, containerWidth)
    const pageIndex = pageNum - 1
    
    if (pageIndex < 0 || pageIndex >= layout.length) return null
    
    const pageLayout = layout[pageIndex]
    const scale = pageLayout.scale
    const y = bbox[1] * scale
    const height = (bbox[3] - bbox[1]) * scale
    const centerY = y + height / 2
    const absoluteY = pageLayout.y + centerY
    
    // 实时获取当前滚动位置
    const scrollTop = canvasWrapper.scrollTop
    
    // 返回相对于容器可视区域的位置
    const headerHeight = getCanvasHeaderHeight()
    return absoluteY - scrollTop + headerHeight
  } catch (error) {
    console.error('计算差异框相对位置失败:', error)
    return null
  }
}

// 渲染跨容器的连接线
export const renderConnectionLines = (props: ConnectionLineProps) => {
  const { 
    svg, 
    leftWrapper, 
    rightWrapper, 
    middleArea, 
    oldImageInfo, 
    newImageInfo, 
    selectedDiffIndex, 
    filteredResults 
  } = props

  if (!svg || !leftWrapper || !middleArea) return
  
  // 清除现有连接线
  svg.innerHTML = ''
  
  // 获取容器位置信息
  const compareContainer = svg.parentElement
  if (!compareContainer) return
  
  // 设置SVG尺寸与容器一致
  const containerRect = compareContainer.getBoundingClientRect()
  svg.setAttribute('width', containerRect.width.toString())
  svg.setAttribute('height', containerRect.height.toString())
  svg.setAttribute('viewBox', `0 0 ${containerRect.width} ${containerRect.height}`)
  
  console.log('SVG容器设置:', {
    width: containerRect.width,
    height: containerRect.height,
    viewBox: `0 0 ${containerRect.width} ${containerRect.height}`
  })
  
  const leftBox = compareContainer.querySelector('.left-box') as HTMLElement
  const middleAreaElement = compareContainer.querySelector('.middle-interaction-area') as HTMLElement
  const rightBox = compareContainer.querySelector('.right-box') as HTMLElement
  
  if (!leftBox || !middleAreaElement || !rightBox) return
  
  // 获取各容器的位置和尺寸
  const compareRect = compareContainer.getBoundingClientRect()
  const leftRect = leftBox.getBoundingClientRect()
  const middleRect = middleAreaElement.getBoundingClientRect()
  const rightRect = rightBox.getBoundingClientRect()
  
  // 计算相对位置
  const leftRelativeX = leftRect.left - compareRect.left
  const leftRelativeY = leftRect.top - compareRect.top
  const middleRelativeX = middleRect.left - compareRect.left
  const middleRelativeY = middleRect.top - compareRect.top
  const rightRelativeX = rightRect.left - compareRect.left
  const rightRelativeY = rightRect.top - compareRect.top
  
  // 只渲染选中差异项的连接线
  
  // 如果没有选中任何差异项，不渲染连接线
  if (selectedDiffIndex === null) {
    return
  }
  
  // 只处理选中的差异项
  const selectedDiff = filteredResults[selectedDiffIndex]
  if (!selectedDiff) {
    return
  }
  
  // 计算差异图标在中间的Y位置
  const calculateDiffIconYPosition = (diff: DifferenceItem) => {
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
  
  const position = calculateDiffIconYPosition(selectedDiff)
  if (!position || !position.visible) {
    return
  }
  
  const diff = selectedDiff
    
  const middleIconX = middleRelativeX + middleRect.width / 2
  const middleIconY = middleRelativeY + position.relativeY
  
  // 1. DELETE类型左侧连接线：起点=差异框右边线中点，终点=差异图标左边线中点（直线）
  if (diff.operation === 'DELETE') {
    const leftDiffBoxY = calculateDiffBoxRelativeY(diff, 'left', leftWrapper, rightWrapper, oldImageInfo, newImageInfo)
    if (leftDiffBoxY !== null && diff.oldBbox) {
      const diffBoxY = leftRelativeY + leftDiffBoxY
      
      // 获取左侧canvas容器信息
      const leftCanvasWrapper = leftBox.querySelector('.canvas-wrapper') as HTMLElement
      if (leftCanvasWrapper && oldImageInfo) {
        const leftCanvasRect = leftCanvasWrapper.getBoundingClientRect()
        const leftCanvasRelativeX = leftCanvasRect.left - compareRect.left
        
        // 计算差异框的实际右边缘X坐标
        const containerWidth = getCanvasWidth(leftWrapper)
        const layout = calculatePageLayout(oldImageInfo, containerWidth)
        const pageIndex = (diff.pageA || diff.page || 1) - 1
        
        if (pageIndex >= 0 && pageIndex < layout.length) {
          const pageLayout = layout[pageIndex]
          const scale = pageLayout.scale
          const diffBoxRightX = diff.oldBbox[2] * scale // bbox[2] 是右边缘x坐标
          
          // 起点：差异框右边线中点
          const startX = leftCanvasRelativeX + diffBoxRightX
          const startY = diffBoxY
          
          // 终点：差异图标左边线中点
          const endX = middleIconX - 10 // 图标左边缘
          const endY = middleIconY
          
           // 绘制红色连接线，使用DELETE差异框的颜色
           createConnectionLine(svg, startX, startY, endX, endY, getOperationColor('DELETE'))
        }
      }
    }
  }
  
  // 2. INSERT类型左侧连接线：起点=旧文档不同处（prevOldBbox）右边线中点，终点=新增图标左边线中点（直线）
  if (diff.operation === 'INSERT' && diff.prevOldBbox) {
    const leftDiffBoxY = calculateDiffBoxRelativeY(diff, 'left', leftWrapper, rightWrapper, oldImageInfo, newImageInfo)
    if (leftDiffBoxY !== null) {
      const diffBoxY = leftRelativeY + leftDiffBoxY
      
      // 获取左侧canvas容器信息
      const leftCanvasWrapper = leftBox.querySelector('.canvas-wrapper') as HTMLElement
      if (leftCanvasWrapper && oldImageInfo) {
        const leftCanvasRect = leftCanvasWrapper.getBoundingClientRect()
        const leftCanvasRelativeX = leftCanvasRect.left - compareRect.left
        
        // 计算prevOldBbox（旧文档不同处）的实际右边缘X坐标
        const containerWidth = getCanvasWidth(leftWrapper)
        const layout = calculatePageLayout(oldImageInfo, containerWidth)
        const pageIndex = (diff.pageA || diff.page || 1) - 1
        
        if (pageIndex >= 0 && pageIndex < layout.length) {
          const pageLayout = layout[pageIndex]
          const scale = pageLayout.scale
          const diffBoxRightX = diff.prevOldBbox[2] * scale // prevOldBbox右边缘x坐标
          
          // 起点：旧文档不同处右边线中点
          const startX = leftCanvasRelativeX + diffBoxRightX
          const startY = diffBoxY
          
          // 终点：新增图标左边线中点
          const endX = middleIconX - 10 // 图标左边缘
          const endY = middleIconY
          
          // 绘制绿色连接线
          createConnectionLine(svg, startX, startY, endX, endY, getOperationColor('INSERT'))
        }
      }
    }
  }
  
  // 右侧连接线处理
  if (rightWrapper) {
    // 3. DELETE类型右侧连接线：起点=删除图标右边线中点，终点=新文档不同处（prevNewBbox）左边线中点（3段式）
    if (diff.operation === 'DELETE' && diff.prevNewBbox) {
      const rightDiffBoxY = calculateDiffBoxRelativeY(diff, 'right', leftWrapper, rightWrapper, oldImageInfo, newImageInfo)
      if (rightDiffBoxY !== null) {
        const diffBoxY = rightRelativeY + rightDiffBoxY
        
        // 获取右侧canvas容器信息
        const rightCanvasWrapper = rightBox.querySelector('.canvas-wrapper') as HTMLElement
        if (rightCanvasWrapper && newImageInfo) {
          const rightCanvasRect = rightCanvasWrapper.getBoundingClientRect()
          const rightCanvasRelativeX = rightCanvasRect.left - compareRect.left
          
          // 计算prevNewBbox（新文档不同处）的实际左边缘X坐标
          const containerWidth = getCanvasWidth(rightWrapper)
          const layout = calculatePageLayout(newImageInfo, containerWidth)
          const pageIndex = (diff.pageB || diff.page || 1) - 1
          
          if (pageIndex >= 0 && pageIndex < layout.length) {
            const pageLayout = layout[pageIndex]
            const scale = pageLayout.scale
            const diffBoxLeftX = diff.prevNewBbox[0] * scale // prevNewBbox左边缘x坐标
            
            // 起点：删除图标右边线中点
            const startX = middleIconX + 10 // 图标右边缘
            const startY = middleIconY
            
            // 终点：新文档不同处左边线中点
            const endX = rightCanvasRelativeX + diffBoxLeftX
            const endY = diffBoxY
            
            // 右侧canvas的左边缘X坐标
            const rightCanvasLeftX = rightCanvasRelativeX
            
            // 计算第一段水平线的终点：在删除图标右边缘基础上向右延伸，但不超出灰色区域
            const middleAreaWidth = 80 // 中间灰色区域宽度
            const middleAreaRightEdge = middleRelativeX + middleAreaWidth
            const extensionDistance = 20 // 向右延伸20px
            const horizontalEndX = Math.min(startX + extensionDistance, middleAreaRightEdge - 5) // 确保不超出灰色区域
            
            // 绘制三段连接线，使用DELETE差异框的颜色：
            const deleteColor = getOperationColor('DELETE')
            // 1. 从删除图标到中间点的水平线
            createConnectionLine(svg, startX, startY, horizontalEndX, startY, deleteColor)
            
            // 2. 从中间点到canvas左边缘的斜线
            createConnectionLine(svg, horizontalEndX, startY, rightCanvasLeftX, endY, deleteColor)
            
            // 3. 从canvas左边缘到新文档不同处的水平线
            createConnectionLine(svg, rightCanvasLeftX, endY, endX, endY, deleteColor)
          }
        }
      }
    }
    
    // 4. INSERT类型右侧连接线：起点=差异图标右边线中点，终点=差异框左边线中点（3段式）
    if (diff.operation === 'INSERT') {
      const rightDiffBoxY = calculateDiffBoxRelativeY(diff, 'right', leftWrapper, rightWrapper, oldImageInfo, newImageInfo)
      if (rightDiffBoxY !== null && diff.newBbox) {
        const diffBoxY = rightRelativeY + rightDiffBoxY
        
        // 获取右侧canvas容器信息
        const rightCanvasWrapper = rightBox.querySelector('.canvas-wrapper') as HTMLElement
        if (rightCanvasWrapper && newImageInfo) {
          const rightCanvasRect = rightCanvasWrapper.getBoundingClientRect()
          const rightCanvasRelativeX = rightCanvasRect.left - compareRect.left
          
          // 计算差异框的实际左边缘X坐标
          const containerWidth = getCanvasWidth(rightWrapper)
          const layout = calculatePageLayout(newImageInfo, containerWidth)
          const pageIndex = (diff.pageB || diff.page || 1) - 1
          
          if (pageIndex >= 0 && pageIndex < layout.length) {
            const pageLayout = layout[pageIndex]
            const scale = pageLayout.scale
            const diffBoxLeftX = diff.newBbox[0] * scale // bbox[0] 是左边缘x坐标
            
            // 起点：差异图标右边线中点
            const startX = middleIconX + 10 // 图标右边缘
            const startY = middleIconY
            
            // 终点：差异框左边线中点
            const endX = rightCanvasRelativeX + diffBoxLeftX
            const endY = diffBoxY
            
            // 右侧canvas的左边缘X坐标
            const rightCanvasLeftX = rightCanvasRelativeX
            
            // 计算第一段水平线的终点：在差异图标右边缘基础上向右延伸，但不超出灰色区域
            const middleAreaWidth = 80 // 中间灰色区域宽度
            const middleAreaRightEdge = middleRelativeX + middleAreaWidth
            const extensionDistance = 20 // 向右延伸20px
            const horizontalEndX = Math.min(startX + extensionDistance, middleAreaRightEdge - 5) // 确保不超出灰色区域
            
            // 绘制三段连接线，使用INSERT差异框的颜色：
            const insertColor = getOperationColor('INSERT')
            // 1. 从差异图标到中间点的水平线
            createConnectionLine(svg, startX, startY, horizontalEndX, startY, insertColor)
            
            // 2. 从中间点到canvas左边缘的斜线
            createConnectionLine(svg, horizontalEndX, startY, rightCanvasLeftX, endY, insertColor)
            
            // 3. 从canvas左边缘到差异框的水平线
            createConnectionLine(svg, rightCanvasLeftX, endY, endX, endY, insertColor)
          }
        }
      }
    }
  }
}
