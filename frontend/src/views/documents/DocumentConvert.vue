<template>
  <div class="document-convert">
    <PageHeader 
      title="文档格式转换" 
      description="将Word、Excel、PPT等文档快速转换为PDF格式，保留原文档格式与样式。"
      :icon="DocumentCopy"
      tag="格式转换"
      tag-type="info"
    />

    <el-card class="convert-card mb12">
      <template #header>
        <div class="card-header">
          <span>文档转换</span>
          <el-tag type="info" size="small">格式转换</el-tag>
        </div>
      </template>
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-container"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          :accept="acceptedFormats"
          :show-file-list="false"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持格式：Word (.doc, .docx)、Excel (.xls, .xlsx)、PowerPoint (.ppt, .pptx)
              <br />
              文件大小：不超过50MB
            </div>
          </template>
        </el-upload>

        <!-- 已选文件显示 -->
        <div v-if="selectedFile" class="selected-file">
          <div class="file-info">
            <el-icon class="file-icon"><Document /></el-icon>
            <div class="file-details">
              <div class="file-name">{{ selectedFile.name }}</div>
              <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
            </div>
            <el-button 
              type="danger" 
              size="small" 
              :icon="Delete" 
              circle 
              @click="removeFile"
            />
          </div>
        </div>

        <!-- 转换按钮 -->
        <div class="action-buttons">
          <el-button
            type="primary"
            size="large"
            :disabled="!selectedFile || converting"
            :loading="converting"
            @click="startConvert"
          >
            <el-icon><VideoPlay /></el-icon>
            {{ converting ? '转换中...' : '开始转换' }}
          </el-button>
        </div>
      </div>

      <!-- 转换进度 -->
      <div v-if="converting" class="progress-section">
        <el-progress 
          :percentage="progressPercent" 
          :status="progressStatus"
          :indeterminate="true"
        />
        <div class="progress-text">{{ progressText }}</div>
      </div>

      <!-- 转换结果 -->
      <div v-if="convertResult" class="result-section">
        <el-result
          :icon="convertResult.success ? 'success' : 'error'"
          :title="convertResult.success ? '转换成功' : '转换失败'"
          :sub-title="convertResult.message"
        >
          <template #extra>
            <div v-if="convertResult.success" class="download-area">
              <el-button 
                type="primary" 
                size="large"
                :icon="Download"
                @click="downloadPdf"
              >
                下载PDF文件
              </el-button>
              <div class="file-name-hint">文件名：{{ convertResult.originalName }}</div>
            </div>
            <el-button 
              v-else
              type="primary"
              @click="resetConvert"
            >
              重新转换
            </el-button>
          </template>
        </el-result>
      </div>
    </el-card>

    <!-- 使用说明 -->
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <el-icon><InfoFilled /></el-icon>
          <span>使用说明</span>
        </div>
      </template>
      <div class="info-content">
        <h3>支持的文件格式</h3>
        <ul>
          <li><strong>Word文档：</strong>.doc, .docx</li>
          <li><strong>Excel表格：</strong>.xls, .xlsx</li>
          <li><strong>PowerPoint演示：</strong>.ppt, .pptx</li>
        </ul>

        <h3>转换说明</h3>
        <ul>
          <li>转换后的PDF文件会保留原文档的格式和样式</li>
          <li>转换完成后请及时下载，服务器会不定期清理文件</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Delete, VideoPlay, Download, InfoFilled, DocumentCopy } from '@element-plus/icons-vue'
import type { UploadInstance, UploadProps } from 'element-plus'
import convertApi from '@/api/convert'
import { PageHeader } from '@/components/common'

// 组件引用
const uploadRef = ref<UploadInstance>()

// 数据状态
const selectedFile = ref<File | null>(null)
const converting = ref(false)
const progressPercent = ref(0)
const progressStatus = ref<'success' | 'warning' | 'exception'>('warning')
const progressText = ref('')
const convertResult = ref<{
  success: boolean
  message: string
  downloadUrl?: string
  fileName?: string
  originalName?: string
} | null>(null)

// 接受的文件格式
const acceptedFormats = '.doc,.docx,.xls,.xlsx,.ppt,.pptx'

/**
 * 文件选择变化处理
 */
const handleFileChange: UploadProps['onChange'] = (uploadFile) => {
  if (!uploadFile.raw) return
  
  // 验证文件大小（50MB）
  const maxSize = 50 * 1024 * 1024
  if (uploadFile.raw.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return
  }
  
  // 验证文件格式
  const fileName = uploadFile.raw.name
  const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  const validExtensions = ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx']
  
  if (!validExtensions.includes(extension)) {
    ElMessage.error('不支持的文件格式，请上传Word、Excel或PPT文档')
    return
  }
  
  selectedFile.value = uploadFile.raw
  convertResult.value = null
  progressPercent.value = 0
}

/**
 * 移除已选文件
 */
const removeFile = () => {
  selectedFile.value = null
  convertResult.value = null
  uploadRef.value?.clearFiles()
}

/**
 * 开始转换
 */
const startConvert = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要转换的文件')
    return
  }
  
  converting.value = true
  convertResult.value = null
  progressPercent.value = 0
  progressStatus.value = 'warning'
  progressText.value = '正在上传文件...'
  
  try {
    // 模拟进度更新
    const progressInterval = setInterval(() => {
      if (progressPercent.value < 90) {
        progressPercent.value += 10
        if (progressPercent.value > 30) {
          progressText.value = '正在转换文档格式...'
        }
      }
    }, 1000)
    
    // 调用转换API
    const response = await convertApi.convertToPdf(selectedFile.value)
    
    clearInterval(progressInterval)
    progressPercent.value = 100
    progressStatus.value = 'success'
    progressText.value = '转换完成'
    
    // 处理转换结果
    // 响应拦截器返回的格式：{ data: { code: 200, message: "...", data: {...} } }
    const resultData = response.data.data
    if (resultData && resultData.success) {
      convertResult.value = {
        success: true,
        message: '文档已成功转换为PDF格式',
        downloadUrl: resultData.downloadUrl,
        fileName: resultData.fileName,
        originalName: resultData.originalName
      }
      ElMessage.success('转换成功！')
    } else {
      // 这个分支理论上不会执行（响应拦截器会在code!=200时抛出错误）
      convertResult.value = {
        success: false,
        message: '转换失败，请重试'
      }
      progressStatus.value = 'exception'
      ElMessage.error(convertResult.value.message)
    }
    
  } catch (error: any) {
    progressPercent.value = 100
    progressStatus.value = 'exception'
    progressText.value = '转换失败'
    
    const errorMessage = error.response?.data?.message || error.message || '转换失败，请检查网络连接或稍后重试'
    
    convertResult.value = {
      success: false,
      message: errorMessage
    }
    
    ElMessage.error(errorMessage)
    
  } finally {
    converting.value = false
  }
}

/**
 * 下载PDF文件
 */
const downloadPdf = () => {
  if (!convertResult.value?.downloadUrl) return
  
  const link = document.createElement('a')
  link.href = convertResult.value.downloadUrl
  link.download = convertResult.value.originalName || 'converted.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  ElMessage.success('开始下载PDF文件')
}

/**
 * 重置转换
 */
const resetConvert = () => {
  convertResult.value = null
  progressPercent.value = 0
  progressStatus.value = 'warning'
  progressText.value = ''
}

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped lang="scss">
.document-convert {
  padding: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.mb12 {
  margin-bottom: 12px;
}

.mt12 {
  margin-top: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.convert-card {
  margin-bottom: 20px;
  
  .upload-section {
    padding: 20px;
  }
  
  .upload-container {
    margin-bottom: 20px;
    
    :deep(.el-upload-dragger) {
      padding: 40px 20px;
      border: 2px dashed #dcdfe6;
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        border-color: #409eff;
      }
    }
    
    .el-icon--upload {
      font-size: 67px;
      color: #c0c4cc;
      margin-bottom: 16px;
    }
    
    .el-upload__text {
      color: #606266;
      font-size: 14px;
      
      em {
        color: #409eff;
        font-style: normal;
      }
    }
    
    :deep(.el-upload__tip) {
      margin-top: 12px;
      font-size: 12px;
      color: #909399;
      line-height: 1.6;
    }
  }
  
  .selected-file {
    margin: 20px 0;
    
    .file-info {
      display: flex;
      align-items: center;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
      border: 1px solid #e4e7ed;
      
      .file-icon {
        font-size: 32px;
        color: #409eff;
        margin-right: 12px;
      }
      
      .file-details {
        flex: 1;
        
        .file-name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 4px;
        }
        
        .file-size {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
  
  .action-buttons {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    
    .el-button {
      min-width: 160px;
      height: 44px;
      font-size: 16px;
    }
  }
  
  .progress-section {
    padding: 30px 40px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-top: 20px;
    
    .progress-text {
      text-align: center;
      margin-top: 12px;
      font-size: 14px;
      color: #606266;
    }
  }
  
  .result-section {
    margin-top: 20px;
    
    .download-area {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      
      .file-name-hint {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.info-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    
    .el-icon {
      color: #409eff;
    }
  }
  
  .info-content {
    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 20px 0 12px 0;
      
      &:first-child {
        margin-top: 0;
      }
    }
    
    ul {
      margin: 0;
      padding-left: 24px;
      
      li {
        font-size: 14px;
        color: #606266;
        line-height: 1.8;
        margin-bottom: 6px;
        
        strong {
          color: #303133;
        }
      }
    }
  }
}
</style>

