<template>
  <div class="rule-settings">
    <el-page-header @back="$router.back()" content="提取规则设置" />

    <el-card class="toolbar">
      <div class="toolbar-row">
        <div>当前模板：{{ template?.name || '加载中...' }}（类型：{{ cnName(template?.contractType) }}）</div>
        <el-button type="primary" :icon="Check" @click="onSave" :loading="saving">保存</el-button>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="full-tabs">
      <el-tab-pane label="字段定义与规则" name="fields">
        <el-card shadow="hover" class="panel">
          <template #header>
            <div class="card-header">字段定义与规则</div>
          </template>
          <div class="add-field">
            <el-input v-model="newFieldDef" placeholder="新增字段名（例如：合同编号）" style="width: 260px" @keyup.enter="doAddFieldDef" />
            <el-select v-model="newFieldType" placeholder="类型" style="width: 140px">
              <el-option label="string" value="string" />
              <el-option label="number" value="number" />
              <el-option label="date" value="date" />
              <el-option label="boolean" value="boolean" />
            </el-select>
            <el-button type="primary" @click="doAddFieldDef">新增字段定义</el-button>
          </div>
          <el-table :data="fieldRows" border size="small" style="width: 100%" class="mt8">
            <el-table-column prop="name" label="字段名" width="200" />
            <el-table-column label="类型" width="160">
              <template #default="{ row }">
                <el-select v-model="row.rule.type" placeholder="类型" size="small" style="width: 140px">
                  <el-option label="string" value="string" />
                  <el-option label="number" value="number" />
                  <el-option label="integer" value="integer" />
                  <el-option label="date" value="date" />
                  <el-option label="boolean" value="boolean" />
                  <el-option label="object" value="object" />
                  <el-option label="array" value="array" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.rule.required" />
              </template>
            </el-table-column>
            <el-table-column label="pattern/日期/数值" min-width="240">
              <template #default="{ row }">
                <div class="kv-line">
                  <span>pattern</span>
                  <el-input v-model="row.rule.pattern" size="small" class="pattern-input" placeholder="正则表达式" />
                </div> 
                <div class="kv-line" v-if="row.rule.type==='date'">
                  <span>format</span>
                  <el-input v-model="row.rule.outputFormat" size="small" class="short-input" placeholder="yyyy-MM-dd" />
                </div>
                <div class="kv-line" v-if="row.rule.type==='number' || row.rule.type==='integer'">
                  <span>min</span>
                  <el-input v-model.number="row.rule.min" size="small" class="short-input" />
                  <span class="ml8">max</span>
                  <el-input v-model.number="row.rule.max" size="small" class="short-input" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="结构/枚举" min-width="300">
              <template #default="{ row }">
                <div v-if="row.rule.type==='object'">
                  <div class="kv-line">
                    <span>字典（枚举）</span>
                    <TagEditor v-model:items="row.rule.dictionary" placeholder="如 普票, 专票" />
                  </div>
                  <div class="kv-line">
                    <span>严格</span>
                    <el-switch v-model="row.rule.enumStrict" />
                  </div>
                </div>
                <div v-else-if="row.rule.type==='array'">
                  <div class="kv-line">
                    <span>itemType</span>
                    <el-select v-model="row.rule.itemType" placeholder="类型" size="small" class="short-select">
                      <el-option label="string" value="string" />
                      <el-option label="number" value="number" />
                      <el-option label="integer" value="integer" />
                      <el-option label="boolean" value="boolean" />
                    </el-select>
                  </div>
                  <div class="kv-line">
                    <span>itemEnum</span>
                    <TagEditor v-model:items="row.rule.itemEnum" placeholder="如 A, B, C" />
                  </div>
                  <div class="kv-line">
                    <span>minItems</span>
                    <el-input v-model.number="row.rule.minItems" size="small" class="short-input" />
                    <span class="ml8">maxItems</span>
                    <el-input v-model.number="row.rule.maxItems" size="small" class="short-input" />
                  </div>
                </div>
                <div v-else>
                  <div class="kv-line">
                    <span>枚举</span>
                    <TagEditor v-model:items="row.rule.enum" placeholder="如 甲方, 乙方" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="字段规则（prompt）" min-width="420">
              <template #default="{ row }">
                <TagEditor v-model:items="prompt.fields[row.name]" placeholder="添加一条字段规则" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确定删除该字段？" @confirm="deleteField(row.name)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="全局与格式" name="prompt">
        <el-card shadow="hover" class="panel">
          <template #header>
            <div class="card-header">全局与格式</div>
          </template>
          <div class="section">
            <div class="section-title">global（全局约束）</div>
            <TagEditor v-model:items="prompt.global" placeholder="添加一条全局约束" />
          </div>
          <div class="section">
            <div class="section-title">negative（禁止项）</div>
            <TagEditor v-model:items="prompt.negative" placeholder="添加一条禁止项" />
          </div>
          <div class="section">
            <div class="section-title">format（输出格式）</div>
            <TagEditor v-model:items="prompt.format" placeholder="添加一条格式要求" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="ruleDialog.visible" :title="`编辑字段规则：${ruleDialog.field}`" width="680px">
      <div class="section">
        <TagEditor v-model:items="ruleDialog.rules" placeholder="添加一条字段规则" />
      </div>
      <template #footer>
        <el-button @click="ruleDialog.visible=false">取消</el-button>
        <el-button type="primary" @click="saveRuleDialog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, computed, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readRuleByTemplateId, saveRuleByTemplateId } from '@/api/ai/rules'
import api from '@/api/ai/index'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

// 轻量标签编辑器组件（内联）
const TagEditor = defineAsyncComponent(() => import('@/views/contracts/components/TagEditor.vue'))

const route = useRoute()
const router = useRouter()
const templateId = ref<number>()
const template = ref<any>()
const activeTab = ref<'fields'|'prompt'>('fields')
const raw = ref<any>({})
const prompt = reactive<{ global: string[]; negative: string[]; format: string[]; fields: Record<string, string[]> }>(
  { global: [], negative: [], format: [], fields: {} }
)
const fieldsDef = reactive<Record<string, any>>({})

const saving = ref(false)
const newFieldDef = ref('')
const newFieldType = ref<'string'|'number'|'date'|'boolean'>('string')

const typeDict = ref<Record<string, string>>({})
function cnName(t?: string) { return (t && typeDict.value[t]) || (t || '') }

const fieldRows = computed(() => Object.entries(fieldsDef).map(([name, rule]) => ({ name, rule })))

const ruleDialog = reactive<{ visible: boolean; field?: string; rules: string[] }>({ visible: false, rules: [] })

function normalizePrompt(obj: any) {
  const p = obj?.prompt || {}
  prompt.global = Array.isArray(p.global) ? [...p.global] : []
  prompt.negative = Array.isArray(p.negative) ? [...p.negative] : []
  prompt.format = Array.isArray(p.format) ? [...p.format] : []
  prompt.fields = p.fields && typeof p.fields === 'object' ? { ...p.fields } : {}
  // fields definition
  setFields(obj?.fields || {})
}

async function initByTemplateId() {
  const qid = route.query.templateId
  if (!qid) {
    ElMessage.error('缺少 templateId')
    router.back()
    return
  }
  const idNum = Number(qid)
  if (!idNum || Number.isNaN(idNum)) {
    ElMessage.error('templateId 无效')
    router.back()
    return
  }
  templateId.value = idNum
  try {
    const typeRes = await api.aiContract.getContractTypes()
    typeDict.value = typeRes.data || {}
  } catch {}
  // 读取模板详情（auto 模式优先走 auto-fulfillment 模板详情）
  const source = String(route.query.source || '')
  const tplRes: any = source === 'auto'
    ? await (await import('@/api/ai/auto-fulfillment')).getTemplateById(idNum)
    : await api.aiContract.getTemplateById(idNum)
  template.value = tplRes?.data
  // 读取模板规则
  const ruleRes: any = await readRuleByTemplateId(idNum)
  raw.value = ruleRes?.data || {}
  // 若无规则，则以模板字段生成默认规则结构
  if (!raw.value || Object.keys(raw.value).length === 0) {
    try {
      const fieldsArr: string[] = JSON.parse(template.value?.fields || '[]')
      const fieldsObj: Record<string, any> = {}
      for (const f of fieldsArr) fieldsObj[f] = { type: 'string', required: false }
      raw.value = { name: template.value?.name, contractType: template.value?.contractType, fields: fieldsObj, prompt: { global: [], negative: [], format: [], fields: {} } }
    } catch {
      raw.value = { name: template.value?.name, contractType: template.value?.contractType, fields: {}, prompt: { global: [], negative: [], format: [], fields: {} } }
    }
  }
  normalizePrompt(raw.value)
}

function openRuleDialog(field: string) {
  ruleDialog.field = field
  if (!prompt.fields[field]) prompt.fields[field] = []
  ruleDialog.rules = [...prompt.fields[field]]
  ruleDialog.visible = true
}
function saveRuleDialog() {
  if (!ruleDialog.field) return
  prompt.fields[ruleDialog.field] = ruleDialog.rules.filter(s => !!s && s.trim().length > 0)
  ruleDialog.visible = false
}

function setFields(obj: Record<string, any>) {
  // reset reactive map
  Object.keys(fieldsDef).forEach(k => delete fieldsDef[k])
  for (const [k, v] of Object.entries(obj)) {
    const copy = JSON.parse(JSON.stringify(v))
    // 兼容后端已有的 enum → 展示到 dictionary/itemEnum
    if (copy && copy.type === 'object' && Array.isArray(copy.enum) && !copy.dictionary) {
      copy.dictionary = [...copy.enum]
    }
    if (copy && copy.type === 'array' && copy.items && Array.isArray(copy.items.enum) && !copy.itemEnum) {
      copy.itemEnum = [...copy.items.enum]
    }
    fieldsDef[k] = copy
  }
}

function doAddFieldDef() {
  const name = newFieldDef.value.trim()
  if (!name) return
  if (!fieldsDef[name]) fieldsDef[name] = { type: newFieldType.value }
  newFieldDef.value = ''
}

function deleteField(name: string) {
  delete fieldsDef[name]
}

function isStringArray(v: any) {
  return Array.isArray(v) && v.every((x: any) => typeof x === 'string')
}

// 高级编辑已移除

async function onSave() {
  if (!templateId.value) return
  saving.value = true
  try {
    // 深拷贝 fields，避免响应式代理影响序列化
    const outFields: Record<string, any> = JSON.parse(JSON.stringify(fieldsDef))
    // 规范化：object.dictionary → enum；array.itemEnum → items.enum
    for (const [name, rule] of Object.entries(outFields)) {
      if (!rule || typeof rule !== 'object') continue
      if (rule.type === 'object' && Array.isArray(rule.dictionary)) {
        const enums = rule.dictionary.filter((s: any) => typeof s === 'string' && s.trim().length)
        if (enums.length) rule.enum = enums
      }
      if (rule.type === 'array') {
        rule.items = rule.items || {}
        if (!rule.items.type && rule.itemType) rule.items.type = rule.itemType
        if (Array.isArray(rule.itemEnum)) {
          const enums = rule.itemEnum.filter((s: any) => typeof s === 'string' && s.trim().length)
          if (enums.length) rule.items.enum = enums
        }
      }
      if (rule.type === 'integer') {
        // 可选：提供基础的整数校验提示
        rule.pattern = rule.pattern || '^-?\\d+$'
      }
    }
    const out = { ...(raw.value || {}), name: template.value?.name, contractType: template.value?.contractType, fields: outFields, prompt: { global: prompt.global, negative: prompt.negative, format: prompt.format, fields: prompt.fields } }
    await saveRuleByTemplateId(templateId.value, out)
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(initByTemplateId)
</script>

<style scoped>
.rule-settings { padding: 16px; }
.toolbar { margin: 12px 0; }
.toolbar-row { display: flex; gap: 12px; align-items: center; }
.panel { min-height: 420px; }
.card-header { font-weight: 600; }
.section { margin-bottom: 16px; }
.section-title { font-size: 14px; color: #606266; margin-bottom: 8px; }
.fields-box { max-height: 560px; overflow: auto; padding-right: 8px; }
.field-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.field-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.field-name { font-weight: 600; }
.add-field { display: flex; gap: 8px; align-items: center; }
.pattern-input { width: 200px; }
.short-input { width: 120px; }
.ml8 { margin-left: 8px; }
.mr4 { margin-right: 4px; }
.muted { color: #909399; }
.mt8 { margin-top: 8px; }
.mt16 { margin-top: 16px; }
.rule-summary { margin-bottom: 6px; }
</style>


