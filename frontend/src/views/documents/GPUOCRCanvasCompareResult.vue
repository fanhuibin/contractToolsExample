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
        <span class="counter">第 {{ displayActiveNumber }} / {{ totalCount }} 处</span>
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
      <div class="canvas-pane">
        <div class="canvas-header">
          <span class="canvas-title">旧文档</span>
          <span class="canvas-subtitle">（只显示删除内容）</span>
        </div>
        <div class="canvas-container">
          <div class="canvas-wrapper" @scroll="onCanvasScroll('old', $event)" ref="oldCanvasWrapper">
            <div class="canvas-container" ref="oldCanvasContainer" @click="onCanvasClick('old', $event)"></div>
            <canvas 
              ref="oldCanvas"
              style="display: none"
              @wheel="onWheel('old', $event)"
              @click="onCanvasClick('old', $event)"
            />
          </div>
          <div class="marker-line" :style="markerStyle"></div>
          <ConcentricLoader v-if="viewerLoading" color="#1677ff" :size="52" class="canvas-loader left-loader" />
        </div>
      </div>
      <div class="canvas-pane">
        <div class="canvas-header">
          <span class="canvas-title">新文档</span>
          <span class="canvas-subtitle">（只显示新增内容）</span>
        </div>
        <div class="canvas-container">
          <div class="canvas-wrapper" @scroll="onCanvasScroll('new', $event)" ref="newCanvasWrapper">
            <div class="canvas-container" ref="newCanvasContainer" @click="onCanvasClick('new', $event)"></div>
            <canvas 
              ref="newCanvas"
              style="display: none"
              @wheel="onWheel('new', $event)"
              @click="onCanvasClick('new', $event)"
            />
          </div>
          <div class="marker-line" :style="markerStyle"></div>
          <ConcentricLoader v-if="viewerLoading" color="#1677ff" :size="52" class="canvas-loader right-loader" />
        </div>
      </div>
      <div class="result-list">
        <div class="head">GPU OCR比对结果 <span class="em">{{ filteredResults.length }}</span> 处（删 {{ deleteCount }} / 增 {{ insertCount }}）</div>
        <div class="list">
          <div v-if="viewerLoading" class="list-loading">
            <ConcentricLoader color="#1677ff" :size="52" text="比对中...16%" class="list-loader" />
            <div class="loading-text-sub">任务预计处理3分钟，期间您可自由使用其他功能</div>
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
  SCROLL_CONFIG,
  MARKER_CONFIG,
  TEXT_CONFIG,
  COLORS,
  
  // 布局计算
  calculatePageLayout,
  updateVisibleCanvases,
  calculateTotalHeight,
  getCanvasWidth,
  
  // 图片管理
  imageManager,
  
  // Canvas渲染
  createCanvasPool,
  renderPageToCanvas,
  
  // 滚动处理
  alignCanvasViewerContinuous,
  jumpToPage as jumpToPageHelper,
  createPosition
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

// 同轴滚动相关
const syncEnabled = ref(true)
const lastScrollTop = ref({ old: 0, new: 0 })
const wheelActiveSide = ref<ScrollSide | null>(null)
const wheelTimer = ref<number | null>(null)
const isScrollSyncing = ref(false)
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

// 参考线样式配置
const markerStyle = computed(() => ({ 
  top: `calc(${MARKER_CONFIG.RATIO * 100}% + ${MARKER_CONFIG.VISUAL_OFFSET_PX}px)` 
}))

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

// 图片加载现在由 imageManager 处理

// Canvas渲染函数 - 连续滚动版本
const renderCanvas = async (canvas: HTMLCanvasElement, imageInfo: any, mode: 'old' | 'new', differences: any[] = []) => {
  if (!canvas || !imageInfo || !imageInfo.pages) {
    return
  }

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  try {
    // 获取容器宽度
    const wrapper = mode === 'old' ? oldCanvasWrapper.value : newCanvasWrapper.value
    if (!wrapper) return
    
    const canvasWidth = getCanvasWidth(wrapper)
    
    // 计算总高度和每页的位置信息
    let totalHeight = 0
    const pagePositions: { y: number; height: number; scale: number; pageInfo: any }[] = []
    
    for (let i = 0; i < imageInfo.pages.length; i++) {
      const pageInfo = imageInfo.pages[i]
      const scale = canvasWidth / pageInfo.width
      const scaledHeight = pageInfo.height * scale
      
      pagePositions.push({
        y: totalHeight,
        height: scaledHeight,
        scale,
        pageInfo
      })
      
      totalHeight += scaledHeight
      if (i < imageInfo.pages.length - 1) {
        totalHeight += CANVAS_CONFIG.PAGE_SPACING // 页面间距
      }
    }
    
    // 设置Canvas固定尺寸
    canvas.width = canvasWidth
    canvas.height = totalHeight
    canvas.style.width = `${canvasWidth}px`
    canvas.style.height = `${totalHeight}px`
    
    // 记录实际Canvas宽度，用于坐标计算
    if (mode === 'old') {
      actualCanvasWidth.value.old = canvasWidth
    } else {
      actualCanvasWidth.value.new = canvasWidth
    }
    
    console.log(`[${mode}] Canvas尺寸: ${canvasWidth}x${totalHeight.toFixed(2)}px`)
    
    // 清除Canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    
    // 清空点击区域
    if (mode === 'old') {
      oldCanvasClickableAreas.clear()
    } else {
      newCanvasClickableAreas.clear()
    }
    
    // 绘制所有页面
    for (let i = 0; i < imageInfo.pages.length; i++) {
      const pageInfo = imageInfo.pages[i]
      const position = pagePositions[i]
      
      try {
        // 加载图片
        const img = await imageManager.loadImage(pageInfo.imageUrl!)
        
        // 绘制图片
        ctx.drawImage(img, 0, position.y, canvasWidth, position.height)
        
        // 绘制当前页的差异标记
        const pageDifferences = differences.filter(diff => {
          const pageNum = i + 1
          if (mode === 'old') {
            return (diff.pageA || diff.page) === pageNum
          } else {
            return (diff.pageB || diff.page) === pageNum
          }
        })
        
        drawDifferences(ctx, pageDifferences, mode, position.scale, position.y, pageInfo)
        
        // 绘制页码标识
        drawPageNumber(ctx, i + 1, position.y, canvasWidth, position.height)
        
        // 绘制页面分隔线（除了最后一页）
        if (i < imageInfo.pages.length - 1) {
          drawPageSeparator(ctx, position.y + position.height, canvasWidth)
        }
        
      } catch (error) {
        console.error(`渲染第${i + 1}页失败:`, error)
      }
    }
    
  } catch (error) {
    console.error('渲染Canvas失败:', error)
  }
}

// 绘制差异标记 - 分离标记版本
const drawDifferences = (ctx: CanvasRenderingContext2D, differences: any[], mode: 'old' | 'new', scale: number, yOffset: number, pageInfo: any) => {
  if (!differences || differences.length === 0) return

  differences.forEach((diff, diffIndex) => {
    // 根据模式和操作类型选择要绘制的bbox
    let bboxes: number[][] = []
    let color = ''
    let shouldDraw = false
    
    if (mode === 'old' && diff.operation === 'DELETE') {
      // 旧文档只显示删除的内容
      bboxes = diff.oldBboxes || []
      color = 'rgba(255, 99, 99, 0.4)' // 红色，半透明
      shouldDraw = true
    } else if (mode === 'new' && diff.operation === 'INSERT') {
      // 新文档只显示新增的内容
      bboxes = diff.newBboxes || []
      color = 'rgba(103, 194, 58, 0.4)' // 绿色，半透明
      shouldDraw = true
    }

    if (!shouldDraw) return

    // 绘制bbox
    bboxes.forEach((bbox, bboxIndex) => {
      if (bbox && bbox.length >= 4) {
        const x = bbox[0] * scale
        const y = bbox[1] * scale + yOffset
        const width = (bbox[2] - bbox[0]) * scale
        const height = (bbox[3] - bbox[1]) * scale
        
        // 填充背景
        ctx.fillStyle = color
        ctx.fillRect(x, y, width, height)
        
        // 绘制边框
        ctx.strokeStyle = color.replace('0.4', '0.8')
        ctx.lineWidth = 2
        ctx.strokeRect(x, y, width, height)
        
        // 添加点击区域标识（用于双向跳转）
        const clickableId = `${mode}-${diffIndex}-${bboxIndex}`
        const clickableAreas = mode === 'old' ? oldCanvasClickableAreas : newCanvasClickableAreas
        clickableAreas.set(clickableId, {
          x, y, width, height,
          diffIndex,
          operation: diff.operation,
          bbox,
          originalDiff: diff
        })
      }
    })
  })
}

// 绘制页码标识
const drawPageNumber = (ctx: CanvasRenderingContext2D, pageNum: number, yOffset: number, canvasWidth: number, pageHeight: number) => {
  // 绘制页码背景
  const pageNumText = `第 ${pageNum} 页`
  ctx.font = '14px Arial'
  const textWidth = ctx.measureText(pageNumText).width
  const bgWidth = textWidth + 16
  const bgHeight = 28
  const bgX = canvasWidth - bgWidth - 10
  const bgY = yOffset + 10
  
  // 半透明背景
  ctx.fillStyle = 'rgba(0, 0, 0, 0.7)'
  ctx.fillRect(bgX, bgY, bgWidth, bgHeight)
  
  // 页码文字
  ctx.fillStyle = 'white'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(pageNumText, bgX + bgWidth / 2, bgY + bgHeight / 2)
  
  // 重置文字对齐
  ctx.textAlign = 'start'
  ctx.textBaseline = 'alphabetic'
}

// 绘制页面分隔线
const drawPageSeparator = (ctx: CanvasRenderingContext2D, yPosition: number, canvasWidth: number) => {
  const separatorHeight = CANVAS_CONFIG.PAGE_SPACING
  const centerY = yPosition + separatorHeight / 2
  
  // 绘制分隔区域背景 - 使用浅灰色区分白色文档
  ctx.fillStyle = 'rgba(248, 249, 250, 1.0)' // 更浅的灰色背景
  ctx.fillRect(0, yPosition, canvasWidth, separatorHeight)
  
  // 绘制上边框阴影效果
  ctx.fillStyle = 'rgba(0, 0, 0, 0.05)'
  ctx.fillRect(0, yPosition, canvasWidth, 2)
  
  // 绘制下边框阴影效果
  ctx.fillStyle = 'rgba(0, 0, 0, 0.05)'
  ctx.fillRect(0, yPosition + separatorHeight - 2, canvasWidth, 2)
  
  // 绘制分隔线 - 使用更明显的颜色
  ctx.strokeStyle = 'rgba(200, 200, 200, 0.9)' // 中灰色分隔线
  ctx.lineWidth = 1
  ctx.setLineDash([6, 3]) // 虚线样式
  ctx.beginPath()
  ctx.moveTo(40, centerY)
  ctx.lineTo(canvasWidth - 40, centerY)
  ctx.stroke()
  ctx.setLineDash([]) // 重置为实线
  
  // 绘制页码指示器
  const pageIndicator = '··· 页面分隔 ···'
  ctx.font = '11px Arial'
  ctx.fillStyle = 'rgba(140, 140, 140, 0.8)' // 深灰色文字
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(pageIndicator, canvasWidth / 2, centerY)
  
  // 重置文字对齐
  ctx.textAlign = 'start'
  ctx.textBaseline = 'alphabetic'
}

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

// 本地渲染函数（暂时保留，稍后统一到模块）
const renderPageToCanvasLocal = async (
  canvas: HTMLCanvasElement, 
  imageInfo: any, 
  pageIndex: number, 
  mode: 'old' | 'new', 
  differences: any[], 
  layout: any
) => {
  if (!imageInfo.pages[pageIndex]) return
  
  const pageInfo = imageInfo.pages[pageIndex]
  const wrapper = mode === 'old' ? oldCanvasWrapper.value : newCanvasWrapper.value
  // 使用预先计算好的布局参数，避免与分隔高度不一致导致的偏差
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
    ctx.fillStyle = '#f5f6f8'
    ctx.fillRect(0, scaledHeight, canvasWidth, CANVAS_CONFIG.PAGE_SPACING)
  }
  
  // 加载并绘制图片
  // 优先使用后端返回的基路径，避免taskId为空导致的路径错误
  const baseUrl = mode === 'old' ? oldImageBaseUrl.value : newImageBaseUrl.value
  const imageUrl = baseUrl
    ? `${baseUrl}/page-${pageIndex + 1}.png`
    : `/api/gpu-ocr/files/tasks/${taskId.value}/images/${mode}/page-${pageIndex + 1}.png`
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
        ctx.strokeStyle = mode === 'old' ? '#f56c6c' : '#67c23a'
        ctx.lineWidth = 2
        ctx.strokeRect(x, y, width, height)
        
        // 填充半透明背景
        ctx.fillStyle = mode === 'old' ? 'rgba(245, 108, 108, 0.1)' : 'rgba(103, 194, 58, 0.1)'
        ctx.fillRect(x, y, width, height)
      }
    }
  }

  // 右上角页码信息（分层Canvas每页）- 相对本页Canvas坐标，不受分隔影响
  try {
    if ((canvas as any).__renderKey !== renderKey) return
    const labelPaddingX = 8
    const labelPaddingY = 6
    const labelText = `${mode === 'old' ? '旧' : '新'} 第 ${pageIndex + 1} / ${imageInfo.pages.length} 页`
    ctx.font = 'bold 13px Arial, Helvetica, sans-serif'
    const textMetrics = ctx.measureText(labelText)
    const labelWidth = Math.ceil(textMetrics.width) + labelPaddingX * 2
    const labelHeight = 24
    const labelX = canvasWidth - labelWidth - 10
    const labelY = 10 // 相对于本页canvas起点
    
    // 背景（圆角矩形）
    ctx.save()
    const radius = 6
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
    ctx.fillStyle = 'rgba(0, 0, 0, 0.45)'
    ctx.fill()
    
    // 文本
    ctx.fillStyle = '#fff'
    ctx.textBaseline = 'middle'
    ctx.fillText(labelText, labelX + labelPaddingX, labelY + labelHeight / 2)
    ctx.restore()
  } catch (_) {}
}

// 渲染所有页面（使用分层Canvas）
const renderAllPages = async () => {
  if (!oldImageInfo.value || !newImageInfo.value) return
  
  console.log('开始分层Canvas渲染...')
  
  // 初始化Canvas系统
  initLayeredCanvasSystem()
  
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
  
  console.log('容器总高度计算:', {
    oldTotalHeight,
    newTotalHeight,
    oldLastPageY: oldLastPage?.y,
    oldLastPageHeight: oldLastPage?.height,
    pageSpacing: CANVAS_CONFIG.PAGE_SPACING
  })
  
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
  
  console.log('分层Canvas渲染完成')
}

// 更新可见页面渲染
const updateVisiblePagesRender = async (
  oldLayout: any[], 
  newLayout: any[], 
  oldDifferences: any[], 
  newDifferences: any[]
) => {
  if (!oldCanvasWrapper.value || !newCanvasWrapper.value) return
  
  const scrollTop = oldCanvasWrapper.value.scrollTop
  const containerHeight = oldCanvasWrapper.value.clientHeight
  
  // 计算可见范围
  const visibleRange = updateVisibleCanvases(scrollTop, containerHeight, oldLayout)
  
  // 隐藏所有Canvas
  canvasLayers.value.old.forEach(canvas => canvas.style.display = 'none')
  canvasLayers.value.new.forEach(canvas => canvas.style.display = 'none')
  
  // 渲染可见页面
  const visiblePages = visibleRange.visiblePages
  for (let i = 0; i < visiblePages.length && i < CANVAS_CONFIG.MAX_VISIBLE_CANVASES; i++) {
    const pageIndex = visiblePages[i]
    
    if (pageIndex < oldLayout.length && canvasLayers.value.old[i]) {
      await renderPageToCanvasLocal(
        canvasLayers.value.old[i], 
        oldImageInfo.value, 
        pageIndex, 
        'old', 
        oldDifferences, 
        oldLayout
      )
    }
    
    if (pageIndex < newLayout.length && canvasLayers.value.new[i]) {
      await renderPageToCanvasLocal(
        canvasLayers.value.new[i], 
        newImageInfo.value, 
        pageIndex, 
        'new', 
        newDifferences, 
        newLayout
      )
    }
  }
  
  console.log(`渲染了 ${visiblePages.length} 个可见页面: ${visiblePages[0]}-${visiblePages[visiblePages.length - 1]}`, {
    scrollTop,
    containerHeight,
    visibleRange,
    oldLayoutLength: oldLayout.length,
    newLayoutLength: newLayout.length
  })
}

// 滚动时更新可见Canvas
const updateVisibleCanvasesOnScroll = async () => {
  if (!oldImageInfo.value || !newImageInfo.value) return
  
  const containerWidth = getCanvasWidth(oldCanvasWrapper.value || null)
  const oldLayout = calculatePageLayout(oldImageInfo.value, containerWidth)
  const newLayout = calculatePageLayout(newImageInfo.value, containerWidth)
  
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
  
  console.log(`跳转到第${pageNum}页，目标Y位置: ${targetY.toFixed(2)}px`)
  
  // 滚动到目标位置
  if (oldCanvasWrapper.value) {
    oldCanvasWrapper.value.scrollTop = targetY
  }
  if (newCanvasWrapper.value) {
    newCanvasWrapper.value.scrollTop = targetY
  }
}


// wheel 事件处理
const onWheel = (side: 'old' | 'new', event?: WheelEvent) => {
  wheelActiveSide.value = side
  if (wheelTimer.value) clearTimeout(wheelTimer.value)
  wheelTimer.value = window.setTimeout(() => {
    wheelActiveSide.value = null
  }, 150)
}

// Canvas滚动处理（分层版本）
const onCanvasScroll = (side: 'old' | 'new', event: Event) => {
  if (isJumping.value) return

  const target = event.target as HTMLElement
  const currentTop = target.scrollTop
  const delta = currentTop - lastScrollTop.value[side]
  
  // 清除之前的滚动结束定时器
  if (scrollEndTimer.value) {
    clearTimeout(scrollEndTimer.value)
  }
  
  // 立即更新虚拟滚动
  requestAnimationFrame(() => {
    updateVisibleCanvasesOnScroll()
  })
  
  // 设置滚动结束检测（300ms后触发重新渲染）
  scrollEndTimer.value = window.setTimeout(() => {
    console.log('滚动结束，重新渲染页面确保完整性')
    requestAnimationFrame(() => {
      updateVisibleCanvasesOnScroll()
    })
    isScrollEnding.value = false
  }, 300)
  
  isScrollEnding.value = true
  
  // 如果不启用同步或正在同步中，只更新虚拟滚动，不处理同步逻辑
  if (!syncEnabled.value || isScrollSyncing.value) {
    lastScrollTop.value[side] = currentTop
    return
  }
  
  // 检查滚动侧是否匹配（只在启用同步时检查）
  if (wheelActiveSide.value && wheelActiveSide.value !== side) return
  
  if (Math.abs(delta) > 500) {
    console.warn('检测到异常大的滚动增量，重新同步基准位置:', delta)
    lastScrollTop.value[side] = currentTop
    const otherSide = side === 'old' ? 'new' : 'old'
    const otherWrapper = side === 'old' ? newCanvasWrapper.value : oldCanvasWrapper.value
    if (otherWrapper) {
      lastScrollTop.value[otherSide] = otherWrapper.scrollTop
    }
    return
  }
  
  lastScrollTop.value[side] = currentTop

  if (Math.abs(delta) < 1) return

  // 计算同步因子
  const otherWrapper = side === 'old' ? newCanvasWrapper.value : oldCanvasWrapper.value
  if (!otherWrapper) return

  const fromRange = Math.max(1, target.scrollHeight - target.clientHeight)
  const toRange = Math.max(1, otherWrapper.scrollHeight - otherWrapper.clientHeight)
  const factor = toRange / fromRange

  // 应用增量同步
  isScrollSyncing.value = true
  const otherSide = side === 'old' ? 'new' : 'old'
  otherWrapper.scrollTop = Math.max(0, Math.min(
    otherWrapper.scrollHeight - otherWrapper.clientHeight,
    otherWrapper.scrollTop + delta * factor
  ))
  lastScrollTop.value[otherSide] = otherWrapper.scrollTop
  
  setTimeout(() => {
    isScrollSyncing.value = false
  }, 0)
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
  
  console.log(`Canvas容器点击: ${side}`, { x, y, scrollTop: wrapper.scrollTop })
  
  // 查找点击的差异区域
  for (const [clickableId, area] of clickableAreas) {
    if (x >= area.x && x <= area.x + area.width &&
        y >= area.y && y <= area.y + area.height) {
      
      console.log(`点击了差异区域: ${clickableId}`, area)
      
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
  if (syncEnabled.value) {
    // 重新初始化滚动位置
    const oldWrapper = oldCanvasWrapper.value
    const newWrapper = newCanvasWrapper.value
    if (oldWrapper && newWrapper) {
      lastScrollTop.value.old = oldWrapper.scrollTop
      lastScrollTop.value.new = newWrapper.scrollTop
    }
  } else {
    if (wheelTimer.value) {
      clearTimeout(wheelTimer.value)
      wheelTimer.value = null
    }
    wheelActiveSide.value = null
    isScrollSyncing.value = false
  }
}

// 跳转到指定差异 - 连续滚动版本
const jumpTo = (i: number) => {
  activeIndex.value = i
  const r = results.value[i]
  if (!r) return

  console.log(`前端跳转调试 - 差异项 ${i + 1}:`, r)

  // 计算跳转位置（本地函数）
  const createPositionLocal = (bbox: number[] | undefined, page: number, description: string) => {
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
    console.log('差异项跳转完成，重新渲染Canvas')
    requestAnimationFrame(() => {
      updateVisibleCanvasesOnScroll()
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

// 文本处理函数（复用原有逻辑）
const getTruncatedText = (allTextList: string[], diffRanges: any[], type: 'insert' | 'delete', isExpanded: boolean) => {
  if (!allTextList || allTextList.length === 0) return '无'
  
  const fullText = allTextList.join('\n')
  if (!fullText) return '无'
  
  if (isExpanded || fullText.length <= TEXT_CONFIG.TRUNCATE_LIMIT) {
    return highlightDiffText([fullText], diffRanges, type)
  }
  
  const truncatedText = fullText.substring(0, TEXT_CONFIG.TRUNCATE_LIMIT) + '...'
  return highlightDiffText([truncatedText], diffRanges, type)
}

const needsExpand = (allTextList: string[]) => {
  if (!allTextList || allTextList.length === 0) return false
  const fullText = allTextList.join('\n')
  return fullText && fullText.length > TEXT_CONFIG.TRUNCATE_LIMIT
}

// 高亮文本函数（复用原有逻辑）
const highlightDiffText = (allTextList: string[], diffRanges: any[], type: 'insert' | 'delete') => {
  if (!allTextList || allTextList.length === 0) return '无'
  const fullText = allTextList.join('\n')
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
      
      console.log('Canvas比对结果加载成功:', {
        taskId: id,
        oldFileName: oldFileName.value,
        newFileName: newFileName.value,
        differencesCount: results.value.length,
        totalPages: totalPages.value
      })
      
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
    if ((results.value?.length || 0) > 0) {
      isPolling.value = false
      clearPoll()
      viewerLoading.value = false
    }
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
  if (wheelTimer.value) {
    clearTimeout(wheelTimer.value)
  }
  if (scrollEndTimer.value) {
    clearTimeout(scrollEndTimer.value)
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
  grid-template-columns: 1fr 1fr 320px; 
  gap: 12px; 
  padding: 12px; 
  overflow: hidden; 
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
}

.canvas-container {
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

.marker-line { 
  position: absolute; 
  left: 0; 
  right: 0; 
  height: 0; 
  border-top: 1px dashed #f56c6c; 
  pointer-events: none; 
}

.canvas-loader { 
  position: absolute; 
  top: 50%; 
  left: 50%; 
  transform: translate(-50%, -50%); 
  z-index: 10; 
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
</style>
