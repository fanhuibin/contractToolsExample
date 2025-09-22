<template>
  <div class="gpu-compare-fullscreen">
    <div class="compare-toolbar">
      <div class="left">
        <div class="title">GPU OCR合同比对 (Canvas版本)</div>
        <div v-if="displayFileNames" class="file-names">
          <span class="file-name old">{{ oldFileName }}</span>
          <span class="vs">VS</span>
          <span class="file-name new">{{ newFileName }}</span>
        </div>
      </div>
      <div class="center">
        <el-button-group>
          <el-button size="small" :disabled="prevDisabled" @click="prevResult">
            <el-icon><ArrowLeft /></el-icon>
            上一处
          </el-button>
          <el-button size="small" type="primary" :disabled="nextDisabled" @click="nextResult">
            下一处
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        <span class="counter">{{ totalCount === 0 ? '无差异' : `第 ${displayActiveNumber} / ${totalCount} 处` }}</span>
      </div>
      <div class="right">
        <div class="page-controls">
          <span class="page-info">第</span>
          <el-input-number 
            v-model="currentPage" 
            :min="1" 
            :max="totalPages" 
            size="small" 
            style="width: 80px;"
            @change="onPageChange"
          />
          <span class="page-info">/ {{ totalPages }} 页</span>
          <span class="page-tip">（连续滚动模式）</span>
        </div>
        <el-switch v-model="syncEnabled" @change="onSyncScrollToggle" size="small" active-text="同轴滚动" inactive-text=""
          style="margin-right: 8px;" />
        <el-radio-group v-model="filterMode" size="small" class="filter-group">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="DELETE">仅删除</el-radio-button>
          <el-radio-button label="INSERT">仅新增</el-radio-button>
        </el-radio-group>
        <el-button size="small" type="warning" @click="startDebug" :loading="debugLoading">调试模式</el-button>
        <el-button size="small" text @click="goBack">返回上传</el-button>
      </div>
    </div>
    <div class="compare-body" v-loading="loading">
      <!-- 主要对比区域容器 -->
      <div class="compare-container" @click.self="clearSelection">
        <!-- SVG连接线覆盖层 -->
        <svg 
          ref="connectionLinesSvg"
          class="connection-lines-overlay"
        >
        </svg>
        
        <!-- 左侧文档容器盒子 -->
        <div class="document-box left-box">
          <div class="canvas-pane">
            <div class="canvas-header">
              <span class="canvas-title">旧文档</span>
              <span class="canvas-subtitle">（只显示删除内容）</span>
            </div>
            <div class="canvas-container">
              <div class="canvas-wrapper" ref="oldCanvasWrapper">
                <div class="canvas-container" ref="oldCanvasContainer" @click="onCanvasClick('old', $event)"></div>
                <canvas 
                  ref="oldCanvas"
                  style="display: none"
                  @click="onCanvasClick('old', $event)"
                />
                <!-- 左侧Canvas加载特效 - 覆盖整个canvas-wrapper并居中 -->
                <div 
                  v-if="viewerLoading" 
                  class="canvas-loader-wrapper"
                  :style="{
                    position: 'absolute',
                    top: '0',
                    left: '0',
                    right: '0',
                    bottom: '0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 10000,
                    pointerEvents: 'none',
                    background: 'rgba(248, 249, 250, 0.9)'
                  }"
                >
                  <ConcentricLoader 
                    color="#1677ff" 
                    :size="52" 
                    class="canvas-loader left-loader"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间Canvas区域 -->
        <div class="middle-interaction-area">
          <canvas 
            ref="middleCanvas"
            class="middle-canvas"
          ></canvas>
        </div>

        <!-- 右侧文档容器盒子 -->
        <div class="document-box right-box">
          <div class="canvas-pane">
            <div class="canvas-header">
              <span class="canvas-title">新文档</span>
              <span class="canvas-subtitle">（只显示新增内容）</span>
            </div>
            <div class="canvas-container">
              <div class="canvas-wrapper" ref="newCanvasWrapper">
                <div class="canvas-container" ref="newCanvasContainer" @click="onCanvasClick('new', $event)"></div>
                <canvas 
                  ref="newCanvas"
                  style="display: none"
                  @click="onCanvasClick('new', $event)"
                />
                <!-- 右侧Canvas加载特效 - 覆盖整个canvas-wrapper并居中 -->
                <div 
                  v-if="viewerLoading" 
                  class="canvas-loader-wrapper"
                  :style="{
                    position: 'absolute',
                    top: '0',
                    left: '0',
                    right: '0',
                    bottom: '0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 10000,
                    pointerEvents: 'none',
                    background: 'rgba(248, 249, 250, 0.9)'
                  }"
                >
                  <ConcentricLoader 
                    color="#1677ff" 
                    :size="52" 
                    class="canvas-loader right-loader"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧结果列表 -->
      <div class="result-list">
        <div class="head">GPU OCR比对结果 <span class="em">{{ filteredResults.length }}</span> 处（删 {{ deleteCount }} / 增 {{ insertCount }}）</div>
        <div class="list">
          <div v-if="viewerLoading" class="list-loading">
            <ConcentricLoader color="#1677ff" :size="52" text="比对中...16%" class="list-loader" />
            <div class="loading-text-sub">任务预计处理3分钟，期间您可自由使用其他功能</div>
          </div>
          <div v-else-if="filteredResults.length === 0" class="no-differences">
            <div class="no-diff-icon">✓</div>
            <div class="no-diff-title">未发现差异</div>
            <div class="no-diff-desc">两个文档的内容完全一致，没有发现任何差异项。</div>
          </div>
          <div v-else>
            <div
              v-for="(r, i) in filteredResults"
              :key="i"
              class="result-item"
              :class="{ active: indexInAll(i) === activeIndex }"
              @click="jumpTo(indexInAll(i))"
            >
              <div class="headline">
                <span class="index">{{ i + 1 }}</span>
                <span class="badge" :class="r.operation === 'DELETE' ? 'del' : (r.operation === 'INSERT' ? 'ins' : 'mod')">
                  {{ r.operation === 'DELETE' ? '删除' : '新增' }}
                </span>
              </div>
              <div class="content">
                <div class="text">
                  <span
                    v-html="getTruncatedText(
                      r.operation === 'DELETE' ? (r.allTextA || []) : (r.allTextB || []),
                      r.operation === 'DELETE' ? (r.diffRangesA || []) : (r.diffRangesB || []),
                      r.operation === 'DELETE' ? 'delete' : 'insert',
                      isExpanded(indexInAll(i))
                    )"
                  ></span>
                  <span 
                    v-if="needsExpand(r.operation === 'DELETE' ? (r.allTextA || []) : (r.allTextB || []))"
                    class="toggle-btn" 
                    @click.stop="toggleExpand(indexInAll(i))"
                  >
                    {{ isExpanded(indexInAll(i)) ? '收起' : '展开' }}
                  </span>
                </div>
                <div class="meta">
                  第 {{ r.operation === 'DELETE' ? (r.pageA || r.page) : (r.pageB || r.page) }} 页
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getGPUOCRCanvasCompareResult, getGPUOCRCompareTaskStatus, debugGPUCompareLegacy } from '@/api/gpu-ocr-compare'
import ConcentricLoader from '@/components/ai/ConcentricLoader.vue'

// 导入GPU OCR Canvas模块
import {
  // 类型
  type PageLayout,
  type DocumentImageInfo,
  type DifferenceItem,
  type Position,
  type ClickableArea,
  type VisibleRange,
  type CanvasMode,
  type ScrollSide,
  type FilterMode,
  
  // 常量
  CANVAS_CONFIG,
  MARKER_CONFIG,
  TEXT_CONFIG,
  
  // 布局计算
  calculatePageLayout,
  updateVisibleCanvases,
  calculateTotalHeight,
  getCanvasWidth,
  
  // 图片管理
  imageManager,
  
  // 差异数据预处理
  preprocessDifferences,
  
  // Canvas渲染
  renderPageToCanvas,
  createCanvasPool,
  
  // 滚动处理
  alignCanvasViewerContinuous,
  
  // 中间Canvas交互
  createMiddleCanvasInteraction,
  type MiddleCanvasInteractionProps,
  type MiddleCanvasInteraction,
  
  // 同步滚动
  createAdvancedSyncScrollManager,
  type AdvancedSyncScrollManager
} from './gpu-ocr-canvas'

const route = useRoute()
const router = useRouter()

// 基础状态
const loading = ref(false)
const debugLoading = ref(false)
const viewerLoading = ref(true)
const results = ref<DifferenceItem[]>([])
const activeIndex = ref(-1)
const expandedSet = ref<Set<number>>(new Set())
const filterMode = ref<FilterMode>('ALL')
const taskId = ref('')
const compareData = ref<any>(null)

// Canvas相关状态
const oldCanvas = ref<HTMLCanvasElement>()
const newCanvas = ref<HTMLCanvasElement>()
const middleCanvas = ref<HTMLCanvasElement>()
// SVG连接线覆盖层
const connectionLinesSvg = ref<SVGElement>()
const oldCanvasWrapper = ref<HTMLElement>()
const newCanvasWrapper = ref<HTMLElement>()
const oldCanvasContainer = ref<HTMLElement>()
const newCanvasContainer = ref<HTMLElement>()
const oldImageInfo = ref<DocumentImageInfo | null>(null)
const newImageInfo = ref<DocumentImageInfo | null>(null)
// 后端返回的图片基路径，避免前端手动拼接导致taskId缺失
const oldImageBaseUrl = ref<string>('')
const newImageBaseUrl = ref<string>('')
const currentPage = ref(1)
const totalPages = ref(1)

// 连续滚动相关状态
const continuousMode = ref(true) // 启用连续滚动模式

// 记录实际Canvas宽度，用于坐标计算
const actualCanvasWidth = ref({ old: 0, new: 0 })

// 分层Canvas管理
const canvasLayers = ref<{old: HTMLCanvasElement[], new: HTMLCanvasElement[]}>({ old: [], new: [] })
const visibleCanvasRange = ref<VisibleRange>({ start: 0, end: 0, visiblePages: [] })

// 这些函数现在从模块中导入，移除本地定义

// 点击区域管理
const oldCanvasClickableAreas = new Map<string, ClickableArea>()
const newCanvasClickableAreas = new Map<string, ClickableArea>()
// 中间Canvas点击区域映射
const middleCanvasClickableAreas = new Map<string, ClickableArea>()

// 选中的差异项状态
const selectedDiffIndex = ref<number | null>(null)

// 中间Canvas交互实例
let middleCanvasInteraction: MiddleCanvasInteraction | null = null

// 同步滚动管理器
let syncScrollManager: AdvancedSyncScrollManager | null = null
const syncEnabled = ref(true)
const isJumping = ref(false)

// 轮询控制
const pollTimer = ref<number | null>(null)
const isPolling = ref(false)

// 滚动防抖控制
const scrollEndTimer = ref<number | null>(null)
const isScrollEnding = ref(false)
const hasShownProcessingTip = ref(false)

// 文件名显示
const oldFileName = ref('')
const newFileName = ref('')
const displayFileNames = computed(() => oldFileName.value && newFileName.value)


// 计算属性
const filteredResults = computed(() => {
  if (filterMode.value === 'DELETE') return results.value.filter(r => r?.operation === 'DELETE')
  if (filterMode.value === 'INSERT') return results.value.filter(r => r?.operation === 'INSERT')
  return results.value
})

const deleteCount = computed(() => results.value.filter(r => r?.operation === 'DELETE').length)
const insertCount = computed(() => results.value.filter(r => r?.operation === 'INSERT').length)
const totalCount = computed(() => filteredResults.value.length)

const activeFilteredIndex = computed(() => {
  const current = results.value[activeIndex.value]
  if (!current) return -1
  return filteredResults.value.findIndex(r => r === current)
})

const prevDisabled = computed(() => totalCount.value === 0 || activeFilteredIndex.value <= 0)
const nextDisabled = computed(() => totalCount.value === 0 || activeFilteredIndex.value >= totalCount.value - 1)
const displayActiveNumber = computed(() => (activeFilteredIndex.value >= 0 ? activeFilteredIndex.value + 1 : 0))


// 页面变化处理 - 连续滚动版本
const onPageChange = () => {
  if (currentPage.value < 1) currentPage.value = 1
  if (currentPage.value > totalPages.value) currentPage.value = totalPages.value
  
  // 滚动到指定页面
  jumpToPage(currentPage.value)
}

// 初始化分层Canvas系统
const initLayeredCanvasSystem = () => {
  if (!oldCanvasContainer.value || !newCanvasContainer.value) return
  
  // 清空现有Canvas
  oldCanvasContainer.value.innerHTML = ''
  newCanvasContainer.value.innerHTML = ''
  
  // 创建Canvas池
  canvasLayers.value.old = createCanvasPool(CANVAS_CONFIG.MAX_VISIBLE_CANVASES)
  canvasLayers.value.new = createCanvasPool(CANVAS_CONFIG.MAX_VISIBLE_CANVASES)
  
  // 添加到容器
  canvasLayers.value.old.forEach(canvas => oldCanvasContainer.value!.appendChild(canvas))
  canvasLayers.value.new.forEach(canvas => newCanvasContainer.value!.appendChild(canvas))
}

// 初始化中间Canvas交互系统
const initMiddleCanvasInteraction = () => {
  if (!middleCanvas.value || !connectionLinesSvg.value || !oldCanvasWrapper.value) return
  
  const middleArea = middleCanvas.value.parentElement
  if (!middleArea) return
  
  const props: MiddleCanvasInteractionProps = {
    canvas: middleCanvas.value,
    svg: connectionLinesSvg.value,
    leftWrapper: oldCanvasWrapper.value,
    rightWrapper: newCanvasWrapper.value,
    middleArea,
    filteredResults: filteredResults.value,
    oldImageInfo: oldImageInfo.value,
    newImageInfo: newImageInfo.value,
    selectedDiffIndex: selectedDiffIndex.value,
    clickableAreas: middleCanvasClickableAreas,
    onDiffClick: (diffIndex, operation) => {
      // console.log(`从中间Canvas跳转到差异项 ${diffIndex + 1}, 操作: ${operation}`)
      jumpTo(diffIndex)
    },
    onSelectionChange: (diffIndex) => {
      selectedDiffIndex.value = diffIndex
    }
  }
  
  middleCanvasInteraction = createMiddleCanvasInteraction(props)
  middleCanvasInteraction.init()
  
  // 初始化同步滚动管理器
  if (!syncScrollManager) {
    syncScrollManager = createAdvancedSyncScrollManager({
      minDelta: 2,
      scrollEndDelay: 100,
      wheelDetectWindow: 150,
      dragDetectDelay: 50,
      onScroll: handleScrollUpdate, // 添加滚动回调
      isJumping: () => isJumping.value // 添加跳转状态检查
    })
  }
  
  // 初始化同步滚动
  if (syncScrollManager && oldCanvasWrapper.value && newCanvasWrapper.value) {
    syncScrollManager.init(oldCanvasWrapper.value, newCanvasWrapper.value)
    syncScrollManager.setEnabled(syncEnabled.value)
  } else {
  }
  
}

// 渲染页面分隔带（仅针对第1、2页的分隔做特殊样式）
const renderPageSeparators = (container: HTMLElement, layout: Array<{ y: number; height: number }>) => {
  if (!container || !layout || layout.length === 0) return

  // 清除旧的分隔带
  const olds = container.querySelectorAll('.page-separator')
  olds.forEach(el => el.remove())

  // 为每个分页间隙创建分隔带（i 表示上一页索引，分隔发生在 i 与 i+1 之间）
  for (let i = 0; i < layout.length - 1; i++) {
    const sep = document.createElement('div')
    sep.className = 'page-separator'
    sep.style.position = 'absolute'
    sep.style.left = '0'
    sep.style.width = '100%'
    sep.style.pointerEvents = 'none'
    sep.style.zIndex = '0'

    // 分隔带位置：第i页底部 + 间距区域
    const top = layout[i].y + layout[i].height
    sep.style.top = `${top}px`
    sep.style.height = `${CANVAS_CONFIG.PAGE_SPACING}px`

    // 样式规则（统一）：所有页间使用浅灰背景，不画线
    sep.style.background = '#f5f6f8'
    sep.style.borderTop = ''
    sep.style.borderBottom = ''

    container.appendChild(sep)
  }
}

// 使用组件化的渲染函数
const renderPageToCanvasLocal = async (
  canvas: HTMLCanvasElement, 
  imageInfo: any, 
  pageIndex: number, 
  mode: 'old' | 'new', 
  differences: any[], 
  layout: any
) => {
  const baseUrl = mode === 'old' ? oldImageBaseUrl.value : newImageBaseUrl.value
  await renderPageToCanvas(
    canvas,
    imageInfo,
    pageIndex,
    mode,
    differences,
    layout,
    baseUrl,
    taskId.value
  )
}

// 渲染所有页面（使用分层Canvas）
const renderAllPages = async () => {
  if (!oldImageInfo.value || !newImageInfo.value) return
  
  // console.log('开始分层Canvas渲染...')
  
  // 初始化Canvas系统
  initLayeredCanvasSystem()
  
  // 初始化中间Canvas交互
  await nextTick()
  initMiddleCanvasInteraction()
  
  const oldDifferences = results.value.filter(diff => diff.operation === 'DELETE')
  const newDifferences = results.value.filter(diff => diff.operation === 'INSERT')
  
  const containerWidth = getCanvasWidth(oldCanvasWrapper.value || null)
  const oldLayout = calculatePageLayout(oldImageInfo.value, containerWidth)
  const newLayout = calculatePageLayout(newImageInfo.value, containerWidth)
  
  // 记录实际Canvas宽度
  actualCanvasWidth.value.old = containerWidth
  actualCanvasWidth.value.new = containerWidth
  
  // 设置容器总高度（需要包含最后一页的pageSpacing，因为分隔带占用了空间）
  const oldLastPage = oldLayout[oldLayout.length - 1]
  const newLastPage = newLayout[newLayout.length - 1]
  const oldTotalHeight = oldLastPage ? (oldLastPage.y + oldLastPage.height + CANVAS_CONFIG.PAGE_SPACING) : 0
  const newTotalHeight = newLastPage ? (newLastPage.y + newLastPage.height + CANVAS_CONFIG.PAGE_SPACING) : 0
  
  // console.log('容器总高度计算:', {
  //   oldTotalHeight,
  //   newTotalHeight,
  //   oldLastPageY: oldLastPage?.y,
  //   oldLastPageHeight: oldLastPage?.height,
  //   pageSpacing: CANVAS_CONFIG.PAGE_SPACING
  // })
  
  if (oldCanvasContainer.value) {
    oldCanvasContainer.value.style.height = `${oldTotalHeight}px`
    oldCanvasContainer.value.style.position = 'relative'
  }
  if (newCanvasContainer.value) {
    newCanvasContainer.value.style.height = `${newTotalHeight}px`
    newCanvasContainer.value.style.position = 'relative'
  }
  
  // 清除DOM分隔带（现在在Canvas中绘制分隔）
  if (oldCanvasContainer.value) {
    const olds = oldCanvasContainer.value.querySelectorAll('.page-separator')
    olds.forEach(el => el.remove())
  }
  if (newCanvasContainer.value) {
    const olds = newCanvasContainer.value.querySelectorAll('.page-separator')
    olds.forEach(el => el.remove())
  }

  // 初始渲染可见页面
  updateVisiblePagesRender(oldLayout, newLayout, oldDifferences, newDifferences)
  
  // 渲染中间canvas的差异图标和连接线
  await nextTick()
  if (middleCanvasInteraction) {
    middleCanvasInteraction.render()
  }
  
  // console.log('分层Canvas渲染完成')
}



// 更新可见页面渲染（恢复原始逻辑）
const updateVisiblePagesRender = async (
  oldLayout: any[], 
  newLayout: any[], 
  oldDifferences: any[], 
  newDifferences: any[]
) => {
  if (!oldCanvasWrapper.value || !newCanvasWrapper.value) return

  // 预处理差异数据
  const oldPageDiffs = preprocessDifferences(oldDifferences)
  const newPageDiffs = preprocessDifferences(newDifferences)

  // 分别计算两侧的可见范围（使用原始逻辑）
  const oldScrollTop = oldCanvasWrapper.value.scrollTop
  const oldContainerHeight = oldCanvasWrapper.value.clientHeight
  const newScrollTop = newCanvasWrapper.value.scrollTop
  const newContainerHeight = newCanvasWrapper.value.clientHeight

  const oldVisibleRange = updateVisibleCanvases(oldScrollTop, oldContainerHeight, oldLayout)
  const newVisibleRange = updateVisibleCanvases(newScrollTop, newContainerHeight, newLayout)

  // 扩展可见页面以包含所有差异页面
  const oldDiffPageNumbers = Array.from(oldPageDiffs.keys()).map(p => p - 1) // 转换为0基索引
  const newDiffPageNumbers = Array.from(newPageDiffs.keys()).map(p => p - 1) // 转换为0基索引
  
  const extendedOldPages = new Set([...oldVisibleRange.visiblePages, ...oldDiffPageNumbers])
  const extendedNewPages = new Set([...newVisibleRange.visiblePages, ...newDiffPageNumbers])
  

  // 隐藏所有Canvas
  canvasLayers.value.old.forEach(canvas => canvas.style.display = 'none')
  canvasLayers.value.new.forEach(canvas => canvas.style.display = 'none')

  // 渲染旧文档可见页面
  const oldVisiblePages = oldVisibleRange.visiblePages
  for (let i = 0; i < oldVisiblePages.length && i < CANVAS_CONFIG.MAX_VISIBLE_CANVASES; i++) {
    const pageIndex = oldVisiblePages[i]
    if (pageIndex < oldLayout.length && canvasLayers.value.old[i]) {
      await renderPageToCanvasLocal(
        canvasLayers.value.old[i],
        oldImageInfo.value,
        pageIndex,
        'old',
        oldPageDiffs.get(pageIndex + 1) || [], // 传递该页面的差异数据
        oldLayout
      )
    }
  }

  // 渲染新文档扩展页面（包含所有差异页面）
  const newPagesToRender = Array.from(extendedNewPages).sort((a, b) => a - b)
  // console.log(`🖼️ [新文档渲染] 页面: [${newPagesToRender.map(p => p+1).join(',')}]`)
  
  // 动态扩展Canvas池
  while (canvasLayers.value.new.length < Math.min(newPagesToRender.length, 20)) {
    const canvas = document.createElement('canvas')
    canvas.style.position = 'absolute'
    canvas.style.display = 'none'
    canvasLayers.value.new.push(canvas)
    newCanvasContainer.value!.appendChild(canvas)
  }
  
  for (let i = 0; i < newPagesToRender.length && i < 20; i++) {
    const pageIndex = newPagesToRender[i]
    const pageNum = pageIndex + 1
    const pageDiffs = newPageDiffs.get(pageNum) || []
    
    // console.log(`📋 [准备渲染页面${pageNum}] 传递${pageDiffs.length}个差异项到renderPageToCanvasLocal`)
    
    if (pageIndex < newLayout.length && canvasLayers.value.new[i]) {
      await renderPageToCanvasLocal(
        canvasLayers.value.new[i],
        newImageInfo.value,
        pageIndex,
        'new',
        pageDiffs, // 传递该页面的差异数据
        newLayout
      )
    }
  }

}

// 滚动时更新可见Canvas（分别使用各自容器宽度与布局）
const updateVisibleCanvasesOnScroll = async () => {
  if (!oldImageInfo.value || !newImageInfo.value) return

  const oldWidth = getCanvasWidth(oldCanvasWrapper.value || null)
  const newWidth = getCanvasWidth(newCanvasWrapper.value || null)
  const oldLayout = calculatePageLayout(oldImageInfo.value, oldWidth)
  const newLayout = calculatePageLayout(newImageInfo.value, newWidth)

  const oldDifferences = results.value.filter(diff => diff.operation === 'DELETE')
  const newDifferences = results.value.filter(diff => diff.operation === 'INSERT')

  await updateVisiblePagesRender(oldLayout, newLayout, oldDifferences, newDifferences)
}

// 跳转到指定页面
const jumpToPage = (pageNum: number) => {
  if (!oldImageInfo.value || !oldCanvasWrapper.value) return
  
  // 使用记录的Canvas宽度，确保与渲染时一致
  const canvasWidth = actualCanvasWidth.value.old
  const actualWidth = canvasWidth || getCanvasWidth(oldCanvasWrapper.value)
  
  // 计算目标页面的位置（使用实际Canvas宽度）
  let targetY = 0
  
  for (let i = 0; i < pageNum - 1; i++) {
    const pageInfo = oldImageInfo.value.pages[i]
    if (pageInfo) {
      const scale = actualWidth / pageInfo.width
      const scaledHeight = pageInfo.height * scale
      targetY += scaledHeight + CANVAS_CONFIG.PAGE_SPACING
    }
  }
  
  // console.log(`跳转到第${pageNum}页，目标Y位置: ${targetY.toFixed(2)}px`)
  
  // 滚动到目标位置
  if (oldCanvasWrapper.value) {
    oldCanvasWrapper.value.scrollTop = targetY
  }
  if (newCanvasWrapper.value) {
    newCanvasWrapper.value.scrollTop = targetY
  }
  
  // 短暂延迟后建立新的同步基准
  setTimeout(() => {
    if (syncScrollManager) {
      syncScrollManager.syncInitialPositions()
    }
  }, 100)
}


// wheel 事件处理
// 鼠标滚轮处理由 AdvancedSyncScrollManager 自动处理

// 计算加载动画的精准位置 - 参考连接线定位逻辑
const getLoaderPosition = (side: 'old' | 'new') => {
  try {
    // 获取对应的canvas-wrapper元素
    const wrapper = side === 'old' ? oldCanvasWrapper.value : newCanvasWrapper.value
    if (!wrapper) {
      // console.log(`[LoaderPosition] ${side} wrapper not found, using fallback`)
      return {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        zIndex: 1000,
        pointerEvents: 'none'
      }
    }

    // 获取canvas-wrapper的位置和尺寸
    const wrapperRect = wrapper.getBoundingClientRect()
    
    // 检查是否获取到有效的尺寸
    if (wrapperRect.width === 0 || wrapperRect.height === 0) {
      // console.log(`[LoaderPosition] ${side} wrapper has zero size, using fallback`)
      return {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        zIndex: 1000,
        pointerEvents: 'none'
      }
    }
    
    // 获取父容器canvas-container的位置
    const container = wrapper.parentElement
    if (!container) {
      // console.log(`[LoaderPosition] ${side} container not found, using fallback`)
      return {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        zIndex: 1000,
        pointerEvents: 'none'
      }
    }
    
    const containerRect = container.getBoundingClientRect()
    
    // 计算canvas-wrapper相对于canvas-container的位置
    const relativeTop = wrapperRect.top - containerRect.top
    const relativeLeft = wrapperRect.left - containerRect.left
    
    // 计算canvas-wrapper的中心点
    const centerX = relativeLeft + wrapperRect.width / 2
    const centerY = relativeTop + wrapperRect.height / 2
    
    // console.log(`[LoaderPosition] ${side} calculated position:`, {
    //   centerX: centerX.toFixed(1),
    //   centerY: centerY.toFixed(1),
    //   wrapperSize: `${wrapperRect.width}x${wrapperRect.height}`,
    //   containerSize: `${containerRect.width}x${containerRect.height}`
    // })
    
    return {
      position: 'absolute',
      top: `${centerY}px`,
      left: `${centerX}px`,
      transform: 'translate(-50%, -50%)',
      zIndex: 1000,
      pointerEvents: 'none'
    }
  } catch (error) {
    console.error('计算加载动画位置失败:', error)
    // 回退到简单的居中定位
    return {
      position: 'absolute',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      zIndex: 1000,
      pointerEvents: 'none'
    }
  }
}

// Canvas滚动处理由 AdvancedSyncScrollManager 自动处理
// 这里只需要处理虚拟滚动和UI更新
const handleScrollUpdate = () => {
  
  if (isJumping.value) {
    return
  }
  
  // 清除之前的滚动结束定时器
  if (scrollEndTimer.value) {
    clearTimeout(scrollEndTimer.value)
  }
  
  // 立即更新虚拟滚动和中间图标
  requestAnimationFrame(() => {
    updateVisibleCanvasesOnScroll()
    
    // 滚动时总是更新中间图标和连接线（跟随滚动动态更新）
    if (middleCanvasInteraction) {
      middleCanvasInteraction.renderDiffIcons()
    }
  })
  
  // 设置滚动结束检测（200ms后触发重新渲染）
  scrollEndTimer.value = window.setTimeout(() => {
        // console.log('滚动结束，重新渲染页面确保完整性')
    requestAnimationFrame(() => {
      updateVisibleCanvasesOnScroll()
      // 滚动结束后总是更新中间图标
      if (middleCanvasInteraction) {
        middleCanvasInteraction.renderDiffIcons()
      }
    })
    isScrollEnding.value = false
  }, 200)
  
  isScrollEnding.value = true
}

// Canvas点击处理 - 分层Canvas版本
const onCanvasClick = (side: 'old' | 'new', event: MouseEvent) => {
  const container = side === 'old' ? oldCanvasContainer.value : newCanvasContainer.value
  const wrapper = side === 'old' ? oldCanvasWrapper.value : newCanvasWrapper.value
  const clickableAreas = side === 'old' ? oldCanvasClickableAreas : newCanvasClickableAreas
  
  if (!container || !wrapper || clickableAreas.size === 0) return
  
  const rect = container.getBoundingClientRect()
  
  // 计算实际点击位置（考虑滚动）
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top + wrapper.scrollTop
  
  // console.log(`Canvas容器点击: ${side}`, { x, y, scrollTop: wrapper.scrollTop })
  
  // 查找点击的差异区域
  for (const [clickableId, area] of clickableAreas) {
    if (x >= area.x && x <= area.x + area.width &&
        y >= area.y && y <= area.y + area.height) {
      
      // console.log(`点击了差异区域: ${clickableId}`, area)
      
      // 跳转到对应的差异项
      jumpToDifferenceFromCanvas(area.diffIndex, area.operation)
      break
    }
  }
}

// 从Canvas跳转到差异列表项
const jumpToDifferenceFromCanvas = (diffIndex: number, operation: string) => {
  // 在过滤后的结果中找到对应的项
  const targetDiff = results.value[diffIndex]
  if (!targetDiff) return
  
  const filteredIndex = filteredResults.value.findIndex(r => r === targetDiff)
  if (filteredIndex >= 0) {
    // 跳转到列表项
    jumpTo(diffIndex)
    
    // 滚动列表到可见位置
    scrollDifferenceListToItem(filteredIndex)
  }
}

// 滚动差异列表到指定项
const scrollDifferenceListToItem = (filteredIndex: number) => {
  nextTick(() => {
    const resultList = document.querySelector('.result-list .list')
    const targetItem = document.querySelector(`.result-item:nth-child(${filteredIndex + 1})`)
    
    if (resultList && targetItem) {
      const listRect = resultList.getBoundingClientRect()
      const itemRect = targetItem.getBoundingClientRect()
      
      if (itemRect.top < listRect.top || itemRect.bottom > listRect.bottom) {
        targetItem.scrollIntoView({ 
          behavior: 'smooth', 
          block: 'center' 
        })
      }
    }
  })
}

// 同轴滚动开关
const onSyncScrollToggle = () => {
  if (syncScrollManager) {
    syncScrollManager.setEnabled(syncEnabled.value)
    
    if (syncEnabled.value) {
      // 启用时重新同步位置
      syncScrollManager.syncInitialPositions()
    } else {
    }
  }
}

// 跳转到指定差异 - 连续滚动版本
const jumpTo = (i: number) => {
  activeIndex.value = i
  
  // 设置选中的差异项索引，用于显示连接线
  selectedDiffIndex.value = i
  
  const r = results.value[i]
  if (!r) return

  // console.log(`前端跳转调试 - 差异项 ${i + 1}:`, r)

  // 计算跳转位置（本地函数）
  const createPositionLocal = (bbox: number[] | undefined, page: number, description: string) => {
    if (!bbox || bbox.length < 4) {
      // console.log(`前端跳转调试 - ${description}位置创建失败: bbox无效`, bbox)
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

  let oldPos = null
  let newPos = null

  // 根据操作类型确定跳转位置
  if (r.operation === 'INSERT') {
    oldPos = createPositionLocal(r.prevOldBbox, r.pageA || r.page || 0, 'INSERT-old(prevOldBbox)')
    newPos = createPositionLocal(r.newBbox, r.pageB || r.page || 0, 'INSERT-new(newBbox)')
  } else if (r.operation === 'DELETE') {
    oldPos = createPositionLocal(r.oldBbox, r.pageA || r.page || 0, 'DELETE-old(oldBbox)')
    newPos = createPositionLocal(r.prevNewBbox, r.pageB || r.page || 0, 'DELETE-new(prevNewBbox)')
  }

  // 执行Canvas滚动定位
  isJumping.value = true
  
  alignCanvasViewerContinuousLocal('old', oldPos)
  alignCanvasViewerContinuousLocal('new', newPos)
  
  // 跳转后重新渲染Canvas确保页面正确显示
  setTimeout(() => {
    // console.log('差异项跳转完成，重新渲染Canvas')
    requestAnimationFrame(() => {
      updateVisibleCanvasesOnScroll()
      // 跳转后更新中间图标和连接线
      if (middleCanvasInteraction) {
        middleCanvasInteraction.updateProps({
          selectedDiffIndex: selectedDiffIndex.value,
          filteredResults: filteredResults.value
        })
        middleCanvasInteraction.render()
      }
      
      // 以跳转后的位置作为新的同步基准
      if (syncScrollManager) {
        syncScrollManager.syncInitialPositions()
      }
    })
    isJumping.value = false
  }, 200)
}

// Canvas定位函数 - 连续滚动版本
const alignCanvasViewerContinuousLocal = (side: 'old' | 'new', pos: any) => {
  if (!pos || !pos.page) return

  const wrapper = side === 'old' ? oldCanvasWrapper.value : newCanvasWrapper.value
  const imageInfo = side === 'old' ? oldImageInfo.value : newImageInfo.value
  
  if (!wrapper || !imageInfo) return

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

    // console.log(`Canvas连续滚动定位完成: ${side}`, {
    //   页面: pos.page,
    //   页面布局Y: pageLayout.y,
    //   页面高度: pageLayout.height,
    //   缩放比例: pageLayout.scale,
    //   原始坐标: [pos.x, pos.y],
    //   目标坐标: [targetX, targetY],
    //   滚动位置: newScrollTop,
    //   markerY: markerY
    // })

  } catch (error) {
    console.error(`Canvas连续滚动定位失败: ${side}`, error)
  }
}

// 保留原始定位函数用于向后兼容
const alignCanvasViewer = (side: 'old' | 'new', pos: any) => {
  // 重定向到连续滚动版本
  if (pos && !pos.page) {
    pos.page = currentPage.value
  }
  if (side === 'old' && oldCanvasWrapper.value && oldImageInfo.value) {
    alignCanvasViewerContinuous(side, pos, oldCanvasWrapper.value, oldImageInfo.value)
  } else if (side === 'new' && newCanvasWrapper.value && newImageInfo.value) {
    alignCanvasViewerContinuous(side, pos, newCanvasWrapper.value, newImageInfo.value)
  }
}

// 导航函数
const prevResult = () => {
  if (totalCount.value === 0) return
  const i = activeFilteredIndex.value
  if (i > 0) jumpTo(indexInAll(i - 1))
}

const nextResult = () => {
  if (totalCount.value === 0) return
  const i = activeFilteredIndex.value
  if (i >= 0 && i < totalCount.value - 1) jumpTo(indexInAll(i + 1))
}

const goBack = () => {
  router.push({ name: 'GPUOCRCompare' }).catch(() => {})
}

// 映射函数
const indexInAll = (filteredIdx: number): number => {
  const item = filteredResults.value[filteredIdx]
  if (!item) return filteredIdx
  const allIdx = results.value.findIndex(r => r === item)
  return allIdx >= 0 ? allIdx : filteredIdx
}

// 展开/收起
const isExpanded = (idx: number) => expandedSet.value.has(idx)
const toggleExpand = (idx: number) => {
  if (expandedSet.value.has(idx)) {
    expandedSet.value.delete(idx)
  } else {
    expandedSet.value.add(idx)
  }
  expandedSet.value = new Set(expandedSet.value)
}

// 清除选中的差异项和连接线
const clearSelection = () => {
  selectedDiffIndex.value = null
  // 清除连接线
  if (middleCanvasInteraction) {
    middleCanvasInteraction.clearSelection()
  }
}

// 文本处理函数（复用原有逻辑）
const getTruncatedText = (allTextList: string[], diffRanges: any[], type: 'insert' | 'delete', isExpanded: boolean) => {
  if (!allTextList || allTextList.length === 0) return '无'
  
  const fullText = allTextList.join('')
  if (!fullText) return '无'
  
  if (isExpanded || fullText.length <= TEXT_CONFIG.TRUNCATE_LIMIT) {
    return highlightDiffText([fullText], diffRanges, type)
  }
  
  const truncatedText = fullText.substring(0, TEXT_CONFIG.TRUNCATE_LIMIT) + '...'
  return highlightDiffText([truncatedText], diffRanges, type)
}

const needsExpand = (allTextList: string[]) => {
  if (!allTextList || allTextList.length === 0) return false
  const fullText = allTextList.join('')
  return fullText && fullText.length > TEXT_CONFIG.TRUNCATE_LIMIT
}

// 高亮文本函数（复用原有逻辑）
const highlightDiffText = (allTextList: string[], diffRanges: any[], type: 'insert' | 'delete') => {
  if (!allTextList || allTextList.length === 0) return '无'
  const fullText = allTextList.join('')
  if (!fullText) return '无'

  if (!diffRanges || diffRanges.length === 0) {
    return escapeHtml(fullText)
  }

  const originalTextLengths = allTextList.map(text => text.length)
  const originalCumulativeLengths = [0]
  for (let i = 0; i < originalTextLengths.length; i++) {
    originalCumulativeLengths.push(originalCumulativeLengths[i] + originalTextLengths[i])
  }

  const adjustedRanges = diffRanges
    .filter(r => r && typeof r.start === 'number' && typeof r.end === 'number' && r.end > r.start)
    .map(range => {
      let startTextIndex = 0
      let endTextIndex = 0
      
      for (let i = 0; i < originalCumulativeLengths.length - 1; i++) {
        if (range.start >= originalCumulativeLengths[i] && range.start < originalCumulativeLengths[i + 1]) {
          startTextIndex = i
          break
        }
      }
      
      for (let i = 0; i < originalCumulativeLengths.length - 1; i++) {
        if (range.end >= originalCumulativeLengths[i] && range.end < originalCumulativeLengths[i + 1]) {
          endTextIndex = i
          break
        }
      }
      
      const adjustedStart = range.start + startTextIndex
      const adjustedEnd = range.end + endTextIndex
      
      return {
        ...range,
        start: adjustedStart,
        end: adjustedEnd
      }
    })
    .sort((a, b) => a.start - b.start)

  let result = ''
  let lastEnd = 0
  for (const range of adjustedRanges) {
    if (range.start > lastEnd) {
      result += escapeHtml(fullText.substring(lastEnd, range.start))
    }
    const diffText = fullText.substring(range.start, range.end)
    const highlightClass = type === 'insert' ? 'diff-insert' : 'diff-delete'
    result += `<span class="${highlightClass}">${escapeHtml(diffText)}</span>`
    lastEnd = range.end
  }

  if (lastEnd < fullText.length) {
    result += escapeHtml(fullText.substring(lastEnd))
  }

  return result
}

const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 轮询相关函数
const clearPoll = () => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value)
    pollTimer.value = null
  }
}

const schedulePoll = (id: string, delayMs = 1500) => {
  clearPoll()
  isPolling.value = true
  pollTimer.value = window.setTimeout(() => {
    checkStatusAndMaybePoll(id)
  }, delayMs)
}

const checkStatusAndMaybePoll = async (id: string) => {
  try {
    const res = await getGPUOCRCompareTaskStatus(id)
    const code = (res as any)?.code
    const data = (res as any)?.data
    
    if (code !== 200 || !data) {
      viewerLoading.value = true
      schedulePoll(id)
      return
    }

    const status = data.status
    if (status === 'FAILED' || status === 'TIMEOUT') {
      isPolling.value = false
      clearPoll()
      ElMessage.error(data?.statusDesc || '比对任务失败或超时')
      return
    }

    if (status !== 'COMPLETED') {
      viewerLoading.value = true
      schedulePoll(id)
      return
    }

    fetchResult(id)
  } catch (e) {
    schedulePoll(id)
  }
}

// 获取Canvas比对结果
const fetchResult = async (id: string) => {
  if (!id) return
  
  if (id === 'pending') {
    viewerLoading.value = true
    loading.value = false
    ElMessage.info('正在处理比对任务，请稍候...')
    return
  }
  
  loading.value = true
  try {
    const res = await getGPUOCRCanvasCompareResult(id)

    if ((res as any)?.code === 202) {
      viewerLoading.value = true
      if (!hasShownProcessingTip.value) {
        const statusData = (res as any)?.data
        ElMessage.info(statusData?.message || '比对任务处理中，请稍候...')
        hasShownProcessingTip.value = true
      }
      schedulePoll(id)
      return
    } else if ((res as any)?.code !== 200) {
      ElMessage.error((res as any)?.message || '获取比对结果失败')
      return
    }

    const data = (res as any)?.data
    if (data) {
      // 设置图片信息
      oldImageInfo.value = data.oldImageInfo
      newImageInfo.value = data.newImageInfo
      results.value = data.differences || []
      activeIndex.value = results.value.length > 0 ? 0 : -1
      
      // 设置文件名
      oldFileName.value = data.oldFileName || ''
      newFileName.value = data.newFileName || ''
      
      // 设置总页数
      totalPages.value = Math.max(
        oldImageInfo.value?.totalPages || 1,
        newImageInfo.value?.totalPages || 1
      )
      
      // 保存完整的比对结果数据
      compareData.value = data

      // 读取后端提供的图片基路径（如果存在），避免前端手动拼接
      if (typeof (data as any).oldImageBaseUrl === 'string') {
        oldImageBaseUrl.value = (data as any).oldImageBaseUrl
      }
      if (typeof (data as any).newImageBaseUrl === 'string') {
        newImageBaseUrl.value = (data as any).newImageBaseUrl
      }
      
      // console.log('Canvas比对结果加载成功:', {
      //   taskId: id,
      //   oldFileName: oldFileName.value,
      //   newFileName: newFileName.value,
      //   differencesCount: results.value.length,
      //   totalPages: totalPages.value
      // })
      
      // 检查大文档并显示提示
      if (totalPages.value > 50) {
        ElMessage.info(`检测到大文档(${totalPages.value}页)，已自动优化显示性能`)
      }
      
      // 初始化Canvas渲染
      await nextTick()
      renderAllPages()
      
    } else {
      ElMessage.error('加载Canvas OCR比对结果失败')
    }
  } catch (e: any) {
    console.error('加载Canvas比对结果失败:', e)
    ElMessage.error(e?.message || '加载Canvas OCR比对结果失败')
  } finally {
    loading.value = false
    // 无论是否有差异结果，都应该停止loading状态
    //isPolling.value = false
    //clearPoll()
    viewerLoading.value = false
  }
}

// 调试比对
const startDebug = async () => {
  debugLoading.value = true
  try {
    const res = await debugGPUCompareLegacy({
      oldOcrTaskId: '', // 这里需要从当前任务获取
      newOcrTaskId: '', // 这里需要从当前任务获取
      options: {
        ignoreCase: true,
        ignoreSpaces: false
      }
    })

    if ((res as any)?.code !== 200) {
      throw new Error((res as any)?.message || '调试比对失败')
    }

    const newTaskId = (res as any).data?.taskId
    if (!newTaskId) {
      throw new Error('任务ID为空')
    }

    ElMessage.success('调试比对任务已提交，正在处理中...')
    router.push({ name: 'GPUOCRCanvasCompareResult', params: { taskId: newTaskId } }).catch(() => {})

  } catch (e: any) {
    console.error('调试比对失败:', e)
    ElMessage.error(e?.message || '调试比对任务提交失败')
  } finally {
    debugLoading.value = false
  }
}

// 监听筛选模式变化
watch(filterMode, () => {
  if (totalCount.value === 0) return
  const i = activeFilteredIndex.value
  if (i < 0) {
    const first = filteredResults.value[0]
    const idx = results.value.findIndex(r => r === first)
    if (idx >= 0) activeIndex.value = idx
  }
  
  // 筛选模式变化后更新中间图标
  nextTick(() => {
    if (middleCanvasInteraction) {
      middleCanvasInteraction.updateProps({
        filteredResults: filteredResults.value
      })
      middleCanvasInteraction.renderDiffIcons()
    }
  })
})


// 监听路由参数变化
watch(() => route.params.taskId, (newId) => {
  if (typeof newId === 'string' && newId) {
    clearPoll()
    if (newId === 'pending') {
      viewerLoading.value = true
      loading.value = false
    } else {
      checkStatusAndMaybePoll(newId)
    }
  }
})

// 窗口大小变化处理
const handleResize = () => {
  // 重置Canvas宽度记录
  actualCanvasWidth.value.old = 0
  actualCanvasWidth.value.new = 0
  
  // 重新渲染Canvas以适应新的容器宽度
  if (oldImageInfo.value && newImageInfo.value) {
    nextTick(() => {
      renderAllPages()
    })
  }
  
  // 重新初始化中间Canvas
  if (middleCanvasInteraction) {
    nextTick(() => {
      middleCanvasInteraction?.reinit()
    })
  }
}

// 组件挂载
onMounted(() => {
  oldFileName.value = (route.query.oldFileName as string) || ''
  newFileName.value = (route.query.newFileName as string) || ''
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
  
  const id = route.params.taskId as string
  if (id) {
    clearPoll()
    if (id === 'pending') {
      viewerLoading.value = true
      loading.value = false
    } else {
      checkStatusAndMaybePoll(id)
    }
  }
})

// 组件卸载
onUnmounted(() => {
  clearPoll()
  if (scrollEndTimer.value) {
    clearTimeout(scrollEndTimer.value)
  }
  // 销毁中间Canvas交互系统
  if (middleCanvasInteraction) {
    middleCanvasInteraction.destroy()
    middleCanvasInteraction = null
  }
  // 销毁同步滚动管理器
  if (syncScrollManager) {
    syncScrollManager.destroy()
    syncScrollManager = null
  }
  // 移除窗口大小变化监听器
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.gpu-compare-fullscreen { 
  position: fixed; 
  inset: 0; 
  height: 100vh; 
  width: 100vw; 
  background: #f5f6f8; 
  display: flex; 
  flex-direction: column; 
  overflow: hidden; 
}

.compare-toolbar { 
  height: 48px; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 0 12px; 
  border-bottom: 1px solid #e6e8eb; 
  background: #fff; 
}

.compare-toolbar .left { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  flex-direction: column; 
  align-items: flex-start; 
}

.compare-toolbar .title { 
  font-weight: 600; 
  color: #303133; 
  font-size: 14px; 
}

.file-names { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  font-size: 12px; 
  color: #606266; 
  margin-top: 2px; 
}

.file-name { 
  padding: 2px 6px; 
  border-radius: 4px; 
  background: #f5f7fa; 
}

.file-name.old { 
  color: #e6a23c; 
}

.file-name.new { 
  color: #67c23a; 
}

.vs { 
  font-weight: 600; 
  color: #909399; 
}

.compare-toolbar .center { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
}

.compare-toolbar .center .counter { 
  color: #909399; 
  font-size: 12px; 
}

.compare-toolbar .right { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
}

.page-info {
  font-size: 12px;
  color: #606266;
}

.page-tip {
  font-size: 10px;
  color: #909399;
  margin-left: 4px;
}


.filter-group :deep(.el-radio-button__inner) { 
  padding: 6px 10px; 
}

.compare-body { 
  flex: 1; 
  min-height: 0; 
  display: grid; 
  grid-template-columns: 1fr 320px; 
  gap: 12px; 
  padding: 12px; 
  overflow: hidden; 
}

/* 主要对比区域容器 */
.compare-container {
  display: flex;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
  position: relative; /* 为SVG覆盖层提供定位上下文 */
}

/* SVG连接线覆盖层 */
.connection-lines-overlay {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  width: 100% !important;
  height: 100% !important;
  pointer-events: none !important;
  z-index: 9999 !important;
  overflow: visible !important;
}

.canvas-pane { 
  background: #fff; 
  border: 1px solid #ebeef5; 
  border-radius: 6px; 
  overflow: hidden; 
  display: flex; 
  flex-direction: column;
  min-height: 0; 
}

/* 文档容器盒子样式 */
.document-box {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.left-box, .right-box {
  min-height: 0;
}

/* 中间交互区域 */
.middle-interaction-area {
  width: 80px;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

/* 中间Canvas样式 */
.middle-canvas {
  display: block;
  background: transparent;
  width: 100%;
  height: 100%;
  user-select: none; /* 防止选中 */
  transition: opacity 0.2s ease; /* 添加过渡效果 */
}

.middle-canvas:hover {
  opacity: 0.9; /* 悬停时略微透明，提示可交互 */
}

.canvas-header {
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 8px;
}

.canvas-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.canvas-subtitle {
  font-size: 12px;
  color: #909399;
}

.canvas-container { 
  position: relative; 
  flex: 1; 
  min-height: 0; 
}

.canvas-wrapper { 
  width: 100%; 
  height: 100%; 
  min-height: calc(100vh - 120px); /* 撑满页面高度，减去工具栏等固定元素的高度 */
  overflow: auto; 
  position: relative;
}

.canvas-wrapper canvas { 
  display: block; 
  background: #fff;
  cursor: pointer;
  width: 100%; /* 100%宽度 */
}

.canvas-container {
  position: relative;
  width: 100%;
  cursor: pointer;
}

.canvas-container canvas {
  position: absolute;
  left: 0;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
  pointer-events: none; /* Canvas不接收点击，由容器处理 */
  z-index: 1; /* 确保在分隔带之上 */
}


/* Canvas加载特效样式 - 强制居中定位，覆盖组件默认样式 */
.canvas-loader.left-loader,
.canvas-loader.right-loader { 
  position: absolute !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: 1000 !important;
  pointer-events: none !important;
  /* 强制覆盖ConcentricLoader的所有定位样式 */
  inset: unset !important;
  right: unset !important;
  bottom: unset !important;
  /* 确保不被flex布局影响 */
  display: block !important;
  flex-direction: unset !important;
  align-items: unset !important;
  justify-content: unset !important;
}

/* Canvas加载特效包装器样式 - 由内联样式控制定位 */

/* 深度选择器，确保ConcentricLoader组件不影响定位 */
.canvas-loader-wrapper :deep(.concentric-loader) {
  position: static !important;
  inset: unset !important;
  top: unset !important;
  left: unset !important;
  right: unset !important;
  bottom: unset !important;
  transform: none !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
}

.result-list { 
  background: #fff; 
  border: 1px solid #ebeef5; 
  border-radius: 8px; 
  display: flex; 
  flex-direction: column; 
  overflow: hidden; 
}

.result-list .head { 
  padding: 12px; 
  border-bottom: 1px solid #ebeef5; 
  font-weight: 600; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
}

.result-list .head .em { 
  color: #f56c6c; 
}

.result-list .list { 
  flex: 1; 
  overflow: auto; 
  padding: 10px; 
}

.list-loading { 
  position: relative; 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  height: 100%; 
  min-height: 300px; 
}

.list-loader { 
  position: relative; 
  margin-bottom: 20px; 
}

.loading-text-sub { 
  color: #666; 
  font-size: 10px; 
  text-align: center; 
  opacity: 0.8; 
  line-height: 1.4; 
  margin-top: 8px; 
}

.result-item { 
  border: 1px solid #ebeef5; 
  border-radius: 8px; 
  padding: 10px; 
  margin-bottom: 10px; 
  cursor: pointer; 
  background: #fff; 
  transition: box-shadow .2s ease, border-color .2s ease; 
}

.result-item:hover { 
  box-shadow: 0 4px 16px rgba(0,0,0,.06); 
  border-color: #dcdfe6; 
}

.result-item.active { 
  border-color: #409eff; 
  box-shadow: 0 0 0 2px rgba(64,158,255,.15); 
}

.result-item .headline { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  margin-bottom: 6px; 
}

.result-item .index { 
  width: 24px; 
  height: 24px; 
  border-radius: 50%; 
  background: #f2f3f5; 
  color: #606266; 
  display: inline-flex; 
  align-items: center; 
  justify-content: center; 
  font-size: 12px; 
  font-weight: 600; 
}

.result-item .badge { 
  display: inline-block; 
  min-width: 22px; 
  text-align: center; 
  padding: 0 6px; 
  height: 22px; 
  line-height: 22px; 
  border-radius: 6px; 
  font-size: 12px; 
  color: #fff; 
}

.result-item .badge.del { 
  background: #F56C6C; 
}

.result-item .badge.ins { 
  background: #67C23A; 
}

.result-item .badge.mod { 
  background: #E6A23C; 
}

/* 差异文本高亮样式 */
:deep(.diff-insert) {
  background-color: #d4edda;
  color: #155724;
  padding: 1px 2px;
  border-radius: 2px;
  font-weight: bold;
}

:deep(.diff-delete) {
  background-color: #f8d7da;
  color: #721c24;
  padding: 1px 2px;
  border-radius: 2px;
  font-weight: bold;
  text-decoration: line-through;
}

.result-item .content { 
  display: flex; 
  flex-direction: column; 
  gap: 6px; 
}

.result-item .text { 
  color: #303133; 
  font-size: 13px;
  line-height: 1.4;
}

.result-item .text .toggle-btn {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
  margin-left: 4px;
  font-size: 12px;
}

.result-item .text .toggle-btn:hover {
  color: #66b1ff;
}

.result-item .meta { 
  color: #909399; 
  font-size: 12px; 
  margin-top: 4px; 
  display: flex; 
  align-items: center; 
  gap: 8px; 
}

/* 无差异显示样式 */
.no-differences {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
  padding: 40px 20px;
}

.no-diff-icon {
  font-size: 48px;
  color: #67c23a;
  margin-bottom: 16px;
}

.no-diff-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.no-diff-desc {
  font-size: 14px;
  color: #606266;
  text-align: center;
  line-height: 1.5;
}

/* Canvas区域无差异显示样式 */
.no-diff-canvas {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #fafafa;
}

.no-diff-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px;
}

.no-diff-content .no-diff-icon {
  font-size: 36px;
  margin-bottom: 12px;
  opacity: 0.8;
} 

.no-diff-content .no-diff-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
</style>
