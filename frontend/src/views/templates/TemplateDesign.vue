<template>
  <div class="template-design-page">
    <div class="design-body">
      <div class="left-panel">
        <el-card class="fields-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>可插入元素</span>
              <el-tag size="small">{{ elementsCount }}项</el-tag>
            </div>
          </template>
          <el-tabs v-model="activeTab" class="fields-tabs">
            <el-tab-pane label="基础字段" name="base">
              <el-input v-model="keyword" placeholder="搜索字段" size="small" clearable />
              <el-scrollbar class="list">
                <el-empty v-if="filteredBaseFields.length === 0" description="暂无数据" />
                <div v-for="f in filteredBaseFields" :key="f.code" class="field-item">
                  <div class="meta">
                    <div class="name">{{ f.name }}</div>
                    <div class="code">{{ f.code }}</div>
                  </div>
                  <el-button size="small" @click="insertTag('base', f)">插入</el-button>
                </div>
              </el-scrollbar>
            </el-tab-pane>
            <el-tab-pane label="条款" name="clause">
              <el-scrollbar class="list">
                <div v-for="c in fields.clauseFields" :key="c.code" class="field-item">
                  <div class="meta">
                    <div class="name">{{ c.name }}</div>
                    <div class="code">{{ c.code }}</div>
                  </div>
                  <el-button size="small" @click="insertTag('clause', c)">插入</el-button>
                </div>
              </el-scrollbar>
            </el-tab-pane>
            <el-tab-pane label="印章" name="seal">
              <el-scrollbar class="list">
                <div v-for="s in fields.sealFields" :key="s.code" class="field-item">
                  <div class="meta">
                    <div class="name">{{ s.name }}</div>
                    <div class="code">{{ s.code }}</div>
                  </div>
                  <el-button size="small" @click="insertTag('seal', s)">插入</el-button>
                </div>
              </el-scrollbar>
            </el-tab-pane>
            <el-tab-pane label="签约方" name="party">
              <el-scrollbar class="list">
                <div v-for="p in fields.counterpartyFields" :key="p.code" class="field-item">
                  <div class="meta">
                    <div class="name">{{ p.name }}</div>
                    <div class="code">{{ p.code }}</div>
                  </div>
                  <el-button size="small" @click="insertTag('party', p)">插入</el-button>
                </div>
              </el-scrollbar>
            </el-tab-pane>
          </el-tabs>
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
        />
      </div>
      <div class="right-panel">
        <el-card class="elements-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>已插入元素</span>
            </div>
          </template>
          <el-scrollbar class="list">
            <el-empty v-if="elements.length === 0" description="暂无元素" />
            <div v-for="(el, idx) in elements" :key="el.key" class="field-item">
              <div class="meta">
                <div class="name">{{ el.name }}</div>
                <div class="code">{{ el.key }}</div>
                <div class="tag">{{ el.tag }}</div>
              </div>
              <el-button size="small" type="danger" @click="removeElement(idx)">移除</el-button>
            </div>
          </el-scrollbar>
          <div class="actions">
            <el-button type="primary" @click="saveDesign" :loading="saving">保存设计</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
  
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { fetchTemplateFields, saveTemplateDesign, getTemplateDesignDetail } from '@/api/templateDesign'

const route = useRoute()
const templateId = computed(() => (route.query.id as string) || '')
const fileId = computed(() => (route.query.fileId as string) || '')
const designId = ref<string>((route.query.designId as string) || '')

const fields = reactive<any>({ baseFields: [], clauseFields: [], counterpartyFields: [], sealFields: [] })
const activeTab = ref('base')
const keyword = ref('')
const elements = ref<Array<{ key: string; tag: string; type: string; name: string; meta?: any }>>([])
const editorRef = ref<any>(null)
const saving = ref(false)

const elementsCount = computed(() => (fields.baseFields?.length || 0) + (fields.clauseFields?.length || 0) + (fields.sealFields?.length || 0) + (fields.counterpartyFields?.length || 0))
const filteredBaseFields = computed(() => (fields.baseFields || []).filter((f: any) => !keyword.value || f.name.includes(keyword.value) || f.code.includes(keyword.value)))

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

const onEditorReady = () => {
  // 可在此处初始化OnlyOffice插件消息通道
}

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

const insertTag = (type: string, item: any) => {
  const key = generateUniqueKey(item.code)
  const tag = `tagElement${key}`
  // 发送到OnlyOffice进行插入占位符（可由插件处理）
  try {
    const message = { action: 'insertText', text: `{{${tag}}}` }
    ;(window as any).frames['onlyoffice-editor']?.postMessage(JSON.stringify(message), '*')
  } catch (e) {
    // ignore
  }
  elements.value.push({ key, tag, type, name: item.name, meta: item })
  ElMessage.success('已插入占位符')
}

const removeElement = (idx: number) => {
  elements.value.splice(idx, 1)
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
}
.left-panel, .right-panel {
  padding: 10px;
}
.editor-panel {
  padding: 10px 0;
}
.fields-card, .elements-card {
  height: calc(100vh - 20px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fields-tabs {
  height: calc(100% - 10px);
}
.list {
  height: calc(100vh - 220px);
  margin-top: 10px;
}
.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 6px;
  border-bottom: 1px dashed #eee;
}
.field-item .meta {
  display: flex;
  flex-direction: column;
}
.field-item .name { font-weight: 500; }
.field-item .code { font-size: 12px; color: #909399; }
.actions { padding-top: 10px; display: flex; justify-content: flex-end; }
</style>


