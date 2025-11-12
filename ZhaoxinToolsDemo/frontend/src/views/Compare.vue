<template>
  <DemoLayout category="compare" @doc-select="handleDemoDocSelect" @new-task="handleNewTask">
    <div class="compare-content-wrapper">
      <el-card class="main-card">
        <!-- 顶部说明 -->
        <div class="steps-section">
          <el-alert
            title="文件上传"
            description="上传两份 PDF 文档进行智能比对分析（仅支持 PDF 格式，最大 50MB）"
            type="info"
            :closable="false"
            center
          />
        </div>


        <!-- 文件上传区域 -->
        <div class="upload-section">
          <div class="upload-grid">
            <!-- 旧文件上传 -->
            <div class="upload-item">
              <div class="upload-label">
                <el-icon><DocumentCopy /></el-icon>
                <span>基准文件（旧版本）</span>
              </div>
              <el-upload
                drag
                :show-file-list="false"
                :before-upload="() => false"
                :on-change="handleOldFileChange"
                accept=".pdf"
                class="file-uploader"
              >
                <div v-if="!oldFile" class="upload-placeholder">
                  <el-icon :size="60" class="upload-icon">
                    <UploadFilled />
                  </el-icon>
                  <div class="upload-text">
                    <p class="main-text">点击或拖拽文件到此处</p>
                    <p class="sub-text">仅支持 PDF 格式，最大 50MB</p>
                  </div>
                </div>
                <div v-else class="file-selected" @click.stop="">
                  <div class="file-icon-wrapper">
                    <el-icon :size="50" color="#67C23A">
                      <Document />
                    </el-icon>
                    <el-icon :size="20" class="success-badge" color="#67C23A">
                      <CircleCheck />
                    </el-icon>
                  </div>
                  <p class="file-name">{{ oldFile.name }}</p>
                  <p class="file-size">大小: {{ formatFileSize(oldFile.size) }}</p>
                  <el-button 
                    text 
                    type="primary" 
                    @click.stop="removeOldFile"
                    class="reselect-btn"
                  >
                    重新选择
                  </el-button>
                </div>
              </el-upload>
            </div>

            <!-- 比对箭头 -->
            <div class="compare-arrow">
              <el-icon :size="32" color="#409EFF">
                <Right />
              </el-icon>
            </div>

            <!-- 新文件上传 -->
            <div class="upload-item">
              <div class="upload-label">
                <el-icon><Document /></el-icon>
                <span>比对文件（新版本）</span>
              </div>
              <el-upload
                drag
                :show-file-list="false"
                :before-upload="() => false"
                :on-change="handleNewFileChange"
                accept=".pdf"
                class="file-uploader"
              >
                <div v-if="!newFile" class="upload-placeholder">
                  <el-icon :size="60" class="upload-icon">
                    <UploadFilled />
                  </el-icon>
                  <div class="upload-text">
                    <p class="main-text">点击或拖拽文件到此处</p>
                    <p class="sub-text">仅支持 PDF 格式，最大 50MB</p>
                  </div>
                </div>
                <div v-else class="file-selected" @click.stop="">
                  <div class="file-icon-wrapper">
                    <el-icon :size="50" color="#67C23A">
                      <Document />
                    </el-icon>
                    <el-icon :size="20" class="success-badge" color="#67C23A">
                      <CircleCheck />
                    </el-icon>
                  </div>
                  <p class="file-name">{{ newFile.name }}</p>
                  <p class="file-size">大小: {{ formatFileSize(newFile.size) }}</p>
                  <el-button 
                    text 
                    type="primary" 
                    @click.stop="removeNewFile"
                    class="reselect-btn"
                  >
                    重新选择
                  </el-button>
                </div>
              </el-upload>
            </div>
          </div>
        </div>


        <!-- 比对选项 -->
        <div class="compare-options">
          <el-checkbox v-model="removeWatermark" size="large">
            去除水印
          </el-checkbox>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button 
            size="large"
            @click="resetForm"
            :disabled="comparing"
          >
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
          <el-button 
            type="primary"
            size="large"
            class="primary-btn"
            :disabled="!canSubmit || comparing"
            :loading="comparing"
            @click="handleSubmit"
          >
            <el-icon v-if="!comparing"><VideoPlay /></el-icon>
            {{ comparing ? '比对中...' : '开始比对' }}
          </el-button>
        </div>
      </el-card>
    </div>


    <!-- 进度展示区 -->
    <div v-if="comparing" class="result-area">
      <el-card class="progress-card">
        <template #header>
          <div class="progress-header">
            <span>比对进度</span>
            <el-tag type="info">处理中</el-tag>
          </div>
        </template>
        <div class="progress-content">
          <div class="task-info">
            <div class="info-row">
              <span class="label">基准文件:</span>
              <span class="value">{{ oldFile?.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">比对文件:</span>
              <span class="value">{{ newFile?.name }}</span>
            </div>
          </div>
          <div class="progress-bar">
            <el-progress 
              :percentage="progress" 
              :stroke-width="24"
              :text-inside="true"
            />
            <p class="progress-message">{{ statusText }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="result-area">
      <el-alert
        title="比对失败"
        :description="errorMessage"
        type="error"
        :closable="true"
        @close="error = false"
        show-icon
      />
    </div>

    <!-- 结果展示区 -->
    <div v-if="success" class="result-area">
      <el-card class="result-card">
        <template #header>
          <div class="result-header">
            <span>比对完成</span>
            <el-tag type="success">已完成</el-tag>
          </div>
        </template>
        <el-result icon="success" title="文档比对成功">
          <template #sub-title>
            <div class="result-stats">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="比对时长">
                  {{ taskDuration }}
                </el-descriptions-item>
                <el-descriptions-item label="差异数量">
                  <el-tag type="warning">{{ taskDifferenceCount }} 处</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
          <template #extra>
            <div class="result-actions">
              <el-button type="primary" size="large" @click="viewResult">
                <el-icon><View /></el-icon>
                查看详细结果
              </el-button>
              <el-button size="large" @click="downloadComparisonResult">
                <el-icon><Download /></el-icon>
                下载比对报告
              </el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>
  </DemoLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  View, Back, UploadFilled, Document, DocumentCopy, CircleCheck, 
  Right, RefreshLeft, VideoPlay, Download 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DemoLayout from '@/components/DemoLayout.vue'
import compareApi from '@/api/compare'
import { downloadDemoDocument } from '@/api/demo'
import { pollTaskStatus } from '@/utils/compareHelper'

const router = useRouter()

// 文件状态
const oldFile = ref(null)
const newFile = ref(null)
const dragOverOld = ref(false)
const dragOverNew = ref(false)

// 选项
const removeWatermark = ref(false)

// 比对状态
const comparing = ref(false)
const progress = ref(0)
const statusText = ref('')
const currentTaskId = ref(null)

// 结果状态
const success = ref(false)
const error = ref(false)
const errorMessage = ref('')

// 任务统计数据（使用 API 返回的真实数据）
const taskStartTime = ref(null)  // ISO 时间字符串
const taskEndTime = ref(null)    // ISO 时间字符串
const taskDifferenceCount = ref(0)

// 计算属性
const canSubmit = computed(() => {
  return oldFile.value && newFile.value && !comparing.value
})

// 计算任务时长（使用 API 返回的时间）
const taskDuration = computed(() => {
  if (!taskStartTime.value || !taskEndTime.value) return '-'
  
  // 将 ISO 时间字符串转为时间戳
  const startMs = new Date(taskStartTime.value).getTime()
  const endMs = new Date(taskEndTime.value).getTime()
  const durationMs = endMs - startMs
  
  if (durationMs < 1000) {
    return '<1秒'
  } else if (durationMs < 60000) {
    return `${Math.round(durationMs / 1000)}秒`
  } else if (durationMs < 3600000) {
    const minutes = Math.floor(durationMs / 60000)
    const seconds = Math.round((durationMs % 60000) / 1000)
    return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分钟`
  } else {
    const hours = Math.floor(durationMs / 3600000)
    const minutes = Math.round((durationMs % 3600000) / 60000)
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  }
})

// 处理旧文件选择
const handleOldFileChange = (uploadFile) => {
  const file = uploadFile.raw
  if (file) {
    validateAndSetFile(file, 'old')
  }
}

// 处理新文件选择
const handleNewFileChange = (uploadFile) => {
  const file = uploadFile.raw
  if (file) {
    validateAndSetFile(file, 'new')
  }
}

// 验证并设置文件
const validateAndSetFile = (file, type) => {
  // 验证文件大小 (50MB)
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return
  }
  
  // 验证文件类型 - 仅支持 PDF
  const validTypes = ['application/pdf']
  const validExtensions = ['.pdf']
  const fileName = file.name.toLowerCase()
  const hasValidExtension = validExtensions.some(ext => fileName.endsWith(ext))
  
  if (!validTypes.includes(file.type) && !hasValidExtension) {
    ElMessage.error('仅支持 PDF 格式')
    return
  }
  
  if (type === 'old') {
    oldFile.value = file
  } else {
    newFile.value = file
  }
}

// 移除文件
const removeOldFile = () => {
  oldFile.value = null
}

const removeNewFile = () => {
  newFile.value = null
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 提交比对
const handleSubmit = async () => {
  if (!canSubmit.value) return
  
  try {
    // 重置状态
    comparing.value = true
    progress.value = 0
    statusText.value = '正在上传文件...'
    error.value = false
    success.value = false
    currentTaskId.value = null
    taskStartTime.value = null
    taskEndTime.value = null
    taskDifferenceCount.value = 0
    
    console.log('📤 开始上传文件...')
    
    // 保存原始文件名（从File对象直接获取）
    const originalOldFileName = oldFile.value.name
    const originalNewFileName = newFile.value.name
    
    // 1. 上传旧文件
    progress.value = 5
    const oldFileResult = await compareApi.uploadFile(oldFile.value)
    const oldFileUrl = oldFileResult.data.data.fileUrl
    console.log('✅ 旧文件上传成功:', oldFileUrl, '原始文件名:', originalOldFileName)
    
    // 2. 上传新文件
    progress.value = 15
    statusText.value = '正在上传新文件...'
    const newFileResult = await compareApi.uploadFile(newFile.value)
    const newFileUrl = newFileResult.data.data.fileUrl
    console.log('✅ 新文件上传成功:', newFileUrl, '原始文件名:', originalNewFileName)
    
    // 3. 提交比对任务（传递文件URL和原始文件名到Demo后端）
    progress.value = 20
    statusText.value = '正在提交比对任务...'
    const result = await compareApi.submitCompare(
      oldFileUrl, 
      newFileUrl, 
      removeWatermark.value,
      originalOldFileName,
      originalNewFileName
    )
    const taskId = result.data
    currentTaskId.value = taskId
    
    console.log('✅ 任务提交成功，taskId:', taskId)
    console.log('📝 原始文件名已发送到后端: oldFileName={}, newFileName={}', originalOldFileName, originalNewFileName)
    
    // 4. 提交成功后直接跳转到结果页面
    ElMessage.success('任务提交成功！正在跳转到结果页面...')
    setTimeout(() => {
      router.push(`/compare/result/${taskId}`)
    }, 500)
    
  } catch (err) {
    console.error('❌ 比对失败:', err)
    comparing.value = false
    error.value = true
    errorMessage.value = err.message || '未知错误'
  }
}

// 重置表单
const resetForm = () => {
  oldFile.value = null
  newFile.value = null
  removeWatermark.value = false
  comparing.value = false
  progress.value = 0
  statusText.value = ''
  error.value = false
  success.value = false
  currentTaskId.value = null
  taskStartTime.value = null
  taskEndTime.value = null
  taskDifferenceCount.value = 0
}

// 查看结果
const viewResult = () => {
  if (currentTaskId.value) {
    // 在新窗口打开结果页
    const resultUrl = `${window.location.origin}/compare/result/${currentTaskId.value}`
    window.open(resultUrl, '_blank')
  }
}

// 下载比对结果
const downloadComparisonResult = async () => {
  if (!currentTaskId.value) return
  
  try {
    console.log('📥 下载比对结果:', currentTaskId.value)
    
    // 调用导出API，同时导出 doc 和 html 格式（打包成 zip）
    const response = await compareApi.exportReport(currentTaskId.value, ['doc', 'html'])
    
    // 创建 Blob 对象
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/zip'
    })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `comparison-result-${currentTaskId.value}.zip`
    document.body.appendChild(link)
    link.click()
    
    // 清理
    window.URL.revokeObjectURL(url)
    document.body.removeChild(link)
    
    console.log('✅ 下载成功')
  } catch (error) {
    console.error('❌ 下载失败:', error)
    ElMessage.error('下载失败: ' + error.message)
  }
}

// 演示文档选择器（用于比对功能）
const handleDemoDocSelect = async (doc) => {
  console.log('📄 选择演示文档:', doc)
  
  try {
    // 如果文档关联了比对任务ID，直接在新标签页中打开结果页面显示历史比对结果
    if (doc.taskId) {
      console.log('🔍 检测到关联任务ID，在新标签页打开结果页面:', doc.taskId)
      ElMessage.success(`正在新标签页打开比对结果：${doc.name}`)
      
      // 构建完整的URL（包含域名和端口）
      const resultUrl = `${window.location.origin}/compare/result/${doc.taskId}`
      window.open(resultUrl, '_blank')
      
      return
    }
    
    // 以下是旧的逻辑（兼容没有taskId的情况）
    // 比对功能的文档路径用 | 分隔，表示一对文档
    if (!doc.filePath) {
      ElMessage.warning('该演示文档没有关联文件或任务')
      return
    }
    
    const filePaths = doc.filePath.split('|')
    
    if (filePaths.length === 2) {
      // 成对加载两份文档
      ElMessage.info('正在加载文档对...')
      
      // 下载旧版本（原始版）
      const oldRes = await downloadDemoDocument(filePaths[0])
      const oldFileName = filePaths[0].substring(filePaths[0].lastIndexOf('/') + 1)
      oldFile.value = new File([oldRes.data], oldFileName, { type: 'application/pdf' })
      
      // 下载新版本（修订版）
      const newRes = await downloadDemoDocument(filePaths[1])
      const newFileName = filePaths[1].substring(filePaths[1].lastIndexOf('/') + 1)
      newFile.value = new File([newRes.data], newFileName, { type: 'application/pdf' })
      
      ElMessage.success('已加载文档对：原始版和修订版')
    } else {
      // 单个文档（向后兼容）
      const res = await downloadDemoDocument(doc.filePath)
      const file = new File([res.data], doc.name, { type: 'application/pdf' })
      
      if (!oldFile.value) {
        oldFile.value = file
        ElMessage.success('已加载基准文件（旧版本）')
      } else if (!newFile.value) {
        newFile.value = file
        ElMessage.success('已加载修订文件（新版本）')
      } else {
        oldFile.value = file
        ElMessage.success('已更新基准文件')
      }
    }
  } catch (error) {
    console.error('❌ 加载演示文档失败:', error)
    ElMessage.error('加载演示文档失败：' + (error.message || '未知错误'))
  }
}

// 处理新建任务
const handleNewTask = () => {
  console.log('📝 新建比对任务')
  oldFile.value = null
  newFile.value = null
  currentTaskId.value = null
  loading.value = false
  success.value = false
  error.value = null
  ElMessage.info('已清空，可以开始新的比对任务')
}
</script>

<style scoped lang="scss">
@import '@/styles/demo-common.scss';

.compare-content-wrapper {
  padding: $spacing-lg;
  height: 100%;
  overflow-y: auto;
  background: $bg-page;

  /* 主内容卡片 */
  .main-card {
    @include main-card;
    max-width: 1400px;
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.03), 0 8px 16px rgba(0, 0, 0, 0.03);
    }

    /* 顶部说明区域 */
    .steps-section {
      padding: $spacing-xxl $spacing-xxxl $spacing-xl;
      background: $bg-page;
      border-bottom: 1px solid $border-lighter;
      
      :deep(.el-alert) {
        background: $primary-lighter;
        border-color: $primary-color;
      }
    }

    /* 文件上传区域 */
    .upload-section {
      padding: $spacing-xl $spacing-xxxl $spacing-xxl;

      .upload-grid {
        display: grid;
        grid-template-columns: 1fr auto 1fr;
        gap: $spacing-xxl;
        align-items: center;

        .upload-item {
          .upload-label {
            display: flex;
            align-items: center;
            gap: $spacing-xs;
            margin-bottom: $spacing-sm;
            font-size: $font-size-base;
            font-weight: $font-weight-semibold;
            color: $text-primary;
          }

          .file-uploader {
            :deep(.el-upload-dragger) {
                border-radius: $radius-base;
                border: 2px dashed $border-base;
                background: $bg-white;
                min-height: 200px;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: $transition-base;

                &:hover {
                  border-color: $primary-color;
                  background: $primary-lighter;
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

          .compare-arrow {
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      

      /* 比对选项 */
      .compare-options {
        padding: $spacing-lg $spacing-xxxl;
        border-top: 1px solid $border-lighter;
        display: flex;
        justify-content: center;
        background: $bg-lighter;

        :deep(.el-checkbox) {
          font-size: $font-size-md;
        }
      }

      /* 操作按钮 */
      .action-buttons {
        @include action-buttons-section;
      }
    }
  }

  /* 结果展示区 */
  .result-area {
    @include progress-area;
    margin: 0 auto $spacing-lg;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .compare-content-wrapper {
    padding: $spacing-md;

    .main-card {
      .steps-section {
        padding: $spacing-lg;
      }

      .upload-section {
        padding: $spacing-lg;

        .upload-grid {
          grid-template-columns: 1fr;
          gap: $spacing-lg;

          .compare-arrow {
            display: none;
          }
        }
      }

      .compare-options,
      .action-buttons {
        padding: $spacing-lg;
      }

      .action-buttons {
        flex-direction: column;
        
        .el-button {
          width: 100%;
        }
      }
    }

    .result-area {
      .result-actions {
        flex-direction: column;

        .el-button {
          width: 100%;
        }
      }
    }
  }
}
</style>

