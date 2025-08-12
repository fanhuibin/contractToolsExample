<template>
  <div class="contract-review-page">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="page-header-card">
          <div class="page-header">
            <h2>合同智能审核</h2>
            <p>上传合同文件，选择审核清单，系统将进行智能风险预审并返回结果。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="main-content">
      <!-- Main -->
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <h3>上传合同并执行审核</h3>
                <span class="subtitle">右侧选择审核清单或从条款库勾选生成</span>
              </div>
            </div>
          </template>

          <!-- Upload Area -->
          <el-upload
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处，或 <em>点击上传</em>
            </div>
          </el-upload>

          <div v-if="selectedFile" class="file-info-actions">
            <div class="file-info">
              <el-icon><Document /></el-icon>
              <span>{{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})</span>
            </div>
            <el-button type="primary" @click="onStartClick" :loading="reviewing">
              <el-icon style="margin-right: 5px;"><CaretRight /></el-icon>
              开始审核
            </el-button>
          </div>

          <!-- Progress / Error -->
          <div v-if="reviewing" class="extracting-status">
            <el-progress :percentage="progress" :status="progressStatus" :stroke-width="10" />
            <div class="status-text">{{ statusText }}</div>
          </div>
          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="false" show-icon />
          </div>

          <!-- Result -->
          <div v-if="results.length" class="result-area">
            <div class="result-header">
              <h4>审核结果</h4>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="copyJson()">复制JSON</el-button>
              </div>
            </div>
            <el-alert v-if="traceId" :title="'TraceID: ' + traceId + '，耗时 ' + elapsedMs + 'ms'" type="info" show-icon class="mb10" />
            <el-alert v-if="usage" :title="usageTitle" type="success" show-icon class="mb10" />
            <pre class="result-text">{{ prettyJson }}</pre>
          </div>
        </el-card>
      </el-col>

      <!-- Sidebar: Checklist selection -->
      <el-col :span="8">
        <el-card class="template-selector-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <h4>审核清单</h4>
              <el-button size="small" type="primary" plain @click="openChecklistManager">审核清单管理</el-button>
            </div>
          </template>

          <el-radio-group v-model="selectedProfileId" class="template-radio-group">
            <el-radio v-for="pf in profiles" :key="pf.id" :label="pf.id" class="template-radio-item">
              {{ pf.profileName }}
              <el-tag v-if="pf.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
            </el-radio>
          </el-radio-group>

          <el-divider />
            <div class="actions-line">
              <el-button type="primary" plain size="small" @click="openSelectFromLibrary">从条款库选择</el-button>
              <el-input v-model="selectionProfileName" placeholder="方案名称" size="small" style="width:160px" />
              <el-button type="success" plain size="small" @click="saveSelectionAsProfile" :disabled="!selectionPointIds.length">保存为方案</el-button>
            </div>
          <el-divider content-position="left">当前选择</el-divider>
          <el-table :data="selectionPreview" size="small" border height="280">
            <el-table-column prop="clauseType" label="分类" width="140" />
            <el-table-column prop="pointName" label="风险点" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选择条款库弹窗：复用已有 RiskLibrary 树的预览接口 -->
    <el-dialog v-model="libVisible" title="从条款库选择风险点" width="820px" append-to-body>
      <RiskLibraryEmbed ref="libRef" />
      <template #footer>
        <el-button @click="libVisible=false">取消</el-button>
        <el-button type="primary" @click="applySelectionFromLibrary">应用所选</el-button>
      </template>
    </el-dialog>

    
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, defineAsyncComponent } from 'vue'
import { UploadFilled, Document, CaretRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import riskApi from '@/api/ai/risk'

// 轻量内嵌的条款库选择器（最小实现：只提供选择和预览功能）
const RiskLibraryEmbed = defineAsyncComponent(() => import('./RiskLibraryEmbed.vue'))
  

const selectedFile = ref<File | null>(null)
const reviewing = ref(false)
const progress = ref(0)
const progressStatus = ref<'success' | 'warning' | 'exception' | ''>('')
const statusText = ref('')
const error = ref('')

const profiles = ref<any[]>([])
const selectedProfileId = ref<number | null>(null)

const selectionPointIds = ref<number[]>([])
const selectionPreview = ref<any[]>([])
const selectionProfileName = ref('')
const traceId = ref('')
const elapsedMs = ref(0)
const docMeta = ref<any>({ pages: 0, paragraphs: 0 })
const results = ref<any[]>([])
const usage = ref<any | null>(null)
const prettyJson = computed(() => {
  try { return JSON.stringify({ traceId: traceId.value, elapsedMs: elapsedMs.value, docMeta: docMeta.value, results: results.value }, null, 2) }
  catch { return '' }
})
const usageTitle = computed(() => {
  if (!usage.value) return ''
  const u = usage.value as any
  const pt = Number(u.promptTokens ?? 0)
  const ct = Number(u.completionTokens ?? 0)
  const tt = Number(u.totalTokens ?? (pt + ct))
  const chars = Number(u.promptChars ?? 0)
  // 价格：输入 ¥0.012/1K，输出 ¥0.024/1K（用户给出的口径）
  const inCost = (pt / 1000) * 0.012
  const outCost = (ct / 1000) * 0.024
  const totalCost = inCost + outCost
  return `字数: ${chars}，输入tokens: ${pt} 输出tokens: ${ct}，本次调用费用约 ¥${totalCost.toFixed(2)}`
})

const canStartReview = computed(() => !!selectedFile.value && (!!selectedProfileId.value || selectionPointIds.value.length > 0))

onMounted(async () => {
  try {
    const res = await riskApi.listProfiles()
    profiles.value = (res as any)?.data || []
    const def = profiles.value.find((p: any) => p.isDefault)
    if (def) selectedProfileId.value = def.id
  } catch (e: any) {
    ElMessage.error(e?.message || '加载方案失败')
  }
})

function handleFileChange(file: any) {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) { ElMessage.error('文件大小不能超过100MB'); return false }
  selectedFile.value = file.raw
  error.value = ''
  traceId.value = ''
  results.value = []
  return false
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

function tagType(t: string) {
  if (t === 'ERROR') return 'danger'
  if (t === 'WARNING') return 'warning'
  return 'info'
}

function copyJson() {
  try {
    const text = JSON.stringify({ traceId: traceId.value, elapsedMs: elapsedMs.value, docMeta: docMeta.value, results: results.value }, null, 2)
    navigator.clipboard.writeText(text)
    ElMessage.success('已复制JSON')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

async function startReview() {
  if (!selectedFile.value) return
  try {
    reviewing.value = true
    progress.value = 25
    progressStatus.value = ''
    statusText.value = '正在准备审核...'

    const payload = selectionPointIds.value.length ? selectionPointIds.value : undefined
    console.log('[review] start', { profileId: selectedProfileId.value, points: payload, file: selectedFile.value?.name })
    const res: any = await riskApi.executeReview(selectedProfileId.value || undefined, payload, selectedFile.value as File)
    console.log('[review] response', res)
    progress.value = 80
    statusText.value = '生成报告...'
    const data = res?.data || {}
    traceId.value = data.traceId
    elapsedMs.value = data.elapsedMs || 0
    docMeta.value = data.docMeta || { pages: 0, paragraphs: 0 }
    usage.value = data.usage || null
    // 兼容 findings（旧结构）与 results（新结构）
    if (Array.isArray(data.results)) {
      results.value = data.results
    } else if (Array.isArray(data.findings)) {
      const mapped = [] as any[]
      for (const f of data.findings) {
        const prompts = Array.isArray(f.prompts) ? f.prompts : []
        for (const pr of prompts) {
          mapped.push({
            clauseType: f.clauseTypeName || f.clauseType || '',
            pointId: String(f.pointId ?? f.pointCode ?? ''),
            algorithmType: f.algorithmType,
            decisionType: pr.name || pr.promptKey || '',
            statusType: pr.statusType || 'INFO',
            message: pr.message || '',
            actions: [],
            evidence: []
          })
        }
      }
      results.value = mapped
    } else {
      results.value = []
    }
    progress.value = 100
    progressStatus.value = 'success'
    statusText.value = '完成'
  } catch (e: any) {
    error.value = e?.message || '审核失败'
    progressStatus.value = 'exception'
  } finally {
    reviewing.value = false
  }
}

function onStartClick() {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  if (!selectedProfileId.value && selectionPointIds.value.length === 0) {
    ElMessage.warning('请选择审核清单：可选方案，或从条款库勾选风险点')
    return
  }
  // 触发审核
  startReview()
}

// 条款库选择相关
const libVisible = ref(false)
const libRef = ref<any>(null)
function openSelectFromLibrary() { libVisible.value = true }
async function applySelectionFromLibrary() {
  if (!libRef.value || typeof libRef.value.getCheckedPointIds !== 'function') { libVisible.value = false; return }
  const ids: number[] = libRef.value.getCheckedPointIds()
  selectionPointIds.value = ids
  const res: any = await riskApi.previewSelection(ids)
  selectionPreview.value = res?.data || []
  libVisible.value = false
}

// 审核清单管理：跳转独立页面
import { useRouter } from 'vue-router'
const router = useRouter()
function openChecklistManager() { router.push({ path: '/risk-library', query: { from: 'contract-review' } }) }

// 保存为方案
async function saveSelectionAsProfile() {
  if (!selectionPointIds.value.length) return
  const code = 'p_' + Date.now()
  const name = selectionProfileName.value?.trim() || ('方案_' + new Date().toLocaleString())
  const res: any = await riskApi.createProfile({ profileCode: code, profileName: name, isDefault: false, description: '从选择生成' })
  const profile = res?.data
  if (profile?.id) {
    const items = selectionPreview.value.map((it: any, idx: number) => ({ profileId: profile.id, clauseTypeId: 0, pointId: it.pointId, sortOrder: idx + 1 }))
    await riskApi.saveProfileItems(profile.id, items)
    profiles.value = (await riskApi.listProfiles() as any).data || []
    selectedProfileId.value = profile.id
    selectionProfileName.value = ''
    ElMessage.success('已保存为方案')
  }
}
</script>

<style scoped>
.contract-review-page { padding: 20px; }
.page-header-card { margin-bottom: 20px; border-left: 5px solid var(--el-color-primary); }
.page-header { padding: 5px 0; }
.page-header h2 { margin: 0; font-size: 24px; color: #303133; }
.page-header p { margin: 10px 0 0; color: #606266; font-size: 14px; }
.main-content { margin-bottom: 20px; }
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
.muted { color: #909399; }
.bold { font-weight: 600; }
.template-radio-group { display: flex; flex-direction: column; }
.template-radio-item { display: flex; height: 32px; align-items: center; }
.template-tag { margin-left: 8px; }
.actions-line { display: flex; gap: 8px; margin-bottom: 8px; }
.mb10 { margin-bottom: 10px; }
.mr8 { margin-right: 8px; }
</style>

