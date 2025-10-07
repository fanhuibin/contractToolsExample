/**
 * 高性能差距保持同步滚动管理器
 * 
 * 核心特性：
 * - 差距保持同步：拖拽后保持位置差距不变
 * - 鼠标滚轮时保持差距同步滚动
 * - 拖拽滚动条时单向滚动，停止后建立新差距基准
 * - 防止循环触发和抖动
 * - 高性能优化（RAF调度、防抖等）
 */

export interface AdvancedSyncScrollOptions {
  /** 最小同步阈值（像素），避免抖动 */
  minDelta: number
  /** 滚动结束检测延迟（毫秒） */
  scrollEndDelay: number
  /** 鼠标滚轮检测窗口（毫秒） */
  wheelDetectWindow: number
  /** 拖拽检测延迟（毫秒） */
  dragDetectDelay: number
  /** 滚动时的回调函数 */
  onScroll?: () => void
  /** 检查是否正在跳转的函数 */
  isJumping?: () => boolean
}

interface ScrollState {
  /** 当前滚动位置 */
  scrollTop: number
  /** 最后更新时间 */
  lastUpdate: number
  /** 是否正在被用户拖拽 */
  isDragging: boolean
  /** 是否是滚轮触发的滚动 */
  isWheelScroll: boolean
  /** 最后拖拽结束时间 */
  lastDragEndTime: number
}

/** 同步基准信息 */
interface SyncBaseline {
  /** 左侧基准位置 */
  leftPosition: number
  /** 右侧基准位置 */
  rightPosition: number
  /** 位置差距（left - right） */
  offset: number
  /** 基准建立时间 */
  timestamp: number
}

interface SyncEvent {
  side: 'left' | 'right'
  scrollTop: number
  timestamp: number
  type: 'wheel' | 'drag' | 'programmatic'
}

export class AdvancedSyncScrollManager {
  private options: AdvancedSyncScrollOptions
  private leftElement: HTMLElement | null = null
  private rightElement: HTMLElement | null = null
  
  // 状态管理
  private enabled = true
  private leftState: ScrollState = { scrollTop: 0, lastUpdate: 0, isDragging: false, isWheelScroll: false, lastDragEndTime: 0 }
  private rightState: ScrollState = { scrollTop: 0, lastUpdate: 0, isDragging: false, isWheelScroll: false, lastDragEndTime: 0 }
  
  // 同步基准信息
  private syncBaseline: SyncBaseline | null = null
  
  // 事件队列和防抖
  private eventQueue: SyncEvent[] = []
  private rafId: number | null = null
  private isInternalSync = false
  
  // 计时器
  private scrollEndTimer: number | null = null
  private dragDetectTimer: number | null = null
  private wheelDetectTimer: number | null = null
  
  // 滚轮检测
  private lastWheelTime = 0
  private activeWheelSide: 'left' | 'right' | null = null
  
  constructor(options: Partial<AdvancedSyncScrollOptions> = {}) {
    this.options = {
      minDelta: 2,
      scrollEndDelay: 100,
      wheelDetectWindow: 150,
      dragDetectDelay: 50,
      ...options
    }
    
  }
  
  /**
   * 初始化管理器
   */
  init(leftElement: HTMLElement, rightElement: HTMLElement) {
    this.leftElement = leftElement
    this.rightElement = rightElement
    
    if (this.enabled) {
      this.bindEvents()
      this.syncInitialPositions()
    }
    
  }
  
  /**
   * 绑定事件监听器
   */
  private bindEvents() {
    if (!this.leftElement || !this.rightElement) return
    
    // 滚动事件（被动监听提高性能）
    this.leftElement.addEventListener('scroll', (e) => this.handleScroll('left', e), { passive: true })
    this.rightElement.addEventListener('scroll', (e) => this.handleScroll('right', e), { passive: true })
    
    // 滚轮事件（触发同步滚动）
    this.leftElement.addEventListener('wheel', () => this.handleWheel('left'), { passive: true })
    this.rightElement.addEventListener('wheel', () => this.handleWheel('right'), { passive: true })
    
    // 鼠标事件（检测拖拽状态）
    this.leftElement.addEventListener('mousedown', () => this.handleMouseDown('left'), { passive: true })
    this.rightElement.addEventListener('mousedown', () => this.handleMouseDown('right'), { passive: true })
    
    // 全局鼠标释放（拖拽结束）
    document.addEventListener('mouseup', () => this.handleMouseUp(), { passive: true })
    
  }
  
  /**
   * 处理鼠标滚轮事件（外部调用）
   */
  handleWheel(side: 'left' | 'right') {
    const now = Date.now()
    this.lastWheelTime = now
    this.activeWheelSide = side
    
    // 标记对应状态为滚轮滚动
    if (side === 'left') {
      this.leftState.isWheelScroll = true
    } else {
      this.rightState.isWheelScroll = true
    }
    
    // 清除之前的计时器
    if (this.wheelDetectTimer) {
      clearTimeout(this.wheelDetectTimer)
    }
    
    // 设置滚轮检测结束计时器
    this.wheelDetectTimer = window.setTimeout(() => {
      this.activeWheelSide = null
      this.leftState.isWheelScroll = false
      this.rightState.isWheelScroll = false
    }, this.options.wheelDetectWindow)
    
  }
  
  /**
   * 处理鼠标按下（拖拽开始）
   */
  private handleMouseDown(side: 'left' | 'right') {
    if (side === 'left') {
      this.leftState.isDragging = true
    } else {
      this.rightState.isDragging = true
    }
    
  }
  
  /**
   * 处理鼠标释放（拖拽结束）
   */
  private handleMouseUp() {
    const wasLeftDragging = this.leftState.isDragging
    const wasRightDragging = this.rightState.isDragging
    
    // 延迟重置拖拽状态，给滚动事件完成的时间
    if (this.dragDetectTimer) {
      clearTimeout(this.dragDetectTimer)
    }
    
    this.dragDetectTimer = window.setTimeout(() => {
      const now = Date.now()
      
      // 记录拖拽结束时间
      if (wasLeftDragging) {
        this.leftState.lastDragEndTime = now
      }
      if (wasRightDragging) {
        this.rightState.lastDragEndTime = now
      }
      
      this.leftState.isDragging = false
      this.rightState.isDragging = false
      
      // 拖拽结束后建立新基准
      if (wasLeftDragging || wasRightDragging) {
        this.syncAfterDragEnd()
      }
    }, this.options.dragDetectDelay)
    
  }
  
  /**
   * 拖拽结束后建立新的差距基准
   * 核心逻辑：以拖拽结束时的位置差距作为新的同步基准
   */
  private syncAfterDragEnd() {
    if (!this.leftElement || !this.rightElement) return
    
    // 设置标志，防止拖拽结束后的位置更新触发同步
    this.isInternalSync = true
    
    // 获取当前位置
    const leftScrollTop = this.leftElement.scrollTop
    const rightScrollTop = this.rightElement.scrollTop
    const now = Date.now()
    
    
    // 计算并记录新的差距基准
    const offset = leftScrollTop - rightScrollTop
    this.syncBaseline = {
      leftPosition: leftScrollTop,
      rightPosition: rightScrollTop,
      offset: offset,
      timestamp: now
    }
    
    // 更新状态记录
    this.leftState.scrollTop = leftScrollTop
    this.leftState.lastUpdate = now
    this.rightState.scrollTop = rightScrollTop
    this.rightState.lastUpdate = now
    
    // 清空同步队列，防止之前排队的同步事件执行
    this.eventQueue = []
    
    
    // 延长保护时间到500ms，确保拖拽后的所有副作用事件都被忽略
    setTimeout(() => {
      this.isInternalSync = false
    }, 500)
  }
  
  /**
   * 处理滚动事件
   */
  private handleScroll(side: 'left' | 'right', event: Event) {
    if (!this.enabled || this.isInternalSync) return
    
    // 检查是否正在跳转
    if (this.options.isJumping && this.options.isJumping()) {
      return
    }
    
    const element = side === 'left' ? this.leftElement : this.rightElement
    const state = side === 'left' ? this.leftState : this.rightState
    
    if (!element) return
    
    const currentScrollTop = element.scrollTop
    const now = Date.now()
    
    // 更新状态
    state.scrollTop = currentScrollTop
    state.lastUpdate = now
    
    // 判断滚动类型
    const isRecentWheel = this.isRecentWheelEvent() && this.activeWheelSide === side
    const isDragging = state.isDragging
    
    // 创建同步事件
    const syncEvent: SyncEvent = {
      side,
      scrollTop: currentScrollTop,
      timestamp: now,
      type: isRecentWheel ? 'wheel' : (isDragging ? 'drag' : 'programmatic')
    }
    
    // 根据滚动类型决定是否同步
    if (syncEvent.type === 'wheel') {
      // 滚轮事件：立即同步
      this.queueSync(syncEvent)
    } else if (syncEvent.type === 'drag') {
      // 拖拽事件：不同步，等待拖拽结束
    } else {
      // 其他事件：正常同步
      this.queueSync(syncEvent)
    }
    
    // 只有在非拖拽状态下才重置滚动结束计时器
    if (syncEvent.type !== 'drag') {
      this.resetScrollEndTimer()
    }
    
    // 调用滚动回调
    if (this.options.onScroll) {
      this.options.onScroll()
    }
  }
  
  /**
   * 判断是否是最近的滚轮事件
   */
  private isRecentWheelEvent(): boolean {
    return Date.now() - this.lastWheelTime < this.options.wheelDetectWindow
  }
  
  /**
   * 队列化同步操作
   */
  private queueSync(event: SyncEvent) {
    this.eventQueue.push(event)
    
    // 如果没有待处理的RAF，调度一个
    if (!this.rafId) {
      this.rafId = requestAnimationFrame(() => this.processEventQueue())
    }
  }
  
  /**
   * 处理事件队列
   */
  private processEventQueue() {
    if (this.eventQueue.length === 0) {
      this.rafId = null
      return
    }
    
    // 取最新的事件进行处理
    const latestEvent = this.eventQueue[this.eventQueue.length - 1]
    this.eventQueue = []
    
    // 执行同步
    this.executeSyncEvent(latestEvent)
    
    this.rafId = null
  }
  
  /**
   * 执行同步事件
   */
  private executeSyncEvent(event: SyncEvent) {
    if (!this.leftElement || !this.rightElement) return
    
    const sourceElement = event.side === 'left' ? this.leftElement : this.rightElement
    const targetElement = event.side === 'left' ? this.rightElement : this.leftElement
    
    this.performPixelSync(sourceElement, targetElement)
  }
  

  /**
   * 执行基于差距保持的同步
   * 核心逻辑：保持拖拽后建立的位置差距不变
   * 修复：处理文档高度不同时的边界情况
   */
  private performPixelSync(source: HTMLElement, target: HTMLElement) {
    if (!this.enabled) {
      return
    }
    
    // 强制检查内部同步标志 - 这是最重要的保护
    if (this.isInternalSync) {
      return
    }
    
    // 如果没有基准差距，使用传统同步（首次启用时）
    if (!this.syncBaseline) {
      this.establishInitialBaseline()
      return
    }
    
    // 检查是否在拖拽保护期内
    const now = Date.now()
    const recentDragThreshold = 1000
    const hasRecentLeftDrag = (now - this.leftState.lastDragEndTime) < recentDragThreshold
    const hasRecentRightDrag = (now - this.rightState.lastDragEndTime) < recentDragThreshold
    
    if (hasRecentLeftDrag || hasRecentRightDrag) {
      return
    }
    
    // 获取当前位置和滚动边界
    const sourceScrollTop = source.scrollTop
    const targetScrollTop = target.scrollTop
    const targetMaxScrollTop = target.scrollHeight - target.clientHeight
    
    // 获取上次记录的源文档位置，用于判断滚动方向
    const sourceSide = source === this.leftElement ? 'left' : 'right'
    const sourceState = sourceSide === 'left' ? this.leftState : this.rightState
    const lastSourceScrollTop = sourceState.scrollTop
    const isScrollingUp = sourceScrollTop < lastSourceScrollTop // 向上滚动（数值减小）
    const isScrollingDown = sourceScrollTop > lastSourceScrollTop // 向下滚动（数值增大）
    
    // 计算目标应该到达的位置（保持基准差距）
    let expectedTargetPosition: number
    
    if (sourceSide === 'left') {
      // 左侧滚动，右侧应该保持差距：right = left - offset
      expectedTargetPosition = sourceScrollTop - this.syncBaseline.offset
    } else {
      // 右侧滚动，左侧应该保持差距：left = right + offset
      expectedTargetPosition = sourceScrollTop + this.syncBaseline.offset
    }
    
    // 检查目标文档是否已达到边界
    const isTargetAtTop = targetScrollTop <= 0
    const isTargetAtBottom = targetScrollTop >= targetMaxScrollTop - 1 // 允许1px误差
    
    // 如果期望位置超出边界，检查是否应该允许源文档独立滚动
    if (expectedTargetPosition < 0) {
      // 目标位置会是负数，说明目标文档应该超出顶部边界
      if (isTargetAtTop && !isScrollingDown) {
        // 目标文档在顶部且源文档不是向下滚动，允许源文档继续滚动
        return
      }
      // 目标文档还没到顶部，或源文档开始向下滚动，正常同步到0
      expectedTargetPosition = 0
    } else if (expectedTargetPosition > targetMaxScrollTop) {
      // 目标位置会超出底部边界
      if (isTargetAtBottom && !isScrollingUp) {
        // 目标文档在底部且源文档不是向上滚动，允许源文档继续滚动
        return
      }
      // 目标文档还没到底部，或源文档开始向上滚动，同步到最大位置
      expectedTargetPosition = targetMaxScrollTop
    }
    
    const delta = Math.abs(targetScrollTop - expectedTargetPosition)
    
    // 如果差值小于阈值，跳过同步
    if (delta < this.options.minDelta) {
      return
    }
    
    // 设置内部同步标志
    this.isInternalSync = true
    
    // 执行差距保持同步
    target.scrollTop = expectedTargetPosition
    
    
    // 更新目标状态
    const targetSide = target === this.leftElement ? 'left' : 'right'
    const targetState = targetSide === 'left' ? this.leftState : this.rightState
    targetState.scrollTop = expectedTargetPosition
    targetState.lastUpdate = Date.now()
    
    // 同时更新源文档状态，确保下次滚动方向检测准确
    sourceState.scrollTop = sourceScrollTop
    sourceState.lastUpdate = Date.now()
    
    // 异步重置同步标志
    requestAnimationFrame(() => {
      this.isInternalSync = false
    })
    
  }
  
  /**
   * 建立初始基准差距（首次启用同步时）
   */
  private establishInitialBaseline() {
    if (!this.leftElement || !this.rightElement) return
    
    const leftScrollTop = this.leftElement.scrollTop
    const rightScrollTop = this.rightElement.scrollTop
    const offset = leftScrollTop - rightScrollTop
    
    
    this.syncBaseline = {
      leftPosition: leftScrollTop,
      rightPosition: rightScrollTop,
      offset: offset,
      timestamp: Date.now()
    }
    
  }
  
  /**
   * 重置滚动结束计时器
   */
  private resetScrollEndTimer() {
    if (this.scrollEndTimer) {
      clearTimeout(this.scrollEndTimer)
    }
    
    this.scrollEndTimer = window.setTimeout(() => {
      this.onScrollEnd()
    }, this.options.scrollEndDelay)
  }
  
  /**
   * 滚动结束处理
   */
  private onScrollEnd() {
    // 确保最终同步
    if (!this.leftState.isDragging && !this.rightState.isDragging) {
      this.ensureFinalSync()
    }
    
  }
  
  /**
   * 确保最终差距同步
   * 基于差距基准进行最终微调同步
   */
  private ensureFinalSync() {
    if (!this.leftElement || !this.rightElement || !this.syncBaseline) return
    
    const now = Date.now()
    const recentDragThreshold = 500 // 500ms内的拖拽操作被认为是"最近的"
    
    // 如果有任何一侧正在拖拽，跳过最终同步
    if (this.leftState.isDragging || this.rightState.isDragging) {
      return
    }
    
    // 如果有任何一侧最近刚结束拖拽，也跳过最终同步
    // 让拖拽结果保持，作为新的基准
    const hasRecentLeftDrag = (now - this.leftState.lastDragEndTime) < recentDragThreshold
    const hasRecentRightDrag = (now - this.rightState.lastDragEndTime) < recentDragThreshold
    
    if (hasRecentLeftDrag || hasRecentRightDrag) {
      return
    }
    
    const leftScrollTop = this.leftElement.scrollTop
    const rightScrollTop = this.rightElement.scrollTop
    
    // 计算当前实际差距与基准差距的偏差
    const currentOffset = leftScrollTop - rightScrollTop
    const offsetDelta = Math.abs(currentOffset - this.syncBaseline.offset)
    
    // 只有在偏差显著时才进行微调
    if (offsetDelta > this.options.minDelta) {
      // 以最近更新的一侧为准（通常是滚轮操作的一侧）
      if (this.leftState.lastUpdate > this.rightState.lastUpdate) {
        this.performPixelSync(this.leftElement, this.rightElement)
      } else {
        this.performPixelSync(this.rightElement, this.leftElement)
      }
      
    }
  }
  
  /**
   * 同步初始位置（记录当前差距作为新基准）
   */
  syncInitialPositions() {
    if (!this.leftElement || !this.rightElement) return
    
    // 建立基于当前位置的差距基准
    this.establishInitialBaseline()
    
    // 更新内部状态
    this.leftState.scrollTop = this.leftElement.scrollTop
    this.rightState.scrollTop = this.rightElement.scrollTop
    
  }
  
  /**
   * 重置滚动位置
   */
  resetScrollPositions() {
    if (!this.leftElement || !this.rightElement) return
    
    this.isInternalSync = true
    
    this.leftElement.scrollTop = 0
    this.rightElement.scrollTop = 0
    
    // 重置状态
    this.leftState.scrollTop = 0
    this.rightState.scrollTop = 0
    
    requestAnimationFrame(() => {
      this.isInternalSync = false
    })
    
  }
  
  /**
   * 启用/禁用同步
   */
  setEnabled(enabled: boolean) {
    this.enabled = enabled
    
    if (enabled && this.leftElement && this.rightElement) {
      this.bindEvents()
      this.syncInitialPositions()
    }
    
  }
  
  /**
   * 销毁管理器
   */
  destroy() {
    // 清理计时器
    if (this.scrollEndTimer) {
      clearTimeout(this.scrollEndTimer)
    }
    if (this.dragDetectTimer) {
      clearTimeout(this.dragDetectTimer)
    }
    if (this.wheelDetectTimer) {
      clearTimeout(this.wheelDetectTimer)
    }
    if (this.rafId) {
      cancelAnimationFrame(this.rafId)
    }
    
    // 清理事件监听器
    if (this.leftElement && this.rightElement) {
      // 注意：这里需要保存原始函数引用才能正确移除
      // 实际使用中应该在bindEvents时保存引用
    }
    
    // 移除全局事件
    document.removeEventListener('mouseup', () => this.handleMouseUp())
    
  }
  
  
}

/**
 * 工厂函数
 */
export const createAdvancedSyncScrollManager = (options?: Partial<AdvancedSyncScrollOptions>) => {
  return new AdvancedSyncScrollManager(options)
}
