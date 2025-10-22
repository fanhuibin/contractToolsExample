<template>
  <div class="rule-extract-result-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-title">
          <el-button 
            text 
            @click="handleBack"
            style="margin-right: 12px;"
          >
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          智能文档抽取结果
          <el-tag v-if="taskInfo" :type="statusType" size="large" style="margin-left: 12px;">
            {{ statusText }}
          </el-tag>
        </div>
        <div class="task-meta" v-if="taskInfo">
          <span>任务ID: {{ taskId }}</span>
          <span>模板: {{ taskInfo.templateName || '-' }}</span>
          <span>文档: {{ taskInfo.fileName || '-' }}</span>
          <span>耗时: {{ taskInfo.durationSeconds || 0 }}秒</span>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-section">
        <el-skeleton :rows="8" animated />
      </div>

      <!-- 结果显示 -->
      <div class="result-section" v-else-if="taskInfo">
        <!-- 显示模式切换 -->
        <div class="display-controls" style="margin-bottom: 16px;">
          <el-radio-group v-model="displayMode">
            <el-radio-button value="canvas">图片模式</el-radio-button>
            <el-radio-button value="text">文本模式</el-radio-button>
          </el-radio-group>
          
          <el-button 
            style="margin-left: 16px;"
            @click="exportResult"
            :disabled="!resultData || resultData.length === 0"
          >
            <el-icon><Download /></el-icon>
            导出结果
          </el-button>
        </div>

        <!-- 双栏布局：左侧Canvas/文本，右侧提取结果 -->
        <div class="dual-panel-layout">
          <!-- 左侧面板 -->
          <div class="left-panel">
            <el-card 
              class="content-card"
              :body-style="{ 
                padding: '12px', 
                height: 'calc(100vh - 280px)', 
                overflow: displayMode === 'canvas' ? 'hidden' : 'auto',
                display: displayMode === 'canvas' ? 'flex' : 'block',
                flexDirection: displayMode === 'canvas' ? 'column' : 'column'
              }"
            >
              <template #header>
                <div class="panel-header">
                  {{ displayMode === 'canvas' ? '文档图像' : '文档文本' }}
                  <span class="page-info" v-if="displayMode === 'canvas' && totalPages > 0">
                    第 {{ currentPage }} / {{ totalPages }} 页
                  </span>
                </div>
              </template>
              
              <!-- 图片模式 -->
              <div v-if="displayMode === 'canvas'" class="canvas-container" style="flex: 1; min-height: 0; overflow: hidden;">
                <canvas-viewer 
                  v-if="totalPages > 0"
                  ref="canvasViewer"
                  :task-id="taskId"
                  :bbox-mappings="bboxMappings"
                  :char-boxes="charBoxes"
                  :extractions="resultData"
                  :total-pages="totalPages"
                  :api-prefix="'/api/rule-extract/extract/page-image'"
                  @bbox-click="onBboxClick"
                  @page-change="currentPage = $event"
                  style="height: 100%; display: flex; flex-direction: column;"
                />
                <div v-else class="image-fallback">
                  <div class="page-controls" style="margin-bottom: 12px;">
                    <el-button-group>
                      <el-button 
                        size="small" 
                        :disabled="currentPage <= 1"
                        @click="currentPage--"
                      >
                        <el-icon><ArrowLeft /></el-icon>
                        上一页
                      </el-button>
                      <el-button 
                        size="small" 
                        :disabled="currentPage >= totalPages"
                        @click="currentPage++"
                      >
                        下一页
                        <el-icon><ArrowRight /></el-icon>
                      </el-button>
                    </el-button-group>
                  </div>
                  
                  <div class="image-viewer" v-loading="imageLoading">
                    <img 
                      v-if="currentPageImage" 
                      :src="currentPageImage" 
                      alt="Document Page"
                      @load="imageLoading = false"
                      @error="onImageError"
                    />
                    <el-empty v-else description="图片加载失败" />
                  </div>
                </div>
              </div>
              
              <!-- 文本模式 -->
              <div v-else class="text-container">
                <text-viewer 
                  v-if="ocrText"
                  ref="textViewer"
                  :content="ocrText"
                  :extractions="resultData"
                  :bbox-mappings="bboxMappings"
                  @text-click="onTextClick"
                />
                <div v-else class="text-viewer">
                  <pre class="text-content">{{ ocrText || '暂无文本内容' }}</pre>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 右侧面板 - 提取结果 -->
          <div class="right-panel">
            <el-card 
              class="results-card"
              :body-style="{ padding: '12px', height: 'calc(100vh - 280px)', overflow: 'auto' }"
            >
              <template #header>
                <div class="panel-header">
                  提取结果
                  <el-tag size="small" type="info">共 {{ resultData.length }} 个字段</el-tag>
                </div>
              </template>
              
              <div v-if="resultData && resultData.length > 0" class="extraction-results">
                <div 
                  v-for="(item, index) in resultData" 
                  :key="index"
                  class="result-item"
                  :class="{ 
                    'has-value': item.value, 
                    'no-value': !item.value,
                    'clickable': item.charInterval && item.charInterval.startPos !== undefined
                  }"
                  @click="onExtractionClick(item)"
                >
                  <div class="item-header">
                    <span class="field-name">
                      <el-icon><Document /></el-icon>
                      {{ item.fieldName }}
                    </span>
                    <el-tag 
                      size="small" 
                      :type="item.success ? 'success' : 'danger'"
                    >
                      {{ item.success ? '成功' : '失败' }}
                    </el-tag>
                  </div>
                  
                  <div class="item-body">
                    <div class="field-code">
                      <span class="label">编码：</span>
                      <el-tag size="small">{{ item.fieldCode }}</el-tag>
                    </div>
                    
                    <div class="field-value">
                      <span class="label">值：</span>
                      <div class="value-content">
                        <!-- 表格数据的图形化显示 -->
                        <template v-if="item.tableData && Array.isArray(item.tableData)">
                          <div class="table-display">
                            <el-table 
                              :data="item.tableData" 
                              border 
                              size="small"
                              style="width: 100%; margin-top: 8px;"
                              max-height="300"
                            >
                              <el-table-column 
                                v-for="(header, headerIndex) in getTableHeaders(item.tableData)"
                                :key="headerIndex"
                                :prop="header"
                                :label="header"
                                :min-width="80"
                                show-overflow-tooltip
                              />
                            </el-table>
                            <div class="table-info">
                              <el-tag size="small" type="info">
                                表格数据：{{ item.tableData.length }} 行
                              </el-tag>
                            </div>
                          </div>
                        </template>
                        <!-- 数组数据 -->
                        <template v-else-if="Array.isArray(item.value)">
                          <el-tag 
                            v-for="(val, idx) in item.value" 
                            :key="idx" 
                            style="margin: 2px;"
                            type="success"
                          >
                            {{ val }}
                          </el-tag>
                        </template>
                        <!-- 格式化字段类型数据（金额、数字、日期、布尔等） -->
                        <template v-else-if="item.value && typeof item.value === 'object'">
                          <div class="formatted-value-display">
                            <!-- 金额类型 -->
                            <template v-if="item.fieldType === 'amount' && item.value.value !== undefined">
                              <div class="amount-display">
                                <div class="primary-value">
                                  <el-tag type="success" size="large">{{ item.value.formatted || item.value.value }}</el-tag>
                                </div>
                                <div class="secondary-info">
                                  <el-descriptions :column="2" size="small" border>
                                    <el-descriptions-item label="原始文本">{{ item.value.raw }}</el-descriptions-item>
                                    <el-descriptions-item label="数值">{{ item.value.value }}</el-descriptions-item>
                                    <el-descriptions-item label="中文大写" :span="2">{{ item.value.chinese }}</el-descriptions-item>
                                  </el-descriptions>
                                </div>
                              </div>
                            </template>
                            <!-- 数字类型 -->
                            <template v-else-if="item.fieldType === 'number' && item.value.value !== undefined">
                              <div class="number-display">
                                <div class="primary-value">
                                  <el-tag type="primary" size="large">{{ item.value.formatted || item.value.value }}</el-tag>
                                </div>
                                <div class="secondary-info">
                                  <el-descriptions :column="2" size="small" border>
                                    <el-descriptions-item label="原始文本">{{ item.value.raw }}</el-descriptions-item>
                                    <el-descriptions-item label="数值">{{ item.value.value }}</el-descriptions-item>
                                    <el-descriptions-item label="类型">{{ item.value.type === 'decimal' ? '小数' : '整数' }}</el-descriptions-item>
                                  </el-descriptions>
                                </div>
                              </div>
                            </template>
                            <!-- 日期类型 -->
                            <template v-else-if="item.fieldType === 'date' && item.value.iso !== undefined">
                              <div class="date-display">
                                <div class="primary-value">
                                  <el-tag type="warning" size="large">{{ item.value.chinese || item.value.iso }}</el-tag>
                                </div>
                                <div class="secondary-info">
                                  <el-descriptions :column="2" size="small" border>
                                    <el-descriptions-item label="原始文本">{{ item.value.raw }}</el-descriptions-item>
                                    <el-descriptions-item label="ISO格式">{{ item.value.iso }}</el-descriptions-item>
                                    <el-descriptions-item label="中文格式" :span="2">{{ item.value.chinese }}</el-descriptions-item>
                                  </el-descriptions>
                                </div>
                              </div>
                            </template>
                            <!-- 布尔类型 -->
                            <template v-else-if="item.fieldType === 'boolean' && item.value.value !== undefined">
                              <div class="boolean-display">
                                <div class="primary-value">
                                  <el-tag :type="item.value.value ? 'success' : 'info'" size="large">
                                    {{ item.value.display }}
                                  </el-tag>
                                </div>
                                <div class="secondary-info">
                                  <el-descriptions :column="2" size="small" border>
                                    <el-descriptions-item label="原始文本">{{ item.value.raw }}</el-descriptions-item>
                                    <el-descriptions-item label="布尔值">{{ item.value.english }}</el-descriptions-item>
                                  </el-descriptions>
                                </div>
                              </div>
                            </template>
                            <!-- 其他对象类型，显示为JSON -->
                            <template v-else>
                              <pre class="json-value">{{ JSON.stringify(item.value, null, 2) }}</pre>
                            </template>
                          </div>
                        </template>
                        <!-- 普通文本数据 -->
                        <template v-else>
                          <span :class="{ 'empty-value': !item.value }">
                            {{ item.value || '-' }}
                          </span>
                        </template>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <el-empty v-else description="暂无提取结果" />
            </el-card>
          </div>
        </div>

        <!-- 原始数据（可折叠） -->
        <el-collapse style="margin-top: 20px;">
          <el-collapse-item title="查看原始响应数据" name="raw">
            <pre class="raw-data">{{ JSON.stringify(taskInfo, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 错误显示 -->
      <div class="error-section" v-else>
        <el-empty description="未找到任务数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Download, Document } from '@element-plus/icons-vue'
import { getRuleExtractTaskResult } from '@/api/rule-extract'
import { extractObjectData } from '@/utils/response-helper'
import CanvasViewer from '@/views/extract/components/CanvasViewer.vue'
import TextViewer from '@/views/extract/components/TextViewer.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => route.params.taskId as string)
const loading = ref(true)
const imageLoading = ref(false)
const taskInfo = ref<any>(null)
const resultData = ref<any[]>([])
const ocrText = ref('')
const displayMode = ref<'canvas' | 'text'>('canvas')
const currentPage = ref(1)
const totalPages = ref(0)
const bboxMappings = ref<any[]>([])
const charBoxes = ref<any[]>([])

// 组件引用
const canvasViewer = ref()
const textViewer = ref()

const statusType = computed(() => {
  if (!taskInfo.value) return 'info'
  const status = taskInfo.value.status
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'processing') return 'warning'
  return 'info'
})

const statusText = computed(() => {
  if (!taskInfo.value) return '未知'
  const status = taskInfo.value.status
  const statusMap: Record<string, string> = {
    'pending': '等待中',
    'processing': '处理中',
    'completed': '已完成',
    'failed': '失败',
    'cancelled': '已取消'
  }
  return statusMap[status] || status
})

const currentPageImage = computed(() => {
  if (!taskId.value || currentPage.value < 1) return ''
  // 构建图片URL
  return `/api/rule-extract/extract/page-image/${taskId.value}/${currentPage.value}`
})

const loadResult = async () => {
  loading.value = true
  try {
    const res: any = await getRuleExtractTaskResult(taskId.value)
    const resultInfo = extractObjectData(res)
    
    if (resultInfo) {
      taskInfo.value = resultInfo
      
      // 处理OCR文本
      if (resultInfo.ocrText) {
        ocrText.value = resultInfo.ocrText
      } else if (resultInfo.text) {
        ocrText.value = resultInfo.text
      }
      
      // 处理页数
      if (resultInfo.totalPages) {
        totalPages.value = resultInfo.totalPages
      } else if (resultInfo.pageCount) {
        totalPages.value = resultInfo.pageCount
      } else {
        totalPages.value = 1
      }
      
      // 处理提取结果
      if (resultInfo.extractResults) {
        // 如果是对象格式，转换为数组
        if (typeof resultInfo.extractResults === 'object' && !Array.isArray(resultInfo.extractResults)) {
          resultData.value = Object.entries(resultInfo.extractResults).map(([key, value]: [string, any]) => ({
            fieldName: value.fieldName || key,
            fieldCode: value.fieldCode || key,
            value: value.value,
            success: value.success !== false,
            charInterval: value.charInterval || null
          }))
        } else {
          resultData.value = resultInfo.extractResults.map((item: any) => ({
            ...item,
            success: item.status === 'success' || item.success !== false
          }))
        }
      } else if (resultInfo.results) {
        resultData.value = resultInfo.results
      } else if (resultInfo.fields) {
        resultData.value = resultInfo.fields
      }
      
      // 处理位置映射数据
      if (resultInfo.bboxMappings) {
        try {
          bboxMappings.value = typeof resultInfo.bboxMappings === 'string' 
            ? JSON.parse(resultInfo.bboxMappings) 
            : resultInfo.bboxMappings
        } catch (e) {
          console.warn('解析bboxMappings失败:', e)
        }
      }
      
      // 处理字符框数据
      if (resultInfo.charBoxes) {
        try {
          charBoxes.value = typeof resultInfo.charBoxes === 'string' 
            ? JSON.parse(resultInfo.charBoxes) 
            : resultInfo.charBoxes
        } catch (e) {
          console.warn('解析charBoxes失败:', e)
        }
      }
    } else {
      ElMessage.error('获取任务结果失败')
    }
  } catch (error: any) {
    console.error('Load result error:', error)
    ElMessage.error('加载结果失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const onImageError = () => {
  imageLoading.value = false
  ElMessage.warning('图片加载失败')
}

const handleBack = () => {
  router.push('/rule-extract')
}

const exportResult = () => {
  if (!resultData.value || resultData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  // 构建导出数据
  const exportData = {
    taskId: taskId.value,
    taskInfo: {
      templateName: taskInfo.value?.templateName,
      fileName: taskInfo.value?.fileName,
      status: taskInfo.value?.status,
      durationSeconds: taskInfo.value?.durationSeconds,
      exportTime: new Date().toISOString()
    },
    extractResults: resultData.value.map(item => ({
      fieldName: item.fieldName,
      fieldCode: item.fieldCode,
      value: item.value,
      status: item.success ? '成功' : '失败',
      success: item.success,
      charInterval: item.charInterval,
      tableData: item.tableData
    }))
  }

  // 转换为JSON格式
  const jsonContent = JSON.stringify(exportData, null, 2)

  // 创建下载链接
  const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `智能文档抽取结果_${taskId.value}_${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(link.href)

  ElMessage.success('导出成功')
}

// 交互事件处理
const onBboxClick = (bboxInfo: any) => {
  // 可以在这里添加高亮对应字段的逻辑
}

const onTextClick = (textInfo: any) => {
  // 可以在这里添加高亮对应字段的逻辑
}

const onExtractionClick = (extraction: any) => {
  // 在Canvas或文本视图中导航并高亮对应区域
  if (extraction.charInterval) {
    if (displayMode.value === 'canvas' && canvasViewer.value) {
      // 导航到提取内容的位置（包含滚动和高亮）
      canvasViewer.value.navigateToExtraction(extraction)
    } else if (displayMode.value === 'text' && textViewer.value) {
      // TextViewer使用highlightText方法，传入charIntervals数组
      textViewer.value.highlightText([{
        start: extraction.charInterval.startPos,
        end: extraction.charInterval.endPos
      }])
    }
  }
}

// 获取表格的表头
const getTableHeaders = (tableData: any[]) => {
  if (!tableData || tableData.length === 0) return []
  
  // 从第一行数据中获取所有键作为表头
  const firstRow = tableData[0]
  return Object.keys(firstRow)
}

// 监听页码变化，加载图片
watch(currentPage, () => {
  if (displayMode.value === 'canvas') {
    imageLoading.value = true
  }
})

onMounted(() => {
  loadResult()
})
</script>

<style scoped lang="scss">
.rule-extract-result-container {
  padding: 16px;
  
  .card-title {
    display: flex;
    align-items: center;
    font-size: 18px;
    font-weight: 600;
  }
  
  .task-meta {
    display: flex;
    gap: 20px;
    margin-top: 8px;
    font-size: 13px;
    color: #606266;
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
      
      &::before {
        content: '•';
        color: #409eff;
      }
    }
  }
  
  .loading-section {
    padding: 20px;
  }
  
  .display-controls {
    display: flex;
    align-items: center;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
  }
  
  .dual-panel-layout {
    display: flex;
    gap: 16px;
    margin-top: 16px;
    
    .left-panel {
      flex: 1;
      min-width: 0;
    }
    
    .right-panel {
      flex: 0 0 400px;
    }
    
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 500;
      
      .page-info {
        font-size: 13px;
        color: #606266;
        font-weight: normal;
      }
    }
    
    .canvas-container {
      .page-controls {
        display: flex;
        justify-content: center;
      }
      
      .image-viewer {
        display: flex;
        justify-content: center;
        align-items: flex-start;
        min-height: 400px;
        background: #f5f7fa;
        border-radius: 4px;
        padding: 16px;
        
        img {
          max-width: 100%;
          height: auto;
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        }
      }
    }
    
    .text-container {
      .text-viewer {
        background: #f5f7fa;
        border-radius: 4px;
        padding: 16px;
        
        .text-content {
          margin: 0;
          white-space: pre-wrap;
          word-wrap: break-word;
          font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          font-size: 13px;
          line-height: 1.8;
          color: #303133;
        }
      }
    }
    
    .extraction-results {
      .result-item {
        padding: 16px;
        margin-bottom: 12px;
        background: #f5f7fa;
        border-radius: 8px;
        border-left: 4px solid #e4e7ed;
        transition: all 0.3s;
        
        &.has-value {
          border-left-color: #67c23a;
        }
        
        &.no-value {
          border-left-color: #f56c6c;
          opacity: 0.7;
        }
        
        &.clickable {
          cursor: pointer;
          
          &:hover {
            box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
            transform: translateY(-2px);
            border-left-color: #409eff;
          }
        }
        
        &:hover {
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          transform: translateY(-2px);
        }
        
        .item-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
          
          .field-name {
            display: flex;
            align-items: center;
            gap: 6px;
            font-weight: 600;
            font-size: 14px;
            color: #303133;
          }
        }
        
        .item-body {
          display: flex;
          flex-direction: column;
          gap: 8px;
          
          .field-code,
          .field-value {
            display: flex;
            align-items: flex-start;
            font-size: 13px;
            
            .label {
              color: #909399;
              min-width: 60px;
              flex-shrink: 0;
            }
            
            .value-content {
              flex: 1;
              
              .json-value {
                background: #fff;
                padding: 8px;
                border-radius: 4px;
                font-size: 12px;
                margin: 0;
              }
              
              .empty-value {
                color: #c0c4cc;
                font-style: italic;
              }
              
              .table-display {
                .table-info {
                  margin-top: 8px;
                  text-align: right;
                }
                
                :deep(.el-table) {
                  .el-table__header-wrapper {
                    th {
                      background-color: #f5f7fa;
                      font-weight: 600;
                    }
                  }
                  
                  .el-table__body-wrapper {
                    td {
                      padding: 8px;
                    }
                  }
                }
              }
              
              .formatted-value-display {
                .primary-value {
                  margin-bottom: 12px;
                  
                  :deep(.el-tag) {
                    font-size: 16px;
                    padding: 8px 16px;
                    font-weight: 500;
                  }
                }
                
                .secondary-info {
                  :deep(.el-descriptions) {
                    .el-descriptions__label {
                      background-color: #f5f7fa;
                      font-weight: 500;
                      width: 80px;
                    }
                    
                    .el-descriptions__content {
                      background-color: #fff;
                    }
                  }
                }
                
                .amount-display,
                .number-display,
                .date-display,
                .boolean-display {
                  padding: 8px;
                  border-radius: 4px;
                  background: #fafafa;
                }
              }
            }
          }
        }
      }
    }
  }
  
  .raw-data {
    background: #f5f7fa;
    padding: 16px;
    border-radius: 4px;
    font-size: 12px;
    max-height: 400px;
    overflow: auto;
    line-height: 1.6;
    margin: 0;
  }
}
</style>
