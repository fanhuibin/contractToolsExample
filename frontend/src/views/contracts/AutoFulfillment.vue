<template>
  <div class="auto-fulfillment">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <h3>自动履约任务</h3>
                <span class="subtitle">请选择模板并上传合同文件</span>
              </div>
              <el-button type="primary" link @click="historyDialogVisible = true">识别记录</el-button>
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

          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="false" show-icon />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="template-selector-card">
          <template #header><h4>选择识别模板</h4></template>
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
                    <el-button type="primary" link size="small" class="copy-button" disabled>复制并编辑</el-button>
                  </div>
                </div>
                <div v-else class="no-template-tip">暂无该类型模板</div>
              </el-collapse-item>
            </el-collapse>
          </el-radio-group>
        </el-card>

        <el-card class="mt16">
          <template #header><div class="card-header">任务类型与关键词</div></template>
          <div class="filter-row">
            <div class="filter-col">
              <div class="filter-title">任务类型（可多选）</div>
              <el-tree
                :data="taskTypeTree"
                show-checkbox
                node-key="id"
                :default-expanded-keys="defaultExpanded"
                :props="{ label: 'label', children: 'children' }"
                @check="onTaskTypeCheck"
                ref="taskTreeRef"
              />
            </div>
            <div class="filter-col">
              <div class="filter-title">关联关键词（随任务类型变化，可增删）</div>
              <el-select v-model="selectedKeywords" multiple filterable allow-create default-first-option style="width:100%" placeholder="选择或输入关键词">
                <el-option v-for="k in keywordOptions" :key="k" :label="k" :value="k" />
              </el-select>
            </div>
          </div>
        </el-card>
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
import { ref, computed, onMounted } from 'vue'
import { UploadFilled, Document, CaretRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/ai'

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
const activeCollapse = ref<string>('operation')

// 任务类型树 & 关键词
const taskTypeTree = ref<any[]>([])
const defaultExpanded = ref<number[]>([])
const selectedTaskTypeIds = ref<number[]>([])
const selectedKeywords = ref<string[]>([])
const keywordOptions = ref<string[]>([])
const taskTreeRef = ref()

async function refreshKeywordsBySelected() {
  if (!selectedTaskTypeIds.value.length) {
    keywordOptions.value = []
    return
  }
  const resp: any = await api.aiAutoFulfillment.getKeywordsByTaskTypeIds(selectedTaskTypeIds.value)
  const list: any[] = resp?.data || []
  keywordOptions.value = list.map(x => x.name)
}

async function onTaskTypeCheck(_: any, payload: any) {
  const checked: number[] = payload.checkedKeys || []
  selectedTaskTypeIds.value = checked
  await refreshKeywordsBySelected()
}

const historyDialogVisible = ref(false)
const historyList = ref<any[]>([])
const historyDetailVisible = ref(false)
const historyDetail = ref('')

onMounted(async () => {
  await loadInit()
  try {
    const tree: any = await api.aiAutoFulfillment.getTaskTypesTree()
    const transform = (nodes: any[]): any[] => nodes.map(n => ({ id: n.id, label: n.label, children: n.children ? transform(n.children) : [] }))
    taskTypeTree.value = transform((tree as any)?.data || [])
    defaultExpanded.value = (taskTypeTree.value || []).map((n: any) => n.id)
  } catch {}
})

async function loadInit() {
  try {
    const [typesRes, templatesRes] = await Promise.all([
      api.aiAutoFulfillment.getContractTypes(),
      api.aiAutoFulfillment.getTemplates('default-user')
    ])
    contractTypes.value = (typesRes as any)?.data || {}
    templates.value = (templatesRes as any)?.data || []
    const def = templates.value.find((t: any) => t.isDefault)
    if (def) selectedTemplateId.value = def.id
  } catch (e) {
    ElMessage.error('加载模板数据失败')
  }
}

function templatesByType(type: string) { return templates.value.filter((t: any) => t.contractType === type) }

function onFileChange(file: any) {
  selectedFile.value = file.raw
  error.value = ''
  result.value = ''
}

const canStart = computed(() => !!selectedFile.value && typeof selectedTemplateId.value === 'number')

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
    const resp: any = await api.aiAutoFulfillment.extractInfo(selectedFile.value!, undefined, selectedTemplateId.value!, selectedTaskTypeIds.value as unknown as string[], selectedKeywords.value)
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
.template-radio-group { display: flex; flex-direction: column; }
.template-radio-item { display: flex; height: 32px; align-items: center; }
.template-item-wrapper { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.copy-button { margin-left: 10px; flex-shrink: 0; }
.template-tag { margin-left: 8px; }
.no-template-tip { color: #909399; font-size: 14px; padding: 10px; }
:deep(.el-collapse-item__header) { font-size: 16px; font-weight: 500; }
:deep(.el-collapse-item__content) { padding-bottom: 0; }
</style>


