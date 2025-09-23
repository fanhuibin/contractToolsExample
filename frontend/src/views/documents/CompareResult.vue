<template>
  <div class="compare-fullscreen">
    <div class="compare-toolbar">
      <div class="left">
        <div class="title">合同比对</div>
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
        <div class="sync-scroll-control">
          <span class="sync-scroll-label">同轴滚动</span>
          <el-switch
            v-model="syncScrollEnabled"
            @change="onSyncScrollToggle"
            active-color="#13ce66"
            inactive-color="#ff4949"
            size="small"
          />
        </div>
        <el-radio-group v-model="filterMode" size="small" class="filter-group">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="DELETE">仅删除</el-radio-button>
          <el-radio-button label="INSERT">仅新增</el-radio-button>
        </el-radio-group>
        <el-button 
          size="small" 
          :type="showOutline ? 'primary' : 'default'"
          @click="toggleOutline"
          :class="{ 'outline-active': showOutline }"
        >
          <el-icon><List /></el-icon>
          大纲
        </el-button>
        <el-button size="small" text @click="goBack">返回上传</el-button>
      </div>
    </div>
    <div class="compare-body" v-loading="loading">
      <!-- 调试：显示加载状态与路由参数 -->
      <div v-if="viewerLoading" class="loader-debug">loading=true; routeId={{ route.params.id }}</div>
      <div class="pdf-pane">
        <div class="pdf-wrapper">
          <iframe
            v-if="oldPdf"
            id="oldPdfFrame"
            :src="viewerUrl(oldPdf)"
            @load="onFrameLoad('old', $event)"
          />
          <div class="marker-line" :style="markerStyle"></div>
          <!-- 左侧PDF加载特效 -->
          <ConcentricLoader v-if="viewerLoading" color="#1677ff" :size="52" class="pdf-loader left-loader" />
        </div>
      </div>
      <div class="pdf-pane">
        <div class="pdf-wrapper">
          <iframe
            v-if="newPdf"
            id="newPdfFrame"
            :src="viewerUrl(newPdf)"
            @load="onFrameLoad('new', $event)"
          />
          <div class="marker-line" :style="markerStyle"></div>
          <!-- 右侧PDF加载特效 -->
          <ConcentricLoader v-if="viewerLoading" color="#1677ff" :size="52" class="pdf-loader right-loader" />
        </div>
      </div>
      <div class="result-list">
        <div class="head">比对结果 <span class="em">{{ filteredResults.length }}</span> 处（删 {{ deleteCount }} / 增 {{ insertCount }}）</div>
        <div class="list">
          <!-- 加载状态显示 -->
          <div v-if="viewerLoading" class="list-loading">
            <ConcentricLoader color="#1677ff" :size="52" text="比对中..." class="list-loader" />
            <div class="loading-text-sub">任务处理中，请稍候</div>
          </div>
          <!-- 正常结果列表 -->
          <div v-else>
            <div
              v-for="(r, i) in filteredResults"
              :key="i"
              class="result-item"
              :class="{ active: indexInAll(i) === activeIndex }"
              @click="jumpTo(indexInAll(i))"
            >
              <div class="line">
                <span class="badge" :class="r.diff.operation === 'DELETE' ? 'del' : 'ins'">{{ r.diff.operation === 'DELETE' ? '删' : '增' }}</span>
                <span class="text">{{ r.diff.text }}</span>
              </div>
              <div class="meta">旧文档第 {{ r.oldPosition?.page ?? 0 }} 页 / 新文档第 {{ r.newPosition?.page ?? 0 }} 页</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, List } from '@element-plus/icons-vue'
import ConcentricLoader from '@/components/ai/ConcentricLoader.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const viewerLoading = ref(true)
const loadedStatus: Record<'old' | 'new', boolean> = { old: false, new: false }
const oldPdf = ref('')
const newPdf = ref('')
const results = ref<any[]>([])
const activeIndex = ref(-1)
const filterMode = ref<'ALL' | 'DELETE' | 'INSERT'>('ALL')
const syncScrollEnabled = ref(true) // 默认开启同轴滚动
let isScrollSyncing = false // 防止循环触发滚动
// 仅在“鼠标滚轮”触发的滚动时执行同步；拖动滚动条产生的滚动不触发同步
const wheelActiveSide = ref<null | 'old' | 'new'>(null)
let wheelClearTimer: number | undefined
// 记录每侧上一次滚动位置（用于增量同步）
const lastScrollTop: Record<'old' | 'new', number> = { old: 0, new: 0 }
const showOutline = ref(false) // 大纲显示状态

// 存储原始文件名
const oldFileName = ref('')
const newFileName = ref('')
const filteredResults = computed(() => {
  if (filterMode.value === 'DELETE') return results.value.filter(r => r?.diff?.operation === 'DELETE')
  if (filterMode.value === 'INSERT') return results.value.filter(r => r?.diff?.operation === 'INSERT')
  return results.value
})
const deleteCount = computed(() => results.value.filter(r => r?.diff?.operation === 'DELETE').length)
const insertCount = computed(() => results.value.filter(r => r?.diff?.operation === 'INSERT').length)

const totalCount = computed(() => filteredResults.value.length)
const activeFilteredIndex = computed(() => {
  const current = results.value[activeIndex.value]
  if (!current) return -1
  return filteredResults.value.findIndex(r => r === current)
})
const prevDisabled = computed(() => totalCount.value === 0 || activeFilteredIndex.value <= 0)
const nextDisabled = computed(() => totalCount.value === 0 || activeFilteredIndex.value >= totalCount.value - 1)
const displayActiveNumber = computed(() => (activeFilteredIndex.value >= 0 ? activeFilteredIndex.value + 1 : 0))
const frameWin: Record<'old' | 'new', Window | null> = { old: null, new: null }
// 参考线（仅视觉）：位置占比 + 视觉偏移。
const markerRatio = 0.35
const markerVisualOffsetPx = 200
const markerStyle = computed(() => ({ top: `calc(${markerRatio * 100}% + ${markerVisualOffsetPx}px)` }))
// 对齐修正（仅用于滚动计算，不影响参考线位置）。正数代表多滚动一些，让命中点更靠上；负数反之。
const alignCorrectionPx = 24

// 注意：这里使用 encodeURI 而不是 encodeURIComponent，以避免将 '?' 编码为 %3F 导致 404
// 强制从第一页开始显示，避免 PDF.js 恢复历史滚动位置
const viewerUrl = (fileUrl: string) => `/pdfviewer/web/viewer.html?file=${encodeURI(fileUrl)}#page=1`

const onFrameLoad = (side: 'old' | 'new', ev: Event) => {
  try {
    const frame = ev.target as HTMLIFrameElement
    const w = frame?.contentWindow as any
    const search = frame?.contentWindow?.location?.search || ''
    // 打印 viewer 加载的 file 参数与可用 API
    const params = new URLSearchParams(search)
    // eslint-disable-next-line no-console
    console.log(`[viewer:${side}] loaded`, {
      viewerHref: frame?.contentWindow?.location?.href,
      fileParam: params.get('file'),
      hasPDFApp: !!w?.PDFViewerApplication,
    })
    frameWin[side] = frame?.contentWindow
    
    // 隐藏PDF工具栏按钮
    hidePDFToolbarButtons(side)
    
    // 设置同轴滚动监听（含 wheel 来源标记）
    setupSyncScrollListener(side)
    // 标记该侧已加载
    if (side === 'old') loadedStatus.old = true
    if (side === 'new') loadedStatus.new = true
    if (loadedStatus.old && loadedStatus.new) viewerLoading.value = false
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`[viewer:${side}] onload inspect failed`, e)
  }
}

// 隐藏PDF工具栏按钮并添加文档信息显示
const hidePDFToolbarButtons = (side: 'old' | 'new') => {
  try {
    const w = frameWin[side] as any
    if (!w || !w.document) return
    
    // 等待PDF.js完全加载
    const hideButtonsAndAddInfo = () => {
      const doc = w.document
      
      // 要隐藏的按钮选择器列表
      const buttonsToHide = [
        '#findbar', // 查找栏
        '#previous', // 上一页
        '#next', // 下一页
        '#zoomOut', // 缩小
        '#zoomIn', // 放大
        '#scaleSelectContainer', // 自动缩放
        '#toggleHandTool', // 手型工具
        '#toggleTextSelection', // 文本选择
        '#toggleAnnotationTools', // 注释工具
        '#editorModeButtons', // 编辑模式按钮
        '#print', // 打印
        '#download', // 下载/保存
        '#openFile', // 打开文件
        '#secondaryToolbar', // 二级工具栏
        '#secondaryToolbarButton', // 二级工具栏按钮
        '.findbar', // 查找栏类
        '.toolbarButton', // 所有工具栏按钮
        '.splitToolbarButton', // 分割工具栏按钮
        '.dropdownToolbarButton', // 下拉工具栏按钮
        'button[title*="Find"]', // 查找按钮
        'button[title*="Previous"]', // 上一页按钮
        'button[title*="Next"]', // 下一页按钮
        'button[title*="Zoom"]', // 缩放按钮
        'button[title*="Print"]', // 打印按钮
        'button[title*="Download"]', // 下载按钮
        'button[title*="Tools"]', // 工具按钮
        'button[data-l10n-id="findbar_find_button"]', // 查找按钮
        'button[data-l10n-id="previous_label"]', // 上一页
        'button[data-l10n-id="next_label"]', // 下一页
        'button[data-l10n-id="zoom_out_label"]', // 缩小
        'button[data-l10n-id="zoom_in_label"]', // 放大
        'button[data-l10n-id="print_label"]', // 打印
        'button[data-l10n-id="save_label"]', // 保存
        'button[data-l10n-id="tools_label"]' // 工具
      ]
      
      // 隐藏所有指定的按钮
      buttonsToHide.forEach(selector => {
        try {
          const elements = doc.querySelectorAll(selector)
          elements.forEach((element: HTMLElement) => {
            element.style.display = 'none'
            element.style.visibility = 'hidden'
          })
        } catch (e) {
          // 忽略选择器错误
        }
      })
      
      // 获取工具栏容器
      const toolbarLeft = doc.querySelector('#toolbarViewerLeft')
      const toolbarRight = doc.querySelector('#toolbarViewerRight')
      const toolbarMiddle = doc.querySelector('#toolbarViewerMiddle')
      
      // 清空左右工具栏内容，但保留容器
      if (toolbarLeft) {
        toolbarLeft.innerHTML = ''
        toolbarLeft.style.display = 'flex'
        toolbarLeft.style.alignItems = 'center'
        toolbarLeft.style.padding = '0 8px'
        toolbarLeft.style.minHeight = '32px'
        
        // 添加文档信息显示
        const fileName = getFileNameFromSide(side)
        
        // 获取页数信息的函数
        const updateDocumentInfo = () => {
          try {
            const pdfApp = w.PDFViewerApplication
            if (pdfApp && pdfApp.pdfDocument) {
              const totalPages = pdfApp.pdfDocument.numPages || 0
              const currentPage = pdfApp.page || 1
              
              toolbarLeft.innerHTML = `
                <span style="margin-right: 12px; font-weight: 500; color: #333; font-size: 12px;">${fileName}</span>
                <span style="color: #666; font-size: 11px;">${currentPage} / ${totalPages}</span>
              `
            } else {
              toolbarLeft.innerHTML = `
                <span style="margin-right: 12px; font-weight: 500; color: #333; font-size: 12px;">${fileName}</span>
                <span style="color: #666; font-size: 11px;">加载中...</span>
              `
            }
          } catch (e) {
            console.warn('获取文档信息失败', e)
            toolbarLeft.innerHTML = `
              <span style="font-weight: 500; color: #333; font-size: 12px;">${fileName}</span>
            `
          }
        }
        
        // 初始化文档信息
        updateDocumentInfo()
        
        // 监听页面变化更新页码
        if (w.PDFViewerApplication && w.PDFViewerApplication.eventBus) {
          w.PDFViewerApplication.eventBus.on('pagechanging', updateDocumentInfo)
          w.PDFViewerApplication.eventBus.on('documentloaded', updateDocumentInfo)
        }
      }
      
      if (toolbarRight) {
        toolbarRight.innerHTML = ''
        toolbarRight.style.display = 'flex'
        toolbarRight.style.alignItems = 'center'
        toolbarRight.style.padding = '0 8px'
        toolbarRight.style.minHeight = '32px'
      }
      
      console.log(`[PDF:${side}] 工具栏按钮已隐藏，文档信息已添加`)
    }
    
    // 立即尝试隐藏
    hideButtonsAndAddInfo()
    
    // 延迟再次尝试，确保PDF完全加载后也能隐藏
    setTimeout(hideButtonsAndAddInfo, 500)
    setTimeout(hideButtonsAndAddInfo, 1000)
    setTimeout(hideButtonsAndAddInfo, 2000)
    
    // 监听PDF应用加载完成事件
    if (w.PDFViewerApplication) {
      w.PDFViewerApplication.eventBus?.on('documentloaded', hideButtonsAndAddInfo)
    }
    
  } catch (e) {
    console.warn(`[PDF:${side}] 隐藏工具栏按钮失败`, e)
  }
}

// 根据side获取对应的文件名
const getFileNameFromSide = (side: 'old' | 'new'): string => {
  try {
    // 优先使用路由查询参数中的文件名（从上传页面传递过来的）
    const routeFileName = side === 'old' ? route.query.oldFileName : route.query.newFileName
    if (routeFileName && typeof routeFileName === 'string' && routeFileName.trim()) {
      return routeFileName.trim()
    }
    
    // 其次使用从API获取的文件名
    const savedFileName = side === 'old' ? oldFileName.value : newFileName.value
    if (savedFileName && savedFileName.trim()) {
      return savedFileName.trim()
    }
    
    // 尝试从PDF URL中提取文件名
    const pdfUrl = side === 'old' ? oldPdf.value : newPdf.value
    if (pdfUrl) {
      try {
        // 解码URL并提取文件名
        const decodedUrl = decodeURIComponent(pdfUrl)
        const fileName = decodedUrl.split('/').pop() || decodedUrl
        
        // 移除可能的查询参数
        const cleanFileName = fileName.split('?')[0]
        
        // 如果文件名有意义（不是UUID或纯数字），则使用它
        if (cleanFileName && cleanFileName.length > 0 && !cleanFileName.match(/^[0-9a-f-]{32,}$/i)) {
          return cleanFileName
        }
      } catch (e) {
        console.warn('解析PDF URL失败', e)
      }
    }
    
    // 最后的备用方案
    return side === 'old' ? '原文档' : '新文档'
  } catch (e) {
    console.warn('获取文件名失败', e)
    return side === 'old' ? '原文档' : '新文档'
  }
}

// 设置同轴滚动监听器
const setupSyncScrollListener = (side: 'old' | 'new') => {
  try {
    const w = frameWin[side] as any
    if (!w || !w.document) return
    
    const viewerContainer = w.document.getElementById('viewerContainer')
    if (!viewerContainer) return

    // 防重复绑定
    if ((viewerContainer as any)._syncAttached) return
    ;(viewerContainer as any)._syncAttached = true
    
    // 鼠标滚轮事件：仅当最近一次滚动来源为该侧的 wheel 时，才进行同步
    const handleWheel = () => {
      wheelActiveSide.value = side
      // 记录 wheel 开始时的起点，确保“从当前各自位置继续”
      try {
        lastScrollTop[side] = (viewerContainer as HTMLElement).scrollTop
      } catch {}
      if (wheelClearTimer) window.clearTimeout(wheelClearTimer)
      wheelClearTimer = window.setTimeout(() => {
        wheelActiveSide.value = null
      }, 150)
    }

    // 添加滚动监听器
    const handleScroll = () => {
      if (!syncScrollEnabled.value || isScrollSyncing) return
      // 仅在由该侧“鼠标滚轮”引发的滚动时执行同步；
      // 拖动滚动条/程序性滚动将不会触发同步，从而实现“松开后按当前位置继续”
      if (wheelActiveSide.value !== side) return
      
      const otherSide = side === 'old' ? 'new' : 'old'
      try {
        const fromWin = frameWin[side] as any
        const toWin = frameWin[otherSide] as any
        if (!fromWin || !toWin) return
        const fromContainer = fromWin.document.getElementById('viewerContainer') as HTMLElement | null
        const toContainer = toWin.document.getElementById('viewerContainer') as HTMLElement | null
        if (!fromContainer || !toContainer) return

        const currentTop = fromContainer.scrollTop
        const delta = currentTop - (lastScrollTop[side] ?? 0)
        // 无有效位移则不同步
        if (Math.abs(delta) < 0.5) {
          lastScrollTop[side] = currentTop
          return
        }
        lastScrollTop[side] = currentTop

        // 依据内容高度比例换算对侧位移，保证不同高度时滚动感一致
        const fromRange = Math.max(1, fromContainer.scrollHeight - fromContainer.clientHeight)
        const toRange = Math.max(0, toContainer.scrollHeight - toContainer.clientHeight)
        const factor = toRange / fromRange

        isScrollSyncing = true
        toContainer.scrollTop = Math.max(0, Math.min(toRange, toContainer.scrollTop + delta * factor))
        // 快速释放同步锁，避免卡顿
        setTimeout(() => { isScrollSyncing = false }, 0)
      } catch (e) {
        console.warn('[syncScroll] delta sync failed', e)
        isScrollSyncing = false
      }
    }
    
    viewerContainer.addEventListener('wheel', handleWheel, { passive: true })
    viewerContainer.addEventListener('scroll', handleScroll, { passive: true })
  } catch (e) {
    console.warn(`[syncScroll] setup failed for ${side}`, e)
  }
}

// 同步滚动到另一侧
const syncScrollToOther = (fromSide: 'old' | 'new', toSide: 'old' | 'new') => {
  try {
    const fromWin = frameWin[fromSide] as any
    const toWin = frameWin[toSide] as any
    
    if (!fromWin || !toWin) return
    
    const fromContainer = fromWin.document.getElementById('viewerContainer')
    const toContainer = toWin.document.getElementById('viewerContainer')
    
    if (!fromContainer || !toContainer) return
    
    // 计算滚动比例
    const scrollRatio = fromContainer.scrollTop / 
      Math.max(1, fromContainer.scrollHeight - fromContainer.clientHeight)
    
    // 计算目标滚动位置
    const targetScrollTop = scrollRatio * 
      Math.max(0, toContainer.scrollHeight - toContainer.clientHeight)
    
    // 设置同步标志，防止循环触发
    isScrollSyncing = true
    
    // 执行同步滚动
    toContainer.scrollTo({
      top: targetScrollTop,
      behavior: 'auto' // 使用 auto 而不是 smooth，避免延迟
    })
    
    // 重置同步标志
    setTimeout(() => {
      isScrollSyncing = false
    }, 50)
    
  } catch (e) {
    console.warn('[syncScroll] sync failed', e)
    isScrollSyncing = false
  }
}

// 同轴滚动开关切换处理
const onSyncScrollToggle = (enabled: boolean) => {
  console.log(`[syncScroll] ${enabled ? '开启' : '关闭'}同轴滚动`)
  
  if (enabled) {
    // 重新设置监听器
    setupSyncScrollListener('old')
    setupSyncScrollListener('new')
  }
  // 关闭时不需要特殊处理，监听器会检查 syncScrollEnabled 状态
}

const fetchResult = async (id: string) => {
  if (!id) return
  loading.value = true
  try {
    const resp = await fetch(`/api/compare/result/${encodeURIComponent(id)}`)
    const data = await resp.json()
    if (data?.code === 200) {
      oldPdf.value = data.data.oldPdf
      newPdf.value = data.data.newPdf
      results.value = data.data.results || []
      activeIndex.value = results.value.length > 0 ? 0 : -1
      
      // 保存文件名信息
      oldFileName.value = data.data.oldFileName || ''
      newFileName.value = data.data.newFileName || ''
      
      sessionStorage.setItem('lastCompareId', id)
    } else {
      ElMessage.error(data?.message || '加载比对结果失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载比对结果失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const id = route.params.id as string
  // eslint-disable-next-line no-console
  console.log('[CompareResult] mounted with id =', id)
  if (id) {
    if (id === 'pending') {
      // 占位状态：仅显示加载动画，等待真正的 id 替换
      viewerLoading.value = true
      // eslint-disable-next-line no-console
      console.log('[CompareResult] pending mode, waiting for real id')
    } else {
      // eslint-disable-next-line no-console
      console.log('[CompareResult] fetch result for id =', id)
      fetchResult(id)
    }
  } else {
    const lastId = sessionStorage.getItem('lastCompareId')
    // eslint-disable-next-line no-console
    console.log('[CompareResult] no id, lastId =', lastId)
    if (lastId) {
      router.replace({ name: 'CompareResult', params: { id: lastId } }).catch(() => {})
    }
  }
})

watch(() => route.params.id, (newId) => {
  // eslint-disable-next-line no-console
  console.log('[CompareResult] route id changed =>', newId)
  if (typeof newId === 'string' && newId) {
    if (newId !== 'pending') fetchResult(newId)
  }
})

// 当筛选变化时，若当前激活项在新集合中找不到，则重置为首项，便于连续“上一处/下一处”操作
watch(filterMode, () => {
  if (totalCount.value === 0) return
  const i = activeFilteredIndex.value
  if (i < 0) {
    const first = filteredResults.value[0]
    const idx = results.value.findIndex(r => r === first)
    if (idx >= 0) activeIndex.value = idx
  }
})

const jumpTo = (i: number) => {
  activeIndex.value = i
  const r = results.value[i]
  if (!r) return
  // 双侧联动：分页 + 垂直对齐到 marker 线
  alignViewer('old', r.oldPosition)
  alignViewer('new', r.newPosition)
}

function alignViewer(side: 'old' | 'new', pos: any) {
  try {
    const w = frameWin[side] as any
    if (!w || !w.PDFViewerApplication || !pos) return
    const app = w.PDFViewerApplication
    const pageNumber = (pos.page || 0) + 1
    // 先跳到对应页
    app.pdfViewer.currentPageNumber = pageNumber
    // 计算 PDF 空间到视口像素
    setTimeout(() => {
      try {
        const pv = app.pdfViewer.getPageView(pageNumber - 1)
        if (!pv || !pv.viewport || !pv.div) return
        const yBL = (pos.pageHeight || 0) > 0 ? (pos.pageHeight - (pos.y || 0)) : 0
        const xBL = pos.x || 0
        const pt = pv.viewport.convertToViewportPoint(xBL, yBL)
        const vc = w.document.getElementById('viewerContainer') as HTMLElement | null
        if (!vc) return
        const targetY = pv.div.offsetTop + (pt?.[1] || 0)
        const markerY = vc.clientHeight * markerRatio + markerVisualOffsetPx
        vc.scrollTop = Math.max(0, targetY - markerY + alignCorrectionPx)
      } catch {}
    }, 50)
  } catch {}
}

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
  router.push({ name: 'CompareUpload' }).catch(() => {})
}

// 切换大纲显示状态
const toggleOutline = () => {
  showOutline.value = !showOutline.value
}

// 将 filtered 索引映射回原始 results 索引
function indexInAll(filteredIdx: number): number {
  const item = filteredResults.value[filteredIdx]
  if (!item) return filteredIdx
  const allIdx = results.value.findIndex(r => r === item)
  return allIdx >= 0 ? allIdx : filteredIdx
}
</script>

<style scoped>
.compare-fullscreen { position: fixed; inset: 0; height: 100vh; width: 100vw; background: #f5f6f8; display: flex; flex-direction: column; overflow: hidden; }
.compare-toolbar { height: 48px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; border-bottom: 1px solid #e6e8eb; background: #fff; }
.compare-toolbar .left { display: flex; align-items: center; gap: 8px; }
.compare-toolbar .title { font-weight: 600; color: #303133; font-size: 14px; }
.compare-toolbar .center { display: flex; align-items: center; gap: 12px; }
.compare-toolbar .center .counter { color: #909399; font-size: 12px; }
.compare-toolbar .right { display: flex; align-items: center; gap: 12px; }
.sync-scroll-control { display: flex; align-items: center; gap: 6px; }
.sync-scroll-label { font-size: 12px; color: #606266; white-space: nowrap; }
.filter-group :deep(.el-radio-button__inner) { padding: 6px 10px; }

/* 隐藏PDF查看器工具栏按钮，但保留工具栏容器 */

/* 隐藏具体的工具按钮，但不隐藏工具栏容器 */
:global(#findbar),
:global(#previous),
:global(#next), 
:global(#zoomOut),
:global(#zoomIn),
:global(#scaleSelectContainer),
:global(#toggleHandTool),
:global(#toggleTextSelection),
:global(#toggleAnnotationTools),
:global(#editorModeButtons),
:global(#print),
:global(#download),
:global(#openFile),
:global(#secondaryToolbar),
:global(#secondaryToolbarButton),
:global(.findbar),
:global(.splitToolbarButton),
:global(.dropdownToolbarButton) {
  display: none !important;
  visibility: hidden !important;
}

/* 不隐藏工具栏容器的内容，因为我们需要显示文档信息 */

/* 隐藏中间工具栏的按钮，但保留页码显示 */
:global(#toolbarViewerMiddle .toolbarButton) {
  display: none !important;
}

/* 确保页码显示可见 */
:global(#pageNumber),
:global(#numPages),
:global(.splitToolbarButtonSeparator) {
  display: inline-block !important;
}

/* 隐藏特定功能按钮 */
:global(button[title*="查找"]),
:global(button[title*="上一页"]),
:global(button[title*="下一页"]),
:global(button[title*="放大"]),
:global(button[title*="缩小"]),
:global(button[title*="自动缩放"]),
:global(button[title*="高亮"]),
:global(button[title*="文本"]),
:global(button[title*="绘图"]),
:global(button[title*="图像"]),
:global(button[title*="打印"]),
:global(button[title*="保存"]),
:global(button[title*="工具"]),
:global(button[title*="Find"]),
:global(button[title*="Previous"]),
:global(button[title*="Next"]),
:global(button[title*="Zoom"]),
:global(button[title*="Print"]),
:global(button[title*="Download"]),
:global(button[title*="Tools"]) {
  display: none !important;
}

/* 确保工具栏容器保持可见，用于显示文档信息 */
:global(#toolbar) {
  display: flex !important;
  visibility: visible !important;
}

:global(#toolbarViewerLeft),
:global(#toolbarViewerRight) {
  display: flex !important;
  visibility: visible !important;
  min-height: 32px;
}
.compare-body { position: relative; flex: 1; min-height: 0; display: grid; grid-template-columns: 1fr 1fr 320px; gap: 12px; padding: 12px; overflow: hidden; }
.pdf-pane { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; display: flex; min-height: 0; }
.pdf-wrapper { position: relative; flex: 1; min-height: 0; }
.pdf-pane iframe { width: 100%; height: 100%; border: none; display: block; }
.marker-line { position: absolute; left: 0; right: 0; height: 0; border-top: 1px dashed #f56c6c; pointer-events: none; }
.result-list { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; display: flex; flex-direction: column; overflow: hidden; }
.result-list .head { padding: 10px; border-bottom: 1px solid #ebeef5; font-weight: 600; }
.result-list .head .em { color: #f56c6c; }
.result-list .list { flex: 1; overflow: auto; padding: 8px; }
.result-item { border: 1px solid #f0f0f0; border-radius: 6px; padding: 8px; margin-bottom: 8px; cursor: pointer; }
.result-item.active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,.15); }
.result-item .line { display: flex; align-items: center; gap: 6px; }
.result-item .badge { display: inline-block; min-width: 20px; text-align: center; padding: 0 6px; height: 20px; line-height: 20px; border-radius: 4px; font-size: 12px; color: #fff; }
.result-item .badge.del { background: #F56C6C; }
.result-item .badge.ins { background: #67C23A; }
.result-item .text { color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.result-item .meta { color: #909399; font-size: 12px; margin-top: 4px; }

.loader-debug { position: absolute; bottom: 8px; left: 12px; color: rgba(0,0,0,0.45); font-size: 12px; pointer-events: none; }

/* PDF加载特效样式 */
.pdf-loader {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  pointer-events: none;
}

.left-loader {
  /* 左侧PDF加载特效 */
}

.right-loader {
  /* 右侧PDF加载特效 */
}

/* 结果列表加载状态样式 */
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
</style>


