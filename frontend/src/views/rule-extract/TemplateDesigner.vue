<template>
  <div class="template-designer-page">
    <el-page-header @back="handleBack">
      <template #content>
        <div class="page-header-content">
          <span>{{ templateName || '模板设计' }}</span>
          <el-tag v-if="templateCode" size="small" type="info">
            {{ templateCode }}
          </el-tag>
        </div>
      </template>
      <template #extra>
        <el-button @click="editBasicInfo">
          <el-icon><Edit /></el-icon>
          编辑基本信息
        </el-button>
        <el-button type="primary" @click="saveTemplate" :loading="saving">
          <el-icon><Check /></el-icon>
          保存模板
        </el-button>
      </template>
    </el-page-header>

    <el-card class="main-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>字段配置</span>
          <div class="header-actions">
            <el-button size="small" @click="openBatchTest">
              <el-icon><View /></el-icon>
              批量测试 
            </el-button>
            <el-button type="primary" size="small" @click="addField">
              <el-icon><Plus /></el-icon>
              新增字段
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="fieldList" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        
        <el-table-column prop="fieldName" label="字段名称" min-width="150">
          <template #default="{ row }">
            <div class="field-name-cell">
              <el-icon><Document /></el-icon>
              <span>{{ row.fieldName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="fieldCode" label="字段编码" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.fieldCode }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fieldType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getFieldTypeColor(row.fieldType)">
              {{ row.fieldType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="规则概要" min-width="450">
          <template #default="{ row }">
            <el-tooltip effect="dark" placement="top" :show-after="500">
              <template #content>
                <div class="rule-detail-tooltip">
                  <div class="tooltip-title">完整配置详情</div>
                  <pre class="config-json">{{ JSON.stringify(row.ruleConfig, null, 2) }}</pre>
                </div>
              </template>
              <div class="rule-summary-wrapper">
                <el-tag 
                  size="small" 
                  :type="getRuleTypeTagType(row.ruleType)"
                  effect="plain"
                  class="rule-type-tag"
                >
                  {{ getRuleTypeLabel(row.ruleType) }}
                </el-tag>
                <span class="rule-summary">
                  {{ getRuleSummary(row) }}
                </span>
              </div>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.required" color="#67c23a" :size="18">
              <Check />
            </el-icon>
            <span v-else style="color: #ccc;">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row, $index }">
            <el-button link size="small" @click="editField(row, $index)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link size="small" type="primary" @click="testField(row, $index)">
              <el-icon><View /></el-icon>
              测试
            </el-button>
            <el-popconfirm
              title="确定删除该字段吗？"
              @confirm="deleteField($index)"
            >
              <template #reference>
                <el-button link size="small" type="danger">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="fieldList.length === 0" description="暂无字段，点击上方按钮添加">
        <el-button type="primary" @click="addField">立即添加</el-button>
      </el-empty>
    </el-card>

    <!-- 字段编辑对话框 -->
    <el-dialog
      v-model="fieldDialogVisible"
      :title="isNewField ? '新增字段' : '编辑字段'"
      width="1200px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="handleFieldDialogClosed"
    >
      <div class="dialog-content">
        <div class="left-config">
          <el-scrollbar height="600px">
            <FieldConfigForm 
              ref="fieldFormRef"
              :model-value="currentFieldData"
              @update:model-value="handleFieldUpdate"
            />
          </el-scrollbar>
        </div>

        <div class="right-test">
          <FieldTestPanel 
            ref="testPanelRef"
            :field="currentFieldData"
            :test-result="testResult"
            @test="handleTest"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="fieldDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveField">
          {{ isNewField ? '添加' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 基本信息编辑对话框 -->
    <el-dialog
      v-model="basicInfoDialogVisible"
      title="编辑基本信息"
      width="600px"
    >
      <el-form :model="basicInfoForm" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="basicInfoForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编号" required>
          <el-input v-model="basicInfoForm.templateCode" placeholder="请输入模板编号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="basicInfoForm.status" style="width: 100%">
            <el-option label="草稿" value="draft" />
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="basicInfoForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入模板描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="basicInfoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBasicInfo">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量测试对话框 -->
    <el-dialog
      v-model="batchTestDialogVisible"
      title="批量测试"
      width="1000px"
      destroy-on-close
    >
      <BatchTestPanel :template="getTemplateData()" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Edit, Plus, View, Delete, Document } from '@element-plus/icons-vue'
import { getTemplate, updateTemplate } from '@/api/rule-extract'
import { testExtractRule } from '@/api/rule-test'
import { extractObjectData } from '@/utils/response-helper'
import FieldConfigForm from './components/FieldConfigForm.vue'
import FieldTestPanel from './components/FieldTestPanel.vue'
import BatchTestPanel from './components/BatchTestPanel.vue'
import { useEmbedMode } from '@/composables/useEmbedMode'

const route = useRoute()
const router = useRouter()

// 使用统一的嵌入模式管理
const { shouldHideBack, handleBack: embedHandleBack } = useEmbedMode()

// 基本状态
const loading = ref(true)
const saving = ref(false)
const fieldFormRef = ref()
const testPanelRef = ref()

// 模板数据 - 使用普通变量，不使用响应式
let templateId = ''
let templateName = ref('')
let templateCode = ref('')
let templateDescription = ref('')
let templateStatus = ref('draft')
let fieldList = ref<any[]>([])

// 对话框状态
const fieldDialogVisible = ref(false)
const basicInfoDialogVisible = ref(false)
const batchTestDialogVisible = ref(false)

// 字段编辑相关
const isNewField = ref(false)
const currentFieldIndex = ref(-1)
const currentFieldData = ref<any>(null)
const testResult = ref<any>(null)

// 基本信息表单
const basicInfoForm = ref({
  templateName: '',
  templateCode: '',
  description: '',
  status: 'draft'
})

const getFieldTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    string: '',
    number: 'success',
    date: 'warning',
    amount: 'danger',
    boolean: 'info'
  }
  return colorMap[type] || ''
}

// 获取规则类型标签文本
const getRuleTypeLabel = (ruleType: string) => {
  const typeMap: Record<string, string> = {
    'KEYWORD_ANCHOR': '关键词锚点',
    'REGEX_PATTERN': '纯正则',
    'CONTEXT_BOUNDARY': '上下文边界',
    'TABLE_CELL': '表格提取'
  }
  return typeMap[ruleType] || ruleType
}

// 获取规则类型标签颜色
const getRuleTypeTagType = (ruleType: string) => {
  const colorMap: Record<string, string> = {
    'KEYWORD_ANCHOR': 'primary',
    'REGEX_PATTERN': 'warning',
    'CONTEXT_BOUNDARY': 'info',
    'TABLE_CELL': 'success'
  }
  return colorMap[ruleType] || ''
}

const getRuleSummary = (field: any) => {
  if (!field.ruleConfig) return '未配置规则'
  
  const cfg = field.ruleConfig
  const parts: string[] = []
  
  switch (field.ruleType) {
    case 'KEYWORD_ANCHOR':
      // 1. 锚点关键词（必显）
      parts.push(`锚点: "${cfg.anchor || '未设置'}"`)
      
      // 2. 锚点序号（重要！当锚点重复时）
      if (cfg.anchorOccurrence && cfg.anchorOccurrence > 1) {
        parts.push(`锚点第${cfg.anchorOccurrence}个`)
      }
      
      // 3. 方向（重要！总是显示）
      const directionMap: Record<string, string> = {
        'after': '向后',
        'before': '向前', 
        'both': '双向'
      }
      parts.push(`方向: ${directionMap[cfg.direction || 'after'] || cfg.direction}`)
      
      // 4. 提取方法（重要！）
      if (cfg.extractMethod) {
        const methodMap: Record<string, string> = {
          'regex': '正则',
          'line': '行',
          'delimiter': '分隔符',
          'paragraph': '段落'
        }
        parts.push(`方法: ${methodMap[cfg.extractMethod] || cfg.extractMethod}`)
      }
      
      // 5. 正则表达式（重要！）
      if (cfg.pattern) {
        const displayPattern = cfg.pattern.length > 30 
          ? cfg.pattern.substring(0, 30) + '...' 
          : cfg.pattern
        parts.push(`正则: ${displayPattern}`)
      }
      
      // 6. 分隔符（当提取方法是 delimiter 时很重要）
      if (cfg.extractMethod === 'delimiter' && cfg.delimiter) {
        parts.push(`分隔符: "${cfg.delimiter}"`)
      }
      
      // 7. 结果序号（重要！当匹配多次时）
      if (cfg.occurrence && cfg.occurrence > 1) {
        parts.push(`取第${cfg.occurrence}个`)
      }
      
      // 8. 偏移量（非0时显示）
      if (cfg.offset && cfg.offset !== 0) {
        parts.push(`偏移: ${cfg.offset}`)
      }
      
      // 9. 长度限制或最大距离
      const maxLen = cfg.length || cfg.maxDistance
      if (maxLen) {
        parts.push(`范围: ${maxLen}`)
      }
      
      // 10. 锚点类型（非默认值时显示）
      if (cfg.anchorType && cfg.anchorType !== 'exact') {
        const typeMap: Record<string, string> = {
          'fuzzy': '模糊匹配',
          'regex': '正则匹配'
        }
        parts.push(typeMap[cfg.anchorType] || cfg.anchorType)
      }
      
      return parts.join(' | ')
      
    case 'CONTEXT_BOUNDARY':
      const start = cfg.startBoundary || '无'
      const end = cfg.endBoundary || '无'
      parts.push(`开始: "${start}"`)
      parts.push(`结束: "${end}"`)
      if (cfg.inclusive !== undefined) {
        parts.push(cfg.inclusive ? '包含边界' : '不含边界')
      }
      return parts.join(' | ')
      
    case 'REGEX_PATTERN':
      // 1. 正则表达式（必显）
      const displayPattern = cfg.pattern?.length > 50 
        ? cfg.pattern.substring(0, 50) + '...' 
        : (cfg.pattern || '未设置')
      parts.push(`正则: ${displayPattern}`)
      
      // 2. 捕获组（重要！）
      if (cfg.group !== undefined && cfg.group !== 0) {
        parts.push(`捕获组: ${cfg.group}`)
      }
      
      // 3. 序号（重要！）
      if (cfg.occurrence && cfg.occurrence > 1) {
        parts.push(`第${cfg.occurrence}个`)
      }
      
      // 4. 多行模式
      if (cfg.multiline) {
        parts.push('多行模式')
      }
      
      return parts.join(' | ')
      
    case 'TABLE_CELL':
      // 1. 提取模式（必显）
      const mode = cfg.extractMode === 'table' ? '整表' : '单元格'
      parts.push(`模式: ${mode}`)
      
      // 2. 表格定位关键词（重要！）
      if (cfg.tableKeyword) {
        parts.push(`表格: "${cfg.tableKeyword}"`)
      }
      
      // 3. 表头模式（重要！）
      if (cfg.headerPattern) {
        const displayHeader = cfg.headerPattern.length > 30 
          ? cfg.headerPattern.substring(0, 30) + '...'
          : cfg.headerPattern
        parts.push(`表头: ${displayHeader}`)
      }
      
      // 4. 列名（单元格模式下重要）
      if (cfg.columnName) {
        parts.push(`列: "${cfg.columnName}"`)
      }
      
      // 5. 行条件（单元格模式下重要）
      if (cfg.rowCondition) {
        const displayCondition = cfg.rowCondition.length > 30
          ? cfg.rowCondition.substring(0, 30) + '...'
          : cfg.rowCondition
        parts.push(`行: ${displayCondition}`)
      }
      
      // 6. 表格序号（当表格重复时）
      if (cfg.tableOccurrence && cfg.tableOccurrence > 1) {
        parts.push(`第${cfg.tableOccurrence}个表格`)
      }
      
      // 7. 提取方向（对于表格周围的文本）
      if (cfg.direction) {
        const dirMap: Record<string, string> = {
          'above': '表格上方',
          'below': '表格下方',
          'left': '表格左侧',
          'right': '表格右侧'
        }
        parts.push(dirMap[cfg.direction] || cfg.direction)
      }
      
      return parts.join(' | ')
      
    default:
      return '未知规则'
  }
}

const loadTemplate = async () => {
  const id = route.params.id as string
  if (!id || id === 'new') {
    loading.value = false
    return
  }

  templateId = id

  try {
    const res: any = await getTemplate(id as any)
    const templateData = extractObjectData(res)
    
    if (templateData) {
      templateName.value = templateData.templateName
      templateCode.value = templateData.templateCode
      templateDescription.value = templateData.description
      templateStatus.value = templateData.status
      // 转换后端字段格式为前端格式
      fieldList.value = (templateData.fields || []).map((field: any) => convertFieldFromBackend(field))
    }
  } catch (error: any) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const editBasicInfo = () => {
  basicInfoForm.value = {
    templateName: templateName.value,
    templateCode: templateCode.value,
    description: templateDescription.value,
    status: templateStatus.value
  }
  basicInfoDialogVisible.value = true
}

const saveBasicInfo = async () => {
  if (!basicInfoForm.value.templateName) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!basicInfoForm.value.templateCode) {
    ElMessage.warning('请输入模板编号')
    return
  }

  templateName.value = basicInfoForm.value.templateName
  templateCode.value = basicInfoForm.value.templateCode
  templateDescription.value = basicInfoForm.value.description
  templateStatus.value = basicInfoForm.value.status
  basicInfoDialogVisible.value = false
  
  // 立即保存到后台
  try {
    // 转换字段数据为后端格式
    const convertedFields = fieldList.value.map(field => convertFieldToBackend(field))
    
    const data = {
      id: templateId,
      templateName: templateName.value,
      templateCode: templateCode.value,
      description: templateDescription.value,
      status: templateStatus.value,
      fields: convertedFields
    }
    await updateTemplate(templateId as any, data)
    ElMessage.success('基本信息保存成功')
  } catch (error: any) {
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  }
}

const addField = () => {
  const newField = {
    id: `field_${Date.now()}`,
    fieldName: '新字段',
    fieldCode: `field_${fieldList.value.length + 1}`,
    fieldType: 'string',
    required: false,
    ruleType: 'KEYWORD_ANCHOR',
    ruleConfig: {
      anchor: '',
      direction: 'after',
      extractMethod: 'regex',
      pattern: '.*',
      maxDistance: 200,
      delimiter: '：',
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    }
  }
  
  currentFieldData.value = { ...newField }
  isNewField.value = true
  currentFieldIndex.value = -1
  testResult.value = null
  fieldDialogVisible.value = true
}

const editField = (field: any, index: number) => {
  currentFieldData.value = JSON.parse(JSON.stringify(field))
  isNewField.value = false
  currentFieldIndex.value = index
  testResult.value = null
  fieldDialogVisible.value = true
}

const testField = (field: any, index: number) => {
  currentFieldData.value = JSON.parse(JSON.stringify(field))
  isNewField.value = false
  currentFieldIndex.value = index
  testResult.value = null
  fieldDialogVisible.value = true
  setTimeout(() => {
    testPanelRef.value?.focus()
  }, 100)
}

const handleFieldUpdate = (newData: any) => {
  currentFieldData.value = newData
}

const saveField = async () => {
  if (fieldFormRef.value) {
    const fieldData = fieldFormRef.value.getData()
    
    if (!fieldData.fieldName) {
      ElMessage.warning('请输入字段名称')
      return
    }
    if (!fieldData.fieldCode) {
      ElMessage.warning('请输入字段编码')
      return
    }

    if (isNewField.value) {
      fieldList.value.push(fieldData)
    } else {
      fieldList.value[currentFieldIndex.value] = fieldData
    }
    
    fieldDialogVisible.value = false
    
    // 立即保存到后台
    try {
      // 转换字段数据为后端格式
      const convertedFields = fieldList.value.map(field => convertFieldToBackend(field))
      
      const data = {
        id: templateId,
        templateName: templateName.value,
        templateCode: templateCode.value,
        description: templateDescription.value,
        status: templateStatus.value,
        fields: convertedFields
      }
      await updateTemplate(templateId as any, data)
      ElMessage.success(isNewField.value ? '字段添加成功并已保存' : '字段保存成功')
    } catch (error: any) {
      ElMessage.error('保存失败：' + (error.message || '未知错误'))
    }
  }
}

const deleteField = async (index: number) => {
  fieldList.value.splice(index, 1)
  
  // 立即保存到后台
  try {
    // 转换字段数据为后端格式
    const convertedFields = fieldList.value.map(field => convertFieldToBackend(field))
    
    const data = {
      id: templateId,
      templateName: templateName.value,
      templateCode: templateCode.value,
      description: templateDescription.value,
      status: templateStatus.value,
      fields: convertedFields
    }
    await updateTemplate(templateId as any, data)
    ElMessage.success('字段删除成功并已保存')
  } catch (error: any) {
    ElMessage.error('删除失败：' + (error.message || '未知错误'))
  }
}

const handleFieldDialogClosed = () => {
  currentFieldData.value = null
  testResult.value = null
}

const handleTest = async (testText: string) => {
  if (!testText) {
    ElMessage.warning('请输入测试文本')
    return
  }

  try {
    const fieldData = fieldFormRef.value ? fieldFormRef.value.getData() : currentFieldData.value
    
    const res: any = await testExtractRule({
      text: testText,
      ruleType: fieldData.ruleType,
      config: fieldData.ruleConfig,
      debug: true
    })

    const resultData = extractObjectData(res)
    if (resultData) {
      testResult.value = resultData
    }
  } catch (error: any) {
    console.error('测试失败:', error)
    ElMessage.error('测试失败：' + (error.message || '未知错误'))
  }
}

const openBatchTest = () => {
  if (fieldList.value.length === 0) {
    ElMessage.warning('请先添加字段')
    return
  }
  batchTestDialogVisible.value = true
}

/**
 * 将前端字段格式转换为后端格式
 */
const convertFieldToBackend = (field: any) => {
  // 前端格式: { ruleType, ruleConfig }
  // 后端格式: { rules: [{ruleType, ruleContent}] }
  return {
    id: field.id || `field_${Date.now()}_${Math.random()}`,
    fieldName: field.fieldName,
    fieldCode: field.fieldCode,
    fieldType: field.fieldType,
    isRequired: field.required || false,
    rules: [{
      id: `rule_${Date.now()}_${Math.random()}`,
      ruleName: `${field.fieldName}的提取规则`,
      ruleType: field.ruleType,
      ruleContent: JSON.stringify(field.ruleConfig || {}),
      priority: 1,
      isEnabled: true
    }]
  }
}

/**
 * 将后端字段格式转换为前端格式
 */
const convertFieldFromBackend = (field: any) => {
  // 后端格式: { rules: [{ruleType, ruleContent}] }
  // 前端格式: { ruleType, ruleConfig }
  const firstRule = field.rules && field.rules.length > 0 ? field.rules[0] : null
  
  return {
    id: field.id,
    fieldName: field.fieldName,
    fieldCode: field.fieldCode,
    fieldType: field.fieldType,
    required: field.isRequired || false,
    ruleType: firstRule ? firstRule.ruleType : 'KEYWORD_ANCHOR',
    ruleConfig: firstRule ? (typeof firstRule.ruleContent === 'string' ? JSON.parse(firstRule.ruleContent) : firstRule.ruleContent) : {}
  }
}

const getTemplateData = () => {
  return {
    id: templateId,
    templateName: templateName.value,
    templateCode: templateCode.value,
    fields: fieldList.value
  }
}

const saveTemplate = async () => {
  if (!templateName.value) {
    ElMessage.warning('请输入模板名称')
    editBasicInfo()
    return
  }
  if (!templateCode.value) {
    ElMessage.warning('请输入模板编号')
    editBasicInfo()
    return
  }
  if (fieldList.value.length === 0) {
    ElMessage.warning('请至少添加一个字段')
    return
  }

  saving.value = true
  try {
    // 转换字段数据为后端格式
    const convertedFields = fieldList.value.map(field => convertFieldToBackend(field))
    
    const data = {
      id: templateId,
      templateName: templateName.value,
      templateCode: templateCode.value,
      description: templateDescription.value,
      status: templateStatus.value,
      fields: convertedFields
    }
    await updateTemplate(templateId as any, data)
    ElMessage.success('模板保存成功')
  } catch (error: any) {
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const handleBack = () => {
  embedHandleBack(() => {
    // 默认的返回逻辑
    router.push('/rule-extract/templates')
  })
}

onMounted(() => {
  loadTemplate()
})
</script>

<style scoped lang="scss">
.template-designer-page {
  padding: 16px;

  .page-header-content {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 18px;
    font-weight: 600;
  }

  .main-card {
    margin-top: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;

      .header-actions {
        display: flex;
        gap: 8px;
      }
    }

    .field-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .rule-summary-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: help;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background-color 0.2s;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      .rule-type-tag {
        flex-shrink: 0;
        font-weight: 600;
        border-width: 1.5px;
      }
      
      .rule-summary {
        font-size: 12px;
        color: #606266;
        line-height: 1.6;
        flex: 1;
      }
    }
  }
  
  // 工具提示样式
  .rule-detail-tooltip {
    max-width: 600px;
    
    .tooltip-title {
      font-weight: 600;
      margin-bottom: 8px;
      padding-bottom: 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.2);
      font-size: 14px;
    }
    
    .config-json {
      margin: 0;
      padding: 8px;
      background-color: rgba(0, 0, 0, 0.3);
      border-radius: 4px;
      font-size: 12px;
      line-height: 1.5;
      max-height: 400px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-all;
      
      &::-webkit-scrollbar {
        width: 6px;
      }
      
      &::-webkit-scrollbar-thumb {
        background-color: rgba(255, 255, 255, 0.3);
        border-radius: 3px;
      }
    }
  }

  .dialog-content {
    display: grid;
    grid-template-columns: 700px 1fr;
    gap: 20px;
    height: 600px;

    .left-config {
      border-right: 1px solid #ebeef5;
      padding-right: 20px;
    }

    .right-test {
      padding-left: 20px;
      height: 600px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
  }
}
</style>

