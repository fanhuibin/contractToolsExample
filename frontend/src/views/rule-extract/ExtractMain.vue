<template>
  <div class="extract-main-page">
    <!-- 头部 -->
    <div class="header-section">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon><Document /></el-icon>
          规则抽取
        </h1>
        <p class="page-description">
          基于预定义规则和模板的合同信息提取功能，无需AI，快速准确
        </p>
      </div>
      <div class="header-right">
        <el-button 
          type="primary" 
          size="large"
          @click="$router.push('/rule-extract/templates')"
        >
          <el-icon><Setting /></el-icon>
          模板管理
        </el-button>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="main-content">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <el-card class="upload-card">
          <template #header>
            <div class="card-header">
              <span>文档上传</span>
              <el-button 
                type="text" 
                size="small" 
                @click="$router.push('/rule-extract/templates')"
              >
                管理模板
              </el-button>
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
            <div class="el-upload__text">点击或拖拽PDF文件到此区域</div>
            <div class="el-upload__tip">仅支持PDF格式，文件大小不超过100MB</div>
          </el-upload>
          
          <div v-if="selectedFile" class="selected-file">
            <el-alert
              :title="`已选择: ${selectedFile?.name}`"
              :description="`大小: ${formatFileSize(selectedFile?.size || 0)}`"
              type="info"
              show-icon
              closable
              @close="clearFile"
            />
          </div>
        </el-card>

        <el-card class="config-card">
          <template #header>提取配置</template>
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
                {{ isExtracting ? '抽取中...' : '开始抽取' }}
              </el-button>
            </el-form-item>

            <div v-if="selectedTemplateInfo" class="template-preview">
              <el-divider>模板信息</el-divider>
              <div class="template-info">
                <div class="info-item">
                  <span class="label">模板名称：</span>
                  <span class="value">{{ selectedTemplateInfo.templateName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">字段数量：</span>
                  <span class="value">{{ selectedTemplateInfo.fields?.length || 0 }} 个</span>
                </div>
                <div class="info-item">
                  <span class="label">描述：</span>
                  <span class="value">{{ selectedTemplateInfo.description || '无' }}</span>
                </div>
              </div>
            </div>
          </el-form>
        </el-card>

        <el-card class="history-card" v-if="recentTasks.length > 0">
          <template #header>最近任务</template>
          <div class="task-list">
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
        </el-card>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <div v-if="!currentTask" class="empty-state">
          <el-empty 
            description="上传文件并选择模板开始抽取"
            :image-size="200"
          >
            <template #image>
              <el-icon :size="100" color="#909399">
                <Document />
              </el-icon>
            </template>
          </el-empty>
        </div>

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
              <span>抽取完成</span>
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
            title="信息抽取完成"
            :sub-title="`耗时 ${currentTask.durationSeconds || 0} 秒`"
          >
            <template #extra>
              <el-button type="primary" @click="viewDetailedResult">
                查看详细结果
              </el-button>
              <el-button @click="startNewTask">
                继续抽取
              </el-button>
            </template>
          </el-result>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled, Refresh, Setting, InfoFilled } from '@element-plus/icons-vue'
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
    if (res.code === 200) {
      templates.value = res.data || []
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
    if (res.code === 200) {
      recentTasks.value = res.data || []
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

    const res: any = await uploadAndExtract(formData)
    
    if (res.code === 200) {
      const taskId = res.data.taskId
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
    ElMessage.error('开始抽取失败：' + (error.message || '未知错误'))
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
      if (res.code === 200) {
        currentTask.value = res.data
        
        if (['completed', 'failed', 'cancelled'].includes(currentTask.value.status)) {
          stopStatusPolling()
          isExtracting.value = false
          
          if (currentTask.value.status === 'completed') {
            ElMessage.success('抽取完成！')
            loadRecentTasks()
          } else if (currentTask.value.status === 'failed') {
            ElMessage.error('抽取失败：' + (currentTask.value.errorMessage || '未知错误'))
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
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;

  .header-section {
    margin-bottom: 30px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      .page-title {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 28px;
        font-weight: 600;
        color: #303133;
        margin: 0 0 12px 0;
      }

      .page-description {
        color: #606266;
        font-size: 14px;
        margin: 0;
      }
    }

    .header-right {
      .el-button {
        padding: 12px 24px;
        font-size: 15px;
      }
    }
  }

  .main-content {
    display: grid;
    grid-template-columns: 400px 1fr;
    gap: 20px;

    .left-panel {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .selected-file {
        margin-top: 16px;
      }

      .config-card {
        .template-option {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .form-tip {
          margin-top: 8px;
          font-size: 13px;
          color: #909399;
          display: flex;
          align-items: center;
          gap: 4px;

          .el-link {
            margin-left: 4px;
            font-size: 13px;
          }
        }

        .template-preview {
          margin-top: 20px;

          .template-info {
            .info-item {
              margin-bottom: 12px;
              font-size: 14px;

              .label {
                color: #909399;
                margin-right: 8px;
              }

              .value {
                color: #303133;
                font-weight: 500;
              }
            }
          }
        }
      }

      .history-card {
        .task-list {
          .task-item {
            padding: 12px;
            border-radius: 4px;
            margin-bottom: 8px;
            cursor: pointer;
            transition: all 0.3s;
            border: 1px solid #ebeef5;

            &:hover {
              background-color: #f5f7fa;
              border-color: #409eff;
            }

            .task-name {
              font-size: 14px;
              color: #303133;
              margin-bottom: 8px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .task-status {
              display: flex;
              justify-content: flex-end;
            }
          }
        }
      }
    }

    .right-panel {
      .empty-state {
        height: 600px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f7fa;
        border-radius: 4px;
      }

      .progress-card, .result-card {
        .progress-header, .result-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .progress-content {
          .task-info {
            background: #f5f7fa;
            padding: 16px;
            border-radius: 4px;
            margin-bottom: 24px;

            .info-row {
              display: flex;
              margin-bottom: 8px;
              font-size: 14px;

              &:last-child {
                margin-bottom: 0;
              }

              .label {
                color: #909399;
                min-width: 80px;
              }

              .value {
                color: #303133;
                font-weight: 500;
              }
            }
          }

          .progress-bar {
            margin-bottom: 30px;

            .progress-message {
              text-align: center;
              margin-top: 12px;
              color: #606266;
              font-size: 14px;
            }
          }

          .status-timeline {
            padding-left: 20px;
          }
        }
      }
    }
  }
}
</style>

