<template>
  <div class="enhanced-extract-container">
    <el-card :shadow="never">
      <template #header>
        <div class="card-title">
          <el-button 
            text 
            @click="goBack"
            style="margin-right: 12px;"
          >
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          智能合同信息提取 - 增强版
        </div>
      </template>
      <!-- 文件上传区域 -->
      <div class="upload-section" v-if="!taskId">
        <el-upload
          drag
          :multiple="false"
          accept=".pdf"
          :before-upload="beforeUpload"
          :show-file-list="false"
          :auto-upload="false"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">点击或拖拽PDF文件到此区域上传</div>
          <div class="el-upload__tip">支持PDF格式文件，系统将自动进行OCR识别和信息提取</div>
        </el-upload>

        <!-- 提取选项 -->
        <div class="extract-options" style="margin-top: 24px;">
          <el-form :inline="true">
            <el-form-item label="提取模式">
              <el-select v-model="extractOptions.schemaType" style="width: 200px;">
                <el-option value="contract" label="合同信息" />
                <el-option value="invoice" label="发票信息" />
                <el-option value="custom" label="自定义" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="OCR引擎">
              <el-select v-model="ocrProvider" style="width: 120px;">
                <el-option value="dotsocr" label="DotSOCR" />
                <el-option value="qwen" label="通义千问" />
                <el-option value="rapidocr" label="RapidOCR" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 处理进度 -->
      <div class="progress-section" v-if="taskId && !isCompleted">
        <el-progress 
          :percentage="progress" 
          :status="progressStatus"
        />
        <p class="progress-text">{{ statusMessage }}</p>
        
        <!-- 处理步骤指示器 -->
        <el-steps 
          :active="currentStep" 
          :process-status="progressStatus === 'exception' ? 'error' : 'process'"
          style="margin-top: 16px;"
        >
          <el-step title="文件上传" description="上传PDF文件" />
          <el-step title="OCR识别" description="提取文字和位置信息" />
          <el-step title="位置映射" description="建立文字位置索引" />
          <el-step title="信息提取" description="智能提取关键信息" />
          <el-step title="结果生成" description="生成可视化结果" />
        </el-steps>
      </div>

      <!-- 增强结果显示 -->
      <div class="enhanced-result-section" v-if="isCompleted && extractResult">
        <!-- 显示模式切换 -->
        <div class="display-controls" style="margin-bottom: 16px;">
          <el-radio-group v-model="displayMode">
            <el-radio-button value="canvas">图片模式</el-radio-button>
            <el-radio-button value="text">文本模式</el-radio-button>
          </el-radio-group>
          
        </div>

        <!-- 双栏布局：左侧Canvas/文本，右侧提取结果 -->
        <div class="dual-panel-layout">
          <!-- 左侧面板 -->
          <div class="left-panel">
            <el-card 
              class="canvas-card"
              :body-style="{ padding: '12px' }"
            >
              <template #header>{{ displayMode === 'canvas' ? '文档图像' : '文档文本' }}</template>
              <!-- Canvas模式 -->
              <div v-if="displayMode === 'canvas'" class="canvas-container">
                <canvas-viewer 
                  ref="canvasViewer"
                  :task-id="taskId"
                  :bbox-mappings="bboxMappings"
                  :char-boxes="charBoxes"
                  :extractions="extractResult?.extractions || []"
                  :total-pages="ocrMetadata?.totalPages || 1"
                  @bbox-click="onBboxClick"
                />
              </div>
              
              <!-- 文本模式 -->
              <div v-else class="text-container" @click.self="clearSelection">
                <text-viewer 
                  ref="textViewer"
                  :content="ocrResult?.content || ''"
                  :extractions="extractResult?.extractions || []"
                  :bbox-mappings="bboxMappings"
                  @text-click="onTextClick"
                />
              </div>
            </el-card>
          </div>

          <!-- 右侧面板 -->
          <div class="right-panel">
            <el-card 
              class="results-card"
              :body-style="{ padding: '12px' }"
            >
              <template #header>提取结果</template>
              <extraction-results 
                ref="extractionResults"
                :extractions="extractResult.extractions"
                :schema-type="extractResult.schemaType"
                @extraction-click="onExtractionClick"
              />
            </el-card>
          </div>
        </div>

      </div>

      <!-- 错误显示 -->
      <div class="error-section" v-if="error">
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
import { UploadFilled, ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { uploadFileEnhanced, getTaskStatus, getEnhancedTaskResult, getTaskCharBoxes, getTaskBboxMappings } from '@/api/extract'
import CanvasViewer from './components/CanvasViewer.vue'
import TextViewer from './components/TextViewer.vue'
import ExtractionResults from './components/ExtractionResults.vue'

// Vue Router
const router = useRouter()

// 响应式数据
const taskId = ref<string>('')
const progress = ref<number>(0)
const progressStatus = ref<'normal' | 'exception' | 'success'>('normal')
const statusMessage = ref<string>('准备就绪')
const currentStep = ref<number>(0)
const isCompleted = ref<boolean>(false)
const error = ref<string>('')

const displayMode = ref<'canvas' | 'text'>('canvas')
const ocrProvider = ref<string>('dotsocr')

// 提取选项
const extractOptions = reactive({
  schemaType: 'contract'
})

// 结果数据
const extractResult = ref<any>(null)
const ocrResult = ref<any>(null)
const ocrMetadata = ref<any>(null)
const bboxMappings = ref<any[]>([])
const charBoxes = ref<any[]>([])

// 组件引用
const canvasViewer = ref()
const textViewer = ref()
const extractionResults = ref()

// 轮询定时器
let pollingTimer: number | null = null

// 计算属性
const stepMapping = computed(() => ({
  'file_uploaded': 0,
  'ocr_processing': 1,
  'position_mapping': 2,
  'extracting': 3,
  'saving_results': 4,
  'completed': 4
}))

// 文件上传前处理
const beforeUpload = (file: File): boolean => {
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只支持PDF格式文件')
    return false
  }
  
  if (file.size > 50 * 1024 * 1024) { // 50MB
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  
  handleFileUpload(file)
  return false // 阻止默认上传
}

// 处理文件上传
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
    formData.append('schemaType', extractOptions.schemaType)
    formData.append('ocrProvider', ocrProvider.value)
    
    // 上传文件并开始提取
    const response = await uploadFileEnhanced(formData)
    
    if (response.code === 200) {
      taskId.value = response.data.taskId
      ElMessage.success('文件上传成功，开始提取...')
      startPolling()
    } else {
      throw new Error(response.message || '上传失败')
    }
    
  } catch (err: any) {
    error.value = err.message || '上传失败'
    progressStatus.value = 'exception'
    ElMessage.error(error.value)
  }
}

// 拖拽处理
const handleDrop = (e: DragEvent) => {
  e.preventDefault()
}

// 开始轮询任务状态
const startPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  pollingTimer = setInterval(async () => {
    try {
      const statusResponse = await getTaskStatus(taskId.value)
      
      if (statusResponse && statusResponse.data) {
        const status = statusResponse.data
        progress.value = status.progress || 0
        statusMessage.value = status.message || ''
        currentStep.value = stepMapping.value[status.status as keyof typeof stepMapping.value] || 0
        
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

// 加载结果数据
const loadResults = async () => {
  try {
    const resultResponse = await getEnhancedTaskResult(taskId.value)
    
    if (resultResponse.code === 200) {
      const data = resultResponse.data
      extractResult.value = data.extractResult
      ocrResult.value = data.ocrResult
      ocrMetadata.value = data.ocrMetadata
      bboxMappings.value = data.bboxMappings || []
      
      // 加载CharBox数据
      try {
        const charBoxResponse = await getTaskCharBoxes(taskId.value)
        if (charBoxResponse.code === 200) {
          charBoxes.value = charBoxResponse.data || []
          console.log('CharBox数据加载完成:', charBoxes.value.length)
        }
      } catch (charBoxErr) {
        console.warn('CharBox数据加载失败:', charBoxErr)
      }
      
      ElMessage.success('结果加载完成')
    } else {
      throw new Error(resultResponse.message || '加载结果失败')
    }
  } catch (err: any) {
    error.value = err.message || '加载结果失败'
    ElMessage.error(error.value)
  }
}

// 交互事件处理
const onBboxClick = (bboxInfo: any) => {
  // 高亮对应的提取结果
  if (extractionResults.value) {
    extractionResults.value.highlightExtraction(bboxInfo.extractionId)
  }
}

const onTextClick = (textInfo: any) => {
  // 高亮对应的提取结果
  if (extractionResults.value) {
    extractionResults.value.highlightExtraction(textInfo.extractionId)
  }
}

const onExtractionClick = (extraction: any) => {
  // 在Canvas或文本视图中高亮对应区域
  if (displayMode.value === 'canvas' && canvasViewer.value) {
    // 定位到提取内容并高亮
    canvasViewer.value.navigateToExtraction(extraction)
  } else if (displayMode.value === 'text' && textViewer.value) {
    textViewer.value.highlightText(extraction.charIntervals)
  }
}

// 清除选中高亮
const clearSelection = () => {
  if (displayMode.value === 'text' && textViewer.value) {
    textViewer.value.highlightText([]) // 传入空数组清除选中
  }
}

// 返回功能
const goBack = () => {
  // 如果是从URL参数进入的，尝试关闭窗口或返回上一页
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('taskId')) {
    // 如果能关闭窗口就关闭，否则跳转到主页面
    try {
      window.close()
      // 如果窗口没有关闭（比如不是弹出窗口），则跳转
      setTimeout(() => {
        router.push('/info-extract')
      }, 100)
    } catch (error) {
      router.push('/info-extract')
    }
  } else {
    // 使用Vue Router跳转到主页面
    router.push('/info-extract')
  }
}


// 生命周期
onMounted(() => {
  // 检查URL参数，如果有taskId则加载对应的结果
  const urlParams = new URLSearchParams(window.location.search)
  const urlTaskId = urlParams.get('taskId')
  
  if (urlTaskId) {
    taskId.value = urlTaskId
    isCompleted.value = true
    loadResults()
  }
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style scoped>
.enhanced-extract-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f0f2f5;
}

.upload-section {
  max-width: 800px;
  margin: 0 auto;
}

.progress-section {
  max-width: 800px;
  margin: 24px auto;
  padding: 24px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.progress-text {
  text-align: center;
  margin: 16px 0;
  color: #666;
}

.enhanced-result-section {
  margin-top: 24px;
}

.display-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.dual-panel-layout {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  height: 70vh; /* 设置固定高度，避免被挤压 */
}

.left-panel {
  flex: 2; /* 左侧占更大比例 */
  min-width: 0; /* 防止flex收缩问题 */
  height: 100%; /* 确保高度继承 */
}

.right-panel {
  flex: 1; /* 右侧占较小比例 */
  min-width: 300px; /* 设置最小宽度，防止被挤压 */
  max-width: 500px; /* 设置最大宽度 */
  height: 100%; /* 确保高度继承 */
}

.canvas-card,
.results-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.canvas-card :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
}

.results-card :deep(.ant-card-body) {
  flex: 1;
  overflow: auto;
}

.canvas-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.text-container {
  width: 100%;
  height: 100%;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.stats-section {
  padding: 16px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.error-section {
  margin-top: 16px;
}

.card-title {
  display: flex;
  align-items: center;
}
</style>
