<template>
  <div class="canvas-viewer">
    <!-- 页面控制 -->
    <div class="page-controls">
      <el-button-group size="small">
        <el-button @click="previousPage" :disabled="currentPage <= 1">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        
        <el-input-number 
          v-model="currentPage" 
          :min="1" 
          :max="totalPages"
          size="small"
          style="width: 80px;"
          @change="onPageChange"
          controls-position="right"
        />
        
        <el-button @click="nextPage" :disabled="currentPage >= totalPages">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-button-group>
      
      <span class="page-info">共 {{ totalPages }} 页</span>
    </div>

    <!-- 连续滚动Canvas容器 -->
    <div class="canvas-scroll-container" ref="canvasContainer" @scroll="onScroll">
      <!-- 虚拟滚动内容 -->
      <div class="virtual-content" :style="{ height: totalHeight + 'px' }">
        <!-- 动态渲染的Canvas页面 -->
        <canvas 
          v-for="page in visiblePages" 
          :key="page.index"
          :ref="el => setCanvasRef(el as HTMLCanvasElement, page.index)"
          :data-page="page.index"
          class="page-canvas"
          :style="{
            position: 'absolute',
            top: page.y + 'px',
            left: '50%',
            transform: 'translateX(-50%)',
            width: page.width + 'px',
            height: page.height + 'px'
          }"
          @click="onCanvasClick($event, page.index)"
        />
        
        <!-- 页面标签 -->
        <div 
          v-for="page in visiblePages" 
          :key="'label-' + page.index"
          class="page-label"
          :style="{
            position: 'absolute',
            top: (page.y + 10) + 'px',
            right: '20px'
          }"
        >
          第 {{ page.index + 1 }} 页
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div class="loading-overlay" v-if="loading">
        <el-loading-spinner size="large" text="加载页面图像..." />
      </div>
      
      <!-- 错误状态 -->
      <div class="error-overlay" v-if="error">
        <el-result
          icon="error"
          :title="error"
          sub-title="无法加载页面图像"
        >
          <template #extra>
            <el-button type="primary" @click="retryLoad">重试</el-button>
          </template>
        </el-result>
      </div>
    </div>

    <!-- 缩放控制 -->
    <div class="zoom-controls">
      <el-button-group size="small">
        <el-button @click="zoomOut" :disabled="scale <= 0.2">
          <el-icon><Minus /></el-icon>
        </el-button>
        
        <el-button @click="resetZoom">
          {{ Math.round(scale * 100) }}%
        </el-button>
        
        <el-button @click="zoomIn" :disabled="scale >= 3.0">
          <el-icon><Plus /></el-icon>
        </el-button>
        
        <el-button @click="fitToWidth" text>
          适合宽度
        </el-button>
      </el-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, 
  ArrowRight, 
  Minus, 
  Plus 
} from '@element-plus/icons-vue'

// Props定义
interface Props {
  taskId: string
  totalPages: number
  charBoxes: any[]
  extractions: any[]
  bboxMappings: any[]
  apiPrefix?: string  // 可选的API前缀，用于不同的后端路径
}

const props = withDefaults(defineProps<Props>(), {
  totalPages: 0,
  charBoxes: () => [],
  extractions: () => [],
  bboxMappings: () => [],
  apiPrefix: '/api/extract/files/tasks'  // 默认使用智能提取的路径
})

// Emits
const emit = defineEmits<{
  bboxClick: [bboxInfo: any]
  pageChange: [page: number]
}>()

// 响应式数据
const canvasContainer = ref<HTMLDivElement>()
const currentPage = ref<number>(1)
const scale = ref<number>(1.0)
const loading = ref<boolean>(false)
const error = ref<string>('')

// 连续滚动相关状态
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
const pageImages = ref<Map<number, HTMLImageElement>>(new Map())
const pageLayout = ref<Array<{
  index: number
  y: number
  width: number
  height: number
  actualWidth: number
  actualHeight: number
}>>([])

// 渲染配置
const PAGE_SPACING = 20 // 页面间距
const MAX_VIRTUAL_PAGES = 20 // 超过20页才使用虚拟滚动
const SCROLL_BUFFER = 500 // 滚动缓冲区
const PRELOAD_PAGES = 5 // 预加载前后页数

// 计算Canvas宽度（容器宽度的90%）
const canvasWidth = ref(600) // 默认值

// 更新Canvas宽度
const updateCanvasWidth = () => {
  if (canvasContainer.value) {
    canvasWidth.value = Math.floor(canvasContainer.value.clientWidth * 0.9)
  }
}

// 交互状态
const highlightedBboxes = ref<any[]>([])
const scrollTop = ref(0) // 【修复】响应式的scrollTop，用于触发visiblePages重新计算

// 计算属性
// 总高度计算
const totalHeight = computed(() => {
  if (pageLayout.value.length === 0) return 0
  const lastPage = pageLayout.value[pageLayout.value.length - 1]
  return lastPage.y + lastPage.height
})

// 可见页面计算（智能渲染）
const visiblePages = computed(() => {
  if (pageLayout.value.length === 0) {
    console.log('⚠️ visiblePages: pageLayout为空')
    return []
  }
  
  // 【修复】页面数少于20页时，直接渲染所有页面
  if (props.totalPages <= MAX_VIRTUAL_PAGES) {
    console.log(`✅ visiblePages: 页面数≤${MAX_VIRTUAL_PAGES}，返回所有 ${pageLayout.value.length} 页`)
    return pageLayout.value
  }
  
  // 页面数较多时才使用虚拟滚动
  if (!canvasContainer.value) {
    console.log('⚠️ visiblePages: canvasContainer为空，返回第一页')
    // 【修复】即使容器未就绪，也返回第一页，确保初始渲染
    return pageLayout.value.length > 0 ? [pageLayout.value[0]] : []
  }
  
  // 【修复】使用响应式的scrollTop，而不是直接读取DOM
  const currentScrollTop = scrollTop.value
  const containerHeight = canvasContainer.value.clientHeight
  
  // 【修复】如果容器高度为0（可能还没渲染完成），返回第一页
  if (containerHeight === 0) {
    console.log('⚠️ visiblePages: 容器高度为0，返回第一页')
    return pageLayout.value.length > 0 ? [pageLayout.value[0]] : []
  }
  
  const visibleTop = currentScrollTop - SCROLL_BUFFER
  const visibleBottom = currentScrollTop + containerHeight + SCROLL_BUFFER
  
  const visible = pageLayout.value.filter(page => {
    const pageBottom = page.y + page.height
    return pageBottom >= visibleTop && page.y <= visibleBottom
  })
  
  // 【修复】如果没有可见页面，至少返回第一页
  if (visible.length === 0) {
    console.log('⚠️ visiblePages: 没有可见页面，返回第一页')
    return pageLayout.value.length > 0 ? [pageLayout.value[0]] : []
  }
  
  console.log(`📊 visiblePages [虚拟滚动]: scrollTop=${scrollTop.value}, 可见范围=[${visibleTop}, ${visibleBottom}], 可见页面数=${visible.length}`, 
    visible.map(p => p.index + 1))
  
  return visible
})

// 预加载页面计算（包含可见页面 + 前后几页）
const preloadPages = computed(() => {
  if (pageLayout.value.length === 0) return []
  
  // 页面数少时，直接返回所有页面
  if (props.totalPages <= MAX_VIRTUAL_PAGES) {
    return pageLayout.value
  }
  
  // 获取当前可见页面的索引范围
  const visibleIndexes = visiblePages.value.map(p => p.index)
  if (visibleIndexes.length === 0) return []
  
  const minVisible = Math.min(...visibleIndexes)
  const maxVisible = Math.max(...visibleIndexes)
  
  // 计算预加载范围
  const preloadStart = Math.max(0, minVisible - PRELOAD_PAGES)
  const preloadEnd = Math.min(pageLayout.value.length - 1, maxVisible + PRELOAD_PAGES)
  
  const preloadList = pageLayout.value.slice(preloadStart, preloadEnd + 1)
  
  console.log(`🔄 preloadPages: 可见范围[${minVisible + 1}-${maxVisible + 1}], 预加载范围[${preloadStart + 1}-${preloadEnd + 1}], 预加载页数=${preloadList.length}`)
  
  return preloadList
})

// 监听属性变化
watch(() => props.taskId, () => {
  if (props.taskId) {
    initializePages()
  }
})

watch(() => props.totalPages, () => {
  if (props.totalPages > 0) {
    initializePages()
  }
})

watch(scale, () => {
  updatePageLayout()
})

watch(() => props.bboxMappings, () => {
  renderVisiblePages()
}, { deep: true })

watch(visiblePages, () => {
  renderVisiblePages()
}, { deep: true })

// 监听预加载页面变化，后台预加载图片
watch(preloadPages, () => {
  preloadImages()
}, { deep: true })

// 页面控制
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    scrollToPage(currentPage.value)
  }
}

const nextPage = () => {
  if (currentPage.value < props.totalPages) {
    currentPage.value++
    scrollToPage(currentPage.value)
  }
}

const onPageChange = (page: number) => {
  currentPage.value = page
  scrollToPage(page)
}

// 缩放控制
const zoomIn = () => {
  scale.value = Math.min(scale.value * 1.2, 3.0)
}

const zoomOut = () => {
  scale.value = Math.max(scale.value / 1.2, 0.2)
}

const resetZoom = () => {
  scale.value = 1.0
}

const fitToWidth = () => {
  // 固定宽度600px，不需要适配宽度
  scale.value = 1.0
}

// 初始化页面布局
const initializePages = async () => {
  if (!props.taskId || props.totalPages === 0) {
    console.warn('CanvasViewer: 无法初始化页面', { taskId: props.taskId, totalPages: props.totalPages })
    return
  }
  
  console.log('CanvasViewer: 开始初始化页面', { totalPages: props.totalPages, taskId: props.taskId })
  
  try {
    loading.value = true
    error.value = ''
    
    // 清空之前的数据
    pageImages.value.clear()
    pageLayout.value = []
    
    let currentY = 0
    
    // 页面数少时，一次性加载所有图片
    if (props.totalPages <= MAX_VIRTUAL_PAGES) {
      console.log('CanvasViewer: 一次性加载所有页面')
      
      for (let i = 0; i < props.totalPages; i++) {
        console.log(`CanvasViewer: 加载第 ${i + 1} 页`)
        const img = await loadPageImage(i + 1)
        if (img) {
          pageImages.value.set(i, img)
          console.log(`CanvasViewer: 第 ${i + 1} 页加载成功`, { width: img.naturalWidth, height: img.naturalHeight })
          
          // 计算缩放后的尺寸
          const aspectRatio = img.naturalHeight / img.naturalWidth
          const displayWidth = canvasWidth.value * scale.value
          const displayHeight = displayWidth * aspectRatio
          
          pageLayout.value.push({
            index: i,
            y: currentY,
            width: displayWidth,
            height: displayHeight,
            actualWidth: img.naturalWidth,
            actualHeight: img.naturalHeight
          })
          
          currentY += displayHeight + PAGE_SPACING
        }
      }
    } else {
      // 页面数多时，只先加载第一页确定尺寸
      
      for (let i = 0; i < props.totalPages; i++) {
        if (i === 0) {
          const img = await loadPageImage(i + 1)
          if (img) {
            pageImages.value.set(i, img)
            
            const aspectRatio = img.naturalHeight / img.naturalWidth
            const displayWidth = canvasWidth.value * scale.value
            const displayHeight = displayWidth * aspectRatio
            
            pageLayout.value.push({
              index: i,
              y: currentY,
              width: displayWidth,
              height: displayHeight,
              actualWidth: img.naturalWidth,
              actualHeight: img.naturalHeight
            })
            
            currentY += displayHeight + PAGE_SPACING
          }
        } else {
          // 假设其他页面尺寸相同
          const firstPage = pageLayout.value[0]
          if (firstPage) {
            pageLayout.value.push({
              index: i,
              y: currentY,
              width: firstPage.width,
              height: firstPage.height,
              actualWidth: firstPage.actualWidth,
              actualHeight: firstPage.actualHeight
            })
            
            currentY += firstPage.height + PAGE_SPACING
          }
        }
      }
    }
    
    console.log('CanvasViewer: 页面布局初始化完成', { 
      totalPages: props.totalPages, 
      loadedPages: pageImages.value.size,
      pageLayoutLength: pageLayout.value.length
    })
    
    loading.value = false
    
    // 【修复】立即触发首次渲染和预加载
    await nextTick()
    
    // 强制触发渲染
    console.log('CanvasViewer: 触发首次渲染')
    await renderVisiblePages()
    
    // 【修复】对于大文档，立即触发预加载
    if (props.totalPages > MAX_VIRTUAL_PAGES) {
      console.log('CanvasViewer: 触发预加载')
      await nextTick()
      preloadImages()
    }
    
  } catch (err: any) {
    console.error('CanvasViewer: 初始化页面失败:', err)
    error.value = err.message || '初始化失败'
    loading.value = false
  }
}

// 加载单页图像
const loadPageImage = async (pageNum: number): Promise<HTMLImageElement | null> => {
  try {
    // 根据apiPrefix生成图片URL
    let imageUrl: string
    if (props.apiPrefix === '/api/rule-extract/extract/page-image' || 
        props.apiPrefix === '/api/ocr/extract/page-image') {
      // 智能文档抽取和智能文档解析API格式
      imageUrl = `${props.apiPrefix}/${props.taskId}/${pageNum}`
    } else {
      // 智能提取API格式（默认）
      imageUrl = `${props.apiPrefix}/${props.taskId}/images/page-${pageNum}.png`
    }
    
    console.log(`CanvasViewer: 开始加载图片`, { pageNum, imageUrl })
    
    const img = new Image()
    img.crossOrigin = 'anonymous'
    
    await new Promise((resolve, reject) => {
      img.onload = () => {
        console.log(`CanvasViewer: 图片加载成功`, { pageNum, url: imageUrl })
        resolve(null)
      }
      img.onerror = () => {
        console.error(`CanvasViewer: 图片加载失败`, { pageNum, url: imageUrl })
        reject(new Error(`页面${pageNum}图像加载失败`))
      }
      img.src = imageUrl
    })
    
    return img
  } catch (err) {
    console.error(`加载页面${pageNum}失败:`, err)
    return null
  }
}

// 预加载图片（后台静默加载）
const preloadImages = async () => {
  if (props.totalPages <= MAX_VIRTUAL_PAGES) {
    // 页面数少时，在初始化时已经全部加载，无需预加载
    return
  }
  
  const pagesToPreload = preloadPages.value.filter(page => {
    // 只预加载尚未加载的页面
    return !pageImages.value.has(page.index)
  })
  
  if (pagesToPreload.length === 0) {
    return
  }
  
  console.log(`🔄 开始预加载图片`, pagesToPreload.map(p => p.index + 1))
  
  // 并行预加载，但不阻塞主线程
  const preloadPromises = pagesToPreload.map(async (page) => {
    try {
      const img = await loadPageImage(page.index + 1)
      if (img) {
        pageImages.value.set(page.index, img)
        console.log(`✅ 预加载成功: 第${page.index + 1}页`)
      }
    } catch (err) {
      console.warn(`⚠️ 预加载失败: 第${page.index + 1}页`, err)
    }
  })
  
  // 不等待预加载完成，让它在后台进行
  Promise.all(preloadPromises).then(() => {
    console.log(`✅ 预加载完成，共预加载 ${pagesToPreload.length} 页`)
  }).catch((err) => {
    console.warn('⚠️ 部分预加载失败:', err)
  })
}

// 更新页面布局
const updatePageLayout = () => {
  if (pageLayout.value.length === 0) return
  
  let currentY = 0
  
  pageLayout.value.forEach((page, index) => {
    const displayWidth = canvasWidth.value * scale.value
    const aspectRatio = page.actualHeight / page.actualWidth
    const displayHeight = displayWidth * aspectRatio
    
    page.y = currentY
    page.width = displayWidth
    page.height = displayHeight
    
    currentY += displayHeight + PAGE_SPACING
  })
  
  // 布局更新后重新渲染
  nextTick(() => {
    renderVisiblePages()
  })
}

// 设置Canvas引用
const setCanvasRef = (el: HTMLCanvasElement | null, pageIndex: number) => {
  if (el) {
    canvasRefs.value.set(pageIndex, el)
  } else {
    canvasRefs.value.delete(pageIndex)
  }
}

// 渲染可见页面
const renderVisiblePages = async () => {
  console.log('🎨 renderVisiblePages开始', { 
    visiblePagesCount: visiblePages.value.length,
    visiblePagesIndexes: visiblePages.value.map(p => p.index + 1),
    totalPages: props.totalPages,
    taskId: props.taskId
  })
  
  if (visiblePages.value.length === 0) {
    console.warn('⚠️ renderVisiblePages: 没有可见页面需要渲染')
    return
  }
  
  // 等待DOM更新完成
  await nextTick()
  
  // 【调试】检查DOM中的canvas元素
  const allCanvasElements = document.querySelectorAll('canvas[data-page]')
  console.log(`📊 DOM中找到 ${allCanvasElements.length} 个canvas元素`)
  
  for (const page of visiblePages.value) {
    console.log(`🖼️ 准备渲染页面 ${page.index + 1}`)
    
    let canvas = canvasRefs.value.get(page.index)
    if (!canvas) {
      // 尝试重新查找Canvas元素
      const canvasEl = document.querySelector(`canvas[data-page="${page.index}"]`) as HTMLCanvasElement
      if (canvasEl) {
        canvasRefs.value.set(page.index, canvasEl)
        canvas = canvasEl
        console.log(`  ✅ 找到Canvas元素 (页${page.index + 1})`)
      } else {
        console.warn(`  ⚠️ 未找到Canvas元素 (页${page.index + 1})`)
        console.warn(`  可用的canvas元素:`, Array.from(allCanvasElements).map(c => c.getAttribute('data-page')))
        continue
      }
    }
    
    // 确保图片已加载
    let img = pageImages.value.get(page.index)
    if (!img) {
      console.log(`  📥 开始加载图片 (页${page.index + 1})`)
      const loadedImg = await loadPageImage(page.index + 1)
      if (loadedImg) {
        img = loadedImg
        pageImages.value.set(page.index, img)
        console.log(`  ✅ 图片加载成功 (页${page.index + 1})`)
      } else {
        console.error(`  ❌ 页面 ${page.index + 1} 图片加载失败`)
        continue
      }
    } else {
      console.log(`  ♻️ 使用缓存图片 (页${page.index + 1})`)
    }
    
    // 设置Canvas尺寸
    const dpr = window.devicePixelRatio || 1
    canvas.width = page.width * dpr
    canvas.height = page.height * dpr
    canvas.style.width = page.width + 'px'
    canvas.style.height = page.height + 'px'
    
    const ctx = canvas.getContext('2d')
    if (!ctx) continue
    
    ctx.scale(dpr, dpr)
    
    // 清空Canvas
    ctx.clearRect(0, 0, page.width, page.height)
    
    // 绘制图片
    ctx.drawImage(img, 0, 0, page.width, page.height)
    
    // 绘制该页面的提取内容标记
    drawPageExtractions(ctx, page)
  }
}

// 滚动到指定页面
const scrollToPage = (pageNum: number) => {
  if (!canvasContainer.value || pageNum < 1 || pageNum > props.totalPages) return
  
  const page = pageLayout.value[pageNum - 1]
  if (page) {
    canvasContainer.value.scrollTo({
      top: page.y,
      behavior: 'smooth'
    })
  }
}

// 滚动到指定的bbox位置
const scrollToBbox = (bboxInfo: any) => {
  if (!canvasContainer.value || !bboxInfo || !bboxInfo.bbox || !bboxInfo.page) {
    return
  }
  
  const pageIndex = bboxInfo.page - 1
  const page = pageLayout.value[pageIndex]
  
  if (!page) return
  
  const [x1, y1, x2, y2] = bboxInfo.bbox
  const scale = page.width / page.actualWidth
  
  // 计算bbox在canvas上的y坐标和高度
  const bboxY = Math.round(y1 * scale)
  const bboxHeight = Math.round((y2 - y1) * scale)
  
  // 计算bbox在整个滚动容器中的绝对位置
  const absoluteY = page.y + bboxY
  const absoluteBottom = absoluteY + bboxHeight
  
  // 获取当前滚动位置和容器高度
  const currentScrollTop = canvasContainer.value.scrollTop
  const containerHeight = canvasContainer.value.clientHeight
  const visibleTop = currentScrollTop
  const visibleBottom = currentScrollTop + containerHeight
  
  // 检查bbox是否已经在可视区域内
  const isVisible = absoluteY >= visibleTop && absoluteBottom <= visibleBottom
  
  if (isVisible) {
    return
  }
  
  // 计算滚动位置，让bbox显示在容器中间（留出一些边距）
  const margin = 50 // 顶部留出50px边距
  let targetScrollTop = absoluteY - margin
  
  // 如果bbox比容器还高，就滚动到bbox顶部
  if (bboxHeight > containerHeight - margin * 2) {
    targetScrollTop = absoluteY - margin
  } else {
    // 否则居中显示
    targetScrollTop = absoluteY - (containerHeight - bboxHeight) / 2
  }
  
  // 确保不会滚动到负值
  targetScrollTop = Math.max(0, targetScrollTop)
  
  canvasContainer.value.scrollTo({
    top: targetScrollTop,
    behavior: 'smooth'
  })
}

// 滚动事件处理
const onScroll = () => {
  // 【修复】更新响应式的scrollTop，触发visiblePages重新计算
  if (canvasContainer.value) {
    scrollTop.value = canvasContainer.value.scrollTop
  }
  
  console.log('🔄 onScroll触发', { 
    scrollTop: scrollTop.value,
    totalPages: props.totalPages,
    pageLayoutLength: pageLayout.value.length
  })
  updateCurrentPage()
  // 注意：不需要手动调用renderVisiblePages，因为scrollTop变化会触发visiblePages重新计算，
  // 然后watch(visiblePages)会自动调用renderVisiblePages
}

// 更新当前页面
const updateCurrentPage = () => {
  if (!canvasContainer.value || pageLayout.value.length === 0) {
    return
  }
  
  // 【修复】使用响应式的scrollTop
  const containerHeight = canvasContainer.value.clientHeight
  const centerY = scrollTop.value + containerHeight / 2
  
  // 找到中心点所在的页面
  for (let i = 0; i < pageLayout.value.length; i++) {
    const page = pageLayout.value[i]
    if (centerY >= page.y && centerY <= page.y + page.height) {
      const newPage = i + 1
      
      if (currentPage.value !== newPage) {
        currentPage.value = newPage
        // 通知父组件页码变化
        emit('pageChange', newPage)
      }
      break
    }
  }
}

// 绘制页面的提取内容
const drawPageExtractions = (ctx: CanvasRenderingContext2D, page: any) => {
  console.log(`🎨 开始绘制第${page.index + 1}页的提取内容`, {
    charBoxesCount: props.charBoxes.length,
    bboxMappingsCount: props.bboxMappings?.length || 0
  })
  
  // 【调试】输出前几个charBoxes的详细信息
  if (props.charBoxes.length > 0) {
    console.log('📦 charBoxes示例（前3个）:', props.charBoxes.slice(0, 3))
  }
  
  // 【调试】输出前几个bboxMappings的详细信息
  if (props.bboxMappings && props.bboxMappings.length > 0) {
    console.log('📦 bboxMappings示例（前3个）:', props.bboxMappings.slice(0, 3))
  }
  
  // 创建高亮bbox的查找集合（用于快速判断）
  const highlightedSet = new Set(
    highlightedBboxes.value
      .filter(b => b.page === page.index + 1)
      .map(b => `${b.page}-${b.bbox.join('-')}`)
  )
  
  let drawnCount = 0
  
  // 【修复】判断是否为OCR模式 - 检查charBoxes是否为textBoxes格式
  const hasTextBoxes = props.charBoxes.length > 0 && props.charBoxes[0]?.text !== undefined
  // 【修复】处理bboxMappings可能是undefined的情况
  const bboxMappingsCount = props.bboxMappings?.length || 0
  const isOcrMode = hasTextBoxes && bboxMappingsCount === 0
  
  console.log(`📊 绘制模式判断:`, {
    hasTextBoxes,
    isOcrMode,
    charBoxesLength: props.charBoxes.length,
    bboxMappingsLength: bboxMappingsCount,
    bboxMappingsRaw: props.bboxMappings,
    firstCharBox: props.charBoxes[0]
  })
  
  if (isOcrMode) {
    // 【修复】OCR模式：charBoxes实际是textBoxes，直接绘制
    const pageTextBoxes = props.charBoxes.filter((tb: any) => tb.page === page.index + 1)
    
    console.log(`📝 OCR模式: 第${page.index + 1}页找到 ${pageTextBoxes.length} 个textBox`)
    
    // 【调试】输出前几个textBox的详细信息
    if (pageTextBoxes.length > 0) {
      console.log('📦 当前页textBox示例（前3个）:', pageTextBoxes.slice(0, 3))
    }
    
    pageTextBoxes.forEach((textBox: any, index: number) => {
      if (textBox.bbox && textBox.bbox.length >= 4) {
        const bboxKey = `${textBox.page}-${textBox.bbox.join('-')}`
        const isHighlighted = highlightedSet.has(bboxKey)
        
        // 【调试】输出前几个bbox的绘制参数
        if (index < 3) {
          console.log(`  绘制textBox[${index}]:`, {
            page: textBox.page,
            bbox: textBox.bbox,
            text: textBox.text?.substring(0, 20),
            isHighlighted
          })
        }
        
        drawExtractionBbox(ctx, textBox, page, isHighlighted)
        drawnCount++
      } else {
        console.warn(`  ⚠️ textBox[${index}] bbox无效:`, textBox)
      }
    })
    
    console.log(`✅ OCR模式：第${page.index + 1}页绘制了 ${drawnCount} 个bbox`)
  } else if (bboxMappingsCount > 0) {
    // 智能提取模式：直接遍历bboxMappings
    props.bboxMappings!.forEach((mapping, mappingIndex) => {
      // 检查这个mapping是否在当前页面
      if (mapping.pages && mapping.pages.includes(page.index + 1)) {
        // 遍历该mapping的所有bbox（已经合并过，不是字符级的）
        if (mapping.bboxes && Array.isArray(mapping.bboxes)) {
          mapping.bboxes.forEach((bboxInfo: any, bboxIndex: number) => {
            // 只绘制当前页面的bbox
            if (bboxInfo.page === page.index + 1) {
              const bboxKey = `${bboxInfo.page}-${bboxInfo.bbox.join('-')}`
              const isHighlighted = highlightedSet.has(bboxKey)
              
              // 【调试】输出前几个bbox的绘制参数
              if (drawnCount < 3) {
                console.log(`  绘制bboxMapping[${mappingIndex}].bbox[${bboxIndex}]:`, {
                  page: bboxInfo.page,
                  bbox: bboxInfo.bbox,
                  isHighlighted
                })
              }
              
              // 直接绘制，每个bbox只绘制一次
              drawExtractionBbox(ctx, bboxInfo, page, isHighlighted)
              drawnCount++
            }
          })
        }
      }
    })
    
    if (drawnCount > 0) {
      console.log(`✅ 智能提取模式：第${page.index + 1}页绘制了 ${drawnCount} 个bbox`)
    } else {
      console.warn(`⚠️ 智能提取模式：第${page.index + 1}页没有绘制任何bbox`)
    }
  } else {
    console.warn(`⚠️ 第${page.index + 1}页没有可绘制的bbox数据`)
  }
}

// 将字符级CharBox合并为词组bbox（优化性能和可读性）
const mergeCharBoxesToWords = (charBoxes: any[]): any[] => {
  if (!charBoxes || charBoxes.length === 0) return []
  
  const merged: any[] = []
  let currentBox: any = null
  
  for (let i = 0; i < charBoxes.length; i++) {
    const charBox = charBoxes[i]
    
    // 跳过换行符和空格
    if (charBox.ch === '\n' || charBox.ch === ' ') {
      if (currentBox) {
        merged.push(currentBox)
        currentBox = null
      }
      continue
    }
    
    if (!currentBox) {
      // 开始新的词组
      currentBox = {
        page: charBox.page,
        bbox: [...charBox.bbox],
        category: charBox.category,
        text: charBox.ch
      }
    } else {
      // 检查是否应该合并（在同一行且距离较近）
      const distance = charBox.bbox[0] - currentBox.bbox[2] // 水平距离
      const verticalDistance = Math.abs(charBox.bbox[1] - currentBox.bbox[1]) // 垂直距离
      
      if (distance < 20 && verticalDistance < 5) {
        // 扩展当前bbox
        currentBox.bbox[2] = charBox.bbox[2] // 更新右边界
        currentBox.bbox[3] = Math.max(currentBox.bbox[3], charBox.bbox[3]) // 更新下边界
        currentBox.bbox[1] = Math.min(currentBox.bbox[1], charBox.bbox[1]) // 更新上边界
        currentBox.text += charBox.ch
      } else {
        // 开始新词组
        merged.push(currentBox)
        currentBox = {
          page: charBox.page,
          bbox: [...charBox.bbox],
          category: charBox.category,
          text: charBox.ch
        }
      }
    }
  }
  
  // 添加最后一个词组
  if (currentBox) {
    merged.push(currentBox)
  }
  
  return merged
}

// 绘制提取内容的bbox（参考合同比对实现）
const drawExtractionBbox = (ctx: CanvasRenderingContext2D, bboxInfo: any, page: any, isHighlighted: boolean = false) => {
  if (!bboxInfo.bbox || bboxInfo.bbox.length < 4) {
    console.warn('❌ 无效的bbox数据:', bboxInfo)
    return
  }
  
  const [x1, y1, x2, y2] = bboxInfo.bbox
  
  // 【修复】计算缩放比例 - 确保使用正确的缩放算法
  const scale = page.width / page.actualWidth
  
  // 【修复】计算在Canvas上的精确位置 - 添加边界检查
  const x = Math.max(0, Math.round(x1 * scale))
  const y = Math.max(0, Math.round(y1 * scale))
  const width = Math.min(page.width - x, Math.round((x2 - x1) * scale))
  const height = Math.min(page.height - y, Math.round((y2 - y1) * scale))
  
  // 【调试】输出计算过程
  const debugInfo = {
    原始bbox: bboxInfo.bbox,
    页面信息: { 
      width: page.width, 
      height: page.height,
      actualWidth: page.actualWidth,
      actualHeight: page.actualHeight
    },
    缩放比例: scale.toFixed(3),
    计算结果: { x, y, width, height },
    是否高亮: isHighlighted
  }
  
  // 【修复】确保bbox有效尺寸
  if (width <= 0 || height <= 0) {
    console.warn('❌ Bbox尺寸无效:', debugInfo)
    return
  }
  
  // 【修复】确保bbox在Canvas范围内
  if (x >= page.width || y >= page.height) {
    console.warn('❌ Bbox完全超出Canvas范围:', debugInfo)
    return
  }
  
  // 【修复】保存和恢复Canvas状态，避免样式污染
  ctx.save()
  
  try {
    if (isHighlighted) {
      // 高亮状态：使用黄色边框突出显示
      ctx.strokeStyle = '#E6A23C' // 橙黄色边框
      ctx.lineWidth = 2 // 【修复】增加高亮时的边框宽度
      ctx.setLineDash([]) // 【修复】确保是实线
      
      // 黄色填充，透明度0.15，确保文字可读
      ctx.fillStyle = 'rgba(230, 162, 60, 0.15)'
      ctx.fillRect(x, y, width, height)
      
      // 绘制边框
      ctx.strokeRect(x, y, width, height)
      
      console.log(`✅ 绘制高亮bbox成功:`, debugInfo)
    } else {
      // 普通状态：绿色边框
      ctx.strokeStyle = 'rgba(103, 194, 58, 0.8)' // 【修复】增加透明度
      ctx.lineWidth = 1
      ctx.setLineDash([]) // 【修复】确保是实线
      
      // 绿色填充，透明度0.08，更淡一些
      ctx.fillStyle = 'rgba(103, 194, 58, 0.08)'
      ctx.fillRect(x, y, width, height)
      
      // 绘制边框
      ctx.strokeRect(x, y, width, height)
    }
  } catch (error) {
    console.error('❌ 绘制bbox失败:', error, debugInfo)
  } finally {
    ctx.restore() // 【修复】恢复Canvas状态
  }
}

// Canvas交互事件
const onCanvasClick = (event: MouseEvent, pageIndex: number) => {
  const canvas = canvasRefs.value.get(pageIndex)
  if (!canvas) return
  
  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 查找点击的bbox
  const clickedBbox = findBboxAtPosition(x, y, pageIndex)
  if (clickedBbox) {
    emit('bboxClick', clickedBbox)
  }
}

// 查找指定位置的bbox（参考合同比对的精确算法）
const findBboxAtPosition = (x: number, y: number, pageIndex: number): any => {
  const page = pageLayout.value[pageIndex]
  if (!page) return null
  
  // 使用统一的缩放比例
  const scale = page.width / page.actualWidth
  
  // 转换为原始图像坐标
  const imageX = x / scale
  const imageY = y / scale
  
  const pageNum = pageIndex + 1
  
  // 【修复】判断是否为OCR模式 - 处理bboxMappings可能是undefined的情况
  const bboxMappingsCount = props.bboxMappings?.length || 0
  const isOcrMode = props.charBoxes.length > 0 && bboxMappingsCount === 0
  
  if (isOcrMode) {
    // OCR模式：查找charBoxes（实际是textBoxes）
    const pageTextBoxes = props.charBoxes.filter((tb: any) => tb.page === pageNum)
    
    // 从后往前查找，优先选择上层的bbox
    for (let i = pageTextBoxes.length - 1; i >= 0; i--) {
      const textBox = pageTextBoxes[i]
      if (textBox.bbox && textBox.bbox.length >= 4) {
        const [x1, y1, x2, y2] = textBox.bbox
        
        // 精确的点击检测
        if (imageX >= x1 && imageX <= x2 && imageY >= y1 && imageY <= y2) {
          return {
            ...textBox,
            text: textBox.text,
            category: textBox.category
          }
        }
      }
    }
  } else if (bboxMappingsCount > 0) {
    // 【修复】智能提取模式：查找bboxMappings，添加安全检查
    const pageBboxes = props.bboxMappings!.filter(mapping => 
      mapping.pages && mapping.pages.includes(pageNum)
    )
    
    // 从后往前查找，优先选择上层的bbox
    for (let i = pageBboxes.length - 1; i >= 0; i--) {
      const mapping = pageBboxes[i]
      if (mapping.bboxes) {
        for (let j = mapping.bboxes.length - 1; j >= 0; j--) {
          const bboxInfo = mapping.bboxes[j]
          if (bboxInfo.page === pageNum && bboxInfo.bbox && bboxInfo.bbox.length >= 4) {
            const [x1, y1, x2, y2] = bboxInfo.bbox
            
            // 精确的点击检测
            if (imageX >= x1 && imageX <= x2 && imageY >= y1 && imageY <= y2) {
              return {
                ...bboxInfo,
                mappingId: mapping.interval?.id,
                text: mapping.text,
                fieldName: mapping.fieldName || mapping.field || mapping.name,
                startPos: mapping.interval?.startPos,
                endPos: mapping.interval?.endPos
              }
            }
          }
        }
      }
    }
  }
  
  return null
}

// 重试加载
const retryLoad = () => {
  initializePages()
}

// 导航到提取内容
const navigateToExtraction = (extraction: any) => {
  if (!extraction.charInterval) {
    return
  }
  
  // 从bboxMappings中查找对应的mapping
  const mapping = findMappingForExtraction(extraction)
  
  if (mapping && mapping.bboxes && mapping.bboxes.length > 0) {
    const firstBbox = mapping.bboxes[0]
    
    if (firstBbox.page) {
      currentPage.value = firstBbox.page
      
      // 滚动到bbox的具体位置（而不是页面顶部）
      scrollToBbox(firstBbox)
      
      // 高亮选中的提取内容
      highlightExtractionBboxes([extraction])
    }
  }
}

// 高亮提取内容的bboxes
const highlightExtractionBboxes = (extractions: any[]) => {
  // 清空之前的高亮
  highlightedBboxes.value = []
  
  // 添加新的高亮
  extractions.forEach(extraction => {
    if (extraction.charInterval) {
      // 从bboxMappings中查找对应的mapping
      const mapping = findMappingForExtraction(extraction)
      if (mapping && mapping.bboxes) {
        highlightedBboxes.value.push(...mapping.bboxes)
      }
    }
  })
  
  // 重新绘制
  renderVisiblePages()
}

// 根据extraction查找对应的bboxMapping
const findMappingForExtraction = (extraction: any) => {
  if (!extraction.charInterval) return null
  
  // 【修复】如果没有bboxMappings，直接返回null
  if (!props.bboxMappings || props.bboxMappings.length === 0) return null
  
  const interval = extraction.charInterval
  const start = interval.startPos || interval.start || 0
  const end = interval.endPos || interval.end || 0
  
  // 在bboxMappings中查找匹配的mapping
  return props.bboxMappings.find(mapping => {
    // 支持两种数据格式
    let mappingStart, mappingEnd
    
    if (mapping.interval) {
      // 格式1: { interval: { startPos, endPos }, bboxes: [...] }
      mappingStart = mapping.interval.startPos || mapping.interval.start || 0
      mappingEnd = mapping.interval.endPos || mapping.interval.end || 0
    } else {
      // 格式2: { startPos, endPos, bboxes: [...] }（智能文档抽取使用的格式）
      mappingStart = mapping.startPos || 0
      mappingEnd = mapping.endPos || 0
    }
    
    return mappingStart === start && mappingEnd === end
  })
}

// 高亮单个bbox（用于文本点击联动）
const highlightBbox = (bboxInfo: any) => {
  if (!bboxInfo || !bboxInfo.bbox) return
  
  // 清空之前的高亮
  highlightedBboxes.value = []
  
  // 添加新的高亮
  highlightedBboxes.value.push(bboxInfo)
  
  // 重新绘制所有可见页面
  renderVisiblePages()
  
  // 滚动到bbox的精确位置（类似合同比对的差异跳转）
  scrollToBbox(bboxInfo)
}

// 暴露方法给父组件
defineExpose({
  navigateToExtraction,
  highlightExtractionBboxes,
  highlightBbox,
  goToPage: (page: number) => {
    if (page >= 1 && page <= props.totalPages) {
      currentPage.value = page
      scrollToPage(page)
    }
  }
})

// 生命周期
onMounted(() => {
  // 初始化Canvas宽度
  updateCanvasWidth()
  
  if (props.taskId && props.totalPages > 0) {
    initializePages()
  }
  
  // 监听容器尺寸变化
  const resizeObserver = new ResizeObserver(() => {
    updateCanvasWidth()
    nextTick(() => {
      updatePageLayout()
      renderVisiblePages()
    })
  })
  
  if (canvasContainer.value) {
    resizeObserver.observe(canvasContainer.value)
  }
  
  // 清理函数
  onUnmounted(() => {
    resizeObserver.disconnect()
  })
})
</script>

<style scoped>
.canvas-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.page-info {
  font-size: 12px;
  color: #666;
}

.canvas-scroll-container {
  flex: 1;
  position: relative;
  overflow-y: auto !important;
  overflow-x: auto !important;
  background: #f5f5f5;
  min-height: 0; /* 确保flex子元素可以滚动 */
  height: 100%; /* 确保容器有高度 */
}

.virtual-content {
  position: relative;
  width: 100%;
  min-width: 650px; /* 确保有足够宽度容纳600px的canvas + padding */
}

.page-canvas {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: block;
}

.page-label {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  z-index: 10;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 1000;
}

.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 1000;
}

.zoom-controls {
  display: flex;
  justify-content: center;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
}
</style>