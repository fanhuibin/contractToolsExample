<template>
  <div class="compare-fullscreen">
    <div class="compare-toolbar">
      <div class="left">
        <div class="title">OCR合同比对</div>
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
      <div class="pdf-pane">
        <div class="pdf-wrapper">
          <iframe
            v-if="oldPdf"
            id="oldPdfFrame"
            :src="viewerUrl(oldPdf)"
            @load="onFrameLoad('old', $event)"
          />
          <div class="marker-line" :style="markerStyle"></div>
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
        </div>
      </div>
      <div class="result-list">
        <div class="head">OCR比对结果 <span class="em">{{ filteredResults.length }}</span> 处（删 {{ deleteCount }} / 增 {{ insertCount }}）</div>
        <div class="list">
          <div
            v-for="(r, i) in filteredResults"
            :key="i"
            class="result-item"
            :class="{ active: indexInAll(i) === activeIndex }"
            @click="jumpTo(indexInAll(i))"
          >
            <div class="line">
              <span class="badge" :class="r.operation === 'DELETE' ? 'del' : 'ins'">{{ r.operation === 'DELETE' ? '删' : '增' }}</span>
              <span class="text">{{ r.text }}</span>
            </div>
            <div class="meta">旧文档第 {{ r.oldPosition?.page ?? 0 }} 页 / 新文档第 {{ r.newPosition?.page ?? 0 }} 页</div>
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
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getOCRCompareResult, debugCompareWithExistingOCR, type OCRCompareResult } from '@/api/ocr-compare'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const debugLoading = ref(false)
const oldPdf = ref('')
const newPdf = ref('')
const results = ref<any[]>([])
const activeIndex = ref(-1)
const filterMode = ref<'ALL' | 'DELETE' | 'INSERT'>('ALL')
const taskId = ref('')
const oldOcrTaskId = ref('')
const newOcrTaskId = ref('')

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

const frameWin: Record<'old' | 'new', Window | null> = { old: null, new: null }

// 参考线（仅视觉）：位置占比 + 视觉偏移。
const markerRatio = 0.35
const markerVisualOffsetPx = 200
const markerStyle = computed(() => ({ top: `calc(${markerRatio * 100}% + ${markerVisualOffsetPx}px)` }))

// 对齐修正（仅用于滚动计算，不影响参考线位置）。正数代表多滚动一些，让命中点更靠上；负数反之。
const alignCorrectionPx = 24

// 注意：这里使用 encodeURI 而不是 encodeURIComponent，以避免将 '?' 编码为 %3F 导致 404
const viewerUrl = (fileUrl: string) => `/pdfviewer/web/viewer.html?file=${encodeURI(fileUrl)}`

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
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`[viewer:${side}] onload inspect failed`, e)
  }
}

const fetchResult = async (id: string) => {
  if (!id) return
  loading.value = true
  try {
    const res = await getOCRCompareResult(id)
    if (res?.data) {
      oldPdf.value = res.data.oldPdfUrl
      newPdf.value = res.data.newPdfUrl
      results.value = res.data.differences || []
      activeIndex.value = results.value.length > 0 ? 0 : -1
      sessionStorage.setItem('lastOCRCompareId', id)
      
      // 保存任务ID和OCR任务ID，用于调试
      taskId.value = id
      oldOcrTaskId.value = res.data.oldOcrTaskId || ''
      newOcrTaskId.value = res.data.newOcrTaskId || ''
    } else {
      ElMessage.error('加载OCR比对结果失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载OCR比对结果失败')
  } finally {
    loading.value = false
  }
}

// 开始调试比对
const startDebug = async () => {
  if (!oldOcrTaskId.value || !newOcrTaskId.value) {
    ElMessage.warning('无法获取OCR任务ID，无法进行调试')
    return
  }
  
  debugLoading.value = true
  
  try {
    const res = await debugCompareWithExistingOCR({
      oldOcrTaskId: oldOcrTaskId.value,
      newOcrTaskId: newOcrTaskId.value,
      options: {
        ignoreCase: true,
        ignoreSpaces: false
      }
    })
    
    console.log('调试比对响应:', res)
    
    // 获取任务ID
    const newTaskId = res.data.taskId
    
    if (!newTaskId) {
      throw new Error('任务ID为空')
    }
    
    ElMessage.success('调试比对任务已提交，正在处理中...')
    
    // 跳转到新的比对结果页面
    router.push({ name: 'OCRCompareResult', params: { taskId: newTaskId } }).catch(() => {})
    
  } catch (e: any) {
    console.error('调试比对失败:', e)
    ElMessage.error(e?.message || '调试比对任务提交失败')
  } finally {
    debugLoading.value = false
  }
}

onMounted(() => {
  const id = route.params.taskId as string
  if (id) {
    fetchResult(id)
  } else {
    const lastId = sessionStorage.getItem('lastOCRCompareId')
    if (lastId) {
      router.replace({ name: 'OCRCompareResult', params: { taskId: lastId } }).catch(() => {})
    }
  }
})

watch(() => route.params.taskId, (newId) => {
  if (typeof newId === 'string' && newId) {
    fetchResult(newId)
  }
})

// 当筛选变化时，若当前激活项在新集合中找不到，则重置为首项，便于连续"上一处/下一处"操作
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
  router.push({ name: 'OCRCompare' }).catch(() => {})
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
.compare-toolbar .right { display: flex; align-items: center; gap: 8px; }
.filter-group :deep(.el-radio-button__inner) { padding: 6px 10px; }
.compare-body { flex: 1; min-height: 0; display: grid; grid-template-columns: 1fr 1fr 320px; gap: 12px; padding: 12px; overflow: hidden; }
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
</style>
