<template>
  <div class="compose-page">
    <div class="toolbar">
      <div class="left">前端合成</div>
      <div class="right">
        <el-button type="primary" :loading="submitting" @click="doCompose">合成</el-button>
      </div>
    </div>
    <div class="body" v-loading="loading">
      <div class="preview">
        <OnlyOfficeEditor
          v-if="templateFileId"
          ref="editorRef"
          :file-id="templateFileId"
          :can-edit="true"
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
            <el-form-item :label="el.customName || el.name || el.tag" class="field-card">
              <template v-if="isRich(el)">
                <div class="field-box is-rich">
                  <el-input
                    v-model="formValues[el.tag]"
                    type="textarea"
                    :rows="6"
                    placeholder="支持HTML，默认可粘贴'销售产品清单'表格"
                    clearable
                    resize="vertical"
                    @focus="onFieldFocus(el)"
                    @input="onRichFieldInput(el)"
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
                    <span>边框颜色</span>
                    <el-color-picker v-model="richOptions[el.tag].borderColor" />
                  </div>
                  <div style="display:flex; align-items:center; gap:6px;">
                    <span>文字颜色</span>
                    <el-color-picker v-model="richOptions[el.tag].textColor" />
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
                  <el-button class="mt8" type="warning" plain @click="rebuildContentControl(el)">重建内容块</el-button>
                </div>
              </template>
              <template v-else>
                <div class="field-box">
                  <el-input
                    v-model="formValues[el.tag]"
                    placeholder="请输入"
                    clearable
                    size="large"
                    :prefix-icon="EditPen"
                    @focus="onFieldFocus(el)"
                    @input="onFieldInput(el)"
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
import { composeContract } from '@/api/contract-compose'
import { getTemplateDesignByTemplateId } from '@/api/templateDesign'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const templateId = ref<string>('')
const templateFileId = ref<string>('')

// elements: [{ key, tag, type, name, customName, richText, meta }]
const formElements = ref<Array<any>>([])
const formValues = ref<Record<string, string>>({})
const editorRef = ref<any>(null)

// 富文本选项：按字段tag存储
const richOptions = ref<Record<string, { align: 'left' | 'center' | 'right'; headerBg: string; borderColor: string; textColor: string; fontSize: number; fontUnit: 'px' | 'pt' | 'em' | 'rem' }>>({})

const isDefaultTemplate = computed(() => 
  templateId.value === 'demo' && templateFileId.value === '9999'
)

onMounted(async () => {
  // 从路由读取模板信息（无则默认demo）
  templateId.value = String(route.query.templateId || 'demo')
  templateFileId.value = String(route.query.fileId || '')
  await loadTemplateDesign()
})

async function loadTemplateDesign() {
  try {
    loading.value = true
    
    // 动态从后端读取模板设计记录
    const resp = await getTemplateDesignByTemplateId(templateId.value) as any
    if (resp?.code !== 200) {
      throw new Error(resp?.message || '获取模板设计失败')
    }
    const record = resp.data
    if (!record) {
      throw new Error('未找到模板设计记录')
    }
    // record.elementsJson 结构假设为 { elements: [...] } 或 直接数组，做兼容
    try {
      const parsed = JSON.parse(record.elementsJson || '{}')
      const elements = Array.isArray(parsed) ? parsed : (parsed.elements || [])
      formElements.value = elements
      // 初始化富文本选项
      elements.forEach((el: any) => {
        if (isRich(el)) ensureRichOptions(el.tag)
      })
    } catch (e) {
      formElements.value = []
    }
    // 设置模板文件ID（优先使用记录中的fileId）
    if (record.fileId) {
      templateFileId.value = record.fileId
    } else if (!templateFileId.value) {
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

function ensureRichOptions(tag: string) {
  if (!richOptions.value[tag]) {
    richOptions.value[tag] = {
      align: 'center',
      headerBg: '#f5f7fa',
      borderColor: '#dddddd',
      textColor: '#333333',
      fontSize: 14,
      fontUnit: 'px'
    }
  }
  return richOptions.value[tag]
}

function insertDefaultTable(tag: string) {
  const opts = ensureRichOptions(tag)
  const align = opts.align
  const headerBg = opts.headerBg
  const borderColor = opts.borderColor
  const textColor = opts.textColor
  const fontSize = opts.fontSize
  const fontUnit = opts.fontUnit
  const html = `
  <table style="width:100%; border-collapse: collapse; text-align:${align}; color:${textColor}; font-size:${fontSize}${fontUnit};">
    <thead>
      <tr style="background:${headerBg};">
        <th style="border:1px solid ${borderColor}; padding:6px;">序号</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">产品名称</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">规格型号</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">数量</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">单价(元)</th>
        <th style="border:1px solid ${borderColor}; padding:6px;">金额(元)</th>
      </tr>
    </thead>
    <tbody>
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

async function doCompose() {
  if (!templateFileId.value) {
    ElMessage.warning('缺少模板文件ID')
    return
  }
  try {
    submitting.value = true
    const actualFileId = isDefaultTemplate.value ? '9999' : templateFileId.value
    const payload = {
      templateFileId: actualFileId,
      values: { ...formValues.value }
    }
    const res = await composeContract(payload)
    const fileId = res.data.fileId
    ElMessage.success('合成成功')
    router.push({
      path: `/contract-compose-frontend/result/${encodeURIComponent(fileId)}`,
      query: { templateId: templateId.value, fileId: templateFileId.value }
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '合成失败')
  } finally {
    submitting.value = false
  }
}

// 联动功能：当输入框获得焦点时，定位到左侧文档对应位置
async function onFieldFocus(el: { tag: string }) {
  try {
    await (editorRef.value?.postToPlugin?.({ action: 'getContentControl', Tag: el.tag }) || Promise.resolve())
  } catch (e) {
    console.warn('定位到文档位置失败:', e)
  }
}

// 联动功能：当输入内容变化时，实时更新左侧文档内容
async function onFieldInput(el: { tag: string }) {
  try {
    await (editorRef.value?.postToPlugin?.({
      action: 'setContentControlText',
      Tag: el.tag,
      Text: (formValues.value as any)[el.tag] || ''
    }) || Promise.resolve())
  } catch (e) {
    console.warn('更新文档内容失败:', e)
  }
}

// 富文本字段的联动处理
async function onRichFieldInput(el: { tag: string }) {
  try {
    await (editorRef.value?.postToPlugin?.({
      action: 'setContentControlText',
      Tag: el.tag,
      Text: (formValues.value as any)[el.tag] || ''
    }) || Promise.resolve())
  } catch (e) {
    console.warn('更新富文本内容失败:', e)
  }
}

async function rebuildContentControl(el: { key: string; tag: string; name: string }) {
  if (!editorRef.value) {
    ElMessage.warning('编辑器未就绪')
    return
  }
  try {
    ElMessage.info('正在重建内容块...')
    
    // 1. 删除已存在的内容控件
    await editorRef.value.postToPlugin({ action: 'deleteContentControl', Tag: el.tag })

    // 等待编辑器处理删除操作
    await new Promise(resolve => setTimeout(resolve, 200))

    // 2. 重新创建块级内容控件
    await editorRef.value.createBlockContentControl(el.key, el.tag, el.name, true, true)
    
    // 3. 恢复输入框中的内容到新创建的控件中
    await onRichFieldInput(el)

    ElMessage.success('内容块已成功重建')
  } catch (error) {
    console.error('重建内容块失败:', error)
    ElMessage.error('重建内容块失败，请查看控制台')
  }
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
.field-card :deep(.el-form-item__label) { color: #606266; font-weight: 600; }
.field-box { display: flex; flex: 1; }
.field-box :deep(.el-input__wrapper) { padding: 6px 10px; border-radius: 8px; box-shadow: inset 0 0 0 1px #dcdfe6; }
.field-box :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 3px rgba(51,112,255,.12), inset 0 0 0 1px #3370ff; }
.field-box :deep(.el-textarea__inner) { border-radius: 8px; box-shadow: inset 0 0 0 1px #dcdfe6; }
.field-box.is-rich :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 3px rgba(51,112,255,.12), inset 0 0 0 1px #3370ff; }

/* 调整颜色选择与数值输入组件间距 */
.form :deep(.el-color-picker) { vertical-align: middle; }
.form :deep(.el-input-number) { width: 120px; }
</style>
