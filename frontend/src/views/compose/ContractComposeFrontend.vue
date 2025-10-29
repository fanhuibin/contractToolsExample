<template>
  <div class="compose-page">
    <div class="toolbar">
      <div class="left">前端合成 - 编辑合同</div>
      <div class="right">
        <el-button type="success" @click="saveAndDownload" :disabled="!contractFileId">
          <el-icon style="margin-right: 4px;"><Check /></el-icon>
          完成编辑
        </el-button>
      </div>
    </div>
    <div class="body" v-loading="loading">
      <div class="preview">
        <OnlyOfficeEditor
          v-if="workingFileId"
          :key="editorKey"
          ref="editorRef"
          :file-id="workingFileId"
          :can-edit="true"
          :can-review="false"
          :height="'100%'"
          :show-toolbar="false"
          :show-status="false"
          :update-onlyoffice-key="true"
        />
        <div v-else class="empty">
          <el-icon class="is-loading" style="font-size: 32px; margin-bottom: 16px;"><Loading /></el-icon>
          <p>正在创建合同文档，请稍候...</p>
        </div>
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
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { EditPen, DocumentAdd, Check, Loading } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { composeContract, createContractFromTemplate } from '@/api/contract-compose'
import { getTemplateDesignByTemplateId, getTemplateDesignDetail } from '@/api/templateDesign'
import { forceSaveFile } from '@/api/file'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const recordId = ref<string>('')
const templateId = ref<string>('')
const templateFileId = ref<string>('')  // 模板文件ID
const contractFileId = ref<string>('')  // 创建的合同文件ID（合成后）
const workingFileId = computed(() => contractFileId.value || templateFileId.value)  // 显示在编辑器中的文件ID
const editorKey = ref(0)  // 用于强制刷新编辑器

// elements: [{ key, tag, type, name, customName, richText, meta }]
const formElements = ref<Array<any>>([])
const formValues = ref<Record<string, string>>({})
const editorRef = ref<any>(null)

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

// 监听合同创建，刷新编辑器
watch(contractFileId, async (newVal) => {
  if (newVal) {
    editorKey.value++
    await nextTick()
  }
})

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
    } else if (!templateFileId.value) {
      throw new Error('模板记录中未找到文件ID')
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
    
    // 自动创建合同（前端合成需要立即进入编辑模式）
    await autoCreateContract()
    
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
      headerTextColor: '#333333',
      bodyTextColor: '#606266',
      borderColor: '#dddddd',
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

// 自动创建合同（加载模板后自动调用）
async function autoCreateContract() {
  if (!templateFileId.value) {
    return
  }
  try {
    // 调用后端接口复制模板文件，创建合同
    const res = await createContractFromTemplate(templateFileId.value) as any
    
    if (res?.data?.code !== 200) {
      throw new Error(res?.data?.message || '创建合同失败')
    }
    
    const contractFile = res.data.data || res.data
    const fileId = String(contractFile.id)
    
    if (!fileId) {
      throw new Error('合同创建成功但未返回文件ID')
    }
    
    // 设置合同文件ID，编辑器会自动切换到合同文档
    contractFileId.value = fileId
  } catch (e: any) {
    ElMessage.error('创建合同失败：' + (e?.message || '未知错误'))
    throw e
  }
}

// 保存并查看结果
async function saveAndDownload() {
  if (!contractFileId.value) {
    ElMessage.warning('请先创建合同')
    return
  }
  
  try {
    // 调用 Command Service 强制保存
    const commandResult = await forceSaveFile(contractFileId.value)
    
    // 检查响应并显示结果
    if (commandResult.data && commandResult.data.code === 200) {
      const responseData = commandResult.data.data
      
      if (responseData && responseData.commandServiceResponse) {
        const errorCode = responseData.commandServiceResponse.error
        
        if (errorCode === 0) {
          ElMessage.success('保存完成')
        } else if (errorCode === 4) {
          ElMessage.success('文档未修改，无需保存')
        } else if (errorCode === 6) {
          console.error('error=6: 令牌无效')
          ElMessage.error('保存失败：令牌无效，请检查配置')
        } else {
          ElMessage.warning(`保存失败，错误码: ${errorCode}`)
        }
      } else {
        ElMessage.success('保存完成')
      }
    } else {
      console.error('保存失败，响应码不是200:', commandResult)
      ElMessage.error('保存失败：' + (commandResult.data?.message || '未知错误'))
    }
    
    // 跳转到结果页面
    router.push({
      path: `/contract-compose-frontend/result/${encodeURIComponent(contractFileId.value)}`,
      query: { 
        id: recordId.value,
        templateId: templateId.value, 
        fileId: templateFileId.value 
      }
    })
  } catch (error: any) {
    console.error('保存失败', error)
    ElMessage.error('保存失败：' + (error?.message || '未知错误'))
  }
}

// 联动功能：当输入框获得焦点时，定位到左侧文档对应位置
async function onFieldFocus(el: { tag: string }) {
  try {
    await (editorRef.value?.postToPlugin?.({ action: 'getContentControl', Tag: el.tag }) || Promise.resolve())
  } catch (e) {
    // 静默处理定位失败
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
    // 静默处理更新失败
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
    // 静默处理更新失败
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
.empty { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  height: 100%; 
  padding: 24px; 
  color: #999; 
  font-size: 14px;
}
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
