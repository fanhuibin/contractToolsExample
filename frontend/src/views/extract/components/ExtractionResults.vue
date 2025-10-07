<template>
  <div class="extraction-results">
    <!-- 结果统计 -->
    <div class="results-header">
      <h4>提取结果 ({{ extractions.length }} 项)</h4>
      
      <div class="header-actions">
        <a-select 
          v-model:value="filterType" 
          size="small" 
          style="width: 120px;"
          @change="onFilterChange"
        >
          <a-select-option value="all">全部</a-select-option>
          <a-select-option value="contract">合同信息</a-select-option>
          <a-select-option value="party">当事人</a-select-option>
          <a-select-option value="amount">金额</a-select-option>
          <a-select-option value="date">日期</a-select-option>
        </a-select>
        
      </div>
    </div>

    <!-- 提取结果列表 -->
    <div class="results-list">
      <div 
        class="result-item" 
        v-for="(extraction, index) in filteredExtractions" 
        :key="extraction.id || index"
        :class="{ 
          'highlighted': highlightedExtractionId === extraction.id,
          'clickable': extraction.charInterval && extraction.charInterval.startPos !== undefined
        }"
        @click="onExtractionClick(extraction)"
        @mouseenter="onExtractionHover(extraction)"
        @mouseleave="onExtractionLeave()"
      >
        <!-- 提取项标题 -->
        <div class="item-header">
          <div class="item-title">
            <span class="field-name">{{ extraction.fieldName || extraction.name }}</span>
            <a-tag 
              :color="getFieldTypeColor(getFieldTypeFromName(extraction.field || extraction.fieldName || extraction.name))" 
              size="small"
            >
              {{ getFieldTypeLabel(getFieldTypeFromName(extraction.field || extraction.fieldName || extraction.name)) }}
            </a-tag>
          </div>
          
          <div class="item-meta">
            <span class="confidence" v-if="extraction.confidence">
              置信度: {{ Math.round(extraction.confidence * 100) }}%
            </span>
            
            <a-tooltip title="查看详情">
              <a-button 
                type="text" 
                size="small" 
                @click.stop="showExtractionDetail(extraction)"
              >
                <info-circle-outlined />
              </a-button>
            </a-tooltip>
          </div>
        </div>

        <!-- 提取值 -->
        <div class="item-content">
          <div class="extracted-value">
            {{ formatExtractionValue(extraction.value) }}
          </div>
          
          <!-- 原文位置信息 -->
          <div class="source-info" v-if="extraction.charInterval && extraction.charInterval.startPos !== undefined">
            <a-tag size="small" color="blue">
              位置: {{ extraction.charInterval.startPos }}-{{ extraction.charInterval.endPos }}
            </a-tag>
          </div>
        </div>

        <!-- 验证状态 -->
        <div class="validation-status" v-if="extraction.validationStatus">
          <a-tag 
            :color="getValidationColor(extraction.validationStatus)"
            size="small"
          >
            {{ getValidationLabel(extraction.validationStatus) }}
          </a-tag>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-if="filteredExtractions.length === 0">
        <a-empty 
          description="暂无提取结果"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
        />
      </div>
    </div>

    <!-- 提取详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="提取详情"
      :footer="null"
      width="600px"
    >
      <div v-if="selectedExtraction">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="字段名称">
            {{ selectedExtraction.fieldName || selectedExtraction.name }}
          </a-descriptions-item>
          
          <a-descriptions-item label="字段类型">
            <a-tag :color="getFieldTypeColor(selectedExtraction.fieldType)">
              {{ getFieldTypeLabel(selectedExtraction.fieldType) }}
            </a-tag>
          </a-descriptions-item>
          
          <a-descriptions-item label="提取值">
            <div class="detail-value">
              {{ formatExtractionValue(selectedExtraction.value) }}
            </div>
          </a-descriptions-item>
          
          <a-descriptions-item label="置信度" v-if="selectedExtraction.confidence">
            <a-progress 
              :percent="Math.round(selectedExtraction.confidence * 100)" 
              size="small"
              :stroke-color="getConfidenceColor(selectedExtraction.confidence)"
            />
          </a-descriptions-item>
          
          <a-descriptions-item label="字符区间" v-if="selectedExtraction.charInterval">
            <div class="char-intervals">
              <a-tag 
                size="small"
                color="blue"
              >
                {{ selectedExtraction.charInterval.startPos }} - {{ selectedExtraction.charInterval.endPos }}
              </a-tag>
            </div>
          </a-descriptions-item>
          
          <a-descriptions-item label="原文内容" v-if="selectedExtraction.charInterval && selectedExtraction.charInterval.sourceText">
            <div class="source-text">
              "{{ selectedExtraction.charInterval.sourceText }}"
            </div>
          </a-descriptions-item>
          
          <a-descriptions-item label="验证状态" v-if="selectedExtraction.validationStatus">
            <a-tag :color="getValidationColor(selectedExtraction.validationStatus)">
              {{ getValidationLabel(selectedExtraction.validationStatus) }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message, Empty } from 'ant-design-vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'

// Props
interface Props {
  extractions: any[]
  schemaType: string
}

const props = withDefaults(defineProps<Props>(), {
  extractions: () => [],
  schemaType: 'contract'
})

// Emits
const emit = defineEmits<{
  extractionClick: [extraction: any]
}>()

// 响应式数据
const filterType = ref<string>('all')
const highlightedExtractionId = ref<string>('')
const detailModalVisible = ref<boolean>(false)
const selectedExtraction = ref<any>(null)

// 计算属性
const filteredExtractions = computed(() => {
  if (filterType.value === 'all') {
    return props.extractions
  }
  
  return props.extractions.filter(extraction => {
    const fieldName = extraction.field || extraction.fieldName || extraction.name || ''
    const inferredType = getFieldTypeFromName(fieldName)
    
    // 根据筛选类型匹配
    switch (filterType.value) {
      case 'contract':
        return inferredType === 'contract'
      case 'party':
        return inferredType === 'party'
      case 'amount':
        return inferredType === 'amount'
      case 'date':
        return inferredType === 'date'
      default:
        return true
    }
  })
})

// 监听属性变化
watch(() => props.extractions, () => {
  // 重置高亮状态
  highlightedExtractionId.value = ''
})

// 事件处理
const onFilterChange = (value: string) => {
  filterType.value = value
}

const onExtractionClick = (extraction: any) => {
  // 检查是否有字符区间信息
  const charInterval = extraction.charInterval
  if (charInterval && charInterval.startPos !== undefined && charInterval.endPos !== undefined) {
    // 转换为前端期望的格式
    const charIntervals = [{
      start: charInterval.startPos,
      end: charInterval.endPos,
      sourceText: charInterval.sourceText
    }]
    emit('extractionClick', { ...extraction, charIntervals })
  } else {
    emit('extractionClick', extraction)
  }
}

const onExtractionHover = (extraction: any) => {
  // TODO: 可以添加悬停预览功能
}

const onExtractionLeave = () => {
  // TODO: 清除悬停效果
}

const showExtractionDetail = (extraction: any) => {
  selectedExtraction.value = extraction
  detailModalVisible.value = true
}

// 格式化方法
const formatExtractionValue = (value: any): string => {
  if (value === null || value === undefined) {
    return '未提取到'
  }
  
  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }
  
  return String(value)
}

const formatCharIntervals = (intervals: any[]): string => {
  if (!intervals || intervals.length === 0) {
    return '无'
  }
  
  if (intervals.length === 1) {
    return `${intervals[0].start}-${intervals[0].end}`
  }
  
  return `${intervals.length}个区间`
}

// 从字段名推断类型
const getFieldTypeFromName = (fieldName: string): string => {
  if (!fieldName) return 'text'
  
  const name = fieldName.toLowerCase()
  
  // 合同相关
  if (name.includes('contract') || name.includes('title') || name.includes('合同')) {
    return 'contract'
  }
  
  // 当事人相关
  if (name.includes('party') || name.includes('甲方') || name.includes('乙方') || 
      name.includes('buyer') || name.includes('seller') || name.includes('供应商') || 
      name.includes('采购方')) {
    return 'party'
  }
  
  // 金额相关
  if (name.includes('amount') || name.includes('price') || name.includes('cost') || 
      name.includes('金额') || name.includes('价格') || name.includes('费用') || 
      name.includes('money') || name.includes('value')) {
    return 'amount'
  }
  
  // 日期相关
  if (name.includes('date') || name.includes('time') || name.includes('日期') || 
      name.includes('时间') || name.includes('签署') || name.includes('signing')) {
    return 'date'
  }
  
  // 数字相关
  if (name.includes('number') || name.includes('count') || name.includes('quantity') || 
      name.includes('数量') || name.includes('编号')) {
    return 'number'
  }
  
  return 'text'
}

// 样式相关方法
const getFieldTypeColor = (fieldType: string): string => {
  const colorMap: Record<string, string> = {
    'contract': 'blue',
    'party': 'green',
    'amount': 'orange',
    'date': 'purple',
    'text': 'default',
    'number': 'cyan',
    'boolean': 'magenta'
  }
  
  return colorMap[fieldType?.toLowerCase()] || 'default'
}

const getFieldTypeLabel = (fieldType: string): string => {
  const labelMap: Record<string, string> = {
    'contract': '合同',
    'party': '当事人',
    'amount': '金额',
    'date': '日期',
    'text': '文本',
    'number': '数字',
    'boolean': '布尔值'
  }
  
  return labelMap[fieldType?.toLowerCase()] || fieldType || '未知'
}

const getValidationColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    'validated': 'green',
    'warning': 'orange',
    'error': 'red',
    'pending': 'blue'
  }
  
  return colorMap[status?.toLowerCase()] || 'default'
}

const getValidationLabel = (status: string): string => {
  const labelMap: Record<string, string> = {
    'validated': '已验证',
    'warning': '警告',
    'error': '错误',
    'pending': '待验证'
  }
  
  return labelMap[status?.toLowerCase()] || status || '未知'
}

const getConfidenceColor = (confidence: number): string => {
  if (confidence >= 0.8) return '#52c41a'
  if (confidence >= 0.6) return '#faad14'
  return '#ff4d4f'
}


// 高亮指定的提取项
const highlightExtraction = (extractionId: string) => {
  highlightedExtractionId.value = extractionId
  
  // 滚动到对应项目
  setTimeout(() => {
    const element = document.querySelector(`.result-item[data-id="${extractionId}"]`)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, 100)
}

// 暴露方法给父组件
defineExpose({
  highlightExtraction
})
</script>

<style scoped>
.extraction-results {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.results-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.results-list {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.result-item {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  transition: all 0.2s;
}

.result-item.clickable {
  cursor: pointer;
}

.result-item.clickable:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.result-item.highlighted {
  border-color: #ff4d4f;
  background: #fff2f0;
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.field-name {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.confidence {
  font-size: 12px;
  color: #666;
}

.item-content {
  margin-bottom: 8px;
}

.extracted-value {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
  word-break: break-word;
  word-wrap: break-word;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.source-info {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.validation-status {
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

/* 详情弹窗样式 */
.detail-value {
  word-break: break-word;
  white-space: pre-wrap;
}

.char-intervals {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.source-text {
  font-style: italic;
  color: #666;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  word-break: break-word;
}
</style>
