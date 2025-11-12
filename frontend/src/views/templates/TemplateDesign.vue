<template>
  <div class="template-design-page">
    <div class="design-actions">
      <el-button size="small" @click="goBack">
        <el-icon style="margin-right: 4px;"><ArrowLeft /></el-icon>
        返回
      </el-button>
      <el-button 
        type="primary" 
        size="small" 
        @click="handleSave"
        :loading="saving"
        :disabled="isDemoMode"
      >
        <el-icon style="margin-right: 4px;"><DocumentChecked /></el-icon>
        保存模板
      </el-button>
    </div>
    <div class="design-body" :style="{ gridTemplateColumns: gridColumns }">
      <div class="left-panel">
        <template v-if="!leftCollapsed">
        <InsertableElementsPanel :fields="fields" @insert="insertTag" />
        <!-- 左侧折叠切换：位于与编辑区交界处 -->
        <div class="edge-toggle edge-toggle--left" title="收起" @click="leftCollapsed = true">
          <el-icon><CaretLeft /></el-icon>
        </div>
        </template>
        <template v-else>
          <div class="edge-toggle edge-toggle--left is-collapsed" title="展开" @click="leftCollapsed = false">
            <el-icon><CaretRight /></el-icon>
          </div>
        </template>
      </div>
      <div class="editor-panel">
        <OnlyOfficeEditor
          ref="editorRef"
          :file-id="fileId"
          :can-edit="!isDemoMode"
          :can-review="false"
          :height="'100%'"
          :show-toolbar="false"
          :show-status="false"
          :update-onlyoffice-key="true"
          @ready="onEditorReady"
          @pluginLoaded="onPluginLoaded"
        />
      </div>
      <div class="right-panel">
        <template v-if="!rightCollapsed">
        <InsertedElementsPanel :elements="elements" :fields="fields" @locate="locateElement" @edit="editElement" @delete="deleteElement" />
        <!-- 右侧折叠切换：位于与编辑区交界处 -->
        <div class="edge-toggle edge-toggle--right" title="收起" @click="rightCollapsed = true">
          <el-icon><CaretRight /></el-icon>
        </div>
        </template>
        <template v-else>
          <div class="edge-toggle edge-toggle--right is-collapsed" title="展开" @click="rightCollapsed = false">
            <el-icon><CaretLeft /></el-icon>
          </div>
        </template>
      </div>
    </div>
  </div>
  
  <!-- 新增元素弹窗（统一：显示名 + 仅签约方展示签约方选择） -->
  <el-dialog v-model="insertDialogVisible" title="新增元素" width="420px" append-to-body>
    <el-form label-width="80px">
      <el-form-item label="显示名称">
        <el-input v-model="insertForm.customName" placeholder="请输入元素显示名称" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item v-if="pendingInsert?.type === 'party'" label="签约方">
        <el-select v-model="insertForm.partyIndex" placeholder="请选择">
          <el-option v-for="n in 7" :key="n" :label="`签约方${n}`" :value="String(n)" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="cancelInsert">取 消</el-button>
        <el-button type="primary" @click="confirmInsert" :loading="insertDialogSubmitting">确 定</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Tickets, User, Stamp, Search, Edit, Delete, CaretLeft, CaretRight, ArrowLeft, DocumentChecked } from '@element-plus/icons-vue'
import InsertableElementsPanel from '@/components/template-design/InsertableElementsPanel.vue'
import InsertedElementsPanel from '@/components/template-design/InsertedElementsPanel.vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { fetchTemplateFields, saveTemplateDesign, getTemplateDesignDetail, getTemplateDesignByTemplateId, fetchCustomFieldsConfig } from '@/api/templateDesign'
import { forceSaveFile } from '@/api/file'
import { getSystemConfig } from '@/api/system'

const route = useRoute()
const router = useRouter()
const recordId = computed(() => (route.query.id as string) || '')
const templateId = computed(() => (route.query.templateId as string) || '')
const fileId = computed(() => (route.query.fileId as string) || '')
const returnUrl = computed(() => (route.query.returnUrl as string) || '/templates')
const isEmbedMode = computed(() => route.query.embed === 'true')
const designId = ref<string>('')

const fields = reactive<any>({ baseFields: [], clauseFields: [], counterpartyFields: [], sealFields: [] })
const activeTab = ref<'base' | 'clause' | 'party' | 'seal'>('base')
const keyword = ref('')
const keywordClause = ref('')
const keywordParty = ref('')
const keywordSeal = ref('')
const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const insertDialogVisible = ref(false)
const insertDialogSubmitting = ref(false)
const pendingInsert = ref<{ type: string; item: any; key: string; tag: string; isRich: boolean } | null>(null)
const insertForm = reactive<{ customName: string; partyIndex: string }>({ customName: '', partyIndex: '1' })
const elementsKeyword = ref('')
const elements = ref<Array<{ key: string; tag: string; type: string; name: string; customName?: string; meta?: any }>>([])
const editorRef = ref<any>(null)
const saving = ref(false)
const isDemoMode = ref(false)

function goBack() {
  // Embed 模式：保持 embed 和 hideBack 参数
  if (isEmbedMode.value) {
    router.push({
      path: returnUrl.value,
      query: {
        embed: 'true',
        hideBack: 'true'
      }
    })
  } else {
    // 独立模式：正常跳转
    router.push(returnUrl.value)
  }
}

// 保存模板（调用强制保存API）
async function handleSave() {
  // 演示模式检查
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许保存模板')
    return
  }
  
  if (!fileId.value) {
    ElMessage.warning('缺少文件ID，无法保存')
    return
  }
  
  try {
    saving.value = true
    
    // 显示保存提示
    const loadingMsg = ElMessage.info({
      message: '保存中...',
      duration: 0
    })
    
    // 调用强制保存API
    const result = await forceSaveFile(fileId.value)
    
    loadingMsg.close()
    
    // 检查响应
    if (result.data && result.data.code === 200) {
      const responseData = result.data.data
      
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
      console.error('保存失败，响应码不是200:', result)
      ElMessage.error('保存失败：' + (result.data?.message || '未知错误'))
    }
    
  } catch (error: any) {
    console.error('保存失败', error)
    ElMessage.error('保存失败：' + (error?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const elementsCount = computed(() => (fields.baseFields?.length || 0) + (fields.clauseFields?.length || 0) + (fields.sealFields?.length || 0) + (fields.counterpartyFields?.length || 0))
const filteredBaseFields = computed(() => (fields.baseFields || []).filter((f: any) => !keyword.value || f.name.includes(keyword.value) || f.code.includes(keyword.value)))
const filteredClauseFields = computed(() => (fields.clauseFields || []).filter((c: any) => !keywordClause.value || c.name.includes(keywordClause.value) || c.code.includes(keywordClause.value)))
const filteredPartyFields = computed(() => (fields.counterpartyFields || []).filter((p: any) => !keywordParty.value || p.name.includes(keywordParty.value) || p.code.includes(keywordParty.value)))
const filteredSealFields = computed(() => (fields.sealFields || []).filter((s: any) => !keywordSeal.value || s.name.includes(keywordSeal.value) || s.code.includes(keywordSeal.value)))
const filteredElements = computed(() => elements.value.filter((e) => {
  if (!elementsKeyword.value) return true
  const kw = elementsKeyword.value.toLowerCase()
  return (
    (e.customName || '').toLowerCase().includes(kw) ||
    (e.name || '').toLowerCase().includes(kw) ||
    (e.type || '').toLowerCase().includes(kw) ||
    (e.tag || '').toLowerCase().includes(kw)
  )
}))

const gridColumns = computed(() => {
  const leftWidth = leftCollapsed.value ? '18px' : '320px'
  const rightWidth = rightCollapsed.value ? '18px' : '360px'
  return `${leftWidth} 1fr ${rightWidth}`
})

onMounted(async () => {
  if (!fileId.value) {
    ElMessage.error('缺少文件ID')
    return
  }
  if (!recordId.value && !templateId.value) {
    ElMessage.error('缺少模板ID或记录ID')
    return
  }
  
  // 加载系统配置（演示模式）
  try {
    const configRes = await getSystemConfig() as any
    // 注意：axios 响应结构是 response.data.data
    isDemoMode.value = configRes?.data?.data?.demoMode || false
    if (isDemoMode.value) {
      ElMessage.info('当前为演示模式，文档将以只读方式打开')
    }
  } catch (e) {
    console.error('获取系统配置失败', e)
  }
  
  await loadFields()
  await loadDesignByTemplateId()
})

const loadFields = async () => {
  // 方式1：
  const fieldsConfigUrlParam = route.query.fieldsConfigUrl as string
  const fieldsConfigUrl = fieldsConfigUrlParam ? decodeURIComponent(fieldsConfigUrlParam) : ''
  
  if (fieldsConfigUrl) {
    try {
      console.log('[自定义字段] 通过后端代理获取配置:', fieldsConfigUrl)
      
      const res = await fetchCustomFieldsConfig(fieldsConfigUrl)
      const config = res?.data?.data || res?.data || {}
      
      if (config) {
        fields.baseFields = config.baseFields || []
        fields.clauseFields = config.clauseFields || []
        fields.counterpartyFields = config.counterpartyFields || []
        fields.sealFields = config.sealFields || []
        
        console.log('[自定义字段] 配置加载成功:', config.configName || 'Custom Fields')
        ElMessage.success(`已加载自定义字段配置${config.configName ? ': ' + config.configName : ''}`)
        return
      } else {
        console.error('[自定义字段] 配置格式错误:', config)
        ElMessage.error('自定义字段配置格式错误，请检查字段信息')
        return
      }
    } catch (error: any) {
      console.error('[自定义字段] 获取自定义字段配置失败:', error)
      const msg = error?.response?.data?.message || error?.message || '无法获取自定义字段配置'
      ElMessage.error(msg)
      return
    }
  }
  
  // 如果没有自定义字段，从 API 加载默认字段
  const res = await fetchTemplateFields() as any
  // 根据API响应解析问题文档，响应被axios拦截器包装了一层
  const fieldsData = res?.data?.data || res?.data || {}
  
  fields.baseFields = fieldsData.baseFields || []
  fields.clauseFields = fieldsData.clauseFields || []
  fields.counterpartyFields = fieldsData.counterpartyFields || []
  fields.sealFields = fieldsData.sealFields || []
  
  // 并列新增：二维码公章（仅前端增强展示，不改动后端接口）
  try {
    const exists = (fields.sealFields || []).some((s: any) => String(s.code).toLowerCase() === 'seal_qrcode')
    if (!exists) {
      // 将其插入到列表前部，便于与公司公章/财务专用章并列展示
      const qrSeal = { name: '二维码公章', code: 'seal_qrcode' }
      fields.sealFields = [qrSeal, ...fields.sealFields]
    }
  } catch {}
}

const loadDesignByTemplateId = async () => {
  try {
    let res: any
    
    // 优先使用 recordId（新版），如果没有则使用 templateId（旧版兼容）
    if (recordId.value) {
      res = await getTemplateDesignDetail(recordId.value)
    } else if (templateId.value) {
      res = await getTemplateDesignByTemplateId(templateId.value)
    }
    
    const raw = res?.data?.data || res?.data || {}
    
    // 兼容不同返回结构
    const elementsJson = (raw.elementsJson) || ''
    const parsed = elementsJson ? JSON.parse(elementsJson) : (typeof raw === 'string' ? JSON.parse(raw) : raw)
    elements.value = parsed?.elements || raw?.elements || []
    if (raw?.id) designId.value = String(raw.id)
  } catch (e) {
    // ignore
  }
}

const onEditorReady = () => {}
const onPluginLoaded = () => {}

const generateUniqueKey = (prefix: string) => {
  const base = `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  // 确保唯一
  let key = base
  let idx = 1
  const exists = new Set(elements.value.map(e => e.key))
  while (exists.has(key)) {
    key = `${base}_${idx++}`
  }
  return key
}

const insertTag = async (type: string, item: any) => {
  // 未加载插件前，禁止插入
  if (!(editorRef.value?.isReady?.() && editorRef.value?.postToPlugin)) {
    ElMessage.warning('编辑器或插件未加载完成，稍后再试')
    return
  }
  const key = generateUniqueKey(item.code)
  const tag = `tagElement${key}`
  const isRich = !!(item?.isRichText || item?.richText)
  // 统一在一个弹窗中处理：显示名 + （若为签约方则）签约方选择
  pendingInsert.value = { type, item, key, tag, isRich }
  insertForm.customName = ''
  insertForm.partyIndex = '1'
  insertDialogVisible.value = true
}

const removeElement = (idx: number) => {
  elements.value.splice(idx, 1)
}

const locateElement = async (el: any) => {
  try {
    await (editorRef.value?.postToPlugin?.({ action: 'getContentControl', Tag: el.tag }) || Promise.resolve())
  } catch {}
}

const editElement = async (idx: number) => {
  const current = elements.value[idx]
  try {
    const { value } = await ElMessageBox.prompt('请输入自定义显示名：', '编辑元素', { inputValue: current.customName || current.name })
    current.customName = value
  } catch {}
}

const cancelInsert = () => {
  insertDialogVisible.value = false
  pendingInsert.value = null
}

const confirmInsert = async () => {
  if (!pendingInsert.value) return
  insertDialogSubmitting.value = true
  const { item, key, tag, isRich, type } = pendingInsert.value
  // 准备即将保存的元素
  const newElement = type === 'party'
    ? { key, tag, type: 'party', name: item.name, customName: insertForm.customName, meta: { ...item, richText: isRich, partyIndex: insertForm.partyIndex } }
    : { key, tag, type, name: item.name, customName: insertForm.customName, meta: { ...item, richText: isRich } }
  const newElements = [...elements.value, newElement]
  const prevElements = [...elements.value]
  let savedId: string | undefined
  try {
    // 1) 先保存后台
    const res = await saveTemplateDesign({
      id: designId.value || recordId.value || undefined,
      templateId: templateId.value || undefined,
      fileId: fileId.value,
      elements: newElements
    })
    const responseData = res?.data?.data || res?.data || {}
    if (responseData?.id) savedId = String(responseData.id)
    // 2) 再插入到OnlyOffice
    if (isRich) {
      await (editorRef.value?.createBlockContentControl?.(key, tag, item.name, true, true) || Promise.resolve())
    } else {
      await (editorRef.value?.createContentControl?.(key, tag, item.name, false) || Promise.resolve())
    }
    // 3) 双方都成功后更新本地并提示
    elements.value = newElements
    if (savedId) designId.value = savedId
    // 4) 插入后触发一次强制保存
    try { editorRef.value?.forceSave?.() } catch {}
    ElMessage.success('已保存并插入元素，并已强制保存')
  } catch (err) {
    // 回滚后端保存
    try {
      await saveTemplateDesign({ 
        id: savedId || designId.value || recordId.value || undefined, 
        templateId: templateId.value || undefined, 
        fileId: fileId.value, 
        elements: prevElements 
      })
    } catch {}
    ElMessage.error('保存或插入失败，请重试')
  } finally {
    insertDialogSubmitting.value = false
    insertDialogVisible.value = false
    pendingInsert.value = null
  }
}

const deleteElement = async (idx: number, el: any) => {
  try {
    await (editorRef.value?.postToPlugin?.({ action: 'deleteContentControl', Tag: el.tag }) || Promise.resolve())
  } catch {}
  removeElement(idx)
  ElMessage.success('已删除元素')
}

const typeLabel = (type: string) => {
  switch (type) {
    case 'base': return '字段'
    case 'clause': return '条款'
    case 'party': return '签约方'
    case 'seal': return '印章'
    default: return type
  }
}
const typeTag = (type: string) => {
  switch (type) {
    case 'base': return 'success'
    case 'clause': return 'info'
    case 'party': return 'warning'
    case 'seal': return 'danger'
    default: return 'info'
  }
}

const elementDesc = (el: any) => {
  // 名称与编号合并为一段，并且不再追加多余的标签信息
  const code = el?.meta?.code || ''
  return `${el.name}${code ? `（${code}）` : ''}`
}

// 解析条款变量，如 ${contract_name} -> 从基础字段或签约方字段映射可读名称
const resolveClauseText = (raw: string) => {
  if (!raw) return ''
  const dict: Record<string, string> = {}
  ;(fields.baseFields || []).forEach((f: any) => { dict[f.code] = f.name })
  ;(fields.counterpartyFields || []).forEach((f: any) => { dict[f.code] = f.name })
  return raw.replace(/\$\{([^}]+)\}/g, (_, key) => {
    return dict[key] ? `【${dict[key]}】` : `
${'${' + key + '}'}`
  })
}

// 生成带变量tag样式的HTML（安全：仅包裹替换变量，不执行脚本）
// 规范：${base_code}；签约方可用 ${subject_code#2} 或 ${2#subject_code}；未带编号的 subject_ 默认按1处理
const resolveClauseHtml = (raw: string) => {
  const esc = (s: string) => s.replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'} as any)[c] )
  if (!raw) return ''
  const dict: Record<string, string> = {}
  ;(fields.baseFields || []).forEach((f: any) => { dict[f.code] = f.name })
  ;(fields.counterpartyFields || []).forEach((f: any) => { dict[f.code] = f.name })
  const safe = esc(raw)
  return safe.replace(/\$\{([^}]+)\}/g, (_, rawKey) => {
    let label = ''
    let partyIndex = ''
    if (rawKey.includes('#')) {
      const [a, b] = rawKey.split('#')
      if (/^\d+$/.test(a)) { partyIndex = a; label = dict[b] || b }
      else if (/^\d+$/.test(b)) { partyIndex = b; label = dict[a] || a }
      else { label = dict[rawKey] || rawKey }
    } else {
      label = dict[rawKey] || rawKey
      if (rawKey.startsWith('subject_')) partyIndex = '1'
    }
    const partyTag = partyIndex ? `<span class=\"var-tag\">签约方${partyIndex}</span>` : ''
    return `${partyTag}<span class=\"var-tag\">${label}</span>`
  })
}

// 保存按钮已移除：改为新增元素时自动保存
</script>

<style scoped>
.template-design-page {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #f5f7fa;
}
.design-actions {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  padding: 12px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,.05);
}
.design-body {
  display: grid;
  grid-template-columns: 320px 1fr 360px;
  height: calc(100vh - 56px); /* 减去顶部操作栏高度 */
  margin-top: 56px; /* 给顶部操作栏留出空间 */
  grid-template-rows: 1fr;
  overflow: hidden; /* 固定整体布局，避免因滚动条影响左右抖动 */
  column-gap: 12px; /* 统一左右与中间的间距，更加呼吸 */
}
.editor-panel { height: 100%; display: flex; }
.editor-panel :deep(.onlyoffice-editor) { flex: 1; }
.editor-panel :deep(.editor-container) { height: 100% !important; }
.left-panel, .right-panel { padding: 0 10px; min-height: 0; display: flex; flex-direction: column; height: 100%; }
.left-panel {
  background: #ffffff;
  /* 去掉硬分割线，配合 column-gap 呈现统一留白 */
}
.right-panel { background: #ffffff; }
.fields-card, .elements-card { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.custom-card { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.custom-card .card-header { padding: 10px; border-bottom: 1px solid #ebeef5; }
.custom-card .card-body { padding: 10px; }
.custom-card.fill { display: flex; flex-direction: column; }
.custom-card.fill .card-body { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.custom-card.fill .card-body > .list { flex: 1; min-height: 0; }
.collapse-btn {
  margin-left: auto;
  border: 1px solid #e6ebf5;
  background: #fff;
  color: #909399;
  width: 24px; height: 24px; border-radius: 4px; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
}
.collapse-btn:hover { background: #f7f8fa; }
/* 统一边缘折叠按钮样式与位置（与编辑器交界处） */
.edge-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  background: #fff;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  z-index: 3;
}
.edge-toggle:hover { background: #f7f8fa; }
.edge-toggle--left { right: -12px; }
.edge-toggle--right { left: -12px; }
.left-panel, .right-panel { position: relative; overflow: hidden; }
.edge-toggle.is-collapsed { height: calc(100vh - 20px); border-radius: 0; width: 18px; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.elements-header { display: grid; grid-template-rows: auto auto; gap: 8px; }
.elements-header .header-top { display: flex; align-items: center; justify-content: space-between; }
.elements-header .header-bottom { display: flex; align-items: center; }
.fields-tabs { height: calc(100% - 10px); }
.custom-tabs { display: grid; grid-template-columns: 52px 1fr; }
.custom-tabs .tabs-nav {
  width: 52px;
  border-right: 1px solid #ebeef5;
  background: transparent;
  padding: 4px 4px;
}
.custom-tabs .tab-item {
  width: 100%; height: 48px; padding: 4px; margin: 4px 0; line-height: 1;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  border-radius: 8px; border: 1px solid transparent; background: transparent; color: #606266; cursor: pointer;
  transition: background-color .15s ease, color .15s ease, box-shadow .15s ease, border-color .15s ease;
}
.custom-tabs .tab-item:hover { background: #f7f8fa; }
.custom-tabs .tab-item.active { background: #f0f5ff; color: #3370ff; box-shadow: inset 2px 0 0 #3370ff; border-color: #c6dbff; }
.custom-tabs .tabs-content { padding-left: 0; }
.custom-tabs .tabs-nav.is-left { padding-top: 4px; }
.tab-label { display: inline-flex; flex-direction: column; align-items: center; gap: 4px; font-size: 11px; color: #606266; }
.tab-label :deep(svg) { font-size: 14px; }
.tab-pane-body { padding: 8px 10px; }
.list {
  height: calc(100vh - 140px); /* 拉满到页面底部，减掉头部与内边距 */
  margin-top: 10px;
}
.custom-scroll {
  overflow: auto;
  scrollbar-gutter: stable; /* 保持滚动条预留空间，避免内容抖动/错行 */
}
.custom-search { padding: 6px 0; }
.search-input {
  width: 100%; height: 28px; border: 1px solid #e4e7ed; border-radius: 6px; padding: 0 10px; outline: none;
}
.search-input:focus { border-color: #3370ff; box-shadow: 0 0 0 2px rgba(51,112,255,.1); }
.badge { display: inline-block; padding: 0 6px; background: #f2f3f5; border-radius: 4px; font-size: 12px; color: #606266; }
.badge-rich { display: inline-block; padding: 0 6px; background: #fff1f0; color: #f5222d; border: 1px solid #ffccc7; border-radius: 4px; font-size: 12px; }
.link-btn { background: transparent; border: none; color: #3370ff; cursor: pointer; display: inline-flex; align-items: center; white-space: nowrap; padding: 0 2px; flex: none; }
.link-btn:hover { text-decoration: underline; }
.empty { color: #c0c4cc; text-align: center; padding: 12px 0; }
.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 6px;
  border-bottom: 1px dashed transparent;
  border-radius: 6px;
  transition: background-color .15s ease;
}
.field-item:hover { background: #fafafa; }
.field-item .meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.field-item.custom { gap: 6px; }
.field-item.custom + .field-item.custom { margin-top: 10px; }
.field-item.custom .meta { width: 100%; }
.item-head { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 6px; }
.item-head .name { font-weight: 600; color: #303133; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.link-btn.inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #d9e6ff;
  min-width: 0;
  font-size: 12px;
  line-height: 1;
  text-align: center;
  justify-self: end;
}
.link-btn.inline:hover { background: #f0f5ff; border-color: #c6dbff; }
.field-item .name { font-weight: 500; }
.field-item .code { font-size: 12px; color: #909399; }
.actions { padding-top: 10px; display: flex; justify-content: flex-end; }

/* 新增：左侧分区视觉 */
.section { margin-bottom: 16px; }
.section-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}
.section-title .mr8 { margin-right: 8px; }
.section-title .ml8 { margin-left: 8px; }
.ml6 { margin-left: 6px; }

/* 右侧卡片优化 */
.element-item {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fff;
  transition: box-shadow .2s ease, border-color .2s ease;
}
.element-item:hover { box-shadow: 0 2px 10px rgba(0,0,0,.06); border-color: #e4e7ed; }
.element-main { flex: 1; cursor: pointer; width: 100%; }
.line { display: flex; align-items: center; width: 100%; }
.display-line { margin-bottom: 6px; gap: 6px; }
.display-name { font-weight: 700; color: #303133; display: inline-block; max-width: 100%; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; font-size: 15px; line-height: 20px; }
.meta-line { justify-content: flex-start; margin-bottom: 4px; font-size: 12px; color: #606266; }
.meta-left { min-width: 0; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; color: #606266; font-size: 12px; }
.meta-code { color: #909399; margin-left: 4px; }
.clause-line { margin-top: 6px; }
.elem-clause-preview { color: #606266; font-size: 12px; line-height: 18px; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.display-line :deep(.el-tag) { margin-left: 6px; }
.element-item .actions { display: flex; gap: 2px; padding: 2px 0 0; justify-content: flex-end; width: 100%; }
.element-item .actions :deep(.el-button) { font-size: 12px; height: 20px; padding: 0 0px; min-width: 0; }

/* 右侧卡片区域更大的滚动空间，减少滚动条带来的视觉压迫 */
.right-panel .list { height: auto; max-height: 100%; }
/* 左侧列表滚动高度与右侧保持一致视觉节奏 */
.left-panel .list { height: auto; max-height: 100%; }

/* 去除左侧卡片边框与背景，让内容更轻盈 */
.fields-card--flat { border: none; background: transparent; }

/* 条款两行显示 + hover 展开完整文本 */
.clause-item .clause-preview {
  margin-top: 4px; color: #606266; font-size: 12px; line-height: 18px;
  display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
:deep(.var-tag) { display: inline-block; padding: 0 6px; height: 18px; line-height: 18px; background: #e8f3ff; border: 1px solid #bfe1ff; color: #1677ff; border-radius: 3px; font-size: 12px; margin: 0 2px; }

.clause-item .full-preview { display: none; margin-top: 4px; color: #606266; font-size: 12px; line-height: 18px; white-space: pre-wrap; }
.clause-item:hover .clause-preview { display: none; }
.clause-item:hover .full-preview { display: block; }
</style>


