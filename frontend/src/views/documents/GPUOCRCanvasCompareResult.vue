<template>
  <div class="gpu-compare-fullscreen">
    <div class="compare-toolbar">
      <div class="left">
        <div class="title">合同比对 </div>
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
        
        <el-button 
          size="small" 
          type="info"
          plain
          @click="toggleDiffList"
        >
          <el-icon><View /></el-icon>
          {{ showDiffList ? '隐藏结果' : '显示结果' }}
        </el-button>
        
        <el-switch v-model="syncEnabled" @change="onSyncScrollToggle" size="small" active-text="同轴滚动" inactive-text=""
          style="margin-right: 8px;" />
       
         <el-button 
           size="small" 
           type="primary" 
           @click="saveUserModificationsToBackend" 
           :loading="savingModifications"
           :disabled="!hasUnsavedModifications"
         >
           <el-icon><DocumentChecked /></el-icon>
           保存修改
         </el-button>
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
              <span class="canvas-title">原文档：{{ oldFileName || '未知文件' }}</span>
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
              <span class="canvas-title">新文档：{{ newFileName || '未知文件' }}</span>
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

      <!-- 右侧差异列表 - 参考样式重构 -->
      <div v-show="showDiffList" class="diff-list-container" :style="{ width: diffListWidth + 'px' }">
        <!-- 拖拽手柄 -->
        <div class="diff-list-drag-box" @mousedown="handleDragStart">
          <svg width="32" height="86" viewBox="0 0 32 86" fill="none">
            <g id="Group 261" filter="url(#filter0_d_2219_7163)">
              <path id="矩形复制23" fill-rule="evenodd" clip-rule="evenodd" d="M30 2L30 80L9.3424 76.5571C7.41365 76.2356 6 74.5668 6 72.6115L6 9.38851C6 7.43315 7.41365 5.76439 9.3424 5.44293L30 2Z" fill="url(#paint0_linear_2219_7163)"></path>
              <path id="矩形" fill-rule="evenodd" clip-rule="evenodd" d="M15.9129 40.6284C15.6923 40.827 15.6923 41.173 15.9129 41.3716L19.6655 44.749C19.9873 45.0386 20.5 44.8102 20.5 44.3773L20.5 37.6227C20.5 37.1898 19.9873 36.9614 19.6655 37.251L15.9129 40.6284Z" fill="#979797"></path>
            </g>
            <defs>
              <filter id="filter0_d_2219_7163" x="0" y="0" width="32" height="86" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
                <feFlood flood-opacity="0" result="BackgroundImageFix"></feFlood>
                <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"></feColorMatrix>
                <feOffset dx="-2" dy="2"></feOffset>
                <feGaussianBlur stdDeviation="2"></feGaussianBlur>
                <feComposite in2="hardAlpha" operator="out"></feComposite>
                <feColorMatrix type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.1 0"></feColorMatrix>
                <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_2219_7163"></feBlend>
                <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_2219_7163" result="shape"></feBlend>
              </filter>
              <linearGradient id="paint0_linear_2219_7163" x1="30" y1="41" x2="6" y2="41" gradientUnits="userSpaceOnUse">
                <stop stop-color="white"></stop>
                <stop offset="1" stop-color="#F0F0F0"></stop>
              </linearGradient>
            </defs>
          </svg>
        </div>
        
        <!-- 差异列表头部 -->
        <div class="diff-list-header">
          <span class="diff-list-title">
            差异列表
          </span>

        </div>
        
        <!-- 差异列表容器 -->
        <div class="diff-list-container-inner">
          
          <!-- 状态选项卡 -->
          <div class="diff-list-header-tabs">
            <div class="tab-header-item" :class="{ active: filterMode === 'ALL' }" @click="filterMode = 'ALL'">
              全部 {{ allCount }}
            </div>
            <div class="tab-header-item" :class="{ active: filterMode === 'DELETE' }" @click="filterMode = 'DELETE'">
              删除 {{ deleteCount }}
            </div>
            <div class="tab-header-item" :class="{ active: filterMode === 'INSERT' }" @click="filterMode = 'INSERT'">
              新增 {{ insertCount }}
            </div>
            <div class="tab-header-item" :class="{ active: filterMode === 'IGNORED' }" @click="filterMode = 'IGNORED'">
              已忽略 {{ ignoredCount }}
            </div>
            <div class="bottom-bar" :style="{ transform: `translateX(${getTabBarPosition()}%)` }"></div>
          </div>
          
          <!-- 差异列表内容 -->
          <div class="diff-list-content">
            <div v-if="viewerLoading" class="list-loading">
              <ConcentricLoader color="#1677ff" :size="52" :text="progressCalculator.progressState.value.loadingText" class="list-loader" />
              <div class="loading-text-sub">{{ progressCalculator.estimatedTimeText.value }}</div>
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
                class="diff-item"
                :class="{ 
                  active: indexInAll(i) === activeIndex,
                  'diff_update': false, // 当前系统只有DELETE和INSERT操作
                  'diff_delete': r.operation === 'DELETE',
                  'diff_insert': r.operation === 'INSERT',
                  'ignored': filterMode === 'IGNORED'
                }"
                @click="jumpTo(indexInAll(i))"
              >
                <div class="headline">
                  <div class="headline-left">
                    <span class="index">{{ i + 1 }}</span>
                    <span class="badge" :class="r.operation === 'DELETE' ? 'del' : (r.operation === 'INSERT' ? 'ins' : 'mod')">
                      {{ r.operation === 'DELETE' ? '删除' : '新增' }}
                    </span>
                  </div>
                  <div class="headline-right">
                    <el-button 
                      size="small" 
                      type="text" 
                      class="ignore-btn"
                      @click.stop="toggleIgnore(indexInAll(i))"
                    >
                      {{ filterMode === 'IGNORED' ? '取消忽略' : '忽略' }}
                    </el-button>
                  </div>
                </div>
                <div class="diff-item-content">
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
                  <div class="diff-item-actions">
                    <el-button 
                      size="small" 
                      type="text" 
                      class="remark-btn"
                      @click.stop="showRemarkDialog(indexInAll(i))"
                    >
                      <el-icon><EditPen /></el-icon>
                      备注
                    </el-button>
                  </div>
                  <!-- 备注显示框 -->
                  <div v-if="hasRemark(indexInAll(i))" class="remark-display-box">
                    <div class="remark-header" @click.stop="toggleRemarkExpand(indexInAll(i))">
                      <span class="remark-title">备注信息</span>
                      <el-icon class="expand-icon" :class="{ expanded: isRemarkExpanded(indexInAll(i)) }">
                        <ArrowRight />
                      </el-icon>
                    </div>
                    <div v-show="isRemarkExpanded(indexInAll(i))" class="remark-content-expanded">
                      {{ getRemark(indexInAll(i)) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 备注对话框 -->
    <el-dialog
      v-model="showRemarkDialogVisible"
      title="添加备注"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="currentRemarkText"
        type="textarea"
        :rows="4"
        placeholder="请输入备注内容..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelRemark">取消</el-button>
          <el-button type="primary" @click="saveRemark">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, View, Close, EditPen, DocumentChecked } from '@element-plus/icons-vue'
import { getGPUOCRCanvasCompareResult, getGPUOCRCompareTaskStatus, saveUserModifications as saveUserModificationsAPI } from '@/api/gpu-ocr-compare'
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
  type AdvancedSyncScrollManager,
  
  // 进度计算
  createProgressCalculator,
  type ProgressCalculator
} from './gpu-ocr-canvas'

const route = useRoute()
const router = useRouter()

// 基础状态
const loading = ref(false)
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

// 进度计算器
const progressCalculator = createProgressCalculator()
const pollTimer = ref<number | null>(null)

// 轮询控制
const clearPoll = () => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value)
    pollTimer.value = null
  }
}

const schedulePoll = (id: string, delayMs = 1500) => {
  clearPoll()
  pollTimer.value = window.setTimeout(() => {
    checkStatusAndMaybePoll(id)
  }, delayMs)
}

// 滚动防抖控制
const scrollEndTimer = ref<number | null>(null)
const isScrollEnding = ref(false)
const hasShownProcessingTip = ref(false)

// 文件名显示
const oldFileName = ref('')
const newFileName = ref('')
const displayFileNames = computed(() => oldFileName.value && newFileName.value)

// 差异列表显示控制
const showDiffList = ref(true) // 默认显示差异列表

// 拖拽调整宽度相关状态
const isDragging = ref(false)
const diffListWidth = ref(300) // 默认宽度300px (原500px的3/5)
const dragStartX = ref(0)
const dragStartWidth = ref(0)

// 忽略和备注状态管理
const ignoredSet = ref<Set<number>>(new Set())
const remarksMap = ref<Map<number, string>>(new Map())
const showRemarkDialogVisible = ref(false)
const currentRemarkIndex = ref(-1)
const currentRemarkText = ref('')
// 移除 showIgnoredView，现在使用 filterMode 来控制
const remarkExpandedSet = ref<Set<number>>(new Set()) // 控制备注展开状态

// 保存修改状态管理
const savingModifications = ref(false) // 是否正在保存
const lastSavedIgnoredSet = ref<Set<number>>(new Set()) // 上次保存的忽略集合
const lastSavedRemarksMap = ref<Map<number, string>>(new Map()) // 上次保存的备注映射

// 计算是否有未保存的修改
const hasUnsavedModifications = computed(() => {
  // 检查忽略集合是否有变化
  if (ignoredSet.value.size !== lastSavedIgnoredSet.value.size) return true
  for (const item of ignoredSet.value) {
    if (!lastSavedIgnoredSet.value.has(item)) return true
  }
  
  // 检查备注映射是否有变化
  if (remarksMap.value.size !== lastSavedRemarksMap.value.size) return true
  for (const [key, value] of remarksMap.value) {
    if (lastSavedRemarksMap.value.get(key) !== value) return true
  }
  
  return false
})



// 计算属性
const filteredResults = computed(() => {
  return results.value.filter((r, index) => {
    // 根据过滤模式进行过滤
    if (filterMode.value === 'IGNORED') {
      // 显示已忽略的项目
      if (!ignoredSet.value.has(index)) return false
    } else {
      // 其他模式显示未忽略的项目
      if (ignoredSet.value.has(index)) return false
    }
    
    // 然后根据操作类型过滤
    if (filterMode.value === 'DELETE') return r?.operation === 'DELETE'
    if (filterMode.value === 'INSERT') return r?.operation === 'INSERT'
    return true
  })
})

// 计算全部项数量（未忽略）
const allCount = computed(() => 
  results.value.filter((r, index) => 
    !ignoredSet.value.has(index)
  ).length
)

// 计算未忽略的删除项数量
const deleteCount = computed(() => 
  results.value.filter((r, index) => 
    r?.operation === 'DELETE' && !ignoredSet.value.has(index)
  ).length
)

// 计算未忽略的新增项数量  
const insertCount = computed(() => 
  results.value.filter((r, index) => 
    r?.operation === 'INSERT' && !ignoredSet.value.has(index)
  ).length
)

// 计算当前过滤后的总数
const totalCount = computed(() => filteredResults.value.length)

// 计算已忽略的总数
const ignoredCount = computed(() => ignoredSet.value.size)

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
  
  // 初始化Canvas系统
  initLayeredCanvasSystem()
  
  // 初始化中间Canvas交互
  await nextTick()
  initMiddleCanvasInteraction()
  
  const oldDifferences = results.value.filter(diff => diff.operation === 'DELETE')
  const newDifferences = results.value.filter(diff => diff.operation === 'INSERT')
  
  // 应用缩放比例到容器宽度
  const baseWidth = getCanvasWidth(oldCanvasWrapper.value || null)
  const containerWidth = baseWidth * 1.0
  const oldLayout = calculatePageLayout(oldImageInfo.value, containerWidth)
  const newLayout = calculatePageLayout(newImageInfo.value, containerWidth)
  
  // 记录实际Canvas宽度
  actualCanvasWidth.value.old = containerWidth
  actualCanvasWidth.value.new = containerWidth
  
  // 设置容器总高度和宽度（需要包含最后一页的pageSpacing，因为分隔带占用了空间）
  const oldLastPage = oldLayout[oldLayout.length - 1]
  const newLastPage = newLayout[newLayout.length - 1]
  const oldTotalHeight = oldLastPage ? (oldLastPage.y + oldLastPage.height + CANVAS_CONFIG.PAGE_SPACING) : 0
  const newTotalHeight = newLastPage ? (newLastPage.y + newLastPage.height + CANVAS_CONFIG.PAGE_SPACING) : 0
  
  if (oldCanvasContainer.value) {
    oldCanvasContainer.value.style.height = `${oldTotalHeight}px`
    oldCanvasContainer.value.style.width = `${containerWidth}px` // 设置宽度以支持横向滚动
    oldCanvasContainer.value.style.position = 'relative'
  }
  if (newCanvasContainer.value) {
    newCanvasContainer.value.style.height = `${newTotalHeight}px`
    newCanvasContainer.value.style.width = `${containerWidth}px` // 设置宽度以支持横向滚动
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

  // 渲染原文档可见页面
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

  // 应用缩放比例
  const oldBaseWidth = getCanvasWidth(oldCanvasWrapper.value || null)
  const newBaseWidth = getCanvasWidth(newCanvasWrapper.value || null)
  const oldWidth = oldBaseWidth * 1.0
  const newWidth = newBaseWidth * 1.0
  const oldLayout = calculatePageLayout(oldImageInfo.value, oldWidth)
  const newLayout = calculatePageLayout(newImageInfo.value, newWidth)

  const oldDifferences = results.value.filter(diff => diff.operation === 'DELETE')
  const newDifferences = results.value.filter(diff => diff.operation === 'INSERT')

  await updateVisiblePagesRender(oldLayout, newLayout, oldDifferences, newDifferences)
}

// 跳转到指定页面
const jumpToPage = (pageNum: number) => {
  if (!oldImageInfo.value || !oldCanvasWrapper.value) return
  
  // 使用记录的Canvas宽度，确保与渲染时一致（已包含缩放）
  const canvasWidth = actualCanvasWidth.value.old
  const actualWidth = canvasWidth || (getCanvasWidth(oldCanvasWrapper.value) * 1.0)
  
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

// 计算当前可见的页面编号
const getCurrentVisiblePage = () => {
  if (!oldCanvasWrapper.value || !oldImageInfo.value) return 1
  
  const scrollTop = oldCanvasWrapper.value.scrollTop
  const baseWidth = getCanvasWidth(oldCanvasWrapper.value)
  const containerWidth = baseWidth * 1.0
  const layout = calculatePageLayout(oldImageInfo.value, containerWidth)
  
  // 根据滚动位置确定当前页面
  let currentPageNum = 1
  for (let i = 0; i < layout.length; i++) {
    const pageLayout = layout[i]
    const pageBottom = pageLayout.y + pageLayout.height
    
    // 如果滚动位置在当前页面范围内，则这就是当前页
    if (scrollTop >= pageLayout.y && scrollTop < pageBottom) {
      currentPageNum = i + 1
      break
    }
    // 如果滚动位置超过当前页底部，继续检查下一页
    if (scrollTop >= pageBottom && i < layout.length - 1) {
      continue
    }
    // 如果是最后一页且滚动位置超过，则停在最后一页
    if (i === layout.length - 1) {
      currentPageNum = layout.length
    }
  }
  
  return currentPageNum
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
    
    // 更新页码显示（根据滚动位置）
    const visiblePage = getCurrentVisiblePage()
    if (visiblePage !== currentPage.value) {
      currentPage.value = visiblePage
    }
  })
  
  // 设置滚动结束检测（200ms后触发重新渲染）
  scrollEndTimer.value = window.setTimeout(() => {
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
  
  // 查找点击的差异区域
  for (const [clickableId, area] of clickableAreas) {
    if (x >= area.x && x <= area.x + area.width &&
        y >= area.y && y <= area.y + area.height) {
      
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

  // 计算跳转位置（本地函数）
  const createPositionLocal = (bbox: number[] | undefined, page: number, description: string) => {
    if (!bbox || bbox.length < 4) {
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
    // 使用预计算的布局，确保与渲染一致（应用缩放）
    const baseWidth = getCanvasWidth(wrapper)
    const containerWidth = baseWidth * 1.0
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

// 忽略和备注功能函数
const isIgnored = (diffIndex: number) => ignoredSet.value.has(diffIndex)

const toggleIgnore = (diffIndex: number) => {
  if (ignoredSet.value.has(diffIndex)) {
    ignoredSet.value.delete(diffIndex)
  } else {
    ignoredSet.value.add(diffIndex)
  }
  ignoredSet.value = new Set(ignoredSet.value)
  
  // 如果当前选中的项目被忽略了，重置选中状态
  if (activeIndex.value === diffIndex && ignoredSet.value.has(diffIndex)) {
    activeIndex.value = -1
    selectedDiffIndex.value = null
  }
  
  // 更新中间Canvas的差异图标和连接线
  nextTick(() => {
    if (middleCanvasInteraction) {
      middleCanvasInteraction.updateProps({
        filteredResults: filteredResults.value,
        selectedDiffIndex: selectedDiffIndex.value
      })
      middleCanvasInteraction.render()
    }
  })
  
  // 可以在这里添加保存到后端的逻辑
  console.log(`差异项 ${diffIndex + 1} ${isIgnored(diffIndex) ? '已忽略' : '已取消忽略'}`)
}

const hasRemark = (diffIndex: number) => remarksMap.value.has(diffIndex) && remarksMap.value.get(diffIndex)

const getRemark = (diffIndex: number) => remarksMap.value.get(diffIndex) || ''

const isRemarkExpanded = (diffIndex: number) => remarkExpandedSet.value.has(diffIndex)

const toggleRemarkExpand = (diffIndex: number) => {
  if (remarkExpandedSet.value.has(diffIndex)) {
    remarkExpandedSet.value.delete(diffIndex)
  } else {
    remarkExpandedSet.value.add(diffIndex)
  }
  remarkExpandedSet.value = new Set(remarkExpandedSet.value)
}

const showRemarkDialog = (diffIndex: number) => {
  currentRemarkIndex.value = diffIndex
  currentRemarkText.value = remarksMap.value.get(diffIndex) || ''
  showRemarkDialogVisible.value = true
}

const saveRemark = () => {
  if (currentRemarkIndex.value >= 0) {
    if (currentRemarkText.value.trim()) {
      remarksMap.value.set(currentRemarkIndex.value, currentRemarkText.value.trim())
      // 保存备注后自动展开显示
      remarkExpandedSet.value.add(currentRemarkIndex.value)
      remarkExpandedSet.value = new Set(remarkExpandedSet.value)
    } else {
      remarksMap.value.delete(currentRemarkIndex.value)
      // 删除备注时也删除展开状态
      remarkExpandedSet.value.delete(currentRemarkIndex.value)
      remarkExpandedSet.value = new Set(remarkExpandedSet.value)
    }
    remarksMap.value = new Map(remarksMap.value)
    
    // 可以在这里添加保存到后端的逻辑
    console.log(`差异项 ${currentRemarkIndex.value + 1} 备注已保存:`, currentRemarkText.value)
  }
  showRemarkDialogVisible.value = false
}

const cancelRemark = () => {
  showRemarkDialogVisible.value = false
  currentRemarkText.value = ''
  currentRemarkIndex.value = -1
}

// 保存用户修改到后端
const saveUserModificationsToBackend = async () => {
  if (!taskId.value) {
    ElMessage.error('任务ID不存在')
    return
  }
  
  if (!hasUnsavedModifications.value) {
    ElMessage.info('没有需要保存的修改')
    return
  }
  
  savingModifications.value = true
  
  try {
    const modifications = {
      ignoredDifferences: Array.from(ignoredSet.value),
      remarks: Object.fromEntries(remarksMap.value)
    }
    
    console.log('🔄 正在保存用户修改...', modifications)
    
    const response = await saveUserModificationsAPI(taskId.value, modifications)
    
    if ((response as any)?.code === 200) {
      // 更新上次保存的状态
      lastSavedIgnoredSet.value = new Set(ignoredSet.value)
      lastSavedRemarksMap.value = new Map(remarksMap.value)
      
      ElMessage.success({
        message: '修改已保存！被忽略的差异项已从数据中移除，备注已添加到差异项中。',
        duration: 3000
      })
      
      console.log('✅ 用户修改保存成功')
      
      // 保存成功后，重新加载数据以显示最新结果
      setTimeout(() => {
        fetchResult(taskId.value)
      }, 500)
    } else {
      throw new Error((response as any)?.message || '保存失败')
    }
  } catch (error: any) {
    console.error('❌ 保存用户修改失败:', error)
    ElMessage.error(error?.message || '保存修改失败，请稍后重试')
  } finally {
    savingModifications.value = false
  }
}

// 移除 toggleIgnoredView 函数，现在直接通过选项卡切换



// 新增方法：获取选项卡底部条位置
const getTabBarPosition = () => {
  if (filterMode.value === 'ALL') return 0
  if (filterMode.value === 'DELETE') return 100 // 移动一个选项卡的宽度
  if (filterMode.value === 'INSERT') return 200 // 移动两个选项卡的宽度
  if (filterMode.value === 'IGNORED') return 300 // 移动三个选项卡的宽度
  return 0
}

// 切换差异列表显示/隐藏
const toggleDiffList = async () => {
  showDiffList.value = !showDiffList.value
  
  // 等待DOM更新完成（容器宽度已变化）
  await nextTick()
  
  // 完整重新渲染：图片、差异框、图标、连接线
  await renderAllPages()
  
  // 额外确保中间区域正确渲染
  await nextTick()
  if (middleCanvasInteraction) {
    middleCanvasInteraction.render()
  }
}

// 拖拽开始
const handleDragStart = (event: MouseEvent) => {
  isDragging.value = true
  dragStartX.value = event.clientX
  dragStartWidth.value = diffListWidth.value
  
  // 添加全局鼠标事件监听
  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', handleDragEnd)
  
  // 防止选中文本和改善拖拽体验
  event.preventDefault()
  document.body.classList.add('dragging')
}

// 拖拽移动
const handleDragMove = (event: MouseEvent) => {
  if (!isDragging.value) return
  
  const deltaX = event.clientX - dragStartX.value
  const newWidth = dragStartWidth.value - deltaX // 向左拖拽增加宽度，向右拖拽减少宽度
  
  // 限制宽度范围：最小300px，最大800px
  const clampedWidth = Math.max(300, Math.min(800, newWidth))
  diffListWidth.value = clampedWidth
}

// 拖拽结束
const handleDragEnd = () => {
  isDragging.value = false
  
  // 移除全局事件监听
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  
  // 恢复样式
  document.body.classList.remove('dragging')
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
    
    // 更新阶段信息和进度
    progressCalculator.updateTaskDataAndStageInfo(data)
    
    if (status === 'FAILED' || status === 'TIMEOUT') {
      // 停止所有定时器
      clearPoll()
      progressCalculator.stopProgressUpdates()
      ElMessage.error(data?.errorMessage || '比对任务失败或超时')
      return
    }

    if (status !== 'COMPLETED') {
      viewerLoading.value = true
      schedulePoll(id)
      return
    }

    // 任务完成，停止轮询和进度更新
    clearPoll()
    progressCalculator.setProgressComplete()

    // 获取结果
    fetchResult(id)
  } catch (e) {
    console.error('获取任务状态失败:', e)
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
      
      // 从后端数据中恢复备注和忽略状态
      remarksMap.value.clear()
      ignoredSet.value.clear()
      results.value.forEach((diff, index) => {
        // 恢复备注
        if (diff.remark) {
          remarksMap.value.set(index, diff.remark)
          // 自动展开有备注的项
          remarkExpandedSet.value.add(index)
        }
        // 恢复忽略状态
        if ((diff as any).ignored === true) {
          ignoredSet.value.add(index)
        }
      })
      
      // 更新上次保存的状态（因为是从后端加载的，视为已保存状态）
      lastSavedIgnoredSet.value = new Set(ignoredSet.value)
      lastSavedRemarksMap.value = new Map(remarksMap.value)
      
      console.log('✅ 从后端恢复备注:', remarksMap.value.size, '条')
      
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

      // 缓存比对结果数据到 localStorage，供任务历史页面使用（持久保存）
      try {
        const cacheKey = `gpu-ocr-canvas-result-${id}`
        const cacheData = {
          totalDiffCount: results.value.length,
          deleteCount: results.value.filter(r => r?.operation === 'DELETE').length,
          insertCount: results.value.filter(r => r?.operation === 'INSERT').length,
          oldFileName: data.oldFileName,
          newFileName: data.newFileName,
          taskId: id,
          completedAt: new Date().toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit', 
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
          }),
          cachedAt: Date.now(),
          // 如果后端有提供处理时长，保存它
          processingTime: data.processingTime || null,
          totalPages: Math.max(
            oldImageInfo.value?.totalPages || 1,
            newImageInfo.value?.totalPages || 1
          )
        }
        
        // 使用 localStorage 而不是 sessionStorage 以实现持久保存
        localStorage.setItem(cacheKey, JSON.stringify(cacheData))
        
        // 同时保存一个任务列表索引，便于后续查询
        const taskListKey = 'gpu-ocr-task-history'
        const existingTasks = JSON.parse(localStorage.getItem(taskListKey) || '[]')
        const taskIndex = {
          taskId: id,
          completedAt: cacheData.completedAt,
          totalDiffCount: cacheData.totalDiffCount,
          oldFileName: data.oldFileName,
          newFileName: data.newFileName
        }
        
        // 避免重复添加同一个任务
        const existingIndex = existingTasks.findIndex((task: any) => task.taskId === id)
        if (existingIndex >= 0) {
          existingTasks[existingIndex] = taskIndex
        } else {
          existingTasks.unshift(taskIndex) // 新任务添加到开头
        }
        
        // 限制保存的任务数量（最多保存100个）
        if (existingTasks.length > 100) {
          existingTasks.splice(100)
        }
        
        localStorage.setItem(taskListKey, JSON.stringify(existingTasks))
        
      } catch (error) {
        console.warn('缓存比对结果失败:', error)
      }

      // 读取后端提供的图片基路径（如果存在），避免前端手动拼接
      if (typeof (data as any).oldImageBaseUrl === 'string') {
        oldImageBaseUrl.value = (data as any).oldImageBaseUrl
      }
      if (typeof (data as any).newImageBaseUrl === 'string') {
        newImageBaseUrl.value = (data as any).newImageBaseUrl
      }
      
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
    viewerLoading.value = false
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
    taskId.value = newId  // 设置taskId
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
    taskId.value = id  // 设置taskId
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
  progressCalculator.cleanup()
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
  
  // 清理拖拽相关事件监听器
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  document.body.classList.remove('dragging')
})
</script>

<style scoped>
/* ==================== 主容器样式 ==================== */
.gpu-compare-fullscreen { 
  position: fixed; 
  inset: 0; 
  height: 100vh; 
  width: 100vw; 
  background: var(--zx-bg-page); 
  display: flex; 
  flex-direction: column; 
  overflow: hidden; 
  font-family: var(--zx-font-family);
}

/* ==================== 工具栏样式 ==================== */
.compare-toolbar { 
  height: var(--zx-toolbar-height); 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 0 var(--zx-spacing-xl); 
  border-bottom: 1px solid var(--zx-border-lighter); 
  background: var(--zx-bg-white); 
  box-shadow: var(--zx-shadow-sm);
  z-index: var(--zx-z-sticky);
}

.compare-toolbar .left { 
  display: flex; 
  align-items: center; 
  gap: var(--zx-spacing-md); 
  flex-direction: column; 
  align-items: flex-start; 
}

.compare-toolbar .title { 
  font-weight: var(--zx-font-semibold); 
  color: var(--zx-text-primary); 
  font-size: var(--zx-font-lg); 
  line-height: var(--zx-leading-tight);
}

.file-names { 
  display: flex; 
  align-items: center; 
  gap: var(--zx-spacing-sm); 
  font-size: var(--zx-font-xs); 
  color: var(--zx-text-regular); 
  margin-top: var(--zx-spacing-xs); 
}

.file-name { 
  padding: var(--zx-spacing-xs) var(--zx-spacing-sm); 
  border-radius: var(--zx-radius-base); 
  background: var(--zx-bg-light); 
  transition: all var(--zx-transition-fast) var(--zx-ease-in-out);
}

.file-name.old { 
  color: var(--zx-warning); 
  background: var(--zx-warning-lighter);
}

.file-name.new { 
  color: var(--zx-success); 
  background: var(--zx-success-lighter);
}

.vs { 
  font-weight: var(--zx-font-semibold); 
  color: var(--zx-text-secondary); 
}

.compare-toolbar .center { 
  display: flex; 
  align-items: center; 
  gap: var(--zx-spacing-lg); 
}

.compare-toolbar .center .counter { 
  color: var(--zx-text-secondary); 
  font-size: var(--zx-font-sm); 
  font-weight: var(--zx-font-medium);
}

.compare-toolbar .right { 
  display: flex; 
  align-items: center; 
  gap: var(--zx-spacing-sm); 
}

.page-controls {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-xs);
  margin-right: var(--zx-spacing-md);
  padding: var(--zx-spacing-xs) var(--zx-spacing-md);
  border-radius: var(--zx-radius-md);
  background: var(--zx-bg-light);
}

.page-info {
  font-size: var(--zx-font-xs);
  color: var(--zx-text-regular);
  font-weight: var(--zx-font-medium);
}

.page-tip {
  font-size: var(--zx-font-xs);
  color: var(--zx-text-secondary);
  margin-left: var(--zx-spacing-xs);
}

/* ==================== 主体区域样式 ==================== */
.compare-body { 
  flex: 1; 
  min-height: 0; 
  display: flex; 
  gap: var(--zx-spacing-md); 
  padding: var(--zx-spacing-md); 
  overflow: hidden; 
}

/* 主要对比区域容器 */
.compare-container {
  display: flex;
  gap: var(--zx-spacing-md);
  min-height: 0;
  overflow: hidden;
  position: relative;
  flex: 1;
}

/* SVG连接线覆盖层 */
.connection-lines-overlay {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  width: 100% !important;
  height: 100% !important;
  pointer-events: none !important;
  z-index: var(--zx-z-top) !important;
  overflow: visible !important;
}

/* ==================== Canvas区域样式 ==================== */
.canvas-pane { 
  background: var(--zx-bg-white); 
  border: 1px solid var(--zx-border-lighter); 
  border-radius: var(--zx-radius-lg); 
  overflow: hidden; 
  display: flex; 
  flex-direction: column;
  min-height: 0; 
  box-shadow: var(--zx-shadow-sm);
  transition: box-shadow var(--zx-transition-base) var(--zx-ease-in-out);
}

.canvas-pane:hover {
  box-shadow: var(--zx-shadow-md);
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
  background: var(--zx-bg-light);
  border-top: 1px solid var(--zx-border-lighter);
  border-bottom: 1px solid var(--zx-border-lighter);
  min-height: 0;
  overflow: hidden;
  position: relative;
  border-radius: var(--zx-radius-md);
}

/* 中间Canvas样式 */
.middle-canvas {
  display: block;
  background: transparent;
  width: 100%;
  height: 100%;
  user-select: none;
  transition: opacity var(--zx-transition-fast) var(--zx-ease-in-out);
}

.middle-canvas:hover {
  opacity: 0.9;
}

.canvas-header {
  padding: var(--zx-spacing-sm) var(--zx-spacing-md);
  background: var(--zx-bg-light);
  border-bottom: 1px solid var(--zx-border-lighter);
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-sm);
}

.canvas-title {
  font-weight: var(--zx-font-semibold);
  color: var(--zx-text-primary);
  font-size: var(--zx-font-base);
  line-height: var(--zx-leading-tight);
}

.canvas-subtitle {
  font-size: var(--zx-font-xs);
  color: var(--zx-text-secondary);
}

.canvas-container { 
  position: relative; 
  flex: 1; 
  min-height: 0; 
}

.canvas-wrapper { 
  width: 100%; 
  height: 100%; 
  min-height: calc(100vh - 120px);
  overflow: auto; 
  position: relative;
}

.canvas-wrapper canvas { 
  display: block; 
  background: var(--zx-bg-white);
  cursor: pointer;
  width: 100%;
}

.canvas-container {
  position: relative;
  width: 100%;
  cursor: pointer;
}

.canvas-container canvas {
  position: absolute;
  left: 0;
  background: var(--zx-bg-white);
  cursor: pointer;
  box-shadow: var(--zx-shadow-sm);
  margin-bottom: var(--zx-spacing-xl);
  pointer-events: none;
  z-index: var(--zx-z-base);
}


/* Canvas加载特效样式 */
.canvas-loader.left-loader,
.canvas-loader.right-loader { 
  position: absolute !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: var(--zx-z-modal) !important;
  pointer-events: none !important;
  inset: unset !important;
  right: unset !important;
  bottom: unset !important;
  display: block !important;
  flex-direction: unset !important;
  align-items: unset !important;
  justify-content: unset !important;
}

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

/* ==================== 差异列表样式 ==================== */
.diff-list-container {
  background: var(--zx-bg-white);
  border: 1px solid var(--zx-border-lighter);
  border-radius: var(--zx-radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  min-width: 300px;
  max-width: 800px;
  box-shadow: var(--zx-shadow-md);
}

.diff-list-drag-box {
  position: absolute;
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: var(--zx-z-dropdown);
  cursor: ew-resize;
  transition: opacity var(--zx-transition-base) var(--zx-ease-in-out);
  opacity: 0.6;
}

.diff-list-drag-box:hover {
  opacity: 1;
}

.diff-list-drag-box:active {
  opacity: 1;
}

body.dragging {
  user-select: none !important;
  cursor: ew-resize !important;
}

body.dragging * {
  pointer-events: none !important;
}

.diff-list-header {
  padding: var(--zx-spacing-md) var(--zx-spacing-lg);
  border-bottom: 1px solid var(--zx-border-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--zx-bg-lighter);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.diff-list-title {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-sm);
  font-weight: var(--zx-font-semibold);
  color: var(--zx-text-primary);
  font-size: var(--zx-font-base);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
  min-width: 0;
}

.ignore-box {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-xs);
  font-size: var(--zx-font-xs);
  color: var(--zx-text-secondary);
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: color var(--zx-transition-base) var(--zx-ease-in-out);
}

.ignore-box:hover {
  color: var(--zx-primary);
}

.ignored-text.active {
  color: var(--zx-primary);
  font-weight: var(--zx-font-semibold);
}

.diff-list-container-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.diff-list-header-tabs {
  display: flex;
  border-bottom: 1px solid var(--zx-border-lighter);
  background: var(--zx-bg-white);
  position: relative;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.tab-header-item {
  flex: 1;
  padding: var(--zx-spacing-md) var(--zx-spacing-sm);
  text-align: center;
  font-size: var(--zx-font-base);
  color: var(--zx-text-regular);
  cursor: pointer;
  transition: all var(--zx-transition-base) var(--zx-ease-in-out);
  border-right: 1px solid var(--zx-border-lighter);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.tab-header-item:last-child {
  border-right: none;
}

.tab-header-item.active {
  color: var(--zx-primary);
  font-weight: var(--zx-font-semibold);
  background: var(--zx-primary-light-9);
}

.tab-header-item:hover {
  color: var(--zx-primary-light-2);
  background: var(--zx-primary-light-9);
}

.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 25%;
  height: 2px;
  background: var(--zx-primary);
  transition: transform var(--zx-transition-base) var(--zx-ease-in-out);
  transform-origin: left center;
}

.diff-list-content {
  flex: 1;
  overflow: auto;
  padding: var(--zx-spacing-md);
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
  margin-bottom: var(--zx-spacing-xl); 
}

.loading-text-sub { 
  color: var(--zx-text-secondary); 
  font-size: var(--zx-font-xs); 
  text-align: center; 
  opacity: 0.8; 
  line-height: var(--zx-leading-normal); 
  margin-top: var(--zx-spacing-sm); 
}

/* ==================== 差异项样式 ==================== */
.diff-item {
  border: 1px solid var(--zx-border-lighter);
  border-radius: var(--zx-radius-lg);
  padding: var(--zx-spacing-md);
  margin-bottom: var(--zx-spacing-md);
  cursor: pointer;
  background: var(--zx-bg-white);
  transition: all var(--zx-transition-base) var(--zx-ease-in-out);
}

.diff-item:hover {
  box-shadow: var(--zx-shadow-base);
  border-color: var(--zx-border-base);
}

.diff-item.active {
  border-color: var(--zx-primary);
  box-shadow: 0 0 0 2px var(--zx-primary-light-8);
  background: var(--zx-primary-light-9);
}

.diff-item.diff_update {
  border-left: 4px solid var(--zx-warning);
}

.diff-item.diff_delete {
  border-left: 4px solid var(--zx-danger);
}

.diff-item.diff_insert {
  border-left: 4px solid var(--zx-success);
}

.diff-item .headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--zx-spacing-sm);
}

.diff-item .headline-left {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-sm);
}

.diff-item .headline-right {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-xs);
}

.diff-item .index {
  width: 24px;
  height: 24px;
  border-radius: var(--zx-radius-full);
  background: var(--zx-bg-light);
  color: var(--zx-text-regular);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--zx-font-xs);
  font-weight: var(--zx-font-semibold);
}

.diff-item .badge {
  display: inline-block;
  min-width: 22px;
  text-align: center;
  padding: 0 var(--zx-spacing-sm);
  height: 22px;
  line-height: 22px;
  border-radius: var(--zx-radius-md);
  font-size: var(--zx-font-xs);
  color: var(--zx-bg-white);
  font-weight: var(--zx-font-medium);
}

.diff-item .badge.del {
  background: var(--zx-danger);
}

.diff-item .badge.ins {
  background: var(--zx-success);
}

.diff-item .badge.mod {
  background: var(--zx-warning);
}

.diff-item-content {
  display: flex;
  flex-direction: column;
  gap: var(--zx-spacing-sm);
}

.diff-item-content .text {
  color: var(--zx-text-primary);
  font-size: var(--zx-font-sm);
  line-height: var(--zx-leading-snug);
}

.diff-item-content .text .toggle-btn {
  color: var(--zx-primary);
  cursor: pointer;
  text-decoration: underline;
  margin-left: var(--zx-spacing-xs);
  font-size: var(--zx-font-xs);
  transition: color var(--zx-transition-fast) var(--zx-ease-in-out);
}

.diff-item-content .text .toggle-btn:hover {
  color: var(--zx-primary-light-2);
}

.diff-item-content .meta {
  color: var(--zx-text-secondary);
  font-size: 11px;
  margin-top: 4px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
}

/* 按钮样式 */
.ignore-btn {
  padding: var(--zx-spacing-xs) var(--zx-spacing-sm) !important;
  font-size: var(--zx-font-xs) !important;
  color: var(--zx-danger) !important;
  opacity: 0.8;
  transition: all var(--zx-transition-base) var(--zx-ease-in-out);
  border-radius: var(--zx-radius-base) !important;
}

.ignore-btn:hover {
  opacity: 1;
  background-color: var(--zx-danger-lighter) !important;
}

.diff-item-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  margin-top: 4px;
  padding-top: 0;
  border-top: none;
}

.remark-btn {
  padding: 2px 6px !important;
  font-size: 11px !important;
  color: var(--zx-primary) !important;
  flex-shrink: 0;
  border-radius: var(--zx-radius-base) !important;
  transition: all var(--zx-transition-base) var(--zx-ease-in-out);
  height: auto !important;
  min-height: auto !important;
}

.remark-btn .el-icon {
  font-size: 12px !important;
  margin-right: 2px;
}

.remark-btn:hover {
  background-color: var(--zx-primary-light-9) !important;
}

/* 备注显示框样式 */
.remark-display-box {
  margin-top: var(--zx-spacing-sm);
  border: 1px solid var(--zx-border-light);
  border-radius: var(--zx-radius-md);
  background: var(--zx-bg-lighter);
  overflow: hidden;
}

.remark-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--zx-spacing-sm) var(--zx-spacing-md);
  background: var(--zx-bg-light);
  cursor: pointer;
  transition: background-color var(--zx-transition-base) var(--zx-ease-in-out);
  border-bottom: 1px solid var(--zx-border-light);
}

.remark-header:hover {
  background: var(--zx-primary-light-9);
}

.remark-title {
  font-size: var(--zx-font-xs);
  color: var(--zx-text-regular);
  font-weight: var(--zx-font-medium);
}

.expand-icon {
  font-size: var(--zx-font-xs);
  color: var(--zx-text-secondary);
  transition: transform var(--zx-transition-base) var(--zx-ease-in-out);
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.remark-content-expanded {
  padding: var(--zx-spacing-md);
  background: var(--zx-bg-white);
  font-size: var(--zx-font-sm);
  color: var(--zx-text-primary);
  line-height: var(--zx-leading-normal);
  word-wrap: break-word;
  white-space: pre-wrap;
  border-top: 1px solid var(--zx-border-extra-light);
}

/* 已忽略的差异项样式 */
.diff-item.ignored {
  background-color: var(--zx-bg-lighter) !important;
  border-color: var(--zx-border-base) !important;
}

.diff-item.ignored .diff-item-content {
  opacity: 0.8;
}

/* 差异文本高亮样式 */
:deep(.diff-insert) {
  background-color: var(--zx-success-lighter);
  color: var(--zx-success);
  padding: 1px var(--zx-spacing-xs);
  border-radius: var(--zx-radius-sm);
  font-weight: var(--zx-font-semibold);
  display: inline;
}

:deep(.diff-delete) {
  background-color: var(--zx-warning-lighter);
  color: var(--zx-warning);
  padding: 1px var(--zx-spacing-xs);
  border-radius: var(--zx-radius-sm);
  font-weight: var(--zx-font-semibold);
  display: inline;
  text-decoration: line-through;
}

.result-item .content { 
  display: flex; 
  flex-direction: column; 
  gap: var(--zx-spacing-sm); 
}

.result-item .text { 
  color: var(--zx-text-primary); 
  font-size: var(--zx-font-sm);
  line-height: var(--zx-leading-snug);
}

.result-item .text .toggle-btn {
  color: var(--zx-primary);
  cursor: pointer;
  text-decoration: underline;
  margin-left: var(--zx-spacing-xs);
  font-size: var(--zx-font-xs);
  transition: color var(--zx-transition-fast) var(--zx-ease-in-out);
}

.result-item .text .toggle-btn:hover {
  color: var(--zx-primary-light-2);
}

.result-item .meta { 
  color: var(--zx-text-secondary); 
  font-size: 11px; 
  margin-top: 4px; 
  display: inline-flex; 
  align-items: center; 
  gap: 4px; 
  margin-right: 8px;
}

/* ==================== 空状态样式 ==================== */
.no-differences {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
  padding: var(--zx-spacing-4xl) var(--zx-spacing-xl);
}

.no-diff-icon {
  font-size: var(--zx-font-5xl);
  color: var(--zx-success);
  margin-bottom: var(--zx-spacing-lg);
}

.no-diff-title {
  font-size: var(--zx-font-xl);
  font-weight: var(--zx-font-semibold);
  color: var(--zx-text-primary);
  margin-bottom: var(--zx-spacing-sm);
}

.no-diff-desc {
  font-size: var(--zx-font-base);
  color: var(--zx-text-regular);
  text-align: center;
  line-height: var(--zx-leading-normal);
}

/* Canvas区域空状态 */
.no-diff-canvas {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--zx-bg-lighter);
}

.no-diff-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--zx-spacing-4xl);
}

.no-diff-content .no-diff-icon {
  font-size: var(--zx-font-4xl);
  margin-bottom: var(--zx-spacing-md);
  opacity: 0.8;
} 

.no-diff-content .no-diff-text {
  font-size: var(--zx-font-base);
  color: var(--zx-text-regular);
  font-weight: var(--zx-font-medium);
}
</style>
