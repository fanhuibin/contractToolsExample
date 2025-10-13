/**
 * 简化的合同比对进度计算器
 * 
 * 核心思想：只关注OCR处理时间，通过固定的里程碑控制进度
 * 
 * 进度里程碑：
 * - 0% - 46%: 第一个文档OCR
 * - 46% - 60%: 第一个文档超时缓冲区（缓慢增长）
 * - 60% (或当前) - 96%: 第二个文档OCR  
 * - 96% - 100%: 最终冲刺（0.1秒）
 * 
 * @version 2.0
 * @since 2025-10-13
 */

export interface TaskProgressData {
  // 页面信息
  oldDocPages: number
  newDocPages: number
  completedPagesOld: number
  completedPagesNew: number
  
  // 预估时间（毫秒）
  estimatedOcrTimeOld: number
  estimatedOcrTimeNew: number
  
  // 当前状态
  currentStepDesc: string
  status: string
  startTime?: string
}

export interface ProgressConfig {
  firstDocCompleteProgress: number  // 46.0
  firstDocMaxWaitProgress: number    // 60.0
  secondDocCompleteProgress: number  // 96.0
  slowGrowthFactor: number           // 0.05 (1/20)
  finalSprintTime: number            // 100ms
}

const DEFAULT_CONFIG: ProgressConfig = {
  firstDocCompleteProgress: 46.0,
  firstDocMaxWaitProgress: 60.0,
  secondDocCompleteProgress: 96.0,
  slowGrowthFactor: 0.05,
  finalSprintTime: 100
}

export class SimpleProgressCalculator {
  private config: ProgressConfig
  private taskStartTime: number = 0
  private firstDocCompleteTime: number = 0
  private secondDocStartTime: number = 0
  private finalSprintStartTime: number = 0
  private lastUpdateTime: number = 0
  private phase: 'INIT' | 'FIRST_DOC' | 'WAITING' | 'SECOND_DOC' | 'FINAL' = 'INIT'
  
  constructor(config?: Partial<ProgressConfig>) {
    this.config = { ...DEFAULT_CONFIG, ...config }
  }
  
  /**
   * 计算当前进度
   */
  calculateProgress(taskData: TaskProgressData, currentProgress: number): number {
    const now = Date.now()
    
    // 初始化任务开始时间
    if (this.taskStartTime === 0) {
      this.taskStartTime = taskData.startTime ? new Date(taskData.startTime).getTime() : now
    }
    
    this.lastUpdateTime = now
    const elapsedSinceStart = now - this.taskStartTime
    
    // 如果任务已完成，快速冲刺到100%
    if (taskData.status === 'COMPLETED') {
      return this.calculateFinalSprint(currentProgress, now)
    }
    
    // 判断当前处于哪个阶段
    const stepDesc = taskData.currentStepDesc || ''
    
    // 第一个文档OCR阶段
    if (stepDesc.includes('OCR识别原文档') || stepDesc.includes('原文档')) {
      // 检查第一个文档是否已完成
      if (taskData.completedPagesOld > 0 && taskData.oldDocPages > 0 && 
          taskData.completedPagesOld >= taskData.oldDocPages) {
        // 第一个文档已完成，应该进入第二个文档阶段
        // 但步骤描述还没更新，直接返回46%
        return Math.max(currentProgress, this.config.firstDocCompleteProgress)
      }
      
      this.phase = 'FIRST_DOC'
      return this.calculateFirstDocProgress(taskData, elapsedSinceStart, currentProgress)
    }
    
    // 第二个文档OCR阶段  
    if (stepDesc.includes('OCR识别新文档') || stepDesc.includes('新文档')) {
      this.phase = 'SECOND_DOC'
      
      // 记录第二个文档开始时间
      if (this.secondDocStartTime === 0) {
        this.secondDocStartTime = now
        this.firstDocCompleteTime = now
      }
      
      return this.calculateSecondDocProgress(taskData, currentProgress)
    }
    
    // 其他阶段（文本比对、差异分析等）
    if (currentProgress < this.config.secondDocCompleteProgress) {
      // 直接跳到96%
      return this.config.secondDocCompleteProgress
    }
    
    return currentProgress
  }
  
  /**
   * 计算第一个文档的进度 (0% - 46%)
   */
  private calculateFirstDocProgress(
    taskData: TaskProgressData, 
    elapsed: number, 
    currentProgress: number
  ): number {
    const { oldDocPages, completedPagesOld, estimatedOcrTimeOld } = taskData
    
    if (!oldDocPages || oldDocPages === 0) {
      // 没有页面信息，使用时间基础计算
      return this.calculateTimeBasedProgress(0, this.config.firstDocCompleteProgress, elapsed, estimatedOcrTimeOld)
    }
    
    // 计算页面进度和时间进度
    const pageProgress = completedPagesOld / oldDocPages
    const timeProgress = estimatedOcrTimeOld > 0 ? elapsed / estimatedOcrTimeOld : 0
    
    // 使用两者的最大值（这样即使页面还没完成，时间也在推进进度）
    const progress = Math.max(pageProgress, Math.min(timeProgress, 1.0)) * this.config.firstDocCompleteProgress
    
    // 如果到达预期完成时间但还未完成，进入缓慢增长模式
    if (progress >= this.config.firstDocCompleteProgress && completedPagesOld < oldDocPages) {
      this.phase = 'WAITING'
      return this.calculateSlowGrowth(
        this.config.firstDocCompleteProgress,
        this.config.firstDocMaxWaitProgress,
        currentProgress
      )
    }
    
    return Math.min(progress, this.config.firstDocCompleteProgress)
  }
  
  /**
   * 计算第二个文档的进度 (当前进度 - 96%)
   */
  private calculateSecondDocProgress(taskData: TaskProgressData, currentProgress: number): number {
    const { newDocPages, completedPagesNew, estimatedOcrTimeNew } = taskData
    
    // 起始进度（至少从46%开始，如果当前更高则从当前开始）
    const startProgress = Math.max(currentProgress, this.config.firstDocCompleteProgress)
    const progressRange = this.config.secondDocCompleteProgress - startProgress
    
    if (!newDocPages || newDocPages === 0) {
      // 没有页面信息，使用时间基础计算
      const elapsed = Date.now() - this.secondDocStartTime
      const timeProgress = estimatedOcrTimeNew > 0 ? elapsed / estimatedOcrTimeNew : 0
      return startProgress + (Math.min(timeProgress, 1.0) * progressRange)
    }
    
    // 计算页面进度和时间进度
    const pageProgress = completedPagesNew / newDocPages
    const elapsed = Date.now() - this.secondDocStartTime
    const timeProgress = estimatedOcrTimeNew > 0 ? elapsed / estimatedOcrTimeNew : 0
    
    // 使用两者的最大值（这样即使页面还没完成，时间也在推进进度）
    // 但是页面完成时可以跳过时间限制
    const progress = Math.max(pageProgress, Math.min(timeProgress, 1.0))
    
    return startProgress + (Math.min(progress, 1.0) * progressRange)
  }
  
  /**
   * 缓慢增长模式（1/20速度）
   */
  private calculateSlowGrowth(min: number, max: number, currentProgress: number): number {
    // 每次增长很小的量
    const increment = this.config.slowGrowthFactor * 0.1  // 更慢的增长
    return Math.min(currentProgress + increment, max)
  }
  
  /**
   * 基于时间的进度计算
   */
  private calculateTimeBasedProgress(
    minProgress: number,
    maxProgress: number,
    elapsed: number,
    estimatedTime: number
  ): number {
    if (estimatedTime <= 0) return minProgress
    
    const ratio = Math.min(1.0, elapsed / estimatedTime)
    return minProgress + ((maxProgress - minProgress) * ratio)
  }
  
  /**
   * 最终冲刺 (96% - 100%, 0.1秒完成)
   */
  private calculateFinalSprint(currentProgress: number, now: number): number {
    if (this.finalSprintStartTime === 0) {
      this.finalSprintStartTime = now
    }
    
    const sprintElapsed = now - this.finalSprintStartTime
    const sprintProgress = Math.min(1.0, sprintElapsed / this.config.finalSprintTime)
    
    const startProgress = Math.max(currentProgress, this.config.secondDocCompleteProgress)
    const progressRange = 100 - startProgress
    
    return startProgress + (sprintProgress * progressRange)
  }
  
  /**
   * 重置计算器（用于新任务）
   */
  reset() {
    this.taskStartTime = 0
    this.firstDocCompleteTime = 0
    this.secondDocStartTime = 0
    this.finalSprintStartTime = 0
    this.lastUpdateTime = 0
    this.phase = 'INIT'
  }
  
  /**
   * 获取当前阶段
   */
  getPhase(): string {
    return this.phase
  }
}

