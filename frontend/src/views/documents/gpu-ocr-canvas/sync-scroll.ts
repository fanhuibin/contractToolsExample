/**
 * 同步滚动处理模块 - 优化的双向同步滚动实现
 */

export interface SyncScrollState {
  enabled: boolean
  syncing: boolean
  lastScrollTop: { old: number; new: number }
  wheelActiveSide: 'old' | 'new' | null
  wheelTimer: number | null
}

export interface SyncScrollOptions {
  // 最小滚动差值，小于此值不触发同步
  minDelta: number
  // 最大滚动差值，大于此值重新同步基准位置
  maxDelta: number
  // 滚动同步的平滑因子 (0-1)，值越小越平滑
  smoothFactor: number
  // 滚动结束延迟 (ms)
  scrollEndDelay: number
  // 滚轮活跃状态保持时间 (ms)
  wheelActiveDelay: number
}

const DEFAULT_OPTIONS: SyncScrollOptions = {
  minDelta: 1,
  maxDelta: 500,
  smoothFactor: 0.8,
  scrollEndDelay: 300,
  wheelActiveDelay: 150
}

export class SyncScrollManager {
  private state: SyncScrollState
  private options: SyncScrollOptions
  private scrollEndTimer: number | null = null
  
  constructor(options: Partial<SyncScrollOptions> = {}) {
    this.options = { ...DEFAULT_OPTIONS, ...options }
    this.state = {
      enabled: true,
      syncing: false,
      lastScrollTop: { old: 0, new: 0 },
      wheelActiveSide: null,
      wheelTimer: null
    }
  }
  
  // 设置滚动状态
  setEnabled(enabled: boolean) {
    this.state.enabled = enabled
    if (!enabled) {
      this.resetWheelState()
    }
  }
  
  // 获取滚动状态
  getState() {
    return { ...this.state }
  }
  
  // 处理滚轮事件
  handleWheel(side: 'old' | 'new') {
    this.state.wheelActiveSide = side
    
    if (this.state.wheelTimer) {
      clearTimeout(this.state.wheelTimer)
    }
    
    this.state.wheelTimer = window.setTimeout(() => {
      this.state.wheelActiveSide = null
    }, this.options.wheelActiveDelay)
  }
  
  // 处理滚动事件
  handleScroll(
    side: 'old' | 'new',
    currentScrollTop: number,
    sourceWrapper: HTMLElement,
    targetWrapper: HTMLElement,
    onUpdate?: () => void
  ): boolean {
    // 如果正在跳转，不处理滚动
    if (!this.state.enabled || this.state.syncing) {
      this.state.lastScrollTop[side] = currentScrollTop
      return false
    }
    
    const delta = currentScrollTop - this.state.lastScrollTop[side]
    
    // 更新记录的滚动位置
    this.state.lastScrollTop[side] = currentScrollTop
    
    // 检测异常大的滚动增量，重新同步基准位置
    if (Math.abs(delta) > this.options.maxDelta) {
      console.warn(`检测到异常大的滚动增量: ${delta}px，重新同步基准位置`)
      this.resyncScrollPositions(sourceWrapper, targetWrapper)
      return false
    }
    
    // 滚动增量太小，不处理
    if (Math.abs(delta) < this.options.minDelta) {
      return false
    }
    
    // 仅在滚轮触发且当前侧为主动侧时才进行同步
    if (this.state.wheelActiveSide !== side) {
      return false
    }
    
    // 执行同步滚动
    return this.performSync(side, delta, sourceWrapper, targetWrapper, onUpdate)
  }
  
  // 执行同步滚动
  private performSync(
    side: 'old' | 'new',
    delta: number,
    sourceWrapper: HTMLElement,
    targetWrapper: HTMLElement,
    onUpdate?: () => void
  ): boolean {
    try {
      // 计算同步因子
      const sourceRange = Math.max(1, sourceWrapper.scrollHeight - sourceWrapper.clientHeight)
      const targetRange = Math.max(1, targetWrapper.scrollHeight - targetWrapper.clientHeight)
      
      if (sourceRange <= 1 || targetRange <= 1) {
        return false // 没有可滚动的内容
      }
      
      const syncFactor = targetRange / sourceRange
      
      // 计算目标滚动位置
      const smoothedDelta = delta * syncFactor * this.options.smoothFactor
      const targetScrollTop = Math.max(0, Math.min(
        targetRange,
        targetWrapper.scrollTop + smoothedDelta
      ))
      
      // 防止同步循环
      this.state.syncing = true
      
      // 应用滚动
      targetWrapper.scrollTop = targetScrollTop
      
      // 更新目标侧的记录位置
      const otherSide = side === 'old' ? 'new' : 'old'
      this.state.lastScrollTop[otherSide] = targetScrollTop
      
      // 触发更新回调
      if (onUpdate) {
        requestAnimationFrame(onUpdate)
      }
      
      // 异步重置同步状态
      setTimeout(() => {
        this.state.syncing = false
      }, 16) // 约一帧的时间
      
      return true
    } catch (error) {
      console.error('同步滚动失败:', error)
      this.state.syncing = false
      return false
    }
  }
  
  // 重新同步滚动位置
  private resyncScrollPositions(wrapper1: HTMLElement, wrapper2: HTMLElement) {
    try {
      this.state.lastScrollTop.old = wrapper1.scrollTop
      this.state.lastScrollTop.new = wrapper2.scrollTop
      console.log('滚动位置重新同步完成:', this.state.lastScrollTop)
    } catch (error) {
      console.error('重新同步滚动位置失败:', error)
    }
  }
  
  // 重置滚轮状态
  private resetWheelState() {
    if (this.state.wheelTimer) {
      clearTimeout(this.state.wheelTimer)
      this.state.wheelTimer = null
    }
    this.state.wheelActiveSide = null
    this.state.syncing = false
  }
  
  // 初始化滚动位置
  initScrollPositions(oldWrapper: HTMLElement, newWrapper: HTMLElement) {
    this.state.lastScrollTop.old = oldWrapper.scrollTop
    this.state.lastScrollTop.new = newWrapper.scrollTop
  }
  
  // 销毁管理器
  destroy() {
    this.resetWheelState()
    if (this.scrollEndTimer) {
      clearTimeout(this.scrollEndTimer)
    }
  }
}

// 创建默认的同步滚动管理器
export const createSyncScrollManager = (options?: Partial<SyncScrollOptions>) => {
  return new SyncScrollManager(options)
}
