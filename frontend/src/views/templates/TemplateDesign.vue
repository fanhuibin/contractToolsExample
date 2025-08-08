<template>
  <div class="template-design-page">
    <div class="design-body">
      <div class="left-panel">
        <el-card class="fields-card fields-card--flat" shadow="never" :body-style="{ padding: '0' }">
          <template #header>
            <div class="card-header">
              <span>可插入元素</span>
              <el-tag size="small">{{ elementsCount }}项</el-tag>
            </div>
          </template>
          <div class="fields-tabs custom-tabs">
            <div class="tabs-nav is-left">
              <button
                class="tab-item"
                :class="{ active: activeTab === 'base' }"
                @click="activeTab = 'base'"
                :aria-pressed="activeTab === 'base'"
              >
                <span class="tab-label"><el-icon><Document /></el-icon> 字段</span>
              </button>
              <button
                class="tab-item"
                :class="{ active: activeTab === 'clause' }"
                @click="activeTab = 'clause'"
                :aria-pressed="activeTab === 'clause'"
              >
                <span class="tab-label"><el-icon><Tickets /></el-icon> 条款</span>
              </button>
              <button
                class="tab-item"
                :class="{ active: activeTab === 'party' }"
                @click="activeTab = 'party'"
                :aria-pressed="activeTab === 'party'"
              >
                <span class="tab-label"><el-icon><User /></el-icon> 签约方</span>
              </button>
              <button
                class="tab-item"
                :class="{ active: activeTab === 'seal' }"
                @click="activeTab = 'seal'"
                :aria-pressed="activeTab === 'seal'"
              >
                <span class="tab-label"><el-icon><Stamp /></el-icon> 印章</span>
              </button>
            </div>
            <div class="tabs-content">
              <div v-show="activeTab === 'base'" class="tab-pane-body">
                <div class="custom-search"><input v-model="keyword" placeholder="搜索字段" class="search-input" /></div>
                <div class="custom-scroll list">
                  <div v-if="filteredBaseFields.length === 0" class="empty">暂无数据</div>
                  <div v-for="f in filteredBaseFields" :key="f.code" class="field-item custom">
                    <div class="meta">
                      <div class="item-head">
                        <div class="name">
                          {{ f.name }}
                          <span v-if="f.isRichText" class="badge-rich">富文本</span>
                        </div>
                        <button class="link-btn inline" @click="insertTag('base', f)">插入</button>
                      </div>
                      <div class="code"><span class="badge">{{ f.code }}</span></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-show="activeTab === 'clause'" class="tab-pane-body">
                <div class="custom-search"><input v-model="keywordClause" placeholder="搜索条款" class="search-input" /></div>
                <div class="custom-scroll list">
                  <div v-if="filteredClauseFields.length === 0" class="empty">暂无数据</div>
                  <div v-for="c in filteredClauseFields" :key="c.code" class="field-item custom clause-item">
                    <div class="meta">
                      <div class="item-head">
                        <div class="name">{{ c.name }}</div>
                        <button class="link-btn inline" @click="insertTag('clause', c)">插入</button>
                      </div>
                      <div class="code"><span class="badge">{{ c.code }}</span></div>
                      <div class="clause-preview" v-html="resolveClauseHtml(c.content)"></div>
                      <div class="full-preview" v-html="resolveClauseHtml(c.content)"></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-show="activeTab === 'party'" class="tab-pane-body">
                <div class="custom-scroll list">
                  <div v-if="(fields.counterpartyFields||[]).length === 0" class="empty">暂无数据</div>
                  <div v-for="p in fields.counterpartyFields" :key="p.code" class="field-item custom">
                    <div class="meta">
                      <div class="item-head party">
                        <div class="name">{{ p.name }}</div>
                        <button class="link-btn inline" @click="insertTag('party', p)">插入</button>
                      </div>
                      <div class="code"><span class="badge">{{ p.code }}</span></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-show="activeTab === 'seal'" class="tab-pane-body">
                <div class="custom-scroll list">
                  <div v-if="(fields.sealFields||[]).length === 0" class="empty">暂无数据</div>
                  <div v-for="s in fields.sealFields" :key="s.code" class="field-item custom">
                    <div class="meta">
                      <div class="item-head">
                        <div class="name">{{ s.name }}</div>
                        <button class="link-btn inline" @click="insertTag('seal', s)">插入</button>
                      </div>
                      <div class="code"><span class="badge">{{ s.code }}</span></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div class="editor-panel">
        <OnlyOfficeEditor
          ref="editorRef"
          :file-id="fileId"
          :can-edit="true"
          :can-review="false"
          :height="'100vh'"
          :show-toolbar="false"
          :show-status="false"
          :update-onlyoffice-key="true"
          @ready="onEditorReady"
          @pluginLoaded="onPluginLoaded"
        />
      </div>
      <div class="right-panel">
        <el-card class="elements-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>已插入元素</span>
              <el-input v-model="elementsKeyword" placeholder="检索已插入元素" size="small" clearable prefix-icon="Search" class="ml8" style="max-width: 220px;" />
            </div>
          </template>
          <el-scrollbar class="list">
            <el-empty v-if="elements.length === 0" description="暂无元素" />
            <div v-for="(el, idx) in filteredElements" :key="el.key" class="element-item">
              <div class="element-main" @click="locateElement(el)">
                <div class="title-line">
                  <span class="title">{{ el.customName || el.name }}</span>
                  <el-tag size="small" :type="typeTag(el.type)">{{ typeLabel(el.type) }}</el-tag>
                  <el-tag v-if="el.meta?.partyIndex" size="small" type="warning">签约方{{ el.meta.partyIndex }}</el-tag>
                </div>
                <div class="desc">{{ elementDesc(el) }}</div>
              </div>
              <div class="actions">
                <el-button size="small" text type="primary" @click.stop="locateElement(el)">定位</el-button>
                <el-button size="small" text type="primary" @click.stop="editElement(idx)">编辑</el-button>
                <el-popconfirm title="确认删除该元素？" confirm-button-text="删除" cancel-button-text="取消" @confirm="deleteElement(idx, el)">
                  <template #reference>
                    <el-button size="small" text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </el-scrollbar>
          <div class="actions">
            <el-button type="primary" @click="saveDesign" :loading="saving">保存设计</el-button>
          </div>
        </el-card>
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Tickets, User, Stamp, Search } from '@element-plus/icons-vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { fetchTemplateFields, saveTemplateDesign, getTemplateDesignDetail } from '@/api/templateDesign'

const route = useRoute()
const templateId = computed(() => (route.query.id as string) || '')
// 设计时固定使用一个示例文件（走通用 fileInfo），默认ID为 9999
const fileId = computed(() => (route.query.fileId as string) || '9999')
const designId = ref<string>((route.query.designId as string) || '')

const fields = reactive<any>({ baseFields: [], clauseFields: [], counterpartyFields: [], sealFields: [] })
const activeTab = ref<'base' | 'clause' | 'party' | 'seal'>('base')
const keyword = ref('')
const keywordClause = ref('')
const insertDialogVisible = ref(false)
const insertDialogSubmitting = ref(false)
const pendingInsert = ref<{ type: string; item: any; key: string; tag: string; isRich: boolean } | null>(null)
const insertForm = reactive<{ customName: string; partyIndex: string }>({ customName: '', partyIndex: '1' })
const elementsKeyword = ref('')
const elements = ref<Array<{ key: string; tag: string; type: string; name: string; customName?: string; meta?: any }>>([])
const editorRef = ref<any>(null)
const saving = ref(false)

const elementsCount = computed(() => (fields.baseFields?.length || 0) + (fields.clauseFields?.length || 0) + (fields.sealFields?.length || 0) + (fields.counterpartyFields?.length || 0))
const filteredBaseFields = computed(() => (fields.baseFields || []).filter((f: any) => !keyword.value || f.name.includes(keyword.value) || f.code.includes(keyword.value)))
const filteredClauseFields = computed(() => (fields.clauseFields || []).filter((c: any) => !keywordClause.value || c.name.includes(keywordClause.value) || c.code.includes(keywordClause.value)))
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

onMounted(async () => {
  await loadFields()
  if (designId.value) await loadDesignDetail()
})

const loadFields = async () => {
  const res = await fetchTemplateFields()
  fields.baseFields = res.data.baseFields || []
  fields.clauseFields = res.data.clauseFields || []
  fields.counterpartyFields = res.data.counterpartyFields || []
  fields.sealFields = res.data.sealFields || []
}

const loadDesignDetail = async () => {
  try {
    const res = await getTemplateDesignDetail(designId.value)
    const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
    elements.value = data.elements || []
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
  const isRich = !!item?.richText
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
  try {
    if (isRich) {
      await (editorRef.value?.createBlockContentControl?.(key, tag, item.name, true, true) || Promise.resolve())
    } else {
      await (editorRef.value?.createContentControl?.(key, tag, item.name, false) || Promise.resolve())
    }
    if (type === 'party') {
      elements.value.push({
        key,
        tag,
        type: 'party',
        name: item.name,
        customName: insertForm.customName,
        meta: { ...item, richText: isRich, partyIndex: insertForm.partyIndex }
      })
    } else {
      elements.value.push({ key, tag, type, name: item.name, customName: insertForm.customName, meta: { ...item, richText: isRich } })
    }
    ElMessage.success('已插入占位符')
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
  const parts: string[] = []
  if (el.type === 'party' && el.meta?.partyIndex) {
    parts.push(`签约方${el.meta.partyIndex}`)
  }
  const code = el?.meta?.code || ''
  parts.push(el.name + (code ? `（${code}）` : ''))
  return parts.join(' ')
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

const saveDesign = async () => {
  saving.value = true
  try {
    await saveTemplateDesign({
      id: designId.value || undefined,
      templateId: templateId.value,
      fileId: fileId.value,
      elements: elements.value
    })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.template-design-page {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #f5f7fa;
}
.design-body {
  display: grid;
  grid-template-columns: 320px 1fr 360px;
  height: 100vh;
  overflow: hidden; /* 固定整体布局，避免因滚动条影响左右抖动 */
}
.left-panel, .right-panel {
  padding: 10px;
}
.left-panel {
  background: #ffffff;
  border-right: 1px solid #ebeef5;
}
.right-panel {
  border-left: 1px solid #ebeef5;
}
.fields-card, .elements-card {
  height: calc(100vh - 20px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fields-tabs { height: calc(100% - 10px); }
.custom-tabs { display: grid; grid-template-columns: 84px 1fr; }
.custom-tabs .tabs-nav {
  width: 84px;
  border-right: 1px solid #ebeef5;
  background: transparent;
  padding: 6px 6px;
}
.custom-tabs .tab-item {
  width: 100%; height: 60px; padding: 6px; margin: 4px 0; line-height: 1;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  border-radius: 8px; border: 1px solid transparent; background: transparent; color: #606266; cursor: pointer;
  transition: background-color .15s ease, color .15s ease, box-shadow .15s ease, border-color .15s ease;
}
.custom-tabs .tab-item:hover { background: #f7f8fa; }
.custom-tabs .tab-item.active { background: #f0f5ff; color: #3370ff; box-shadow: inset 2px 0 0 #3370ff; border-color: #c6dbff; }
.custom-tabs .tabs-content { padding-left: 0; }
.custom-tabs .tabs-nav.is-left { padding-top: 4px; }
.tab-label { display: inline-flex; flex-direction: column; align-items: center; gap: 4px; font-size: 12px; color: #606266; }
.tab-label :deep(svg) { font-size: 16px; }
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
  align-items: center;
  justify-content: space-between;
  padding: 12px 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fff;
  transition: box-shadow .2s ease, border-color .2s ease;
}
.element-item:hover { box-shadow: 0 2px 10px rgba(0,0,0,.06); border-color: #e4e7ed; }
.element-main { flex: 1; cursor: pointer; }
.title-line { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.title { font-weight: 600; color: #303133; }
.desc { font-size: 12px; color: #909399; }
.element-item .actions { display: flex; gap: 4px; padding: 0; }

/* 右侧卡片区域更大的滚动空间，减少滚动条带来的视觉压迫 */
.right-panel .list { height: calc(100vh - 220px); }

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


