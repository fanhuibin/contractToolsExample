/**
 * 中间Canvas交互组件 - 整合差异标记和连接线功能
 */

import type { DifferenceItem, ClickableArea } from './types'
import { 
  renderMiddleCanvasDiffIcons, 
  initMiddleCanvas, 
  handleMiddleCanvasClick, 
  handleMiddleCanvasMouseMove, 
  handleMiddleCanvasMouseLeave,
  type DiffMarkersProps 
} from './diff-markers'
import { 
  renderConnectionLines, 
  type ConnectionLineProps 
} from './connection-lines'

export interface MiddleCanvasInteractionProps {
  // Canvas elements
  canvas: HTMLCanvasElement
  svg: SVGElement
  
  // Container elements
  leftWrapper: HTMLElement
  rightWrapper?: HTMLElement
  middleArea: HTMLElement
  
  // Data
  filteredResults: DifferenceItem[]
  oldImageInfo: any
  newImageInfo: any
  selectedDiffIndex: number | null
  
  // State
  clickableAreas: Map<string, ClickableArea>
  
  // Callbacks
  onDiffClick: (diffIndex: number, operation: string) => void
  onSelectionChange?: (diffIndex: number | null) => void
}

export class MiddleCanvasInteraction {
  private props: MiddleCanvasInteractionProps
  private isInitialized = false
  
  constructor(props: MiddleCanvasInteractionProps) {
    this.props = props
  }
  
  // 更新属性
  updateProps(newProps: Partial<MiddleCanvasInteractionProps>) {
    this.props = { ...this.props, ...newProps }
  }
  
  // 初始化中间Canvas系统
  init() {
    if (this.isInitialized) return
    
    initMiddleCanvas(this.props.canvas, this.props.leftWrapper)
    this.setupEventListeners()
    this.isInitialized = true
    
    console.log('中间Canvas交互系统初始化完成')
  }
  
  // 设置事件监听器
  private setupEventListeners() {
    const { canvas } = this.props
    
    // 绑定点击事件
    canvas.addEventListener('click', this.handleClick.bind(this))
    
    // 绑定鼠标移动事件
    canvas.addEventListener('mousemove', this.handleMouseMove.bind(this))
    
    // 绑定鼠标离开事件
    canvas.addEventListener('mouseleave', this.handleMouseLeave.bind(this))
  }
  
  // 移除事件监听器
  destroy() {
    const { canvas } = this.props
    
    canvas.removeEventListener('click', this.handleClick.bind(this))
    canvas.removeEventListener('mousemove', this.handleMouseMove.bind(this))
    canvas.removeEventListener('mouseleave', this.handleMouseLeave.bind(this))
    
    this.isInitialized = false
    console.log('中间Canvas交互系统已销毁')
  }
  
  // 处理点击事件
  private handleClick(event: MouseEvent) {
    handleMiddleCanvasClick(
      event,
      this.props.canvas,
      this.props.clickableAreas,
      (diffIndex, operation) => {
        console.log(`从中间Canvas跳转到差异项 ${diffIndex + 1}, 操作: ${operation}`)
        this.props.onDiffClick(diffIndex, operation)
      }
    )
  }
  
  // 处理鼠标移动事件
  private handleMouseMove(event: MouseEvent) {
    handleMiddleCanvasMouseMove(
      event,
      this.props.canvas,
      this.props.clickableAreas
    )
  }
  
  // 处理鼠标离开事件
  private handleMouseLeave() {
    handleMiddleCanvasMouseLeave(this.props.canvas)
  }
  
  // 渲染差异图标
  renderDiffIcons() {
    const diffMarkersProps: DiffMarkersProps = {
      canvas: this.props.canvas,
      filteredResults: this.props.filteredResults,
      leftWrapper: this.props.leftWrapper,
      oldImageInfo: this.props.oldImageInfo,
      newImageInfo: this.props.newImageInfo,
      clickableAreas: this.props.clickableAreas
    }
    
    // 在差异图标渲染完成后自动渲染连接线
    renderMiddleCanvasDiffIcons(diffMarkersProps, () => {
      this.renderConnectionLines()
    })
  }
  
  // 渲染连接线
  renderConnectionLines() {
    if (!this.props.rightWrapper) {
      console.warn('右侧容器未找到，跳过连接线渲染')
      return
    }
    
    const connectionLineProps: ConnectionLineProps = {
      svg: this.props.svg,
      leftWrapper: this.props.leftWrapper,
      rightWrapper: this.props.rightWrapper,
      middleArea: this.props.middleArea,
      oldImageInfo: this.props.oldImageInfo,
      newImageInfo: this.props.newImageInfo,
      selectedDiffIndex: this.props.selectedDiffIndex,
      filteredResults: this.props.filteredResults
    }
    
    renderConnectionLines(connectionLineProps)
  }
  
  // 完整渲染（差异图标 + 连接线）
  render() {
    if (!this.isInitialized) {
      console.warn('中间Canvas交互系统未初始化，跳过渲染')
      return
    }
    
    // 渲染差异图标
    this.renderDiffIcons()
    
    // 渲染连接线
    this.renderConnectionLines()
    
    console.log('中间Canvas完整渲染完成')
  }
  
  // 重新初始化（用于窗口大小变化等情况）
  reinit() {
    if (this.isInitialized) {
      initMiddleCanvas(this.props.canvas, this.props.leftWrapper)
      this.render()
    }
  }
  
  // 清除选择
  clearSelection() {
    if (this.props.onSelectionChange) {
      this.props.onSelectionChange(null)
    }
    // 重新渲染以清除连接线
    this.renderConnectionLines()
  }
}

// 工厂函数，创建中间Canvas交互实例
export const createMiddleCanvasInteraction = (props: MiddleCanvasInteractionProps): MiddleCanvasInteraction => {
  return new MiddleCanvasInteraction(props)
}

// 导出主要函数
export { 
  initMiddleCanvas,
  renderMiddleCanvasDiffIcons,
  renderConnectionLines,
  handleMiddleCanvasClick,
  handleMiddleCanvasMouseMove,
  handleMiddleCanvasMouseLeave
}
