<template>
  <div class="text-viewer">
    <!-- 工具栏 -->
    <div class="toolbar">
      <a-input-search
        v-model:value="searchText"
        placeholder="搜索文本..."
        size="small"
        style="width: 200px;"
        @search="onSearch"
        @change="onSearchChange"
      />
      
      <div class="search-results" v-if="searchResults.length > 0">
        <span class="search-count">
          {{ currentSearchIndex + 1 }} / {{ searchResults.length }}
        </span>
        
        <a-button-group size="small">
          <a-button 
            @click="previousSearchResult" 
            :disabled="searchResults.length === 0"
          >
            <up-outlined />
          </a-button>
          
          <a-button 
            @click="nextSearchResult" 
            :disabled="searchResults.length === 0"
          >
            <down-outlined />
          </a-button>
        </a-button-group>
      </div>
      
      <div class="view-options">
        <a-checkbox v-model:checked="showLineNumbers">显示行号</a-checkbox>
        <a-checkbox v-model:checked="wordWrap" :disabled="true" style="margin-left: 8px;">自动换行</a-checkbox>
      </div>
    </div>

    <!-- 文本内容 -->
    <div 
      class="text-content" 
      ref="textContainer"
      :class="{ 'word-wrap': wordWrap, 'show-line-numbers': showLineNumbers }"
    >
      <div 
        class="text-line" 
        v-for="(line, index) in displayLines" 
        :key="index"
        :data-line-number="index + 1"
      >
        <span 
          class="line-number" 
          v-if="showLineNumbers"
        >
          {{ index + 1 }}
        </span>
        
        <span 
          class="line-content"
          v-html="line.html"
          @click="onLineClick($event, index)"
        ></span>
      </div>
    </div>

    <!-- 高亮信息提示 -->
    <div 
      class="highlight-tooltip" 
      v-if="hoveredHighlight"
      :style="tooltipStyle"
    >
      <div class="tooltip-content">
        <div class="tooltip-title">{{ hoveredHighlight.extractionName }}</div>
        <div class="tooltip-text">{{ hoveredHighlight.text }}</div>
        <div class="tooltip-meta">
          字符范围: {{ hoveredHighlight.start }} - {{ hoveredHighlight.end }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { UpOutlined, DownOutlined } from '@ant-design/icons-vue'

// Props
interface Props {
  content: string
  extractions?: any[]
  bboxMappings: any[]
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  extractions: () => [],
  bboxMappings: () => []
})

// Emits
const emit = defineEmits<{
  textClick: [textInfo: any]
}>()

// 响应式数据
const textContainer = ref<HTMLDivElement>()
const searchText = ref<string>('')
const showLineNumbers = ref<boolean>(true)
const wordWrap = ref<boolean>(true)

// 搜索相关
const searchResults = ref<Array<{ start: number; end: number; line: number }>>([])
const currentSearchIndex = ref<number>(-1)

// 高亮相关
const highlightedRanges = ref<Array<{ 
  start: number; 
  end: number; 
  className: string; 
  extractionId?: string;
  extractionIndex?: number;
  fieldName?: string;
  value?: any;
  originalClassName?: string;
  isSearchOnly?: boolean;
}>>([])
const hoveredHighlight = ref<any>(null)
const tooltipPosition = reactive({ x: 0, y: 0 })

// 计算属性
const displayLines = computed(() => {
  if (!props.content) return []
  
  const lines = props.content.split('\n')
  return lines.map((line, lineIndex) => {
    const lineStartPos = lines.slice(0, lineIndex).reduce((sum, l) => sum + l.length + 1, 0)
    const lineEndPos = lineStartPos + line.length
    
    // 生成带高亮的HTML
    const html = generateHighlightedHtml(line, lineStartPos, lineEndPos)
    
    return {
      text: line,
      html: html,
      startPos: lineStartPos,
      endPos: lineEndPos
    }
  })
})

const tooltipStyle = computed(() => ({
  left: tooltipPosition.x + 'px',
  top: tooltipPosition.y + 'px',
  display: hoveredHighlight.value ? 'block' : 'none'
}))

// 监听属性变化
watch(() => props.bboxMappings, () => {
  updateBboxHighlights()
}, { deep: true })

watch(searchText, () => {
  if (searchText.value) {
    performSearch()
  } else {
    clearSearch()
  }
})

// 生成高亮HTML
const generateHighlightedHtml = (text: string, lineStartPos: number, lineEndPos: number): string => {
  if (!text) return ''
  
  // 收集该行的所有高亮区间
  const lineHighlights: Array<{ start: number; end: number; className: string; extractionId?: string }> = []
  
  // 添加提取结果高亮
  highlightedRanges.value.forEach(range => {
    const rangeStart = Math.max(range.start, lineStartPos)
    const rangeEnd = Math.min(range.end, lineEndPos)
    
    if (rangeStart < rangeEnd) {
      lineHighlights.push({
        start: rangeStart - lineStartPos,
        end: rangeEnd - lineStartPos,
        className: range.className,
        extractionId: range.extractionId
      })
    }
  })
  
  // 搜索高亮现在统一在 highlightedRanges 中管理，不需要单独处理
  
  // 按开始位置排序
  lineHighlights.sort((a, b) => a.start - b.start)
  
  // 生成HTML
  if (lineHighlights.length === 0) {
    return escapeHtml(text)
  }
  
  let html = ''
  let lastEnd = 0
  
  lineHighlights.forEach(highlight => {
    // 添加高亮前的普通文本
    if (highlight.start > lastEnd) {
      html += escapeHtml(text.substring(lastEnd, highlight.start))
    }
    
    // 添加高亮文本
    const highlightText = text.substring(highlight.start, highlight.end)
    const dataAttrs = highlight.extractionId ? ` data-extraction-id="${highlight.extractionId}"` : ''
    html += `<span class="${highlight.className}"${dataAttrs}>${escapeHtml(highlightText)}</span>`
    
    lastEnd = Math.max(lastEnd, highlight.end)
  })
  
  // 添加剩余的普通文本
  if (lastEnd < text.length) {
    html += escapeHtml(text.substring(lastEnd))
  }
  
  return html
}

// HTML转义
const escapeHtml = (text: string): string => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 更新bbox映射高亮（保留其他高亮）
const updateBboxHighlights = () => {
  // 只清除bbox映射的高亮，保留其他类型的高亮
  highlightedRanges.value = highlightedRanges.value.filter(
    range => range.className !== 'bbox-highlight'
  )
  
  props.bboxMappings.forEach(mapping => {
    if (mapping.interval && mapping.interval.start !== undefined && mapping.interval.end !== undefined) {
      highlightedRanges.value.push({
        start: mapping.interval.start,
        end: mapping.interval.end,
        className: 'bbox-highlight', // 使用不同的类名区分
        extractionId: mapping.extractionId || mapping.interval.id
      })
    }
  })
}

// 搜索功能
const performSearch = () => {
  // 清除之前的搜索高亮
  clearSearchHighlights()
  
  searchResults.value = []
  currentSearchIndex.value = -1
  
  if (!searchText.value || !props.content) return
  
  const searchRegex = new RegExp(escapeRegExp(searchText.value), 'gi')
  let match
  
  while ((match = searchRegex.exec(props.content)) !== null) {
    const line = props.content.substring(0, match.index).split('\n').length - 1
    searchResults.value.push({
      start: match.index,
      end: match.index + match[0].length,
      line: line
    })
  }
  
  // 应用搜索高亮
  applySearchHighlights()
  
  if (searchResults.value.length > 0) {
    currentSearchIndex.value = 0
    scrollToSearchResult(0)
  }
}

// 清除搜索高亮
const clearSearchHighlights = () => {
  // 恢复所有搜索高亮为原始状态
  highlightedRanges.value = highlightedRanges.value.map(range => {
    if (range.originalClassName) {
      // 恢复被搜索覆盖的原始高亮
      return { ...range, className: range.originalClassName, originalClassName: undefined }
    }
    return range
  })
  
  // 移除纯搜索高亮
  highlightedRanges.value = highlightedRanges.value.filter(
    range => !range.className.startsWith('search-highlight')
  )
}

// 应用搜索高亮
const applySearchHighlights = () => {
  searchResults.value.forEach((result, index) => {
    // 检查是否与现有高亮重叠
    let foundOverlap = false
    
    highlightedRanges.value = highlightedRanges.value.map(range => {
      if (range.start < result.end && range.end > result.start) {
        // 有重叠，保存原始类名并应用搜索高亮
        foundOverlap = true
        const searchClass = index === currentSearchIndex.value ? 'search-highlight-current' : 'search-highlight'
        return {
          ...range,
          originalClassName: range.originalClassName || range.className,
          className: searchClass
        }
      }
      return range
    })
    
    if (!foundOverlap) {
      // 没有重叠，添加新的搜索高亮
      const searchClass = index === currentSearchIndex.value ? 'search-highlight-current' : 'search-highlight'
      highlightedRanges.value.push({
        start: result.start,
        end: result.end,
        className: searchClass,
        isSearchOnly: true
      })
    }
  })
}

const clearSearch = () => {
  clearSearchHighlights()
  searchResults.value = []
  currentSearchIndex.value = -1
}

const onSearch = () => {
  performSearch()
}

const onSearchChange = () => {
  if (!searchText.value) {
    clearSearch()
  }
}

const nextSearchResult = () => {
  if (searchResults.value.length > 0) {
    currentSearchIndex.value = (currentSearchIndex.value + 1) % searchResults.value.length
    updateSearchHighlights()
    scrollToSearchResult(currentSearchIndex.value)
  }
}

const previousSearchResult = () => {
  if (searchResults.value.length > 0) {
    currentSearchIndex.value = currentSearchIndex.value <= 0 
      ? searchResults.value.length - 1 
      : currentSearchIndex.value - 1
    updateSearchHighlights()
    scrollToSearchResult(currentSearchIndex.value)
  }
}

// 更新搜索高亮的当前状态
const updateSearchHighlights = () => {
  highlightedRanges.value = highlightedRanges.value.map(range => {
    if (range.className.startsWith('search-highlight')) {
      // 找到对应的搜索结果索引
      const resultIndex = searchResults.value.findIndex(result => 
        result.start === range.start && result.end === range.end
      )
      if (resultIndex !== -1) {
        const newClassName = resultIndex === currentSearchIndex.value ? 'search-highlight-current' : 'search-highlight'
        return { ...range, className: newClassName }
      }
    }
    return range
  })
}

const scrollToSearchResult = (index: number) => {
  if (index < 0 || index >= searchResults.value.length) return
  
  const result = searchResults.value[index]
  // TODO: 实现滚动到指定位置
  console.log('滚动到搜索结果:', result)
}

// 正则表达式转义
const escapeRegExp = (string: string): string => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// 交互事件
const onLineClick = (event: MouseEvent, lineIndex: number) => {
  const target = event.target as HTMLElement
  const extractionId = target.getAttribute('data-extraction-id')
  
  if (extractionId) {
    const line = displayLines.value[lineIndex]
    const clickInfo = {
      extractionId: extractionId,
      lineIndex: lineIndex,
      startPos: line.startPos,
      endPos: line.endPos,
      text: target.textContent
    }
    
    emit('textClick', clickInfo)
  }
}

// 高亮指定的文本区间
const highlightText = (charIntervals: any[]) => {
  // 首先恢复所有提取高亮为默认状态
  highlightedRanges.value = highlightedRanges.value.map(range => {
    if (range.className === 'selected-highlight' && range.extractionIndex !== undefined) {
      // 将之前选中的提取高亮恢复为默认状态
      return { ...range, className: 'extraction-highlight' }
    }
    return range
  })
  
  // 如果有新的选中区间，将对应的提取高亮改为选中状态
  if (charIntervals && charIntervals.length > 0) {
    charIntervals.forEach(interval => {
      // 找到对应的提取高亮并改为选中状态
      highlightedRanges.value = highlightedRanges.value.map(range => {
        if (range.className === 'extraction-highlight' && 
            range.start === interval.start && range.end === interval.end) {
          // 改变现有提取高亮的颜色为选中状态
          console.log(`找到匹配的高亮区间: ${range.start}-${range.end}, 改为选中状态`)
          return { ...range, className: 'selected-highlight' }
        }
        return range
      })
    })
    
    console.log(`选中高亮: ${charIntervals.length} 个区间`)
    console.log('当前所有高亮:', highlightedRanges.value.map(r => `${r.start}-${r.end}:${r.className}`))
    
    // 滚动到第一个高亮区域
    scrollToPosition(charIntervals[0].start)
  } else {
    console.log('清除选中状态，所有提取内容恢复默认高亮')
  }
}

// 添加默认高亮（显示所有提取结果）
const addDefaultHighlights = () => {
  if (!props.extractions || props.extractions.length === 0) {
    console.log('没有提取结果数据，跳过默认高亮')
    return
  }
  
  console.log(`准备添加 ${props.extractions.length} 个提取结果的默认高亮`)
  
  // 清除现有的提取高亮，保留搜索高亮和bbox高亮
  highlightedRanges.value = highlightedRanges.value.filter(
    range => range.className.startsWith('search-highlight') ||
             range.className === 'bbox-highlight'
  )
  
  // 添加所有提取结果的默认高亮
  let addedCount = 0
  props.extractions.forEach((extraction, index) => {
    if (extraction.charInterval && extraction.charInterval.startPos !== undefined) {
      const highlight = {
        start: extraction.charInterval.startPos,
        end: extraction.charInterval.endPos,
        className: 'extraction-highlight',
        extractionIndex: index,
        extractionId: extraction.id || `extraction_${index}`,
        fieldName: extraction.field || extraction.fieldName || extraction.name,
        value: extraction.value
      }
      highlightedRanges.value.push(highlight)
      addedCount++
      console.log(`添加高亮 ${index}: ${highlight.start}-${highlight.end} (${highlight.fieldName})`)
    } else {
      console.log(`跳过提取项 ${index}: 没有字符区间信息`, extraction)
    }
  })
  
  console.log(`实际添加了 ${addedCount} 个默认高亮，总高亮数: ${highlightedRanges.value.length}`)
}

// 滚动到指定位置（改进版本）
const scrollToPosition = (position: number) => {
  if (!textContainer.value || !props.content) {
    return
  }
  
  // 计算字符位置对应的行号
  const lines = props.content.substring(0, position).split('\n')
  const targetLine = lines.length - 1
  
  // 查找对应的行元素
  const lineElements = textContainer.value.querySelectorAll('.text-line')
  if (lineElements[targetLine]) {
    // 滚动到目标行，并居中显示
    lineElements[targetLine].scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
    
    // 添加临时高亮效果
    lineElements[targetLine].classList.add('scroll-target')
    setTimeout(() => {
      lineElements[targetLine].classList.remove('scroll-target')
    }, 2000)
  }
}

// 暴露方法给父组件
defineExpose({
  highlightText,
  scrollToPosition
})

// 监听props变化
watch(() => props.content, () => {
  updateBboxHighlights()
  addDefaultHighlights()
})

watch(() => props.extractions, () => {
  addDefaultHighlights()
}, { deep: true })

watch(() => props.bboxMappings, () => {
  updateBboxHighlights()
}, { deep: true })

// 生命周期
onMounted(() => {
  updateBboxHighlights()
  addDefaultHighlights()
})
</script>

<style scoped>
.text-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-wrap: wrap;
  gap: 8px;
}

.search-results {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-count {
  font-size: 12px;
  color: #666;
}

.view-options {
  display: flex;
  align-items: center;
}

.text-content {
  flex: 1;
  overflow: auto;
  padding: 12px;
  background: #fff;
  font-size: 13px;
  line-height: 1.6;
}

.text-content.word-wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.text-content:not(.word-wrap) {
  white-space: pre;
}

.text-line {
  display: flex;
  min-height: 20px;
}

.line-number {
  color: #999;
  user-select: none;
  padding-right: 12px;
  text-align: right;
  min-width: 40px;
  border-right: 1px solid #eee;
  margin-right: 12px;
  font-size: 11px;
}

.line-content {
  flex: 1;
  cursor: text;
}

/* 高亮样式 - 优化设计 */
:deep(.extraction-highlight) {
  background: linear-gradient(135deg, rgba(64, 169, 255, 0.2), rgba(100, 200, 255, 0.25));
  border: 1px solid rgba(64, 169, 255, 0.4);
  border-radius: 4px;
  padding: 1px 2px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(64, 169, 255, 0.1);
  position: relative;
}


/* bbox映射高亮样式 */
:deep(.bbox-highlight) {
  background: linear-gradient(135deg, rgba(124, 77, 255, 0.2), rgba(147, 112, 255, 0.25));
  border: 1px solid rgba(124, 77, 255, 0.4);
  border-radius: 4px;
  padding: 1px 2px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(124, 77, 255, 0.1);
}

:deep(.extraction-highlight:hover) {
  background: linear-gradient(135deg, rgba(64, 169, 255, 0.35), rgba(100, 200, 255, 0.4));
  border-color: rgba(64, 169, 255, 0.6);
  box-shadow: 0 2px 8px rgba(64, 169, 255, 0.25);
  transform: translateY(-1px);
}

:deep(.bbox-highlight:hover) {
  background: linear-gradient(135deg, rgba(124, 77, 255, 0.35), rgba(147, 112, 255, 0.4));
  border-color: rgba(124, 77, 255, 0.6);
  box-shadow: 0 2px 8px rgba(124, 77, 255, 0.25);
  transform: translateY(-1px);
}

:deep(.selected-highlight) {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.3), rgba(255, 142, 142, 0.35));
  border: 2px solid rgba(255, 107, 107, 0.6);
  border-radius: 4px;
  padding: 1px 2px;
  box-shadow: 0 2px 12px rgba(255, 107, 107, 0.3);
  animation: pulse-highlight 0.6s ease-in-out;
}

@keyframes pulse-highlight {
  0% { transform: scale(1); }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); }
}

:deep(.search-highlight) {
  background-color: rgba(0, 255, 0, 0.3);
  border-radius: 2px;
}

:deep(.search-highlight-current) {
  background-color: rgba(0, 255, 0, 0.6);
  border-radius: 2px;
}

.highlight-tooltip {
  position: fixed;
  z-index: 1000;
  pointer-events: none;
}

.tooltip-content {
  background: rgba(0,0,0,0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  max-width: 300px;
}

/* 滚动目标临时高亮 */
.text-line.scroll-target {
  background-color: rgba(255, 193, 7, 0.15);
  border-left: 4px solid #ffc107;
  padding-left: 8px;
  transition: all 0.3s ease;
  animation: scroll-flash 2s ease-in-out;
}

@keyframes scroll-flash {
  0% { background-color: rgba(255, 193, 7, 0.3); }
  50% { background-color: rgba(255, 193, 7, 0.15); }
  100% { background-color: transparent; }
}

.tooltip-title {
  font-weight: bold;
  margin-bottom: 4px;
}

.tooltip-text {
  margin-bottom: 4px;
  word-break: break-word;
}

.tooltip-meta {
  opacity: 0.8;
  font-size: 11px;
}
</style>
