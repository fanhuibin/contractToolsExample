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
}

const props = withDefaults(defineProps<Props>(), {
  totalPages: 0,
  charBoxes: () => [],
  extractions: () => [],
  bboxMappings: () => []
})

// Emits
const emit = defineEmits<{
  bboxClick: [bboxInfo: any]
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

// 计算属性
// 总高度计算
const totalHeight = computed(() => {
  if (pageLayout.value.length === 0) return 0
  const lastPage = pageLayout.value[pageLayout.value.length - 1]
  return lastPage.y + lastPage.height
})

// 可见页面计算（智能渲染）
const visiblePages = computed(() => {
  if (pageLayout.value.length === 0) return []
  
  // 页面数少于20页时，直接渲染所有页面
  if (props.totalPages <= MAX_VIRTUAL_PAGES) {
    return pageLayout.value
  }
  
  // 页面数较多时才使用虚拟滚动
  if (!canvasContainer.value) return []
  
  const scrollTop = canvasContainer.value.scrollTop
  const containerHeight = canvasContainer.value.clientHeight
  
  const visibleTop = scrollTop - SCROLL_BUFFER
  const visibleBottom = scrollTop + containerHeight + SCROLL_BUFFER
  
  return pageLayout.value.filter(page => {
    const pageBottom = page.y + page.height
    return pageBottom >= visibleTop && page.y <= visibleBottom
  })
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
  if (!props.taskId || props.totalPages === 0) return
  
  try {
    loading.value = true
    error.value = ''
    
    // 清空之前的数据
    pageImages.value.clear()
    pageLayout.value = []
    
    let currentY = 0
    
    // 页面数少时，一次性加载所有图片
    if (props.totalPages <= MAX_VIRTUAL_PAGES) {
      
      for (let i = 0; i < props.totalPages; i++) {
        const img = await loadPageImage(i + 1)
        if (img) {
          pageImages.value.set(i, img)
          
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
    
    loading.value = false
    
    // 立即触发首次渲染
    nextTick(() => {
      renderVisiblePages()
    })
    
  } catch (err: any) {
    console.error('初始化页面失败:', err)
    error.value = err.message || '初始化失败'
    loading.value = false
  }
}

// 加载单页图像
const loadPageImage = async (pageNum: number): Promise<HTMLImageElement | null> => {
  try {
    const imageUrl = `/api/extract/files/tasks/${props.taskId}/images/page-${pageNum}.png`
    
    const img = new Image()
    img.crossOrigin = 'anonymous'
    
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = () => reject(new Error(`页面${pageNum}图像加载失败`))
      img.src = imageUrl
    })
    
    return img
  } catch (err) {
    console.error(`加载页面${pageNum}失败:`, err)
    return null
  }
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
  // 等待DOM更新完成
  await nextTick()
  
  for (const page of visiblePages.value) {
    let canvas = canvasRefs.value.get(page.index)
    if (!canvas) {
      // 尝试重新查找Canvas元素
      const canvasEl = document.querySelector(`canvas[data-page="${page.index}"]`) as HTMLCanvasElement
      if (canvasEl) {
        canvasRefs.value.set(page.index, canvasEl)
        canvas = canvasEl
      } else {
        continue
      }
    }
    
    // 确保图片已加载
    let img = pageImages.value.get(page.index)
    if (!img) {
      const loadedImg = await loadPageImage(page.index + 1)
      if (loadedImg) {
        img = loadedImg
        pageImages.value.set(page.index, img)
      } else {
        console.error(`页面 ${page.index + 1} 图片加载失败`)
        continue
      }
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

// 滚动事件处理
const onScroll = () => {
  // 更新当前页面
  updateCurrentPage()
}

// 更新当前页面
const updateCurrentPage = () => {
  if (!canvasContainer.value || pageLayout.value.length === 0) return
  
  const scrollTop = canvasContainer.value.scrollTop
  const containerHeight = canvasContainer.value.clientHeight
  const centerY = scrollTop + containerHeight / 2
  
  // 找到中心点所在的页面
  for (let i = 0; i < pageLayout.value.length; i++) {
    const page = pageLayout.value[i]
    if (centerY >= page.y && centerY <= page.y + page.height) {
      currentPage.value = i + 1
      break
    }
  }
}

// 绘制页面的提取内容
const drawPageExtractions = (ctx: CanvasRenderingContext2D, page: any) => {
  // 创建高亮bbox的查找集合（用于快速判断）
  const highlightedSet = new Set(
    highlightedBboxes.value
      .filter(b => b.page === page.index + 1)
      .map(b => `${b.page}-${b.bbox.join('-')}`)
  )
  
  // 遍历所有提取内容，根据是否高亮绘制不同样式
  props.extractions.forEach((extraction) => {
    if (extraction.charInterval) {
      const bboxes = findBboxesForCharInterval(extraction.charInterval)
      bboxes.forEach(bboxInfo => {
        if (bboxInfo.page === page.index + 1) {
          // 检查当前bbox是否在高亮列表中
          const bboxKey = `${bboxInfo.page}-${bboxInfo.bbox.join('-')}`
          const isHighlighted = highlightedSet.has(bboxKey)
          
          // 根据状态绘制一次（避免重复绘制）
          drawExtractionBbox(ctx, bboxInfo, page, isHighlighted)
        }
      })
    }
  })
}

// 绘制提取内容的bbox（参考合同比对实现）
const drawExtractionBbox = (ctx: CanvasRenderingContext2D, bboxInfo: any, page: any, isHighlighted: boolean = false) => {
  if (!bboxInfo.bbox || bboxInfo.bbox.length < 4) {
    console.warn('无效的bbox数据:', bboxInfo)
    return
  }
  
  const [x1, y1, x2, y2] = bboxInfo.bbox
  
  // 计算缩放比例（参考合同比对的精确算法）
  const scale = page.width / page.actualWidth
  
  // 计算在Canvas上的精确位置
  const x = Math.round(x1 * scale)
  const y = Math.round(y1 * scale)
  const width = Math.round((x2 - x1) * scale)
  const height = Math.round((y2 - y1) * scale)
  
  
  // 确保bbox在Canvas范围内
  if (x < 0 || y < 0 || x + width > page.width || y + height > page.height) {
    console.warn('Bbox超出Canvas范围:', { x, y, width, height, canvasSize: { width: page.width, height: page.height } })
    return
  }
  
  if (isHighlighted) {
    // 高亮状态：使用黄色边框突出显示
    ctx.strokeStyle = '#E6A23C' // 橙黄色边框
    ctx.lineWidth = 1
    ctx.strokeRect(x, y, width, height)
    
    // 黄色填充，透明度0.1，确保文字可读
    ctx.fillStyle = 'rgba(230, 162, 60, 0.1)'
    ctx.fillRect(x, y, width, height)
  } else {
    // 普通状态：绿色边框
    ctx.strokeStyle = 'rgba(103, 194, 58, 0.6)'
    ctx.lineWidth = 1
    ctx.strokeRect(x, y, width, height)
    
    // 绿色填充，透明度0.1
    ctx.fillStyle = 'rgba(103, 194, 58, 0.1)'
    ctx.fillRect(x, y, width, height)
  }
}

// 根据字符区间查找对应的bbox
const findBboxesForCharInterval = (charInterval: any) => {
  const bboxes: any[] = []
  
  if (!props.charBoxes.length) {
    return bboxes
  }
  
  const startPos = charInterval.startPos || charInterval.start || 0
  const endPos = charInterval.endPos || charInterval.end || 0
  
  
  for (let i = startPos; i < endPos && i < props.charBoxes.length; i++) {
    const charBox = props.charBoxes[i]
    if (charBox && charBox.bbox) {
      bboxes.push({
        page: charBox.page,
        bbox: charBox.bbox,
        char: charBox.ch
      })
    }
  }
  
  
  return bboxes
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
  
  // 使用统一的缩放比例（参考合同比对）
  const scale = page.width / page.actualWidth
  
  // 转换为原始图像坐标
  const imageX = x / scale
  const imageY = y / scale
  
  // 查找包含该点的bbox
  const pageNum = pageIndex + 1
  const pageBboxes = props.bboxMappings.filter(mapping => 
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
              fieldName: mapping.fieldName || mapping.field || mapping.name
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
  if (!extraction.charInterval) return
  
  const bboxes = findBboxesForCharInterval(extraction.charInterval)
  if (bboxes.length > 0) {
    const firstBbox = bboxes[0]
    if (firstBbox.page) {
      currentPage.value = firstBbox.page
      scrollToPage(firstBbox.page)
      
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
      const bboxes = findBboxesForCharInterval(extraction.charInterval)
      highlightedBboxes.value.push(...bboxes)
    }
  })
  
  // 重新绘制
  renderVisiblePages()
}

// 暴露方法给父组件
defineExpose({
  navigateToExtraction,
  highlightExtractionBboxes,
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
  overflow-y: auto;
  overflow-x: auto;
  background: #f5f5f5;
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