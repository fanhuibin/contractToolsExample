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

        <el-table-column label="规则概要" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="rule-summary">{{ getRuleSummary(row) }}</div>
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

const getRuleSummary = (field: any) => {
  if (!field.ruleConfig) return '未配置规则'
  
  switch (field.ruleType) {
    case 'KEYWORD_ANCHOR':
      return `锚点: ${field.ruleConfig.anchor || '未设置'} | 方向: ${field.ruleConfig.direction || 'after'}`
    case 'CONTEXT_BOUNDARY':
      const start = field.ruleConfig.startBoundary || '无'
      const end = field.ruleConfig.endBoundary || '无'
      return `开始: ${start} | 结束: ${end}`
    case 'REGEX_PATTERN':
      return `正则: ${field.ruleConfig.pattern || '未设置'}`
    case 'TABLE_CELL':
      const mode = field.ruleConfig.extractMode === 'table' ? '整表' : '单元格'
      return `模式: ${mode} | 表头: ${field.ruleConfig.headerPattern || '未设置'}`
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

    .rule-summary {
      font-size: 12px;
      color: #606266;
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

