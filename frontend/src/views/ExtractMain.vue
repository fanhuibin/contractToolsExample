<template>
  <DemoLayout category="extract" @doc-select="handleDemoDocSelect" @manage-template="handleManageTemplate">
    <!-- 结果页面iframe（替换右侧内容） -->
    <div v-if="showResultPage" class="result-page-wrapper">
      <div class="result-page-header">
        <el-button 
          text 
          type="primary" 
          @click="backToExtract"
          class="back-button"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回抽取
        </el-button>
        <span class="result-page-title">抽取结果</span>
      </div>
      <iframe 
        :src="resultPageUrl" 
        class="result-page-iframe"
        @load="onResultPageLoad"
      />
    </div>
    
    <!-- 抽取界面（默认显示） -->
    <div v-else class="extract-content-wrapper">
      <el-card class="main-card">
        <!-- 顶部步骤指示器 -->
        <div class="steps-section">
          <el-steps align-center>
            <el-step title="上传文档" description="上传PDF文档进行智能分析" />
            <el-step title="选择模板" description="选择适合的抽取模板" />
            <el-step title="查看结果" description="查看抽取结果并导出" />
          </el-steps>
        </div>

        <!-- 文件上传区域 -->
        <div class="upload-section">
          <el-upload
            drag
            v-model:file-list="fileList"
            :multiple="false"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept=".pdf"
            :on-change="handleFileChange"
            :auto-upload="false"
            class="centered-upload"
          >
            <div v-if="!selectedFile" class="upload-placeholder">
              <el-icon :size="80" class="upload-icon">
                <UploadFilled />
              </el-icon>
              <div class="upload-text">
                <p class="main-text">点击或拖拽文件到此处</p>
                <p class="sub-text">仅支持 PDF 格式，最大 100MB</p>
              </div>
            </div>
            <div v-else class="file-selected">
              <div class="file-icon-wrapper">
                <el-icon :size="60" color="#52c41a">
                  <Document />
                </el-icon>
                <el-icon :size="24" class="success-badge" color="#52c41a">
                  <CircleCheck />
                </el-icon>
              </div>
              <p class="file-name">已选择：{{ selectedFile.name }}</p>
              <p class="file-size">大小: {{ formatFileSize(selectedFile.size) }}</p>
              <el-button 
                text 
                type="primary" 
                @click.stop="clearFile"
                class="reselect-btn"
              >
                重新选择
              </el-button>
            </div>
          </el-upload>
        </div>

        <!-- 模板选择区域 -->
        <div class="template-selection">
          <div class="selection-row">
            <el-select 
              v-model="selectedTemplateId" 
              placeholder="请选择抽取模板"
              :loading="loadingTemplates"
              filterable
              class="template-select"
            >
              <el-option
                v-for="template in templates"
                :key="template.id"
                :label="`${template.templateName} (${template.templateCode || ''})`"
                :value="template.id"
              >
                <div class="template-option">
                  <span>{{ template.templateName }}</span>
                  <el-tag size="small" type="info" v-if="template.templateCode">
                    {{ template.templateCode }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>

            <!-- 页眉页脚设置 -->
            <div class="header-footer-config">
              <el-checkbox v-model="extractSettings.ignoreHeaderFooter">
                忽略页眉页脚
              </el-checkbox>
              
              <div v-if="extractSettings.ignoreHeaderFooter" class="percentage-inputs">
                <div class="input-group">
                  <label>页眉</label>
                  <el-input-number 
                    v-model="extractSettings.headerHeightPercent" 
                    :min="0" 
                    :max="50" 
                    :step="0.5"
                    :precision="1"
                    class="percentage-input"
                    controls-position="right"
                  />
                  <span class="unit">%</span>
                </div>
                <div class="input-group">
                  <label>页脚</label>
                  <el-input-number 
                    v-model="extractSettings.footerHeightPercent" 
                    :min="0" 
                    :max="50" 
                    :step="0.5"
                    :precision="1"
                    class="percentage-input"
                    controls-position="right"
                  />
                  <span class="unit">%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

       

        <!-- 操作按钮区域 -->
        <div class="action-buttons">
          <el-button 
            type="primary" 
            size="large"
            :loading="isExtracting"
            :disabled="!canStartExtraction"
            @click="startExtraction"
            class="primary-btn"
          >
            {{ isExtracting ? '抽取中...' : '信息抽取' }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 结果展示区 -->
    <div class="result-area">
        <el-card v-if="currentTask && currentTask.status !== 'completed'" class="progress-card">
          <template #header>
            <div class="progress-header">
              <span>处理进度</span>
              <el-button 
                type="danger" 
                size="small" 
                text
                @click="cancelTask"
                :disabled="currentTask.status === 'cancelled'"
              >
                取消任务
              </el-button>
            </div>
          </template>

          <div class="progress-content">
            <div class="task-info">
              <div class="info-row">
                <span class="label">文件名：</span>
                <span class="value">{{ currentTask.fileName }}</span>
              </div>
              <div class="info-row">
                <span class="label">任务ID：</span>
                <span class="value">{{ currentTask.taskId }}</span>
              </div>
              <div class="info-row">
                <span class="label">创建时间：</span>
                <span class="value">{{ formatTime(currentTask.createdAt) }}</span>
              </div>
            </div>

            <div class="progress-bar">
              <el-progress 
                :percentage="currentTask.progress" 
                :status="getProgressStatus(currentTask.status)"
                :stroke-width="20"
              />
              <div class="progress-message">{{ currentTask.message }}</div>
            </div>

            <el-timeline class="status-timeline">
              <el-timeline-item 
                :type="getTimelineType('pending')" 
                :hollow="!isStatusPassed('pending')"
              >
                任务创建
              </el-timeline-item>
              <el-timeline-item 
                :type="getTimelineType('file_uploaded')" 
                :hollow="!isStatusPassed('file_uploaded')"
              >
                文件上传完成
              </el-timeline-item>
              <el-timeline-item 
                :type="getTimelineType('ocr_processing')" 
                :hollow="!isStatusPassed('ocr_processing')"
              >
                OCR处理中
              </el-timeline-item>
              <el-timeline-item 
                :type="getTimelineType('extracting')" 
                :hollow="!isStatusPassed('extracting')"
              >
                信息提取中
              </el-timeline-item>
              <el-timeline-item 
                :type="getTimelineType('completed')" 
                :hollow="!isStatusPassed('completed')"
              >
                处理完成
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>

        <el-card v-if="currentTask && currentTask.status === 'completed'" class="result-card">
          <template #header>
            <div class="result-header">
              <span>提取完成</span>
              <el-button 
                type="primary" 
                size="small"
                @click="viewDetailedResult"
              >
                查看详情
              </el-button>
            </div>
          </template>

          <el-result
            icon="success"
            title="信息提取完成"
            :sub-title="currentTask.durationSeconds > 0 ? `耗时 ${currentTask.durationSeconds} 秒` : ''"
          >
            <template #extra>
              <el-button type="primary" @click="viewDetailedResult">
                查看详细结果
              </el-button>
              <el-button @click="startNewTask">
                继续提取
              </el-button>
            </template>
          </el-result>
        </el-card>
    </div>
    
    <!-- 提取结果详情弹窗（自定义UI） -->
    <el-dialog
      v-model="resultDialogVisible"
      title="提取结果详情"
      width="85%"
      align-center
      destroy-on-close
      @close="onResultDialogClose"
      class="extract-result-dialog"
    >
      <div v-if="extractResult" class="result-content">
        <!-- 任务信息 -->
        <el-alert
          :title="`任务ID: ${currentTask.taskId}`"
          type="success"
          :closable="false"
          class="task-alert"
        >
          <template #default>
            <div class="task-meta">
              <span>文件名: {{ currentTask.fileName }}</span>
              <span>耗时: {{ currentTask.durationSeconds || 0 }} 秒</span>
              <span>状态: {{ getTaskStatusLabel(currentTask.status) }}</span>
            </div>
          </template>
        </el-alert>

        <!-- 提取的数据 -->
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>提取的字段数据</span>
              <el-tag type="success" size="small">{{ extractedDataArray.length }} 个字段</el-tag>
            </div>
          </template>

          <div v-if="extractedDataArray.length > 0">
            <el-table 
              :data="extractedDataArray" 
              stripe
              :show-header="true"
              style="width: 100%"
              :max-height="500"
            >
              <el-table-column prop="fieldName" label="字段名称" width="200" />
              <el-table-column prop="fieldValue" label="提取值" min-width="300">
                <template #default="{ row }">
                  <span class="field-value">{{ row.fieldValue || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="未提取到任何数据" :image-size="120" />
        </el-card>

      </div>

      <div v-else class="loading-content">
        <el-skeleton :rows="8" animated />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="exportResult" type="primary">
            <el-icon><Download /></el-icon>
            导出结果
          </el-button>
          <el-button @click="startNewTask" type="success">
            <el-icon><Plus /></el-icon>
            继续提取
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- Template & AI Generator iframe dialogs -->
    <IframeDialog
      v-model="templateDialogVisible"
      :url="templateManageUrl"
      title="模板管理"
      width="90%"
      @close="onTemplateDialogClose"
    />
    
    <IframeDialog
      v-model="aiGeneratorDialogVisible"
      :url="aiGeneratorUrl"
      title="AI模板生成助手"
      width="90%"
      @close="onAIGeneratorDialogClose"
    />
    
  </DemoLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled, CircleCheck, Setting, Download, Plus, ArrowLeft } from '@element-plus/icons-vue'
import DemoLayout from '@/components/DemoLayout.vue'
import IframeDialog from '@/components/IframeDialog.vue'
import { ZHAOXIN_CONFIG } from '@/config'
import { 
  listTemplates, 
  uploadAndExtract, 
  getRuleExtractTaskStatus,
  getRuleExtractTaskResult,
  cancelRuleExtractTask
} from '@/api/ruleExtract'
import { downloadDemoDocument } from '@/api/demo'
import { extractArrayData, formatFileSize, formatTime } from '@/utils/responseHelper'

const router = useRouter()

// 文件相关
const fileList = ref([])
const selectedFile = ref(null)

// 模板相关
const loadingTemplates = ref(false)
const templates = ref([])
const selectedTemplateId = ref('')

// 提取设置
const showExtractSettings = ref(false)  // 默认隐藏文本提取设置
const extractSettings = ref({
  ignoreHeaderFooter: true,  // 默认开启忽略页眉页脚
  headerHeightPercent: 6,    // 页眉高度百分比，默认6%
  footerHeightPercent: 6,    // 页脚高度百分比，默认6%
  changeContract: false      // 是否为变更合同
})

// 任务相关
const isExtracting = ref(false)
const currentTask = ref(null)
let statusCheckTimer = null

// 弹窗状态
const templateDialogVisible = ref(false)
const aiGeneratorDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const showResultPage = ref(false)  // 是否显示结果页面（替换右侧内容）

// 提取结果数据
const extractResult = ref(null)
const currentResultTaskId = ref(null)  // 当前显示的结果任务ID

const canStartExtraction = computed(() => {
  return selectedFile.value && selectedTemplateId.value && !isExtracting.value
})

const selectedTemplateInfo = computed(() => {
  return templates.value.find(t => t.id === selectedTemplateId.value)
})

// 将提取的数据转换为表格数组格式
const extractedDataArray = computed(() => {
  if (!extractResult.value) {
    console.log('❌ extractResult 为空')
    return []
  }
  
  console.log('📊 extractResult 原始数据:', extractResult.value)
  
  // 优先使用 extractResults 数组（后端实际返回的格式）
  if (Array.isArray(extractResult.value.extractResults)) {
    console.log('✅ 使用 extractResults 数组，长度:', extractResult.value.extractResults.length)
    
    return extractResult.value.extractResults.map(item => ({
      fieldName: item.fieldName || item.field_name || '未知字段',
      fieldValue: item.extractedValue || item.extracted_value || item.value || '-'
    }))
  }
  
  // 兼容性：如果是对象格式 { extractedData: {...} }
  if (extractResult.value.extractedData && typeof extractResult.value.extractedData === 'object') {
    console.log('✅ 使用 extractedData 对象格式')
    const data = extractResult.value.extractedData
    
    return Object.keys(data).map(key => ({
      fieldName: key,
      fieldValue: data[key]
    }))
  }
  
  console.log('❌ 无法识别的数据格式')
  return []
})

// iframe URL构建（IframeDialog会自动添加embed=true参数）
const templateManageUrl = computed(() => {
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/templates`
})

const aiGeneratorUrl = computed(() => {
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/ai-generator`
})

// 结果页面URL（用于iframe显示）
const resultPageUrl = computed(() => {
  if (!currentResultTaskId.value) return ''
  try {
    const url = new URL(`${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/result/${currentResultTaskId.value}`)
    // 自动添加嵌入模式参数
    url.searchParams.set('embed', 'true')
    url.searchParams.set('hideBack', 'true')
    return url.toString()
  } catch (error) {
    console.error('❌ 构建结果页面URL失败:', error)
    return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/result/${currentResultTaskId.value}`
  }
})

const loadTemplates = async () => {
  try {
    loadingTemplates.value = true
    const res = await listTemplates({ status: 'active' })
    templates.value = extractArrayData(res)
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败：' + (error.message || '未知错误'))
    templates.value = []
  } finally {
    loadingTemplates.value = false
  }
}

const handleFileChange = (file) => {
  // Demo 版本提示用户使用演示文档
  ElMessageBox.alert(
    '本演示环境暂不支持自定义上传文档，请使用左侧的演示文档进行体验。\n\n如需使用自定义文档抽取功能，请联系我们获取完整版系统。',
    '提示',
    {
      confirmButtonText: '我知道了',
      type: 'info',
      dangerouslyUseHTMLString: false
    }
  )
  // 不设置文件，保持空白状态
  selectedFile.value = null
  fileList.value = []
}

const beforeUpload = (file) => {
  // 阻止文件上传
  return false
}

const clearFile = () => {
  selectedFile.value = null
  fileList.value = []
}

const startExtraction = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择模板')
    return
  }

  try {
    isExtracting.value = true

    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('templateId', selectedTemplateId.value)
    formData.append('ignoreHeaderFooter', String(extractSettings.value.ignoreHeaderFooter))
    formData.append('headerHeightPercent', String(extractSettings.value.headerHeightPercent))
    formData.append('footerHeightPercent', String(extractSettings.value.footerHeightPercent))

    const res = await uploadAndExtract(formData)
    
    if (res.data.code === 200) {
      const taskId = res.data.data.taskId
      ElMessage.success('任务创建成功，开始处理...')
      
      currentTask.value = {
        taskId,
        fileName: selectedFile.value.name,
        status: 'pending',
        progress: 0,
        message: '任务创建成功',
        createdAt: new Date()
      }

      startStatusPolling(taskId)
    } else {
      throw new Error(res.message || '创建任务失败')
    }
  } catch (error) {
    console.error('开始提取失败:', error)
    
    // 友好的错误提示
    let errorMessage = '开始提取失败'
    const errorText = error.message || error.response?.data?.message || ''
    
    if (errorText.includes('模板不存在') || errorText.includes('模板无效')) {
      errorMessage = '所选模板无效或未配置提取字段，请选择其他模板或前往模板管理配置该模板'
      ElMessageBox.alert(
        '该模板尚未配置任何提取字段，无法使用。\n\n解决方法：\n1. 选择其他已配置的模板\n2. 点击"模板管理"为该模板添加字段\n3. 使用"AI模板生成"快速创建新模板',
        '模板配置错误',
        {
          confirmButtonText: '我知道了',
          type: 'warning',
          dangerouslyUseHTMLString: false
        }
      )
    } else {
      errorMessage = '开始提取失败：' + errorText
      ElMessage.error(errorMessage)
    }
    
    isExtracting.value = false
  }
}

const startStatusPolling = (taskId) => {
  if (statusCheckTimer) {
    clearInterval(statusCheckTimer)
  }

  statusCheckTimer = setInterval(async () => {
    try {
      const res = await getRuleExtractTaskStatus(taskId)
      if (res.data.code === 200) {
        currentTask.value = res.data.data
        
        if (['completed', 'failed', 'cancelled'].includes(currentTask.value.status)) {
          stopStatusPolling()
          isExtracting.value = false
          
          if (currentTask.value.status === 'completed') {
            ElMessage.success('提取完成！正在跳转到结果页面...')
            // 完成后直接跳转到结果页面并加载数据
            setTimeout(() => {
              viewDetailedResult()
            }, 500)
          } else if (currentTask.value.status === 'failed') {
            ElMessage.error('提取失败：' + (currentTask.value.errorMessage || '未知错误'))
          }
        }
      }
    } catch (error) {
      console.error('查询任务状态失败', error)
    }
  }, 2000)
}

const stopStatusPolling = () => {
  if (statusCheckTimer) {
    clearInterval(statusCheckTimer)
    statusCheckTimer = null
  }
}

const cancelTask = async () => {
  if (!currentTask.value) return

  try {
    await ElMessageBox.confirm('确定要取消当前任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cancelRuleExtractTask(currentTask.value.taskId)
    ElMessage.success('任务已取消')
    stopStatusPolling()
    isExtracting.value = false
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败：' + (error.message || '未知错误'))
    }
  }
}

const viewDetailedResult = async () => {
  if (!currentTask.value?.taskId) return
  
  try {
    // 加载结果数据
    extractResult.value = null
    resultDialogVisible.value = true
    
    const res = await getRuleExtractTaskResult(currentTask.value.taskId)
    console.log('🔍 原始API响应:', res)
    console.log('🔍 res.data:', res.data)
    
    if (res.data.code === 200) {
      extractResult.value = res.data.data
      console.log('✅ 获取提取结果成功')
      console.log('📦 extractResult.value:', extractResult.value)
      console.log('📦 extractResult.value 类型:', typeof extractResult.value)
      console.log('📦 extractResult.value 的keys:', Object.keys(extractResult.value || {}))
    } else {
      throw new Error(res.data.message || '获取结果失败')
    }
  } catch (error) {
    console.error('获取提取结果失败:', error)
    ElMessage.error('获取结果失败：' + (error.message || '未知错误'))
    resultDialogVisible.value = false
  }
}

const startNewTask = () => {
  currentTask.value = null
  extractResult.value = null
  clearFile()
  selectedTemplateId.value = ''
  resultDialogVisible.value = false
}

// 导出结果
const exportResult = () => {
  if (!extractResult.value) return
  
  try {
    const jsonStr = JSON.stringify(extractResult.value, null, 2)
    const blob = new Blob([jsonStr], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `extract-result-${currentTask.value.taskId}.json`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const openTemplateManage = () => {
  templateDialogVisible.value = true
}

const onTemplateDialogClose = () => {
  console.log('模板管理弹窗已关闭')
  loadTemplates()
}

const openAIGenerator = () => {
  aiGeneratorDialogVisible.value = true
}

const onAIGeneratorDialogClose = () => {
  console.log('AI生成模板弹窗已关闭')
  loadTemplates()
}

// 返回抽取界面
const backToExtract = () => {
  console.log('返回抽取界面')
  showResultPage.value = false
  currentResultTaskId.value = null
}

// 结果页面iframe加载完成
const onResultPageLoad = () => {
  console.log('结果页面iframe加载完成')
}

// 处理演示文档选择
const handleDemoDocSelect = async (doc) => {
  console.log('📄 选择演示文档:', doc)
  
  try {
    // 如果文档关联了抽取任务ID，直接替换右侧内容区域显示后台系统的结果页面
    if (doc.taskId) {
      console.log('🔍 检测到关联任务ID，替换右侧内容显示抽取结果页面:', doc.taskId)
      ElMessage.success(`正在打开抽取结果：${doc.name}`)
      
      // 设置当前任务ID并显示结果页面
      currentResultTaskId.value = doc.taskId
      showResultPage.value = true
      
      return
    }
    
    // 以下是旧的逻辑（兼容没有taskId的情况）
    ElMessage.info('正在加载演示文档...')
    
    // 下载演示文档
    const res = await downloadDemoDocument(doc.filePath)
    
    // 创建 File 对象
    const file = new File([res.data], doc.name, { type: 'application/pdf' })
    
    // 设置选中的文件
    selectedFile.value = file
    fileList.value = [{ name: file.name, size: file.size }]
    
    // 如果文档绑定了模板，自动选择该模板
    if (doc.templateId) {
      selectedTemplateId.value = doc.templateId
      ElMessage.success(`已加载演示文档并选择模板`)
    } else {
      ElMessage.success('演示文档已加载')
    }
  } catch (error) {
    console.error('❌ 加载演示文档失败:', error)
    ElMessage.error('加载演示文档失败：' + (error.message || '未知错误'))
  }
}

// 处理模板管理
const handleManageTemplate = () => {
  console.log('⚙️ 打开模板管理')
  templateDialogVisible.value = true
}

const onResultDialogClose = () => {
  console.log('提取结果详情弹窗已关闭')
}

const getTaskStatusType = (status) => {
  const types = {
    pending: 'info',
    file_uploaded: 'info',
    ocr_processing: 'warning',
    extracting: 'warning',
    completed: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return types[status] || 'info'
}

const getTaskStatusLabel = (status) => {
  const labels = {
    pending: '等待中',
    file_uploaded: '已上传',
    ocr_processing: 'OCR中',
    extracting: '提取中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return labels[status] || status
}

const getProgressStatus = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return undefined
}

const getTimelineType = (status) => {
  if (!currentTask.value) return 'info'
  
  const statusOrder = ['pending', 'file_uploaded', 'ocr_processing', 'extracting', 'completed']
  const currentIndex = statusOrder.indexOf(currentTask.value.status)
  const targetIndex = statusOrder.indexOf(status)
  
  if (currentIndex >= targetIndex) return 'success'
  return 'info'
}

const isStatusPassed = (status) => {
  if (!currentTask.value) return false
  
  const statusOrder = ['pending', 'file_uploaded', 'ocr_processing', 'extracting', 'completed']
  const currentIndex = statusOrder.indexOf(currentTask.value.status)
  const targetIndex = statusOrder.indexOf(status)
  
  return currentIndex >= targetIndex
}

// 处理来自iframe的postMessage消息
const handleMessage = (event) => {
  // 验证消息来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    return
  }
  
  // 处理打开AI生成模板的消息
  if (event.data?.type === 'OPEN_AI_GENERATOR' && event.data?.source === 'zhaoxin-sdk') {
    console.log('🤖 收到打开AI生成模板的消息', event.data.payload)
    openAIGenerator()
  }
}

onMounted(() => {
  loadTemplates()
  // 添加 postMessage 监听器
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

onUnmounted(() => {
  stopStatusPolling()
  // 移除 postMessage 监听器
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>

<style scoped lang="scss">
@import '@/styles/demo-common.scss';

/* 结果页面包装器（替换右侧内容） */
.result-page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  
  .result-page-header {
    height: 56px;
    padding: 0 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    border-bottom: 1px solid #e4e7ed;
    background: #fafafa;
    flex-shrink: 0;
    
    .back-button {
      font-size: 14px;
      padding: 8px 16px;
      
      .el-icon {
        margin-right: 4px;
      }
    }
    
    .result-page-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .result-page-iframe {
    flex: 1;
    width: 100%;
    border: none;
    background: #ffffff;
  }
}

.extract-content-wrapper {
  padding: $spacing-lg;
  height: 100%;
  overflow-y: auto;
  background: $bg-page;

  /* 主内容卡片 */
  .main-card {
    @include main-card;
    max-width: 1400px;
    margin-bottom: $spacing-lg;
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.03), 0 8px 16px rgba(0, 0, 0, 0.03);
    }

    /* 步骤指示器区域 */
    .steps-section {
      @include steps-section;
    }

    /* 文件上传区域 */
    .upload-section {
        padding: 40px 20px;
        
        .centered-upload {
          :deep(.el-upload) {
            width: 100%;
            
            .el-upload-dragger {
              width: 100%;
              padding: 60px 40px;
              border: 2px dashed #d9d9d9;
              border-radius: 8px;
              background: #fafafa;
              transition: all 0.3s;

              &:hover {
                border-color: #409eff;
                background: #f0f9ff;
              }
            }
          }

          .upload-placeholder {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 20px;

            .upload-icon {
              color: #c0c4cc;
            }

            .upload-text {
              text-align: center;

              .main-text {
                font-size: 16px;
                color: #606266;
                margin: 0 0 8px 0;
                font-weight: 500;
              }

              .sub-text {
                font-size: 14px;
                color: #909399;
                margin: 0;
              }
            }
          }

          .file-selected {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
            padding: 20px;

            .file-icon-wrapper {
              position: relative;
              
              .success-badge {
                position: absolute;
                right: -8px;
                bottom: -8px;
                background: white;
                border-radius: 50%;
              }
            }

            .file-name {
              font-size: 15px;
              color: #303133;
              font-weight: 500;
              margin: 8px 0 4px 0;
              text-align: center;
            }

            .file-size {
              font-size: 13px;
              color: #909399;
              margin: 0 0 8px 0;
            }

            .reselect-btn {
              font-size: 14px;
            }
          }
        }
      }

      /* 模板选择区域 */
      .template-selection {
        padding: 20px 40px;
        border-top: 1px solid #f0f0f0;

        .selection-row {
          display: flex;
          align-items: center;
          gap: 32px;
        }

        .template-select {
          width: 276px;
          flex-shrink: 0;

          :deep(.el-select__wrapper) {
            height: 30px;
            min-height: 30px;
          }

          :deep(.el-select__input) {
            height: 28px;
            line-height: 28px;
          }

          :deep(.el-select__placeholder) {
            line-height: 28px;
          }

          :deep(.el-select__suffix) {
            height: 28px;
            display: flex;
            align-items: center;
          }

          .template-option {
            display: flex;
            align-items: center;
            justify-content: space-between;
          }
        }

        .header-footer-config {
          display: flex;
          align-items: center;
          gap: 16px;
          flex: 1;

          :deep(.el-checkbox) {
            font-size: 14px;
            color: #606266;
            white-space: nowrap;
          }

          .percentage-inputs {
            display: flex;
            gap: 24px;
            align-items: center;

            .input-group {
              display: flex;
              align-items: center;
              gap: 8px;

              label {
                font-size: 13px;
                color: #606266;
                white-space: nowrap;
                min-width: 32px;
              }

              .percentage-input {
                width: 100px;

                :deep(.el-input__wrapper) {
                  padding: 1px 11px;
                }

                :deep(.el-input__inner) {
                  text-align: center;
                  font-size: 14px;
                  padding: 0 8px;
                }
              }

              .unit {
                font-size: 13px;
                color: #909399;
              }
            }
          }
        }
      }

      /* 提取选项 */
      .extract-options {
        padding: 20px 40px;
        border-top: 1px solid #f0f0f0;
        display: flex;
        justify-content: center;

        :deep(.el-radio-group) {
          display: flex;
          gap: 40px;
        }
      }

      /* 操作按钮 */
      .action-buttons {
        padding: 30px 40px;
        border-top: 1px solid #f0f0f0;
        display: flex;
        justify-content: center;

        .primary-btn {
          min-width: 160px;
          font-size: 15px;
          padding: 12px 32px;
          border-radius: 6px;
        }
      }
    }

  /* 结果展示区 */
  .result-area {
    margin: 0 auto;
    
    .progress-card, .result-card {
      animation: fadeIn 0.3s ease-out;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      margin-top: 20px;
      
      .progress-header, .result-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
      }

      .progress-content {
        .task-info {
          background: #f0f9ff;
          padding: 20px;
          border-radius: 8px;
          margin-bottom: 24px;
          border-left: 4px solid #409eff;

          .info-row {
            display: flex;
            margin-bottom: 12px;
            font-size: 14px;

            &:last-child {
              margin-bottom: 0;
            }

            .label {
              color: #909399;
              min-width: 90px;
              font-weight: 500;
            }

            .value {
              color: #303133;
              font-weight: 500;
              flex: 1;
              word-break: break-all;
            }
          }
        }

        .progress-bar {
          margin-bottom: 24px;

          .progress-message {
            text-align: center;
            margin-top: 12px;
            color: #606266;
            font-size: 14px;
            font-weight: 500;
          }
        }

        .status-timeline {
          padding: 20px;
          background: #f5f7fa;
          border-radius: 8px;

          :deep(.el-timeline-item__content) {
            color: #606266;
            font-size: 14px;
          }
        }
      }
    }

    .result-card {
      :deep(.el-result) {
        padding: 40px;
      }
    }
  }
}

/* 提取结果弹窗样式 */
.extract-result-dialog {
  :deep(.el-dialog) {
    max-height: 90vh;
    display: flex;
    flex-direction: column;
  }
  
  :deep(.el-dialog__body) {
    flex: 1;
    overflow-y: auto;
    max-height: calc(90vh - 120px); /* 减去header和footer的高度 */
  }
  
  .result-content {
    .task-alert {
      margin-bottom: 20px;
      
      .task-meta {
        display: flex;
        gap: 24px;
        font-size: 13px;
        color: #606266;
        flex-wrap: wrap;
        
        span {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
    
    .data-card {
      margin-bottom: 16px;
      
      .card-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        
        .el-icon {
          font-size: 18px;
          color: #409eff;
        }
      }
      
      .field-value {
        color: #303133;
        font-weight: 500;
        word-break: break-all;
      }
    }
    
  }
  
  .loading-content {
    padding: 20px;
  }
  
  .dialog-footer {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .extract-main-page {
    padding: 16px;

    .main-content-wrapper {
      .main-card {
        .steps-section {
          padding: 20px;
          
          :deep(.el-step__title) {
            font-size: 14px;
          }
          
          :deep(.el-step__description) {
            font-size: 12px;
          }
        }

        .upload-section {
          padding: 20px 10px;
        }

        .template-selection,
        .extract-options,
        .action-buttons {
          padding: 20px;
        }

        .template-selection {
          .selection-row {
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
          }

          .template-select {
            width: 100%;
          }

          .header-footer-config {
            width: 100%;
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;

            .percentage-inputs {
              flex-direction: column;
              gap: 12px;
              align-items: flex-start;
              width: 100%;
            }
          }
        }

        .action-buttons {
          .primary-btn {
            width: 100%;
          }
        }
      }
    }
  }
}
</style>


