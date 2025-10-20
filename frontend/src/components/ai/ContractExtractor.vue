<template>
  <div class="contract-extractor">
    <!-- 文件上传区域 -->
    <div v-if="!taskId" class="upload-section">
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :accept="'.pdf,.doc,.docx'"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <p class="upload-title">将文件拖到此处，或<em>点击上传</em></p>
          <p class="upload-hint">支持 PDF、Word 格式文件</p>
        </div>
      </el-upload>

      <!-- 选项配置 -->
      <div v-if="selectedFile" class="options-section">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>提取选项</span>
            </div>
          </template>

          <el-form :model="options" label-width="140px">
            <el-form-item label="忽略页眉页脚">
              <el-switch v-model="options.ignoreHeaderFooter" />
            </el-form-item>
            
            <el-form-item v-if="options.ignoreHeaderFooter" label="页眉高度(%)">
              <el-input-number 
                v-model="options.headerHeightPercent" 
                :min="0" 
                :max="30" 
                :step="1"
              />
            </el-form-item>

            <el-form-item v-if="options.ignoreHeaderFooter" label="页脚高度(%)">
              <el-input-number 
                v-model="options.footerHeightPercent" 
                :min="0" 
                :max="30" 
                :step="1"
              />
            </el-form-item>
          </el-form>

          <div class="action-buttons">
            <el-button type="primary" @click="startExtraction" :loading="uploading">
              <el-icon><DocumentChecked /></el-icon>
              开始提取
            </el-button>
            <el-button @click="clearFile">清除</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 提取进度 -->
    <div v-else-if="extracting" class="extraction-progress">
      <el-card shadow="hover">
        <div class="progress-content">
          <ConcentricLoader color="#1677ff" :size="52" :text="statusMessage" />
          <el-progress 
            :percentage="progress" 
            :stroke-width="8"
            :color="progressColor"
            class="progress-bar"
          />
          <p class="progress-text">{{ statusMessage }}</p>
          <p class="progress-hint">{{ progressHint }}</p>
        </div>
      </el-card>
    </div>

    <!-- 提取结果 -->
    <div v-else-if="result" class="extraction-result">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>提取结果</span>
            <el-button type="primary" size="small" @click="downloadResult">
              <el-icon><Download /></el-icon>
              下载结果
            </el-button>
          </div>
        </template>

        <div class="result-content">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="文本内容" name="text">
              <el-input
                v-model="result.text"
                type="textarea"
                :rows="20"
                readonly
                class="result-textarea"
              />
            </el-tab-pane>

            <el-tab-pane label="结构化数据" name="structured">
              <pre class="json-preview">{{ JSON.stringify(result.structured, null, 2) }}</pre>
            </el-tab-pane>

            <el-tab-pane label="元数据" name="metadata">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="文件名">{{ result.fileName }}</el-descriptions-item>
                <el-descriptions-item label="页数">{{ result.pageCount }}</el-descriptions-item>
                <el-descriptions-item label="文件大小">{{ formatFileSize(result.fileSize) }}</el-descriptions-item>
                <el-descriptions-item label="提取时间">{{ result.extractTime }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="result-actions">
          <el-button type="primary" @click="resetExtractor">提取新文件</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, DocumentChecked, Download } from '@element-plus/icons-vue'
import type { UploadFile, UploadInstance } from 'element-plus'
import ConcentricLoader from './ConcentricLoader.vue'

// Refs
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<UploadFile | null>(null)
const taskId = ref<string>('')
const uploading = ref(false)
const extracting = ref(false)
const progress = ref(0)
const statusMessage = ref('')
const progressHint = ref('')
const result = ref<any>(null)
const activeTab = ref('text')

// 提取选项
const options = ref({
  ignoreHeaderFooter: true,
  headerHeightPercent: 12,
  footerHeightPercent: 12
})

// 进度条颜色
const progressColor = computed(() => {
  if (progress.value < 30) return '#f56c6c'
  if (progress.value < 70) return '#e6a23c'
  return '#67c23a'
})

// 处理文件选择
const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file
}

// 清除文件
const clearFile = () => {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

// 开始提取
const startExtraction = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  extracting.value = true
  progress.value = 0
  statusMessage.value = '正在上传文件...'

  try {
    // TODO: 实现实际的文件上传和提取逻辑
    // 这里是模拟代码
    await simulateExtraction()
    
    ElMessage.success('提取完成')
  } catch (error: any) {
    ElMessage.error(error.message || '提取失败')
    resetExtractor()
  } finally {
    uploading.value = false
  }
}

// 模拟提取过程（实际应该调用API）
const simulateExtraction = async () => {
  const stages = [
    { progress: 20, message: '正在上传文件...', hint: '文件传输中' },
    { progress: 40, message: '正在解析文档...', hint: 'OCR识别中' },
    { progress: 60, message: '正在提取信息...', hint: '智能分析中' },
    { progress: 80, message: '正在处理数据...', hint: '结构化处理中' },
    { progress: 100, message: '提取完成', hint: '所有处理已完成' }
  ]

  for (const stage of stages) {
    await new Promise(resolve => setTimeout(resolve, 1000))
    progress.value = stage.progress
    statusMessage.value = stage.message
    progressHint.value = stage.hint
  }

  // 模拟结果
  result.value = {
    text: '这是提取的文本内容...\n\n（实际内容将在实现API调用后显示）',
    structured: {
      title: '合同标题',
      parties: ['甲方', '乙方'],
      date: '2024-10-18',
      terms: []
    },
    fileName: selectedFile.value?.name || '',
    pageCount: 10,
    fileSize: selectedFile.value?.size || 0,
    extractTime: new Date().toLocaleString('zh-CN')
  }

  extracting.value = false
}

// 下载结果
const downloadResult = () => {
  if (!result.value) return
  
  const blob = new Blob([result.value.text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `提取结果_${new Date().getTime()}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

// 重置提取器
const resetExtractor = () => {
  taskId.value = ''
  extracting.value = false
  progress.value = 0
  statusMessage.value = ''
  progressHint.value = ''
  result.value = null
  clearFile()
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 暴露方法供父组件调用
defineExpose({
  resetExtractor,
  startExtraction
})
</script>

<style scoped>
.contract-extractor {
  width: 100%;
  min-height: 500px;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-area {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  font-size: 64px;
  color: #1677ff;
  margin-bottom: 16px;
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.upload-title em {
  color: #1677ff;
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 14px;
  color: #999;
}

.options-section {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.extraction-progress {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 40px;
}

.progress-bar {
  width: 100%;
  max-width: 400px;
}

.progress-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.progress-hint {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.extraction-result {
  width: 100%;
}

.result-content {
  min-height: 400px;
}

.result-textarea {
  font-family: 'Courier New', monospace;
}

.json-preview {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  font-size: 14px;
  overflow-x: auto;
  max-height: 600px;
}

.result-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>

