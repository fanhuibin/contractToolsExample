<template>
  <div class="markdown-viewer">
    <div 
      class="markdown-content" 
      v-html="renderedContent"
      @click="handleClick"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  content: string
  charBoxes?: any[]  // 实际上是textBoxes
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  charBoxes: () => []
})

const emit = defineEmits<{
  textClick: [textBoxIndex: number, textBox: any]
}>()

/**
 * 简单的markdown渲染器（支持表格、标题等基本格式）
 */
const parseMarkdown = (text: string): string => {
  if (!text) return '<p class="empty-text">暂无内容</p>'
  
  let html = text
  
  // 处理表格（检测HTML table标签）
  const tableRegex = /<table[^>]*>[\s\S]*?<\/table>/gi
  const tables = html.match(tableRegex)
  if (tables) {
    tables.forEach((table) => {
      // 保留表格HTML，只添加样式类
      html = html.replace(table, `<div class="table-wrapper">${table}</div>`)
    })
  }
  
  // 处理Markdown表格（简化版）
  const lines = html.split('\n')
  const processedLines: string[] = []
  let inTable = false
  let tableRows: string[] = []
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    
    // 检测表格行（以 | 开头和结尾）
    if (line.startsWith('|') && line.endsWith('|')) {
      if (!inTable) {
        inTable = true
        tableRows = []
      }
      tableRows.push(line)
    } else if (inTable) {
      // 表格结束，转换为HTML
      if (tableRows.length > 0) {
        processedLines.push(convertMarkdownTableToHtml(tableRows))
        tableRows = []
      }
      inTable = false
      processedLines.push(line)
    } else {
      processedLines.push(line)
    }
  }
  
  // 处理未闭合的表格
  if (inTable && tableRows.length > 0) {
    processedLines.push(convertMarkdownTableToHtml(tableRows))
  }
  
  html = processedLines.join('\n')
  
  // 处理标题
  html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
  html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
  html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
  
  // 处理段落
  html = html.replace(/\n\n/g, '</p><p>')
  html = `<p>${html}</p>`
  
  // 清理空段落
  html = html.replace(/<p><\/p>/g, '')
  html = html.replace(/<p>\s*<\/p>/g, '')
  
  // 处理换行
  html = html.replace(/\n/g, '<br>')
  
  return html
}

/**
 * 将Markdown表格转换为HTML表格
 */
const convertMarkdownTableToHtml = (rows: string[]): string => {
  if (rows.length < 2) return rows.join('\n')
  
  let html = '<table class="markdown-table">'
  
  // 第一行是表头
  const headerCells = rows[0].split('|').filter(cell => cell.trim() !== '')
  html += '<thead><tr>'
  headerCells.forEach(cell => {
    html += `<th>${cell.trim()}</th>`
  })
  html += '</tr></thead>'
  
  // 跳过分隔行（第二行）
  html += '<tbody>'
  for (let i = 2; i < rows.length; i++) {
    const cells = rows[i].split('|').filter(cell => cell.trim() !== '')
    html += '<tr>'
    cells.forEach(cell => {
      html += `<td>${cell.trim()}</td>`
    })
    html += '</tr>'
  }
  html += '</tbody></table>'
  
  return html
}

/**
 * 识别HTML表格标签的范围
 */
const findHtmlTableRanges = (text: string): Array<{start: number, end: number, html: string}> => {
  const ranges: Array<{start: number, end: number, html: string}> = []
  const tableRegex = /<table[\s\S]*?<\/table>/gi
  let match
  
  while ((match = tableRegex.exec(text)) !== null) {
    ranges.push({
      start: match.index,
      end: match.index + match[0].length,
      html: match[0]
    })
  }
  
  return ranges
}

/**
 * 为HTML表格添加点击事件属性
 * 通过添加data属性，让表格单元格也能点击跳转
 */
const wrapTableWithClickableAttrs = (tableHtml: string, startPos: number, charToTextBox: (number | null)[]): string => {
  // 简化处理：为整个表格添加一个容器，带上字符索引信息
  // 更精确的做法是解析每个单元格，但那样太复杂
  
  // 找到表格内容对应的textBox
  let tableTextBoxIndex: number | null = null
  for (let i = startPos; i < startPos + tableHtml.length && i < charToTextBox.length; i++) {
    if (charToTextBox[i] !== null) {
      tableTextBoxIndex = charToTextBox[i]
      break
    }
  }
  
  if (tableTextBoxIndex !== null) {
    const textBox = props.charBoxes[tableTextBoxIndex]
    return `<div class="table-wrapper clickable-text" data-textbox-index="${tableTextBoxIndex}" data-page="${textBox.page}" data-start="${textBox.startPos}" data-end="${textBox.endPos}">${tableHtml}</div>`
  }
  
  return `<div class="table-wrapper">${tableHtml}</div>`
}

/**
 * 为文本内容添加可点击的包装
 * 基于字符索引（startPos/endPos）来准确标记文本，避免重复文本的问题
 * 同时保留HTML表格标签不被破坏
 */
const wrapTextWithClickableSpans = (plainText: string): string => {
  if (!props.charBoxes || props.charBoxes.length === 0) {
    // 即使没有charBoxes，也要正确渲染HTML表格
    return plainText.replace(/\n/g, '<br>')
  }
  
  // 识别HTML表格的范围
  const htmlRanges = findHtmlTableRanges(plainText)
  const isInHtmlRange = (pos: number): boolean => {
    return htmlRanges.some(range => pos >= range.start && pos < range.end)
  }
  
  // 创建一个字符索引到textBox的映射
  const charToTextBox: (number | null)[] = new Array(plainText.length).fill(null)
  
  props.charBoxes.forEach((textBox, index) => {
    if (textBox.startPos !== undefined && textBox.endPos !== undefined) {
      // 标记这个范围内的字符属于哪个textBox
      for (let i = textBox.startPos; i < textBox.endPos && i < plainText.length; i++) {
        charToTextBox[i] = index
      }
    }
  })
  
  // 根据映射构建HTML，将连续的相同textBox的字符包装在一个span中
  let result = ''
  let i = 0
  
  while (i < plainText.length) {
    // 检查是否在HTML表格范围内
    if (isInHtmlRange(i)) {
      // 找到HTML范围的结束位置
      const htmlRange = htmlRanges.find(range => i >= range.start && i < range.end)
      if (htmlRange) {
        // 为表格添加点击属性
        result += wrapTableWithClickableAttrs(htmlRange.html, htmlRange.start, charToTextBox)
        i = htmlRange.end
        continue
      }
    }
    
    const textBoxIndex = charToTextBox[i]
    
    if (textBoxIndex !== null) {
      // 找到连续属于同一个textBox的字符范围
      let j = i
      while (j < plainText.length && charToTextBox[j] === textBoxIndex && !isInHtmlRange(j)) {
        j++
      }
      
      const textBox = props.charBoxes[textBoxIndex]
      const text = plainText.substring(i, j)
      
      // 包装为可点击的span
      result += `<span class="clickable-text" data-textbox-index="${textBoxIndex}" data-page="${textBox.page}" data-start="${textBox.startPos}" data-end="${textBox.endPos}">${escapeHtml(text)}</span>`
      i = j
    } else {
      // 非textBox区域的字符（如换行符等）
      result += escapeHtml(plainText.charAt(i))
      i++
    }
  }
  
  return result
}

/**
 * HTML转义（只转义纯文本，不转义HTML标签）
 */
const escapeHtml = (text: string): string => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 渲染markdown内容
const renderedContent = computed(() => {
  // 直接对原始文本应用字符索引包装，不经过markdown解析
  // 因为markdown解析会改变字符位置
  const wrappedText = wrapTextWithClickableSpans(props.content)
  
  // 对包装后的文本应用简单的HTML格式化
  let result = wrappedText
  
  // 将换行符转为<br>标签
  result = result.replace(/\n/g, '<br>')
  
  // 包装在段落中
  result = `<div class="ocr-text-content">${result}</div>`
  
  return result
})

/**
 * 处理点击事件
 */
const handleClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  
  // 查找包含data-textbox-index的元素
  let element = target
  while (element && element !== event.currentTarget) {
    const textBoxIndex = element.getAttribute('data-textbox-index')
    if (textBoxIndex !== null) {
      const index = parseInt(textBoxIndex, 10)
      if (!isNaN(index) && props.charBoxes && props.charBoxes[index]) {
        // 高亮被点击的文本
        highlightClickedText(element)
        emit('textClick', index, props.charBoxes[index])
        return
      }
    }
    element = element.parentElement as HTMLElement
  }
}

/**
 * 高亮被点击的文本
 */
const highlightClickedText = (element: HTMLElement) => {
  // 移除之前的高亮
  const previousHighlights = document.querySelectorAll('.clickable-text.highlighted')
  previousHighlights.forEach(el => el.classList.remove('highlighted'))
  
  // 添加当前高亮
  element.classList.add('highlighted')
  
  // 滚动到可见区域
  element.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

/**
 * 通过textBox高亮对应的文本（从外部调用，如从图片点击bbox）
 * 基于字符索引（startPos/endPos）精确查找
 */
const highlightTextByBox = (textBox: any) => {
  if (!textBox) {
    return
  }
  
  // 优先使用字符索引查找
  if (textBox.startPos !== undefined && textBox.endPos !== undefined) {
    // 查找具有相同字符索引的DOM元素
    const element = document.querySelector(
      `[data-start="${textBox.startPos}"][data-end="${textBox.endPos}"]`
    ) as HTMLElement
    
    if (element) {
      highlightClickedText(element)
      return
    }
  }
  
  // 如果字符索引查找失败，尝试通过textBox索引查找
  const index = props.charBoxes.findIndex(tb => 
    tb.page === textBox.page && 
    tb.startPos === textBox.startPos &&
    tb.endPos === textBox.endPos
  )
  
  if (index !== -1) {
    const element = document.querySelector(`[data-textbox-index="${index}"]`) as HTMLElement
    if (element) {
      highlightClickedText(element)
      return
    }
  }
  
  // 尝试模糊匹配：通过文本内容查找
  if (textBox.text && textBox.text.trim()) {
    const textContent = textBox.text.trim()
    const elements = document.querySelectorAll('.clickable-text')
    
    for (const element of elements) {
      if (element.textContent && element.textContent.trim() === textContent) {
        highlightClickedText(element as HTMLElement)
        return
      }
    }
  }
}

// 暴露方法给父组件
defineExpose({
  highlightTextByBox
})
</script>

<style scoped>
.markdown-viewer {
  height: 100%;
  overflow: auto; /* 允许滚动 */
  padding: 16px;
  background: #fff;
}

.markdown-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #1a1a1a;
}

.markdown-content :deep(h1) { font-size: 24px; }
.markdown-content :deep(h2) { font-size: 20px; }
.markdown-content :deep(h3) { font-size: 18px; }
.markdown-content :deep(h4) { font-size: 16px; }
.markdown-content :deep(h5) { font-size: 14px; }
.markdown-content :deep(h6) { font-size: 12px; }

.markdown-content :deep(p) {
  margin: 8px 0;
  text-align: justify;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 13px;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #333;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #fafafa;
}

.markdown-content :deep(table tr:hover) {
  background-color: #f0f2f5;
}

.markdown-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #333;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 16px;
  margin: 12px 0;
  color: #666;
  font-style: italic;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  margin: 12px 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #ddd;
  margin: 16px 0;
}

.empty-text {
  color: #999;
  text-align: center;
  padding: 40px 0;
}

/* 可点击文本的样式 */
.markdown-content :deep(.clickable-text) {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 2px;
  padding: 1px 2px;
}

.markdown-content :deep(.clickable-text:hover) {
  background-color: #fff3cd;
  box-shadow: 0 0 0 2px rgba(255, 193, 7, 0.3);
}

/* 高亮状态 */
.markdown-content :deep(.clickable-text.highlighted) {
  background-color: #ffc107;
  color: #000;
  font-weight: 500;
  box-shadow: 0 0 0 3px rgba(255, 193, 7, 0.5);
}

/* OCR文本内容样式 */
.markdown-content :deep(.ocr-text-content) {
  line-height: 1.8;
  white-space: pre-wrap; /* 保留空白符 */
  word-break: break-word;
}

/* OCR模式下的表格容器样式 */
.markdown-content :deep(.table-wrapper) {
  margin: 16px 0;
}

.markdown-content :deep(.table-wrapper.clickable-text) {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
  padding: 0;
}

.markdown-content :deep(.table-wrapper.clickable-text:hover) {
  box-shadow: 0 0 0 2px rgba(255, 193, 7, 0.3);
}

.markdown-content :deep(.table-wrapper.clickable-text.highlighted) {
  box-shadow: 0 0 0 3px rgba(255, 193, 7, 0.5);
}

/* OCR模式下的表格样式 */
.markdown-content :deep(.ocr-text-content table),
.markdown-content :deep(.table-wrapper table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0;
  font-size: 13px;
  white-space: normal; /* 表格内容不保留空白符 */
}

.markdown-content :deep(.ocr-text-content table th),
.markdown-content :deep(.ocr-text-content table td),
.markdown-content :deep(.table-wrapper table th),
.markdown-content :deep(.table-wrapper table td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(.ocr-text-content table th),
.markdown-content :deep(.table-wrapper table th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #333;
}

.markdown-content :deep(.ocr-text-content table tr:nth-child(even)),
.markdown-content :deep(.table-wrapper table tr:nth-child(even)) {
  background-color: #fafafa;
}

.markdown-content :deep(.ocr-text-content table tr:hover),
.markdown-content :deep(.table-wrapper table tr:hover) {
  background-color: #f0f2f5;
}
</style>

