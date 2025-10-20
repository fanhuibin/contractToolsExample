<template>
  <div class="extract-main-page">
    <!-- 使用 PageHeader 组件 -->
    <PageHeader 
      title="智能文档抽取" 
      description="采用深度版面分析、OCR识别与智能文档检索技术，结合规则引擎精准定位，实现结构化信息高效抽取"
      :icon="Document"
      tag="规则引擎"
      tag-type="success"
    >
      <template #actions>
        <el-button 
          type="primary" 
          size="large"
          @click="$router.push('/rule-extract/templates')"
        >
          <el-icon><Setting /></el-icon>
          模板管理
        </el-button>
      </template>
    </PageHeader>

    <!-- 主内容 - 三列式布局 -->
    <el-row :gutter="16" class="main-operation-area mb16">
      <!-- 左列：文档上传 -->
      <el-col :span="8">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <el-icon><UploadFilled /></el-icon>
              <span>文档上传</span>
            </div>
          </template>
          
          <el-upload
            drag
            v-model:file-list="fileList"
            :multiple="false"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept=".pdf"
            :on-change="handleFileChange"
            :auto-upload="false"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽或点击上传PDF</div>
            <div class="el-upload__tip">仅支持PDF格式，最大100MB</div>
          </el-upload>
          
          <div v-if="selectedFile" class="selected-file">
            <el-alert
              :title="selectedFile?.name"
              :description="`大小: ${formatFileSize(selectedFile?.size || 0)}`"
              type="success"
              show-icon
              closable
              @close="clearFile"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 中列：提取配置 -->
      <el-col :span="8">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <el-icon><Setting /></el-icon>
              <span>提取配置</span>
            </div>
          </template>
          
          <el-form label-position="top">
            <el-form-item label="选择模板" required>
              <el-select 
                v-model="selectedTemplateId" 
                placeholder="请选择规则模板"
                style="width: 100%"
                :loading="loadingTemplates"
                filterable
              >
                <el-option
                  v-for="template in templates"
                  :key="template.id"
                  :label="template.templateName"
                  :value="template.id"
                >
                  <div class="template-option">
                    <span>{{ template.templateName }}</span>
                    <el-tag size="small" type="info" style="margin-left: 8px;" v-if="template.templateCode">
                      {{ template.templateCode }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
              <div class="form-tip">
                <el-icon><InfoFilled /></el-icon>
                没有合适的模板？<el-link type="primary" @click="$router.push('/rule-extract/templates')">去创建</el-link>
              </div>
            </el-form-item>

            <el-form-item v-if="selectedTemplateInfo">
              <div class="template-preview">
                <div class="template-info">
                  <div class="info-item">
                    <span class="label">模板名称：</span>
                    <span class="value">{{ selectedTemplateInfo.templateName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">字段数量：</span>
                    <span class="value">{{ selectedTemplateInfo.fields?.length || 0 }} 个</span>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="文本提取设置">
              <div class="extract-settings">
                <div class="setting-item">
                  <el-checkbox v-model="extractSettings.ignoreHeaderFooter">
                    忽略页眉页脚
                  </el-checkbox>
                  <el-tooltip 
                    content="忽略页眉页脚位置，避免页眉页脚干扰跨页提取" 
                    placement="top"
                  >
                    <el-icon style="margin-left: 4px; cursor: help;"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary" 
                size="large" 
                style="width: 100%"
                :loading="isExtracting"
                :disabled="!canStartExtraction"
                @click="startExtraction"
              >
                <el-icon v-if="!isExtracting"><Refresh /></el-icon>
                {{ isExtracting ? '提取中...' : '开始提取' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右列：最近任务 -->
      <el-col :span="8">
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>最近任务</span>
            </div>
          </template>
          
          <div v-if="recentTasks.length > 0" class="task-list">
            <div 
              v-for="task in recentTasks.slice(0, 5)" 
              :key="task.taskId"
              class="task-item"
              @click="viewTaskResult(task.taskId)"
            >
              <div class="task-name">{{ task.fileName }}</div>
              <div class="task-status">
                <el-tag 
                  :type="getTaskStatusType(task.status)" 
                  size="small"
                >
                  {{ getTaskStatusLabel(task.status) }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <el-empty v-else description="暂无历史任务" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 结果展示区 -->
    <div class="result-area">
      <!-- 使用 EmptyState 组件 -->
      <EmptyState 
        v-if="!currentTask"
        title="准备就绪"
        description="上传文件并选择模板，开始信息提取"
        :icon="Document"
      />

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
            :sub-title="`耗时 ${currentTask.durationSeconds || 0} 秒`"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled, Refresh, Setting, InfoFilled, QuestionFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { 
  listTemplates, 
  uploadAndExtract, 
  getRuleExtractTaskStatus,
  cancelRuleExtractTask,
  listRuleExtractTasks
} from '@/api/rule-extract'

const router = useRouter()

// 文件相关
const fileList = ref([])
const selectedFile = ref<File | null>(null)

// 模板相关
const loadingTemplates = ref(false)
const templates = ref<any[]>([])
const selectedTemplateId = ref('')

// 提取设置
const extractSettings = ref({
  ignoreHeaderFooter: true  // 默认开启忽略页眉页脚
})

// 任务相关
const isExtracting = ref(false)
const currentTask = ref<any>(null)
const recentTasks = ref<any[]>([])
let statusCheckTimer: any = null

const canStartExtraction = computed(() => {
  return selectedFile.value && selectedTemplateId.value && !isExtracting.value
})

const selectedTemplateInfo = computed(() => {
  return templates.value.find(t => t.id === selectedTemplateId.value)
})

const loadTemplates = async () => {
  try {
    loadingTemplates.value = true
    const res: any = await listTemplates({ status: 'active' })
    if (res.data.code === 200) {
      templates.value = res.data.data || []
    }
  } catch (error: any) {
    ElMessage.error('加载模板失败：' + (error.message || '未知错误'))
  } finally {
    loadingTemplates.value = false
  }
}

const loadRecentTasks = async () => {
  try {
    const res: any = await listRuleExtractTasks()
    if (res.data.code === 200) {
      recentTasks.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载任务历史失败', error)
  }
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const beforeUpload = (file: File) => {
  const isPDF = file.type === 'application/pdf'
  const isLt100M = file.size / 1024 / 1024 < 100

  if (!isPDF) {
    ElMessage.error('只能上传PDF文件！')
    return false
  }
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过100MB！')
    return false
  }
  return false
}

const clearFile = () => {
  selectedFile.value = null
  fileList.value = []
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
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
    formData.append('ocrProvider', 'mineru')
    formData.append('ignoreHeaderFooter', String(extractSettings.value.ignoreHeaderFooter))

    const res: any = await uploadAndExtract(formData)
    
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
  } catch (error: any) {
    ElMessage.error('开始提取失败：' + (error.message || '未知错误'))
    isExtracting.value = false
  }
}

const startStatusPolling = (taskId: string) => {
  if (statusCheckTimer) {
    clearInterval(statusCheckTimer)
  }

  statusCheckTimer = setInterval(async () => {
    try {
      const res: any = await getRuleExtractTaskStatus(taskId)
      if (res.data.code === 200) {
        currentTask.value = res.data.data
        
        if (['completed', 'failed', 'cancelled'].includes(currentTask.value.status)) {
          stopStatusPolling()
          isExtracting.value = false
          
          if (currentTask.value.status === 'completed') {
            ElMessage.success('提取完成！')
            loadRecentTasks()
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
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('取消任务失败：' + (error.message || '未知错误'))
    }
  }
}

const viewDetailedResult = () => {
  if (currentTask.value) {
    router.push(`/rule-extract/result/${currentTask.value.taskId}`)
  }
}

const viewTaskResult = (taskId: string) => {
  router.push(`/rule-extract/result/${taskId}`)
}

const startNewTask = () => {
  currentTask.value = null
  clearFile()
  selectedTemplateId.value = ''
}

const getTaskStatusType = (status: string) => {
  const types: Record<string, string> = {
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

const getTaskStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
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

const getProgressStatus = (status: string) => {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return undefined
}

const getTimelineType = (status: string) => {
  if (!currentTask.value) return 'info'
  
  const statusOrder = ['pending', 'file_uploaded', 'ocr_processing', 'extracting', 'completed']
  const currentIndex = statusOrder.indexOf(currentTask.value.status)
  const targetIndex = statusOrder.indexOf(status)
  
  if (currentIndex >= targetIndex) return 'success'
  return 'info'
}

const isStatusPassed = (status: string) => {
  if (!currentTask.value) return false
  
  const statusOrder = ['pending', 'file_uploaded', 'ocr_processing', 'extracting', 'completed']
  const currentIndex = statusOrder.indexOf(currentTask.value.status)
  const targetIndex = statusOrder.indexOf(status)
  
  return currentIndex >= targetIndex
}

const formatTime = (time: any) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadTemplates()
  loadRecentTasks()
})

onUnmounted(() => {
  stopStatusPolling()
})
</script>

<style scoped lang="scss">
.extract-main-page {
  padding: 0;

  /* 三列式主操作区 */
  .main-operation-area {
    /* 统一卡片样式 */
    .el-card {
      min-height: 420px;
      border-radius: var(--zx-radius-md);
      transition: box-shadow var(--zx-transition-base);

      &:hover {
        box-shadow: var(--zx-shadow-md);
      }
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: var(--zx-spacing-sm);
      font-weight: var(--zx-font-semibold);
      font-size: var(--zx-font-base);

      .el-icon {
        font-size: 18px;
      }
    }

    /* 上传卡片 */
    .upload-card {
      :deep(.el-upload-dragger) {
        padding: var(--zx-spacing-2xl);
        border-radius: var(--zx-radius-md);
        transition: all var(--zx-transition-base);

        &:hover {
          border-color: var(--zx-primary);
          background-color: var(--zx-primary-light-9);
        }

        .el-icon--upload {
          font-size: 48px;
          color: var(--zx-text-placeholder);
          margin-bottom: var(--zx-spacing-md);
        }

        .el-upload__text {
          font-size: var(--zx-font-base);
          color: var(--zx-text-regular);
        }

        .el-upload__tip {
          font-size: var(--zx-font-sm);
          color: var(--zx-text-secondary);
          margin-top: var(--zx-spacing-sm);
        }
      }

      .selected-file {
        margin-top: var(--zx-spacing-lg);
      }
      }

    /* 配置卡片 */
      .config-card {
        .template-option {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .form-tip {
        margin-top: var(--zx-spacing-sm);
        font-size: var(--zx-font-sm);
        color: var(--zx-text-secondary);
          display: flex;
          align-items: center;
        gap: var(--zx-spacing-xs);

          .el-link {
          margin-left: var(--zx-spacing-xs);
        }
        }

        .template-preview {
        padding: var(--zx-spacing-md);
        background: var(--zx-bg-light);
        border-radius: var(--zx-radius-sm);
        border-left: 3px solid var(--zx-success);

          .template-info {
            .info-item {
            display: flex;
            margin-bottom: var(--zx-spacing-xs);
            font-size: var(--zx-font-sm);

            &:last-child {
              margin-bottom: 0;
            }

              .label {
              color: var(--zx-text-secondary);
              min-width: 80px;
              }

              .value {
              color: var(--zx-text-primary);
              font-weight: var(--zx-font-medium);
              flex: 1;
            }
          }
        }
      }

      .extract-settings {
        padding: var(--zx-spacing-md);
        background: var(--zx-bg-light);
        border-radius: var(--zx-radius-sm);
        border-left: 3px solid var(--zx-primary);
        margin-bottom: var(--zx-spacing-sm);

        .setting-item {
          display: flex;
          align-items: center;
          font-size: var(--zx-font-sm);

          .el-checkbox {
            font-size: var(--zx-font-sm);
          }
        }
      }

      .setting-hint {
        display: flex;
        align-items: center;
        gap: var(--zx-spacing-xs);
        font-size: var(--zx-font-xs);
        color: var(--zx-text-secondary);
        padding: var(--zx-spacing-sm);
        background: var(--zx-info-light-9);
        border-radius: var(--zx-radius-sm);
        border-left: 3px solid var(--zx-info);

        .el-icon {
          color: var(--zx-info);
        }
      }
    }

    /* 历史任务卡片 */
      .history-card {
        .task-list {
        max-height: 330px;
        overflow-y: auto;

        /* 美化滚动条 */
        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-thumb {
          background: var(--zx-border-light);
          border-radius: 3px;

          &:hover {
            background: var(--zx-border-base);
          }
        }

          .task-item {
          padding: var(--zx-spacing-md);
          border-radius: var(--zx-radius-sm);
          margin-bottom: var(--zx-spacing-sm);
            cursor: pointer;
          transition: all var(--zx-transition-fast);
          border: 1px solid var(--zx-border-lighter);
          background: var(--zx-bg-white);

          &:last-child {
            margin-bottom: 0;
          }

            &:hover {
            background-color: var(--zx-primary-light-9);
            border-color: var(--zx-primary);
            transform: translateX(2px);
            }

            .task-name {
            font-size: var(--zx-font-sm);
            color: var(--zx-text-primary);
            margin-bottom: var(--zx-spacing-sm);
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            font-weight: var(--zx-font-medium);
            }

            .task-status {
              display: flex;
              justify-content: flex-end;
            }
          }
        }
      }
    }

  /* 结果展示区 */
  .result-area {
    .progress-card, .result-card {
      animation: fadeIn 0.3s ease-out;
      border-radius: var(--zx-radius-md);
      
      .progress-header, .result-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: var(--zx-font-semibold);
      }

      .progress-content {
        .task-info {
          background: var(--zx-primary-light-9);
          padding: var(--zx-spacing-lg);
          border-radius: var(--zx-radius-md);
          margin-bottom: var(--zx-spacing-xl);
          border-left: 4px solid var(--zx-primary);

          .info-row {
            display: flex;
            margin-bottom: var(--zx-spacing-sm);
            font-size: var(--zx-font-sm);

            &:last-child {
              margin-bottom: 0;
            }

            .label {
              color: var(--zx-text-secondary);
              min-width: 90px;
              font-weight: var(--zx-font-medium);
            }

            .value {
              color: var(--zx-text-primary);
              font-weight: var(--zx-font-medium);
              flex: 1;
              word-break: break-all;
            }
          }
        }

        .progress-bar {
          margin-bottom: var(--zx-spacing-xl);

          .progress-message {
            text-align: center;
            margin-top: var(--zx-spacing-md);
            color: var(--zx-text-regular);
            font-size: var(--zx-font-sm);
            font-weight: var(--zx-font-medium);
          }
        }

        .status-timeline {
          padding: var(--zx-spacing-lg);
          background: var(--zx-bg-light);
          border-radius: var(--zx-radius-md);

          :deep(.el-timeline-item__node) {
            transition: all var(--zx-transition-base);
          }

          :deep(.el-timeline-item__content) {
            color: var(--zx-text-regular);
            font-size: var(--zx-font-sm);
          }
        }
      }
    }

    .result-card {
      :deep(.el-result) {
        padding: var(--zx-spacing-2xl);
      }
    }
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

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 通用样式类 */
.mb12 {
  margin-bottom: 12px;
}

.mb16 {
  margin-bottom: 16px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .extract-main-page {
    .main-operation-area {
      .el-card {
        min-height: auto;
      }
      
      :deep(.el-col) {
        width: 100% !important;
        margin-bottom: var(--zx-spacing-lg);
      }
    }
  }
}
</style>


