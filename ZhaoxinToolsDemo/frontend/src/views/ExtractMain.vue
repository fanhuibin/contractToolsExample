<template>
  <div class="extract-main-page">
    <!-- 使用 PageHeader 组件 - 英雄区域 -->
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
          @click="openTemplateManage"
        >
          <el-icon><Setting /></el-icon>
          模板管理
        </el-button>
      </template>
    </PageHeader>

    <!-- 主内容区域 - 居中垂直布局 -->
    <div class="main-content-wrapper">
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
    
    <!-- 模板管理弹窗 -->
    <IframeDialog
      v-model="templateDialogVisible"
      :url="templateManageUrl"
      title="模板管理"
      :fullscreen="false"
      width="90%"
      @close="onTemplateDialogClose"
    />
    
    <!-- AI生成模板弹窗 -->
    <IframeDialog
      v-model="aiGeneratorDialogVisible"
      :url="aiGeneratorUrl"
      title="AI生成模板"
      :fullscreen="false"
      width="75%"
      @close="onAIGeneratorDialogClose"
    />
    
    <!-- 提取结果详情弹窗 -->
    <IframeDialog
      v-model="resultDialogVisible"
      :url="resultUrl"
      title="提取结果详情"
      :fullscreen="false"
      width="90%"
      @close="onResultDialogClose"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled, CircleCheck, Setting } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import IframeDialog from '@/components/IframeDialog.vue'
import { ZHAOXIN_CONFIG } from '@/config'
import { 
  listTemplates, 
  uploadAndExtract, 
  getRuleExtractTaskStatus,
  cancelRuleExtractTask
} from '@/api/ruleExtract'
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

const canStartExtraction = computed(() => {
  return selectedFile.value && selectedTemplateId.value && !isExtracting.value
})

const selectedTemplateInfo = computed(() => {
  return templates.value.find(t => t.id === selectedTemplateId.value)
})

// iframe URL构建
const templateManageUrl = computed(() => {
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/templates`
})

const aiGeneratorUrl = computed(() => {
  return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/ai-generator`
})

const resultUrl = computed(() => {
  if (currentTask.value?.taskId) {
    return `${ZHAOXIN_CONFIG.frontendUrl}/rule-extract/result/${currentTask.value.taskId}`
  }
  return ''
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
  selectedFile.value = file.raw
}

const beforeUpload = (file) => {
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
    ElMessage.error('开始提取失败：' + (error.message || '未知错误'))
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
            ElMessage.success('提取完成！')
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

const viewDetailedResult = () => {
  if (currentTask.value?.taskId) {
    resultDialogVisible.value = true
  }
}

const startNewTask = () => {
  currentTask.value = null
  clearFile()
  selectedTemplateId.value = ''
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
.extract-main-page {
  padding: 16px;
  background: #f5f7fa;
  min-height: 100vh;

  /* 主内容包装器 */
  .main-content-wrapper {
    margin: 0 auto;

    .main-card {
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      margin-bottom: 20px;

      /* 步骤指示器区域 */
      .steps-section {
        padding: 30px 40px;
        border-bottom: 1px solid #f0f0f0;
        background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);

        :deep(.el-step__head) {
          color: #909399;
        }

        :deep(.el-step__title) {
          font-weight: 500;
          color: #606266;
        }

        :deep(.el-step__description) {
          color: #909399;
        }

        :deep(.el-step__icon) {
          border-color: #d9d9d9;
          color: #909399;
        }

        :deep(.el-step__line) {
          background-color: #e4e7ed;
        }
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


