<template>
  <div class="ocr-extract-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Document /></el-icon>
            合同OCR文本提取
          </div>
        </div>
      </template>

      <!-- 上传区域 -->
      <div v-if="!taskId" class="upload-section">
        <el-upload
          drag
          :multiple="false"
          accept=".pdf"
          :before-upload="beforeUpload"
          :show-file-list="false"
          :auto-upload="true"
          :http-request="handleUploadRequest"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            点击或拖拽PDF文件到此区域上传
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持PDF格式文件，系统将自动进行OCR识别并提取文本内容
            </div>
          </template>
        </el-upload>

        <!-- OCR选项 -->
        <div class="ocr-options">
          <el-form :inline="true" label-width="120px">
            <el-form-item label="忽略页眉页脚">
              <el-switch v-model="ocrOptions.ignoreHeaderFooter" />
            </el-form-item>
            
            <el-form-item label="页眉高度(%)" v-if="ocrOptions.ignoreHeaderFooter">
              <el-input-number 
                v-model="ocrOptions.headerHeightPercent" 
                :min="0" 
                :max="50" 
                :step="1"
                style="width: 120px;"
              />
            </el-form-item>
            
            <el-form-item label="页脚高度(%)" v-if="ocrOptions.ignoreHeaderFooter">
              <el-input-number 
                v-model="ocrOptions.footerHeightPercent" 
                :min="0" 
                :max="50" 
                :step="1"
                style="width: 120px;"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 处理进度 -->
      <div v-if="taskId && !isCompleted" class="progress-section">
        <el-progress 
          :percentage="progress" 
          :status="progressStatus"
        />
        <p class="progress-text">{{ statusMessage }}</p>
        
        <el-steps 
          :active="currentStep" 
          :process-status="progressStatus === 'exception' ? 'error' : 'process'"
          style="margin-top: 16px;"
        >
          <el-step title="文件上传" description="上传PDF文件" />
          <el-step title="OCR识别" description="提取文字和位置信息" />
          <el-step title="处理结果" description="生成可视化结果" />
          <el-step title="完成" description="提取完成" />
        </el-steps>
      </div>

      <!-- OCR结果显示 -->
      <div v-if="isCompleted && ocrResult" class="result-section">
        <!-- 工具栏 -->
        <div class="result-toolbar">
          <div class="display-controls">
            <span class="toolbar-title">OCR提取结果</span>
          </div>

          <div class="action-buttons">
            <el-button @click="copyText" :icon="CopyDocument">复制文本</el-button>
            <el-button @click="downloadText" :icon="Download">下载文本</el-button>
            <el-button @click="resetTask" :icon="Refresh">重新提取</el-button>
          </div>
        </div>

        <!-- 双栏布局 -->
        <div class="dual-panel-layout">
          <!-- 左侧：图片 -->
          <div class="left-panel">
            <el-card 
              class="image-card"
              :body-style="{ 
                padding: '12px',
                height: '100%',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column'
              }"
            >
              <template #header>
                <div class="panel-header">
                  文档图像
                  <span class="page-info" v-if="totalPages > 0">
                    共 {{ totalPages }} 页
                  </span>
                </div>
              </template>
              
              <canvas-viewer 
                v-if="totalPages > 0"
                ref="canvasViewer"
                :task-id="taskId"
                :char-boxes="textBoxes"
                :extractions="[]"
                :bbox-mappings="bboxMappings"
                :total-pages="totalPages"
                :api-prefix="'/api/ocr/extract/page-image'"
                @bbox-click="onBboxClick"
                @page-change="onPageChange"
                style="height: 100%; display: flex; flex-direction: column;"
              />
            </el-card>
          </div>

          <!-- 右侧：文本 -->
          <div class="right-panel">
            <el-card 
              class="text-card"
              :body-style="{ 
                padding: '0',
                height: '100%',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column'
              }"
            >
              <template #header>
                <div class="panel-header">
                  提取文本
                  <el-tag size="small" type="info">
                    {{ ocrResult.textLength || 0 }} 字符
                  </el-tag>
                </div>
              </template>
              
              <markdown-viewer 
                ref="markdownViewer"
                :content="ocrResult.ocrText || ''"
                :char-boxes="textBoxes"
                @text-click="onTextClick"
              />
            </el-card>
          </div>
        </div>
      </div>

      <!-- 错误显示 -->
      <div v-if="error" class="error-section">
        <el-alert
          :title="error"
          type="error"
          show-icon
          closable
          @close="error = ''"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Document, 
  UploadFilled, 
  CopyDocument, 
  Download, 
  Refresh 
} from '@element-plus/icons-vue'
import { 
  uploadPdfForOcr, 
  getOcrTaskStatus, 
  getOcrResult, 
  getTextBoxes,
  getBboxMappings 
} from '@/api/ocr-extract'
import CanvasViewer from '@/views/extract/components/CanvasViewer.vue'
import MarkdownViewer from './components/MarkdownViewer.vue'

// 响应式数据
const taskId = ref<string>('')
const progress = ref<number>(0)
const progressStatus = ref<'normal' | 'exception' | 'success'>('normal')
const statusMessage = ref<string>('准备就绪')
const currentStep = ref<number>(0)
const isCompleted = ref<boolean>(false)
const error = ref<string>('')

// OCR选项
const ocrOptions = reactive({
  ignoreHeaderFooter: true,
  headerHeightPercent: 12,
  footerHeightPercent: 12
})

// 结果数据
const ocrResult = ref<any>(null)
const textBoxes = ref<any[]>([])
const bboxMappings = ref<any[]>([])
const totalPages = ref<number>(0)

// 组件引用
const canvasViewer = ref()
const markdownViewer = ref()

// 轮询定时器
let pollingTimer: number | null = null

// 步骤映射
const stepMapping: Record<string, number> = {
  'processing': 1,
  'completed': 3
}

/**
 * 文件上传前验证
 */
const beforeUpload = (file: File): boolean => {
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只支持PDF格式文件')
    return false
  }
  
  if (file.size > 100 * 1024 * 1024) { // 100MB
    ElMessage.error('文件大小不能超过100MB')
    return false
  }
  
  return true // 验证通过，继续上传
}

/**
 * 自定义上传请求
 */
const handleUploadRequest = async (options: any) => {
  await handleFileUpload(options.file)
}

/**
 * 处理文件上传
 */
const handleFileUpload = async (file: File) => {
  try {
    error.value = ''
    progress.value = 0
    progressStatus.value = 'normal'
    statusMessage.value = '正在上传文件...'
    currentStep.value = 0
    isCompleted.value = false
    
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)
    formData.append('ignoreHeaderFooter', String(ocrOptions.ignoreHeaderFooter))
    formData.append('headerHeightPercent', String(ocrOptions.headerHeightPercent))
    formData.append('footerHeightPercent', String(ocrOptions.footerHeightPercent))
    
    // 上传文件并开始提取
    const response: any = await uploadPdfForOcr(formData)
    
    // baseRequest拦截器已处理，直接返回data对象
    if (response && response.data) {
      taskId.value = response.data.taskId
      ElMessage.success('文件上传成功，开始OCR提取...')
      startPolling()
    } else {
      throw new Error('上传失败')
    }
    
  } catch (err: any) {
    error.value = err.message || '上传失败'
    progressStatus.value = 'exception'
    ElMessage.error(error.value)
  }
}

/**
 * 开始轮询任务状态
 */
const startPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  pollingTimer = setInterval(async () => {
    try {
      const response: any = await getOcrTaskStatus(taskId.value)
      
      // baseRequest拦截器已处理，response就是data对象
      if (response && response.data) {
        const status = response.data
        progress.value = status.progress || 0
        statusMessage.value = status.message || ''
        currentStep.value = stepMapping[status.status] || 0
        
        if (status.status === 'completed') {
          progressStatus.value = 'success'
          isCompleted.value = true
          clearInterval(pollingTimer!)
          pollingTimer = null
          await loadResults()
        } else if (status.status === 'failed') {
          progressStatus.value = 'exception'
          error.value = status.message || '处理失败'
          clearInterval(pollingTimer!)
          pollingTimer = null
        }
      }
    } catch (err: any) {
      console.error('轮询状态失败:', err)
    }
  }, 2000) // 每2秒轮询一次
}

/**
 * 加载结果数据
 */
const loadResults = async () => {
  try {
    // 获取OCR结果
    const resultResponse: any = await getOcrResult(taskId.value)
    console.log('OCR结果响应:', resultResponse)
    
    // baseRequest拦截器已处理，resultResponse就是data对象
    if (resultResponse && resultResponse.data) {
      ocrResult.value = resultResponse.data
      totalPages.value = ocrResult.value.totalPages || 0
      
      console.log('OCR结果:', ocrResult.value)
      console.log('总页数:', totalPages.value)
      console.log('文本内容:', ocrResult.value.ocrText?.substring(0, 100))
      
      // 如果TextBox数据在结果中
      if (ocrResult.value.textBoxes) {
        textBoxes.value = ocrResult.value.textBoxes
        console.log('TextBox数量（从结果）:', textBoxes.value.length)
        
        // 调试：输出前几个TextBox的字符索引信息
        if (textBoxes.value.length > 0) {
          console.log('前3个TextBox示例:')
          textBoxes.value.slice(0, 3).forEach((tb, idx) => {
            console.log(`  [${idx}] 页码:${tb.page}, 索引:${tb.startPos}-${tb.endPos}, 文本:"${tb.text?.substring(0, 20)}..."`)
          })
        }
      } else if (ocrResult.value.textBoxesAvailable) {
        // 单独加载TextBox数据
        const textBoxResponse: any = await getTextBoxes(taskId.value)
        if (textBoxResponse && textBoxResponse.data) {
          textBoxes.value = textBoxResponse.data || []
          console.log('TextBox数量（单独加载）:', textBoxes.value.length)
        }
      }
      
      // 加载Bbox映射数据（用于处理跨页表格等）
      try {
        const bboxMappingResponse: any = await getBboxMappings(taskId.value)
        if (bboxMappingResponse && bboxMappingResponse.data) {
          bboxMappings.value = bboxMappingResponse.data || []
          console.log('✅ BboxMapping加载成功')
          console.log('  - TextBox数量:', textBoxes.value.length)
          console.log('  - BboxMapping数量:', bboxMappings.value.length)
          
          if (bboxMappings.value.length > 0) {
            console.log('前3个BboxMapping示例:')
            bboxMappings.value.slice(0, 3).forEach((mapping, idx) => {
              const interval = mapping.interval || {}
              const text = mapping.text || ''
              const bboxCount = mapping.bboxes?.length || 0
              const pages = mapping.pages || []
              console.log(`  [${idx}] 文本:"${text.substring(0, 20)}..." 字符索引:${interval.startPos}-${interval.endPos} bbox数:${bboxCount} 页码:[${pages.join(',')}]`)
            })
          }
        }
      } catch (err) {
        console.warn('加载BboxMapping失败，将使用TextBox:', err)
        bboxMappings.value = []
      }
      
      ElMessage.success('OCR提取完成')
    } else {
      console.error('结果响应格式错误:', resultResponse)
    }
  } catch (err: any) {
    console.error('加载结果失败:', err)
    error.value = '加载结果失败: ' + (err.message || '未知错误')
    ElMessage.error(error.value)
  }
}

/**
 * 处理bbox点击 - 在图片上点击bbox，高亮对应的文本
 */
const onBboxClick = (bboxInfo: any) => {
  console.log('📍 点击了bbox:', {
    page: bboxInfo.page,
    text: bboxInfo.text?.substring(0, 30) + '...',
    startPos: bboxInfo.startPos,
    endPos: bboxInfo.endPos,
    bbox: bboxInfo.bbox
  })
  
  if (!bboxInfo || !bboxInfo.text) {
    console.warn('⚠️ bbox缺少文本信息')
    return
  }
  
  // 调用MarkdownViewer的方法高亮对应的文本
  if (markdownViewer.value) {
    markdownViewer.value.highlightTextByBox(bboxInfo)
    console.log('✅ 已高亮文本:', `字符索引 ${bboxInfo.startPos}-${bboxInfo.endPos}`)
  } else {
    console.warn('⚠️ MarkdownViewer组件未就绪')
  }
}

/**
 * 处理文本点击 - 在文本上点击，高亮对应的图片bbox
 */
const onTextClick = (textBoxIndex: number, textBox: any) => {
  console.log('📝 点击了文本:', {
    index: textBoxIndex,
    page: textBox.page,
    text: textBox.text?.substring(0, 30) + '...',
    startPos: textBox.startPos,
    endPos: textBox.endPos
  })
  
  if (!textBox || !canvasViewer.value) {
    console.warn('⚠️ textBox或CanvasViewer未就绪')
    return
  }
  
  // 高亮对应的bbox（会自动滚动到bbox位置，类似合同比对的差异跳转）
  canvasViewer.value.highlightBbox(textBox)
  console.log('✅ 已高亮并滚动到bbox，页码:', textBox.page, `字符索引 ${textBox.startPos}-${textBox.endPos}`)
}

/**
 * 处理页面变化
 */
const onPageChange = (page: number) => {
  console.log('页面变化:', page)
}

/**
 * 复制文本
 */
const copyText = async () => {
  if (!ocrResult.value || !ocrResult.value.ocrText) {
    ElMessage.warning('没有可复制的文本')
    return
  }
  
  try {
    await navigator.clipboard.writeText(ocrResult.value.ocrText)
    ElMessage.success('文本已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制')
  }
}

/**
 * 下载文本
 */
const downloadText = () => {
  if (!ocrResult.value || !ocrResult.value.ocrText) {
    ElMessage.warning('没有可下载的文本')
    return
  }
  
  const blob = new Blob([ocrResult.value.ocrText], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `ocr_extract_${taskId.value}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('文本已下载')
}

/**
 * 重置任务
 */
const resetTask = () => {
  taskId.value = ''
  progress.value = 0
  progressStatus.value = 'normal'
  statusMessage.value = '准备就绪'
  currentStep.value = 0
  isCompleted.value = false
  error.value = ''
  ocrResult.value = null
  textBoxes.value = []
  totalPages.value = 0
  
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style scoped>
.ocr-extract-container {
  background: #f5f7fa;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.upload-section {
  padding: 40px 0;
}

.ocr-options {
  margin-top: 32px;
  padding: 24px;
  background: #f9fafc;
  border-radius: 8px;
}

.progress-section {
  padding: 40px 80px;
}

.progress-text {
  text-align: center;
  margin-top: 12px;
  color: #606266;
  font-size: 14px;
}

.result-section {
  margin-top: 16px;
  max-height: calc(100vh - 200px); /* 限制结果区域的最大高度 */
  overflow: hidden; /* 防止内容溢出 */
  display: flex;
  flex-direction: column;
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  flex-shrink: 0; /* 工具栏不缩小 */
}

.display-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.dual-panel-layout {
  display: flex;
  gap: 16px;
  flex: 1; /* 占据result-section的剩余空间 */
  min-height: 0; /* 允许缩小 */
}

.left-panel {
  flex: 1;
  min-width: 0;
}

.right-panel {
  flex: 1;
  min-width: 0;
}

.image-card,
.text-card {
  height: 100%;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.page-info {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.error-section {
  margin-top: 16px;
}
</style>

