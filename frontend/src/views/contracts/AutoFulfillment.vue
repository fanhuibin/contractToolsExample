<template>
  <div class="auto-fulfillment">
    <PageHeader 
      title="自动履约任务"
      description="上传合同文件，选择识别模板，系统将执行自动履约任务并输出结果。"
      :icon="Document"
    />
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <h3>自动履约任务</h3>
                <span class="subtitle">请选择模板并上传合同文件</span>
              </div>
              <div class="card-header-actions">
                 <el-button type="primary" @click="goRuleSettings">提取规则设置</el-button>
                <el-button type="primary" link @click="showHistoryDialog">识别记录</el-button>
              </div>
            </div>
          </template>

          <el-upload class="upload-area" drag action="#" :auto-upload="false" :show-file-list="false" :on-change="onFileChange" accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png">
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
          </el-upload>

          <div v-if="selectedFile" class="file-info-actions">
            <div class="file-info">
              <el-icon><Document /></el-icon>
              <span>{{ selectedFile.name }} ({{ formatSize(selectedFile.size) }})</span>
            </div>
            <el-button type="primary" :disabled="!canStart" :loading="uploading" @click="startExtract">
              <el-icon style="margin-right: 5px;"><CaretRight /></el-icon>开始识别
            </el-button>
          </div>

          <!-- 提取字段展示（单选/多选：聚合展示） -->
          <div v-if="selectedTemplateFields.length" class="fields-display-area">
            <el-divider content-position="left">提取字段</el-divider>
            <el-tag v-for="f in selectedTemplateFields" :key="f" class="field-tag" size="large">{{ f }}</el-tag>
          </div>

          <div v-if="extracting" class="extracting-status">
            <el-progress :percentage="progress" :status="status" :stroke-width="10" />
            <div class="status-text">{{ statusText }}</div>
          </div>

          <div v-if="result" class="result-area">
            <div class="result-header">
              <h4>识别结果</h4>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="copyResult">复制JSON</el-button>
              </div>
            </div>
            <pre class="result-text">{{ prettyResult }}</pre>
          </div>

          <!-- 自定义编辑区（参考合同信息提取页） -->
          <div v-if="isCustomEditing" class="dynamic-prompts">
            <el-divider content-position="left">自定义提取字段</el-divider>
            <div v-for="(item, idx) in customFields" :key="idx" class="prompt-item">
              <el-input v-model="item.value" placeholder="请输入要提取的字段名称" clearable />
              <el-button type="danger" :icon="Delete" @click="removeCustomField(idx)" circle :disabled="customFields.length <= 1" />
            </div>
            <el-button type="primary" :icon="Plus" @click="addCustomField" plain>添加字段</el-button>
            <div class="save-as-template" v-if="customFields.length > 0 && customFields[0].value">
              <el-divider content-position="left">另存为模板</el-divider>
              <div class="save-template-form">
                <el-input v-model="newTemplateName" placeholder="输入新模板名称" style="width: 200px; margin-right: 10px;" />
                <el-select v-model="newTemplateCategory" placeholder="选择分类" style="width: 150px; margin-right: 10px;">
                  <el-option v-for="(label, code) in contractTypes" :key="code" :label="label" :value="code" />
                </el-select>
                <el-button type="success" :disabled="!canSaveCustom" @click="saveCustomAsTemplate">保存模板</el-button>
              </div>
            </div>
          </div>

          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="false" show-icon />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="template-selector-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <h4>选择识别模板</h4>
              <div style="display:flex;align-items:center;gap:10px;">
                <div>
                  <span style="margin-right:8px;color:#909399;">选择模式</span>
                  <el-radio-group v-model="selectMode" size="small">
                    <el-radio-button label="single">单选</el-radio-button>
                    <el-radio-button label="multiple">多选</el-radio-button>
                  </el-radio-group>
                </div>
                <div v-if="selectMode==='multiple'">
                  <el-switch
                    v-model="mergeMode"
                    active-text="合并识别(默认)"
                    inactive-text="批量识别(旧)"
                  />
                </div>
              </div>
            </div>
          </template>

          <div v-if="selectMode==='single'">
            <el-radio-group v-model="selectedTemplateId" class="template-radio-group">
              <el-collapse v-model="activeCollapse" accordion>
                <el-collapse-item v-for="(label, type) in contractTypes" :key="type" :title="label" :name="type">
                  <div v-if="templatesByType(type).length > 0">
                    <div v-for="tpl in templatesByType(type)" :key="tpl.id" class="template-item-wrapper">
                      <el-radio :label="tpl.id" class="template-radio-item">
                        {{ tpl.name }}
                        <el-tag v-if="tpl.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
                        <el-tag v-if="tpl.type === 'system'" size="small" class="template-tag">系统</el-tag>
                      </el-radio>
                      <div class="template-actions">
                        <el-button type="primary" link size="small" class="copy-button" @click.stop="proxy.$router.push({ path: '/rule-settings', query: { templateId: tpl.id, source: 'auto' } })">编辑该模板规则</el-button>
                        <el-button type="primary" link size="small" class="copy-button" @click.stop="copyTemplateForInlineEditing(tpl)">复制并编辑</el-button>
                        <el-button type="danger" link size="small" class="copy-button" @click.stop="deleteTemplateItem(tpl)">删除</el-button>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-template-tip">暂无该类型模板</div>
                </el-collapse-item>
              </el-collapse>
            </el-radio-group>
          </div>

          <div v-else>
            <el-checkbox-group v-model="selectedTemplateIds" class="template-radio-group">
              <el-collapse v-model="activeCollapse" accordion>
                <el-collapse-item v-for="(label, type) in contractTypes" :key="type" :title="label" :name="type">
                  <div v-if="templatesByType(type).length > 0">
                    <div v-for="tpl in templatesByType(type)" :key="tpl.id" class="template-item-wrapper">
                      <el-checkbox :label="tpl.id" class="template-radio-item">
                        {{ tpl.name }}
                        <el-tag v-if="tpl.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
                        <el-tag v-if="tpl.type === 'system'" size="small" class="template-tag">系统</el-tag>
                      </el-checkbox>
                      <div class="template-actions">
                        <el-button type="primary" link size="small" class="copy-button" @click.stop="proxy.$router.push({ path: '/rule-settings', query: { templateId: tpl.id, source: 'auto' } })">编辑该模板规则</el-button>
                        <el-button type="primary" link size="small" class="copy-button" @click.stop="copyTemplateForInlineEditing(tpl)">复制并编辑</el-button>
                        <el-button type="danger" link size="small" class="copy-button" @click.stop="deleteTemplateItem(tpl)">删除</el-button>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-template-tip">暂无该类型模板</div>
                </el-collapse-item>
              </el-collapse>
            </el-checkbox-group>
            <div class="no-template-tip" style="margin-top:8px;">当前为多选模式，多个模板将按顺序批量识别。</div>
          </div>
        </el-card>

        <!-- 移除任务类型与关键词模块 -->
      </el-col>
    </el-row>

    <el-dialog v-model="historyDialogVisible" title="识别历史记录" width="60%">
      <el-table :data="historyList" style="width: 100%" height="400">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="extractTime" label="识别时间" width="200">
          <template #default="{ row }">{{ new Date(row.extractTime).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button type="primary" link @click="viewHistory(row)">查看详情</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="historyDetailVisible" title="历史记录详情" width="50%">
      <pre class="result-text">{{ prettyHistory }}</pre>
      <template #footer><el-button @click="historyDetailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, getCurrentInstance, watch } from 'vue'
import { UploadFilled, Document, CaretRight, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/ai'
import { PageHeader } from '@/components/common'

const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const extracting = ref(false)
const progress = ref(0)
const status = ref<'success'|'warning'|'exception'|''>('')
const statusText = ref('')
const result = ref('')
const error = ref('')

const contractTypes = ref<Record<string, string>>({})
const templates = ref<any[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedTemplateIds = ref<number[]>([])
const selectMode = ref<'single'|'multiple'>('single')
const activeCollapse = ref<string>('invoice_fulfillment')
const mergeMode = ref(true)

// 已移除任务类型与关键词
const { proxy } = getCurrentInstance() as any

// 提取字段展示
const selectedTemplateFields = ref<string[]>([])
function syncSelectedTemplateFields() {
  try {
    if (selectMode.value === 'single') {
      if (typeof selectedTemplateId.value === 'number') {
        const tpl = templates.value.find((t: any) => t.id === selectedTemplateId.value)
        const fields: string[] = tpl?.fields ? JSON.parse(tpl.fields) : []
        selectedTemplateFields.value = Array.isArray(fields) ? fields : []
      } else {
        selectedTemplateFields.value = []
      }
    } else {
      // 多选：合并所有已选模板的字段并去重
      const allFields: string[] = []
      for (const id of selectedTemplateIds.value) {
        const tpl = templates.value.find((t: any) => t.id === id)
        if (!tpl) continue
        const fields: string[] = (() => { try { return tpl.fields ? JSON.parse(tpl.fields) : [] } catch { return [] } })()
        if (Array.isArray(fields)) allFields.push(...fields)
      }
      const unique = Array.from(new Set(allFields.filter((f) => typeof f === 'string' && f.trim().length > 0)))
      selectedTemplateFields.value = unique
    }
  } catch {
    selectedTemplateFields.value = []
  }
}
  function goRuleSettingsForSelected(tpl: any) {
    selectedTemplateId.value = tpl.id
    proxy.$router.push({ path: '/rule-settings', query: { templateId: tpl.id, source: 'auto' } })
  }
  async function copyAndEdit(tpl: any) {
    try {
      // 复制模板到用户空间再跳规则页
      const userId = 'default-user'
      const resp: any = await api.aiAutoFulfillment.copyTemplate(tpl.id, `${tpl.name} - 副本`, userId)
      const newTpl = resp?.data
      if (newTpl?.id) {
        selectedTemplateId.value = newTpl.id
        proxy.$router.push({ path: '/rules', query: { templateId: newTpl.id, source: 'auto' } })
      }
    } catch (e) {
      ElMessage.error('复制失败')
    }
  }

// 顶部按钮：提取规则设置
function goRuleSettings() {
  if (typeof selectedTemplateId.value === 'number') {
    proxy.$router.push({ path: '/rule-settings', query: { templateId: selectedTemplateId.value, source: 'auto' } })
  } else {
    ElMessage.warning('请先在右侧选择一个模板')
  }
}

// 自定义编辑（复制并编辑）
const isCustomEditing = ref(false)
const customFields = ref<{ value: string }[]>([])
const newTemplateName = ref('')
const newTemplateCategory = ref('')
const canSaveCustom = computed(() => newTemplateName.value.trim().length > 0 && !!newTemplateCategory.value && customFields.value.length > 0 && customFields.value.every(f => f.value && f.value.trim().length > 0))

function addCustomField() { customFields.value.push({ value: '' }) }
function removeCustomField(i: number) {
  if (customFields.value.length <= 1) return
  customFields.value.splice(i, 1)
}

async function copyTemplateForInlineEditing(tpl: any) {
  try {
    const fields = JSON.parse(tpl.fields || '[]') as string[]
    customFields.value = Array.isArray(fields) && fields.length ? fields.map(f => ({ value: f })) : [{ value: '' }]
    newTemplateName.value = `${tpl.name} - 副本`
    newTemplateCategory.value = tpl.categoryCode || tpl.contractType || 'custom_fulfillment'
    isCustomEditing.value = true
    // 同步展示
    selectedTemplateFields.value = fields
    selectedTemplateId.value = tpl.id
    ElMessage.success(`已加载“${tpl.name}”字段到编辑区，可直接保存为新模板或进入“提取规则设置”`) 
  } catch {
    isCustomEditing.value = true
    customFields.value = [{ value: '' }]
    ElMessage.error('模板字段解析失败，已为你打开空白编辑区')
  }
}

async function saveCustomAsTemplate() {
  if (!canSaveCustom.value) return
  try {
    const fields = customFields.value.map(f => f.value.trim())
    const payload = {
      name: newTemplateName.value.trim(),
      categoryCode: newTemplateCategory.value,
      contractType: newTemplateCategory.value, // 兼容后端旧列
      fields: JSON.stringify(fields),
      type: 'user',
      creatorId: 'default-user',
      isDefault: false,
      description: '用户自定义模板'
    }
    const resp: any = await api.aiAutoFulfillment.createTemplate(payload)
    ElMessage.success('模板保存成功')
    // 重新加载模板并选中新模板
    await loadInit()
    const newTpl = (resp as any)?.data
    if (newTpl?.id) selectedTemplateId.value = newTpl.id
    isCustomEditing.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存模板失败')
  }
}

// 删除模板
async function deleteTemplateItem(tpl: any) {
  try {
    await ElMessageBox.confirm(`确定删除模板“${tpl.name}”吗？此操作不可恢复。`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await api.aiAutoFulfillment.deleteTemplate(tpl.id)
    ElMessage.success('删除成功')
    // 刷新模板并重置选择
    await loadInit()
    if (selectedTemplateId.value === tpl.id) selectedTemplateId.value = null
    selectedTemplateIds.value = selectedTemplateIds.value.filter(id => id !== tpl.id)
    selectedTemplateFields.value = []
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

// 移除与任务类型/关键词相关的方法

const historyDialogVisible = ref(false)
const historyList = ref<any[]>([])
const historyDetailVisible = ref(false)
const historyDetail = ref('')

onMounted(async () => {
  await loadInit()
  syncSelectedTemplateFields()
})

watch(selectedTemplateId, () => { syncSelectedTemplateFields() })
watch(selectedTemplateIds, () => { syncSelectedTemplateFields() })
watch(selectMode, () => { syncSelectedTemplateFields() })

async function loadInit() {
  try {
    const [typesRes, templatesRes] = await Promise.all([
      api.aiAutoFulfillment.getContractTypes(),
      api.aiAutoFulfillment.getTemplates('default-user')
    ])
    const allTypes = (typesRes as any)?.data || {}
    // 过滤掉“到期提醒”“事件触发”，新增“自定义”
    const filtered: Record<string,string> = {}
    for (const [k,v] of Object.entries(allTypes)) {
      if (k === 'expiry_reminder' || k === 'event_trigger') continue
      filtered[k] = v as string
    }
    // 如果后端未返回自定义，则补充
    if (!filtered['custom_fulfillment']) filtered['custom_fulfillment'] = '自定义'
    contractTypes.value = filtered
    templates.value = (templatesRes as any)?.data || []
    const def = templates.value.find((t: any) => t.isDefault)
    if (def) selectedTemplateId.value = def.id
  } catch (e) {
    ElMessage.error('加载模板数据失败')
  }
}

function templatesByType(type: string) { return templates.value.filter((t: any) => (t.categoryCode || t.contractType) === type) }

function onFileChange(file: any) {
  selectedFile.value = file.raw
  error.value = ''
  result.value = ''
  // 同步一次字段，避免切换模板后未展示
  syncSelectedTemplateFields()
}

const canStart = computed(() => {
  if (!selectedFile.value) return false
  if (selectMode.value === 'single') return typeof selectedTemplateId.value === 'number'
  return selectedTemplateIds.value.length > 0
})

async function startExtract() {
  if (!canStart.value) return
  uploading.value = true
  extracting.value = true
  progress.value = 10
  status.value = ''
  statusText.value = '正在上传文件...'
  error.value = ''
  result.value = ''
  try {
    const payloadTemplateId = selectMode.value === 'single' ? selectedTemplateId.value! : (selectedTemplateIds.value[0] as number)
    const payloadTemplateIds = selectMode.value === 'multiple' ? selectedTemplateIds.value : undefined
    const resp: any = await api.aiAutoFulfillment.extractInfo(selectedFile.value!, undefined, payloadTemplateId, undefined as any, undefined as any, payloadTemplateIds, selectMode.value==='multiple' ? mergeMode.value : undefined)
    const taskId = resp?.data?.taskId
    if (!taskId) throw new Error(resp?.message || '提交失败')
    statusText.value = '正在处理...'
    progress.value = 30
    const ok = await pollStatus(taskId)
    if (ok) {
      status.value = 'success'
      progress.value = 100
      statusText.value = '完成'
    } else {
      status.value = 'exception'
    }
  } catch (e: any) {
    error.value = e?.message || '识别失败'
    status.value = 'exception'
  } finally {
    uploading.value = false
    extracting.value = false
  }
}

async function pollStatus(taskId: string) {
  let tries = 0
  while (tries < 120) {
    await new Promise(r => setTimeout(r, 2000))
    tries++
    try {
      const s: any = await api.aiAutoFulfillment.getTaskStatus(taskId)
      const task = s?.data?.task
      if (!task) throw new Error('获取任务状态失败')
      if (task.status === 'processing') {
        progress.value = Math.min(90, progress.value + 5)
        continue
      }
      if (task.status === 'completed') {
        result.value = task.result
        return true
      }
      if (task.status === 'failed') {
        error.value = task.error || '处理失败'
        return false
      }
    } catch (e: any) {
      error.value = e?.message || '状态查询失败'
      return false
    }
  }
  error.value = '超时'
  return false
}

function copyResult() {
  if (!result.value) return
  navigator.clipboard.writeText(prettyResult.value)
    .then(() => ElMessage.success('已复制'))
    .catch(() => ElMessage.error('复制失败'))
}

function viewHistory(row: any) {
  historyDetail.value = row.extractedContent || ''
  historyDetailVisible.value = true
}

async function showHistoryDialog() {
  try {
    const res: any = await api.aiAutoFulfillment.getHistory('default-user')
    historyList.value = res?.data || []
    historyDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '加载历史失败')
  }
}

const prettyResult = computed(() => {
  try { return JSON.stringify(JSON.parse(result.value), null, 2) } catch { return result.value }
})
const prettyHistory = computed(() => {
  try { return JSON.stringify(JSON.parse(historyDetail.value), null, 2) } catch { return historyDetail.value }
})

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}
</script>

<style scoped>
.auto-fulfillment { padding: 20px; }
.page-header-card { 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  overflow: hidden;
  transition: all 0.3s ease;
}
.page-header-card:hover { box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); }
.page-header { 
  padding: 16px 20px; 
  position: relative; 
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
}
.header-content { position: relative; z-index: 2; }
.header-decoration { 
  position: absolute; 
  top: 0; 
  right: 0; 
  width: 150px; 
  height: 100%; 
  background: linear-gradient(135deg, transparent, var(--el-color-primary-light-5)); 
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}
.page-header h2 { 
  margin: 0; 
  font-size: 26px; 
  color: var(--el-color-primary-dark-2); 
  display: flex; 
  align-items: center;
  font-weight: 600;
}
.header-icon { 
  margin-right: 10px; 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.page-header p { 
  margin: 10px 0 0; 
  color: #606266; 
  font-size: 15px; 
  max-width: 80%;
}
.main-content-card, .template-selector-card { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header-left h3 { margin: 0; }
.subtitle { font-size: 14px; color: #909399; margin-top: 5px; }
.upload-area { margin-bottom: 20px; }
.file-info-actions { display: flex; justify-content: space-between; align-items: center; margin: 20px 0; padding: 10px; border: 1px solid #dcdfe6; border-radius: 4px; }
.file-info { display: flex; align-items: center; gap: 8px; color: #606266; }
.extracting-status, .result-area, .error-message { margin-top: 20px; }
.status-text { margin-top: 10px; text-align: center; color: #606266; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.result-text { background-color: #f5f5f5; padding: 15px; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word; font-family: monospace; }
.dynamic-prompts { margin-top: 16px; }
.prompt-item { display:flex; align-items:center; gap:10px; margin-bottom: 10px; }
.save-as-template { margin-top: 16px; }
.save-template-form { display:flex; align-items:center; }
.template-radio-group { display: flex; flex-direction: column; }
.template-radio-item { display: flex; height: 32px; align-items: center; }
.template-item-wrapper { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.copy-button { margin-left: 10px; flex-shrink: 0; }
.template-actions { display:flex; align-items:center; gap:8px; }
.template-tag { margin-left: 8px; }
.no-template-tip { color: #909399; font-size: 14px; padding: 10px; }
:deep(.el-collapse-item__header) { font-size: 16px; font-weight: 500; }
:deep(.el-collapse-item__content) { padding-bottom: 0; }
</style>


