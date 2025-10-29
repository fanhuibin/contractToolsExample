<template>
  <div class="compose-page">
    <div class="toolbar">
      <div class="left">合同合成</div>
      <div class="right">
        <el-button type="primary" :loading="submitting" @click="doCompose">合成</el-button>
      </div>
    </div>
    <div class="body" v-loading="loading">
      <div class="preview">
        <OnlyOfficeEditor
          v-if="templateFileId"
          :file-id="templateFileId"
          :can-edit="false"
          :can-review="false"
          :height="'100%'"
          :show-toolbar="false"
          :show-status="false"
        />
        <div v-else class="empty">请选择模板或传入 templateId 以加载预览</div>
      </div>
      <div class="form">
        <el-form label-width="120px" class="compose-form" :inline="false">
          <template v-for="(el, idx) in formElements" :key="idx">
            <el-form-item :label="displayName(el)" class="field-card">
              <template v-if="isRich(el)">
                <div class="field-box is-rich">
                  <el-input
                    v-model="formValues[el.tag]"
                    type="textarea"
                    :rows="6"
                    placeholder="支持HTML，默认可粘贴‘销售产品清单’表格"
                    clearable
                    resize="vertical"
                  />
                </div>
                <div class="mt8" style="display:flex; gap:8px; align-items:center; flex-wrap: wrap;">
                  <el-select v-model="richOptions[el.tag].align" placeholder="对齐" style="width: 120px;">
                    <el-option label="左对齐" value="left" />
                    <el-option label="居中" value="center" />
                    <el-option label="右对齐" value="right" />
                  </el-select>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>表头背景</span>
                    <el-color-picker v-model="richOptions[el.tag].headerBg" />
                  </div>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>表头文字</span>
                    <el-color-picker v-model="richOptions[el.tag].headerTextColor" />
                  </div>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>内容文字</span>
                    <el-color-picker v-model="richOptions[el.tag].bodyTextColor" />
                  </div>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>边框颜色</span>
                    <el-color-picker v-model="richOptions[el.tag].borderColor" />
                  </div>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>字体大小</span>
                    <el-input-number v-model="richOptions[el.tag].fontSize" :min="8" :max="48" :step="1" />
                    <el-select v-model="richOptions[el.tag].fontUnit" style="width: 90px;">
                      <el-option label="px" value="px" />
                      <el-option label="pt" value="pt" />
                      <el-option label="em" value="em" />
                      <el-option label="rem" value="rem" />
                    </el-select>
                  </div>
                  <el-button class="mt8" @click="insertDefaultTable(el.tag)">插入示例表格</el-button>
                </div>
              </template>
              <template v-else>
                <!-- 印章类型 -->
                <div v-if="isSeal(el)" class="field-box" style="flex-direction: column; gap:8px;">
                  <el-input
                    v-model="sealUrl.normal[el.tag]"
                    placeholder="请输入公司公章图片URL（仅用于盖章，不会插入文档）"
                    clearable
                  />
                  <el-input
                    v-model="sealUrl.riding[el.tag]"
                    placeholder="请输入骑缝章图片URL（仅用于盖章，不会插入文档）"
                    clearable
                  />
                </div>
                <!-- 条款类型（使用条款编辑器） -->
                <div v-else-if="isClause(el)" class="field-box clause-field">
                  <ClauseEditor
                    v-model="formValues[el.tag]"
                    :existing-fields="formValues"
                    @update:variables="onClauseVariablesUpdate(el.tag, $event)"
                  />
                </div>
                <!-- 普通字段 -->
                <div v-else class="field-box">
                  <el-input
                    v-model="formValues[el.tag]"
                    placeholder="请输入"
                    clearable
                    size="large"
                    :prefix-icon="EditPen"
                  />
                </div>
              </template>
            </el-form-item>
          </template>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { EditPen } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { composeContract, type ComposeRequest } from '@/api/contract-compose'
import { getTemplateDesignByTemplateId, getTemplateDesignDetail } from '@/api/templateDesign'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import ClauseEditor from '@/components/ClauseEditor.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const recordId = ref<string>('')
const templateId = ref<string>('')
const templateFileId = ref<string>('')

// elements: [{ key, tag, type, name, customName, richText, meta }]
const formElements = ref<Array<any>>([])
const formValues = ref<Record<string, string>>({})
// 用户提供的印章/骑缝章URL（仅提交给后端用于盖章，不插入文档）
const sealUrl = ref<{ normal: Record<string, string>; riding: Record<string, string> }>({ normal: {}, riding: {} })

// 富文本选项：按字段tag存储
const richOptions = ref<Record<string, { 
  align: 'left' | 'center' | 'right'; 
  headerBg: string; 
  headerTextColor: string; 
  bodyTextColor: string; 
  borderColor: string; 
  fontSize: number; 
  fontUnit: 'px' | 'pt' | 'em' | 'rem' 
}>>({})

const isDefaultTemplate = computed(() => 
  templateId.value === 'demo' && templateFileId.value === '9999'
)

onMounted(async () => {
  // 从路由读取模板信息（优先使用新的 id 参数）
  recordId.value = String(route.query.id || '')
  templateId.value = String(route.query.templateId || 'demo')
  templateFileId.value = String(route.query.fileId || '')
  await loadTemplateDesign()
})

async function loadTemplateDesign() {
  try {
    loading.value = true
    
    let resp: any
    let record: any
    
    // 优先使用新的 id 参数（记录ID）
    if (recordId.value) {
      resp = await getTemplateDesignDetail(recordId.value) as any
      if (resp?.data?.code !== undefined) {
        if (resp.data.code !== 200) {
          throw new Error(resp.data.message || '获取模板设计失败')
        }
        record = resp.data.data
      } else {
        record = resp.data
      }
    } else if (templateId.value && templateId.value !== 'demo') {
      // 降级使用旧的 templateId 参数（向后兼容）
      resp = await getTemplateDesignByTemplateId(templateId.value) as any
      if (resp?.code !== 200) {
        throw new Error(resp?.message || '获取模板设计失败')
      }
      record = resp.data
    } else {
      throw new Error('未提供模板ID参数')
    }
    
    if (!record) {
      throw new Error('未找到模板设计记录')
    }
    
    // 更新 fileId
    if (record.fileId) {
      templateFileId.value = record.fileId
    }
    
    // record.elementsJson 结构假设为 { elements: [...] } 或 直接数组，做兼容
    try {
      const parsed = JSON.parse(record.elementsJson || '{}')
      const elements = Array.isArray(parsed) ? parsed : (parsed.elements || [])
      formElements.value = elements
      
      // 初始化各类字段
      elements.forEach((el: any) => {
        // 1. 富文本字段
        if (isRich(el)) {
          ensureRichOptions(el.tag)
        }
        
        // 2. 印章字段
        if (isSeal(el)) {
          if (!sealUrl.value.normal[el.tag]) sealUrl.value.normal[el.tag] = ''
          if (!sealUrl.value.riding[el.tag]) sealUrl.value.riding[el.tag] = ''
          // 确保印章元素在values中占位，以便后端注入隐藏标识
          if (formValues.value[el.tag] === undefined) {
            formValues.value[el.tag] = ''
          }
        }
        
        // 3. 条款字段 - 初始化条款模板文本
        if (isClause(el)) {
          // 从 meta.content 中读取条款模板文本（包含变量的原始文本）
          const clauseTemplate = el.meta?.content || el.content || ''
          if (clauseTemplate) {
            formValues.value[el.tag] = clauseTemplate
          } else {
            // 如果没有模板文本，给一个默认提示
            formValues.value[el.tag] = ''
          }
        }
      })
      
    } catch (e) {
      console.error('解析elementsJson失败:', e)
      formElements.value = []
    }
    
    // 设置模板文件ID（优先使用记录中的fileId）
    if (record.fileId) {
      templateFileId.value = record.fileId
    } else if (templateFileId.value) {
      // 使用路由传入的文件ID
    } else {
      throw new Error('模板记录中未找到文件ID')
    }
    
  } catch (e: any) {
    ElMessage.error(e?.message || '加载模板失败')
  } finally {
    loading.value = false
  }
}

function isRich(el: any): boolean {
  const r1 = el?.richText
  const r2 = el?.meta?.richText
  const r3 = el?.meta?.isRichText
  const toBool = (v: any) => v === true || v === 'true' || v === 'TRUE' || v === 'True'
  return toBool(r1) || toBool(r2) || toBool(r3)
}

function isSeal(el: any): boolean {
  const tag: string = el?.tag || ''
  const name: string = (el?.customName || el?.name || '').toLowerCase()
  return tag.toLowerCase().includes('seal') || name.includes('公章') || name.includes('骑缝章')
}

// 判断是否是条款类型
function isClause(el: any): boolean {
  // 方法1: 通过 type 字段判断
  if (el?.type === 'clause' || el?.type === 'CLAUSE') {
    return true
  }
  // 方法2: 通过 meta 字段判断
  if (el?.meta?.type === 'clause' || el?.meta?.isClause === true) {
    return true
  }
  // 方法3: 通过标签名或显示名判断（包含"条款"关键字）
  const tag: string = el?.tag || ''
  const name: string = (el?.customName || el?.name || '').toLowerCase()
  return tag.toLowerCase().includes('clause') || name.includes('条款')
}

function ensureRichOptions(tag: string) {
  if (!richOptions.value[tag]) {
    richOptions.value[tag] = {
      align: 'center',
      headerBg: '#f5f7fa',
      headerTextColor: '#333333',
      bodyTextColor: '#606266',
      borderColor: '#dddddd',
      fontSize: 14,
      fontUnit: 'px'
    }
  }
  return richOptions.value[tag]
}

function displayName(el: any): string {
  // 优先: 自定义显示名 → 元信息可能的别名 → 退化为 name → 最后 tag
  const n1 = el?.customName
  const n2 = el?.meta?.customName
  const n3 = el?.meta?.displayName
  const n4 = el?.name
  const n5 = el?.tag
  return String(n1 || n2 || n3 || n4 || n5 || '')
}

function insertDefaultTable(tag: string) {
  const opts = ensureRichOptions(tag)
  const align = opts.align
  const headerBg = opts.headerBg
  const headerTextColor = opts.headerTextColor
  const bodyTextColor = opts.bodyTextColor
  const borderColor = opts.borderColor
  const fontSize = opts.fontSize
  const fontUnit = opts.fontUnit
  const html = `
  <table style="width:100%; border-collapse: collapse; text-align:${align}; font-size:${fontSize}${fontUnit};">
    <thead>
      <tr style="background:${headerBg}; color:${headerTextColor};">
        <th style="border:1px solid ${borderColor}; padding:6px;">序号</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">产品名称</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">规格型号</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">数量</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">单价(元)</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">金额(元)</th>
      </tr>
    </thead>
    <tbody style="color:${bodyTextColor};">
      <tr>
        <td style="border:1px solid ${borderColor}; padding:6px;">1</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">示例产品A</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">X-100</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">10</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">1000.00</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">10000.00</td>
      </tr>
      <tr>
        <td style="border:1px solid ${borderColor}; padding:6px;">2</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">示例产品B</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">Z-200</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">5</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">500.00</td>
        <td style="border:1px solid ${borderColor}; padding:6px;">2500.00</td>
      </tr>
    </tbody>
  </table>`
  formValues.value[tag] = html
}

// 处理条款变量更新
function onClauseVariablesUpdate(clauseTag: string, variables: Record<string, string>) {
  // 同步变量值到 formValues
  // ⚠️ 重要：无论变量是否关联表单字段，都要添加到 formValues 中
  // 因为后端需要用这些变量值来替换条款中的占位符
  Object.keys(variables).forEach(varName => {
    const varValue = variables[varName]
    
    // 检查该变量是否对应某个表单字段
    const existingField = formElements.value.find(el => el.tag === varName)
    
    // 无论是否关联字段，都添加到 formValues 中
    formValues.value[varName] = varValue
  })
}

async function doCompose() {
  if (!templateFileId.value) {
    ElMessage.warning('缺少模板文件ID')
    return
  }
  
  try {
    submitting.value = true
    
    // 如果是默认模板，使用模板设计页面的默认文件ID
    const actualFileId = isDefaultTemplate.value ? '9999' : templateFileId.value
    
    const payload: ComposeRequest = {
      templateFileId: actualFileId,
      values: { ...formValues.value },
      stampImageUrls: buildStampImageUrls()
    }
    
    const res = await composeContract(payload)
    const composeData = res.data.data || res.data
    const composedFileId = composeData.fileId
    
    if (!composedFileId) {
      throw new Error('合成成功但未返回文件ID')
    }
    
    ElMessage.success('合成成功')
    router.push({
      path: `/contract-compose/result/${encodeURIComponent(composedFileId)}`,
      query: {
        templateId: templateId.value,
        templateFileId: templateFileId.value, // 模板文件ID
        composedFileId: composedFileId, // 合成后的文件ID
        docxPath: composeData.docxPath || '',
        pdfPath: composeData.pdfPath || '',
        stampedPdfPath: composeData.stampedPdfPath || '',
        ridingStampPdfPath: composeData.ridingStampPdfPath || ''
      }
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '合成失败')
  } finally {
    submitting.value = false
  }
}

function buildStampImageUrls(): Record<string, { normal?: string; riding?: string }> {
  const map: Record<string, { normal?: string; riding?: string }> = {}
  Object.keys(sealUrl.value.normal).forEach(tag => {
    const normal = (sealUrl.value.normal[tag] || '').trim()
    const riding = (sealUrl.value.riding[tag] || '').trim()
    if (normal || riding) {
      map[tag] = {}
      if (normal) map[tag].normal = normal
      if (riding) map[tag].riding = riding
    }
  })
  return map
}
</script>

<style scoped>
.compose-page { display: flex; flex-direction: column; height: 100%; }
.toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; border-bottom: 1px solid #ebeef5; background: #ffffff; }
.toolbar .left { font-weight: 600; font-size: 16px; color: #303133; }
.body { display: grid; grid-template-columns: 1fr 440px; gap: 14px; padding: 14px; height: calc(100vh - 56px); background: #f5f7fa; }
.preview { border: 1px solid #ebeef5; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,.06); }
.preview :deep(.onlyoffice-editor),
.preview :deep(#onlyoffice-editor-container) { width: 100%; height: 100%; }
.form { border: 1px solid #ebeef5; padding: 14px; overflow: auto; background: #fff; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,.06); }
.empty { padding: 24px; color: #999; }
.mt8 { margin-top: 8px; }

/* 表单整体视觉 */
.compose-form :deep(.el-form-item) { margin-bottom: 14px; }
.field-card { padding: 8px 10px; border: 1px solid #f0f2f5; border-radius: 10px; background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%); box-shadow: inset 0 0 0 1px rgba(0,0,0,0.02); }
.field-card :deep(.el-form-item__label) { color: #606266; font-weight: 600; font-size: 16px; line-height: 40px; display: flex; align-items: center; }
.compose-form :deep(.el-form-item) { align-items: center; }
.field-box { display: flex; flex: 1; }
.field-box :deep(.el-input__wrapper) { padding: 6px 10px; border-radius: 8px; box-shadow: inset 0 0 0 1px #dcdfe6; }
.field-box :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 3px rgba(51,112,255,.12), inset 0 0 0 1px #3370ff; }
.field-box :deep(.el-textarea__inner) { border-radius: 8px; box-shadow: inset 0 0 0 1px #dcdfe6; }
.field-box.is-rich :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 3px rgba(51,112,255,.12), inset 0 0 0 1px #3370ff; }

/* 条款字段样式 */
.field-box.clause-field {
  width: 100%;
  flex-direction: column;
  margin-top: 32px; /* 为上方的提示预留空间 */
}
.field-box.clause-field :deep(.clause-editor) {
  width: 100%;
}

/* 条款字段的卡片样式调整 */
.field-card:has(.clause-field) {
  padding-bottom: 16px;
}

/* 调整颜色选择与数值输入组件间距 */
.form :deep(.el-color-picker) { vertical-align: middle; }
.form :deep(.el-input-number) { width: 120px; }
</style>


