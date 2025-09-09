<template>
  <div class="gpu-compare-fullscreen">
    <div class="compare-toolbar">
      <div class="left">
        <div class="title">GPU OCR合同比对</div>
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
          <!-- 暂时禁用修改筛选 -->
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
        <div class="head">GPU OCR比对结果 <span class="em">{{ filteredResults.length }}</span> 处（删 {{ deleteCount }} / 增 {{ insertCount }}）</div>
        <div class="list">
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
                {{ r.operation === 'DELETE' ? '删除' : (r.operation === 'INSERT' ? '新增' : '改') }}
              </span>
            </div>
            <div class="content">
              <div class="text" v-html="r.operation === 'DELETE' ? highlightDiffText(r.allTextA, r.diffRangesA, 'delete') : highlightDiffText(r.allTextB, r.diffRangesB, 'insert')"></div>
              <div class="meta">第 {{ r.operation === 'DELETE' ? (r.pageA || r.page) : (r.pageB || r.page) }} 页</div>
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
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getGPUOCRCompareResult, debugGPUCompareWithExistingOCR, type GPUOCRCompareResult } from '@/api/gpu-ocr-compare'

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
const compareData = ref<any>(null) // 存储完整的比对结果数据

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

// 参考线（仅视觉）：位置占比 + 视觉偏移（提高到更靠上位置）
const markerRatio = 0.25
// 向下微调约半个“12号字”高度，取 ~8px
const markerVisualOffsetPx = 20
const markerStyle = computed(() => ({ top: `calc(${markerRatio * 100}% + ${markerVisualOffsetPx}px)` }))

// 对齐修正（用于滚动计算）。正数代表多滚动一些，让命中点更靠上；负数反之。
const alignCorrectionPx = 24

// 注意：这里使用 encodeURI 而不是 encodeURIComponent，以避免将 '?' 编码为 %3F 导致 404
// 强制从第一页开始显示，避免 PDF.js 恢复历史滚动位置
const viewerUrl = (fileUrl: string) => `/pdfviewer/web/viewer.html?file=${encodeURI(fileUrl)}#page=1`

// 高亮显示差异文本
const highlightDiffText = (allTextList: string[], diffRanges: any[], type: 'insert' | 'delete') => {
  console.log('highlightDiffText 调试:', { allTextList, diffRanges, type })
  
  if (!allTextList || allTextList.length === 0) return '无'
  
  // 合并所有文本
  const fullText = allTextList.join('')
  if (!fullText) return '无'
  
  console.log('合并后完整文本:', fullText)
  
  // 如果没有差异范围，直接返回文本
  if (!diffRanges || diffRanges.length === 0) {
    console.log('没有差异范围，返回完整文本')
    return fullText
  }
  
  console.log('差异范围:', diffRanges)
  
  // 按位置排序差异范围
  const sortedRanges = [...diffRanges].sort((a, b) => a.start - b.start)
  
  let result = ''
  let lastEnd = 0
  
  for (const range of sortedRanges) {
    // 添加差异前的文本
    if (range.start > lastEnd) {
      result += escapeHtml(fullText.substring(lastEnd, range.start))
    }
    
    // 添加高亮的差异文本
    const diffText = fullText.substring(range.start, range.end)
    const highlightClass = type === 'insert' ? 'diff-insert' : 'diff-delete'
    result += `<span class="${highlightClass}">${escapeHtml(diffText)}</span>`
    
    lastEnd = range.end
  }
  
  // 添加最后的文本
  if (lastEnd < fullText.length) {
    result += escapeHtml(fullText.substring(lastEnd))
  }
  
  return result
}

// HTML转义
const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

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
    // 再次兜底：在加载后强制回到第一页顶部，防止历史恢复
    setTimeout(() => {
      try {
        const app = (w && (w as any).PDFViewerApplication) as any
        if (app && app.pdfViewer) {
          app.pdfViewer.currentPageNumber = 1
          const vc = w.document.getElementById('viewerContainer') as HTMLElement | null
          if (vc) vc.scrollTop = 0
        }
      } catch {}
    }, 100)
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`[viewer:${side}] onload inspect failed`, e)
  }
}

const fetchResult = async (id: string) => {
  if (!id) return
  loading.value = true
  try {
    const res = await getGPUOCRCompareResult(id)

    // 检查API响应状态
    if ((res as any)?.code === 202) {
      // 任务尚未完成
      const statusData = (res as any)?.data
      ElMessage.warning(statusData?.message || '比对任务尚未完成')
      return
    } else if ((res as any)?.code !== 200) {
      ElMessage.error((res as any)?.message || '获取比对结果失败')
      return
    }

    // 获取实际数据
    const data = (res as any)?.data
    if (data) {
      // 使用标注版本的PDF文件（如果有的话）
      oldPdf.value = data.annotatedOldPdfUrl || data.oldPdfUrl
      newPdf.value = data.annotatedNewPdfUrl || data.newPdfUrl
      results.value = data.differences || []
      activeIndex.value = results.value.length > 0 ? 0 : -1
      sessionStorage.setItem('lastGPUOCRCompareId', id)

      // 保存任务ID和OCR任务ID，用于调试
      taskId.value = id
      oldOcrTaskId.value = data.oldOcrTaskId || ''
      newOcrTaskId.value = data.newOcrTaskId || ''

      // 保存完整的比对结果数据（包含页面高度信息）
      compareData.value = data

      console.log('加载比对结果成功:', {
        taskId: data.taskId,
        oldFileName: data.oldFileName,
        newFileName: data.newFileName,
        differencesCount: results.value.length,
        oldPdfUrl: oldPdf.value,
        newPdfUrl: newPdf.value
      })
    } else {
      ElMessage.error('加载GPU OCR比对结果失败')
    }
  } catch (e: any) {
    console.error('加载比对结果失败:', e)
    ElMessage.error(e?.message || '加载GPU OCR比对结果失败')
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
    const res = await debugGPUCompareWithExistingOCR({
      oldOcrTaskId: oldOcrTaskId.value,
      newOcrTaskId: newOcrTaskId.value,
      options: {
        ignoreCase: true,
        ignoreSpaces: false
      }
    })

    console.log('调试比对响应:', res)

    // 检查响应状态
    if ((res as any)?.code !== 200) {
      throw new Error((res as any)?.message || '调试比对失败')
    }

    // 获取任务ID
    const newTaskId = (res as any).data?.taskId

    if (!newTaskId) {
      throw new Error('任务ID为空')
    }

    ElMessage.success('调试比对任务已提交，正在处理中...')

    // 跳转到新的比对结果页面
    router.push({ name: 'GPUOCRCompareResult', params: { taskId: newTaskId } }).catch(() => {})

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
    const lastId = sessionStorage.getItem('lastGPUOCRCompareId')
    if (lastId) {
      router.replace({ name: 'GPUOCRCompareResult', params: { taskId: lastId } }).catch(() => {})
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

  console.log(`前端跳转调试 - 差异项 ${i + 1}:`, {
    operation: r.operation,
    page: r.page,
    pageA: r.pageA,
    pageB: r.pageB,
    oldBbox: r.oldBbox,
    newBbox: r.newBbox,
    prevOldBbox: r.prevOldBbox,
    prevNewBbox: r.prevNewBbox,
    text: r.operation === 'DELETE' ? r.oldText : r.newText
  })

  // 根据操作类型和用户要求创建位置信息
  const createPosition = (bbox: number[] | undefined, page: number, description: string) => {
    if (!bbox || bbox.length < 4) {
      console.log(`前端跳转调试 - ${description}位置创建失败: bbox无效`, bbox)
      return null
    }
    const position = {
      page: page, // 保持API返回的1-based页面索引，直接传给alignViewer
      x: bbox[0],      // bbox左上角x坐标
      y: bbox[1],      // bbox左上角y坐标
      width: bbox[2] - bbox[0],   // bbox宽度
      height: bbox[3] - bbox[1],  // bbox高度
      bbox: bbox,      // 完整的bbox数组
      pageHeight: r.pageHeight || (page === (r.pageA || r.page) ? compareData.value?.oldPdfPageHeight : compareData.value?.newPdfPageHeight)
    }
    console.log(`前端跳转调试 - ${description}位置创建成功:`, position)
    return position
  }

  let oldPos = null
  let newPos = null

  // 根据操作类型和用户要求确定跳转位置
  if (r.operation === 'INSERT') {
    // 新增的：A文档按照prevOldBbox的最后一个跳转，B文档按照NewBBox的第一个跳转
    oldPos = createPosition(r.prevOldBbox, r.pageA || r.page, 'INSERT-old(prevOldBbox)')
    newPos = createPosition(r.newBbox, r.pageB || r.page, 'INSERT-new(newBbox)')
  } else if (r.operation === 'DELETE') {
    // 删除的：A文档按照OldBBox跳转，B文档按照prevNewBbox的最后一个跳转
    oldPos = createPosition(r.oldBbox, r.pageA || r.page, 'DELETE-old(oldBbox)')
    newPos = createPosition(r.prevNewBbox, r.pageB || r.page, 'DELETE-new(prevNewBbox)')
  } else {
    // 其他操作：使用默认逻辑
    oldPos = createPosition(r.oldBbox, r.pageA || r.page, `${r.operation}-old(oldBbox)`)
    newPos = createPosition(r.newBbox, r.pageB || r.page, `${r.operation}-new(newBbox)`)
  }

  console.log(`前端跳转调试 - 跳转数据汇总:`, {
    操作类型: r.operation,
    OLD文档: {
      位置: oldPos,
      页面高度: oldPos?.pageHeight,
      PDF坐标: oldPos ? [oldPos.x, oldPos.y] : null
    },
    NEW文档: {
      位置: newPos,
      页面高度: newPos?.pageHeight,  
      PDF坐标: newPos ? [newPos.x, newPos.y] : null
    },
    比对数据: {
      oldPdfPageHeight: compareData.value?.oldPdfPageHeight,
      newPdfPageHeight: compareData.value?.newPdfPageHeight
    }
  })

  // 双侧联动：分页 + 坐标定位
  alignViewer('old', oldPos)
  alignViewer('new', newPos)
}

function alignViewer(side: 'old' | 'new', pos: any) {
  try {
    console.log(`前端坐标调试 - ${side}文档alignViewer调用:`, {
      pos: pos,
      inputCoords: pos ? [pos.x, pos.y] : null,
      page: pos?.page,
      pageHeight: pos?.pageHeight
    })

    const w = frameWin[side] as any
    if (!w || !w.PDFViewerApplication || !pos) {
      console.warn(`前端坐标调试 - ${side}文档alignViewer条件检查失败:`, {
        hasWindow: !!w,
        hasPDFApp: !!(w && w.PDFViewerApplication),
        hasPos: !!pos
      })
      return
    }

    const app = w.PDFViewerApplication
    const pageNumber = pos.page || 1

    console.log(`前端坐标调试 - ${side}文档跳转到第${pageNumber}页`)

    // 先跳到对应页
    app.pdfViewer.currentPageNumber = pageNumber

    // 计算 PDF 空间到视口像素 - 参考OCRCompareResult.vue的简洁实现
    setTimeout(() => { 
      try {
        const pv = app.pdfViewer.getPageView(pageNumber - 1)
        if (!pv || !pv.viewport || !pv.div) {
          console.warn(`前端坐标调试 - ${side}文档第${pageNumber}页视图未准备好`)
          return
        }

        // 垂直居中：以文字块垂直中心为对齐点（pos.y 为顶边）
        const yTop = (pos.y || 0) + ((pos.height || 0) / 2)
        // 左上角→左下角坐标系转换
        const yBL = (pos.pageHeight || 0) > 0 ? (pos.pageHeight - yTop) : 0
        const xBL = pos.x || 0

        console.log(`前端坐标调试 - ${side}文档坐标转换:`, {
          输入坐标: [pos.x, pos.y], 文字高度: pos.height,
          顶边到中心yTop: yTop,
          页面高度: pos.pageHeight,
          左下角坐标: [xBL, yBL],
          转换公式: `yBL = ${pos.pageHeight} - (${pos.y} + ${pos.height ? (pos.height/2) : 0}) = ${yBL}`
        })

        const pt = pv.viewport.convertToViewportPoint(xBL, yBL)
        const vc = w.document.getElementById('viewerContainer') as HTMLElement | null

        if (!vc) {
          console.warn(`前端坐标调试 - ${side}文档viewerContainer未找到`)
          return
        }

        const targetY = pv.div.offsetTop + (pt?.[1] || 0)
        const markerY = vc.clientHeight * markerRatio + markerVisualOffsetPx
        const newScrollTop = Math.max(0, targetY - markerY + alignCorrectionPx)

        vc.scrollTop = newScrollTop

        console.log(`前端坐标调试 - ${side}文档滚动完成:`, {
          视口坐标: pt,
          目标Y位置: targetY,
          页面偏移: pv.div.offsetTop,
          容器高度: vc.clientHeight,
          滚动修正: alignCorrectionPx,
          最终滚动位置: newScrollTop,
          滚动前位置: vc.scrollTop
        })

      } catch (error) {
        console.error(`前端坐标调试 - ${side}文档定位异常:`, error)
      }
    }, 50)
  } catch (error) {
    console.error(`前端坐标调试 - ${side}文档alignViewer外层异常:`, error)
  }
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
  router.push({ name: 'GPUOCRCompare' }).catch(() => {})
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
.gpu-compare-fullscreen { position: fixed; inset: 0; height: 100vh; width: 100vw; background: #f5f6f8; display: flex; flex-direction: column; overflow: hidden; }
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
.result-list { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; display: flex; flex-direction: column; overflow: hidden; }
.result-list .head { padding: 12px; border-bottom: 1px solid #ebeef5; font-weight: 600; display: flex; align-items: center; justify-content: space-between; }
.result-list .head .em { color: #f56c6c; }
.result-list .list { flex: 1; overflow: auto; padding: 10px; }
.result-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; margin-bottom: 10px; cursor: pointer; background: #fff; transition: box-shadow .2s ease, border-color .2s ease; }
.result-item:hover { box-shadow: 0 4px 16px rgba(0,0,0,.06); border-color: #dcdfe6; }
.result-item.active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,.15); }
.result-item .headline { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.result-item .index { width: 24px; height: 24px; border-radius: 50%; background: #f2f3f5; color: #606266; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.result-item .badge { display: inline-block; min-width: 22px; text-align: center; padding: 0 6px; height: 22px; line-height: 22px; border-radius: 6px; font-size: 12px; color: #fff; }
.result-item .badge.del { background: #F56C6C; }
.result-item .badge.ins { background: #67C23A; }
.result-item .badge.mod { background: #E6A23C; }

/* 差异文本高亮样式（scoped + v-html 需用 :deep） */
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
.result-item .content { display: flex; flex-direction: column; gap: 6px; }
.result-item .text { 
  color: #303133; 
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  max-height: calc(1.4em * 4);
}
.result-item .meta { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
