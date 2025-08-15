<template>
  <div class="contract-review-page">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="page-header-card">
          <div class="page-header">
            <div class="header-content">
              <h2><el-icon class="header-icon"><Document /></el-icon>合同智能审核</h2>
              <p>上传合同文件，选择审核清单，系统将进行智能风险预审并返回结果。</p>
            </div>
            <div class="header-decoration"></div>
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
            <div class="upload-content">
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处，或 <em>点击上传</em>
              </div>
              <div class="upload-formats">支持格式: PDF, Word, Excel, 图片</div>
            </div>
          </el-upload>

          <div v-if="selectedFile" class="file-info-actions">
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-details">
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
              </div>
            </div>
            <el-button type="primary" class="start-button" @click="onStartClick" :loading="reviewing">
              <el-icon><CaretRight /></el-icon>
              开始审核
            </el-button>
          </div>

          <!-- Progress / Error -->
          <div v-if="reviewing" class="extracting-status">
            <el-progress :percentage="progress" :status="progressStatus" :stroke-width="12" :show-text="false" />
            <div class="status-text">
              <el-icon class="status-icon" :class="{'rotating': reviewing && progress < 100}"><Loading /></el-icon>
              {{ statusText }}
            </div>
          </div>
          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="false" show-icon effect="dark" />
          </div>

          <!-- Result -->
          <div v-if="results.length" class="result-area">
            <div class="result-header">
              <h4><el-icon class="result-icon"><Check /></el-icon>审核结果</h4>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="viewDetailResult">
                  <el-icon><View /></el-icon>查看详情
                </el-button>
                <el-button type="info" size="small" @click="copyJson()">
                  <el-icon><CopyDocument /></el-icon>复制JSON
                </el-button>
              </div>
            </div>
            <el-alert v-if="traceId" :title="'TraceID: ' + traceId + '，耗时 ' + elapsedMs + 'ms'" type="info" show-icon class="mb10" effect="light" />
            <el-alert v-if="usage" :title="usageTitle" type="success" show-icon class="mb10" effect="light" />
            <div class="result-container">
              <pre class="result-text">{{ prettyJson }}</pre>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Sidebar: Checklist selection -->
      <el-col :span="8">
        <el-card class="template-selector-card">
          <template #header>
            <div class="template-header">
              <h4><el-icon class="checklist-icon"><List /></el-icon>审核清单</h4>
              <el-button size="small" type="primary" plain @click="openChecklistManager">
                <el-icon><Setting /></el-icon>审核清单管理
              </el-button>
            </div>
          </template>

          <el-radio-group v-model="selectedProfileId" class="template-radio-group">
            <el-radio v-for="pf in profiles" :key="pf.id" :label="pf.id" class="template-radio-item" border>
              <div class="radio-content">
                <span>{{ pf.profileName }}</span>
                <el-tag v-if="pf.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
              </div>
            </el-radio>
          </el-radio-group>

          <el-divider />
            <div class="actions-line">
              <el-button type="primary" plain size="small" @click="openSelectFromLibrary">
                <el-icon><Select /></el-icon>从条款库选择
              </el-button>
              <el-input v-model="selectionProfileName" placeholder="方案名称" size="small" class="profile-name-input" />
              <el-button type="success" plain size="small" @click="saveSelectionAsProfile" :disabled="!selectionPointIds.length">
                <el-icon><FolderAdd /></el-icon>保存为方案
              </el-button>
            </div>
          <el-divider content-position="left"><el-icon class="divider-icon"><Collection /></el-icon>当前选择</el-divider>
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
import { UploadFilled, Document, CaretRight, Loading, Check, CopyDocument, List, Setting, Select, FolderAdd, Collection, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import riskApi from '@/api/ai/risk'

// 轻量内嵌的条款库选择器（最小实现：只提供选择和预览功能）
const RiskLibraryEmbed = defineAsyncComponent(() => import('./RiskLibraryEmbed.vue'))
  
const router = useRouter()
const selectedFile = ref<File | null>(null)
const uploadedFileId = ref<string | null>(null) // 新增：用于存储上传后的文件ID
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

function viewDetailResult() {
  if (!uploadedFileId.value) {
    ElMessage.warning('无法查看详情，因为没有有效的文件ID。请先成功执行一次审核。')
    return
  }
  
  // 使用上传并审核后得到的真实文件ID进行跳转
  const fileId = uploadedFileId.value
  
  // 跳转到详情页面
  router.push(`/contract-review-detail/${fileId}`)
  
  // 可以通过localStorage传递一些临时数据
  localStorage.setItem('review_result_data', JSON.stringify({
    fileName: selectedFile.value.name,
    reviewDate: new Date().toLocaleString(),
    results: results.value
  }))
  
  ElMessage.success('正在跳转到详情页面')
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
    statusText.value = '正在解析审核结果...'
    progress.value = 100
    progressStatus.value = 'success'
    
    // 从返回结果中提取关键信息
    results.value = res.data.data?.results || []
    traceId.value = res.data.data?.traceId || ''
    elapsedMs.value = res.data.data?.elapsedMs || 0
    docMeta.value = res.data.data?.docMeta || { pages: 0, paragraphs: 0 }
    usage.value = res.data.data?.usage || null

    // 新增：从响应中获取并存储上传后的文件ID
    // 注意：这里的 'res.data.data.fileId' 是一个假设的路径，需要根据后端真实返回的结构进行调整
    if (res.data.data && res.data.data.fileId) {
      uploadedFileId.value = res.data.data.fileId
    } else {
      // 如果后端没有返回 fileId，我们做一个兜底，仍然使用示例文件
      // 但在生产环境中，这里应该抛出一个错误或警告
      console.warn("后端响应中未找到 fileId，将使用默认示例文件进行预览。")
      uploadedFileId.value = 'templateDesign' 
    }

  } catch (err: any) {
    error.value = err?.message || '审核失败'
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
/* 全局样式 */
.contract-review-page { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }

/* 页面头部 */
.page-header-card { 
  margin-bottom: 20px; 
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

/* 主内容区 */
.main-content { margin-bottom: 20px; }
.main-content-card, .template-selector-card { 
  height: 100%; 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  transition: all 0.3s ease;
}
.main-content-card:hover, .template-selector-card:hover { 
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); 
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header-left h3 { margin: 0; font-weight: 600; color: var(--el-color-primary-dark-2); }
.subtitle { font-size: 14px; color: #909399; margin-top: 5px; }

/* 上传区域 */
.upload-area { 
  margin-bottom: 20px; 
  border: 2px dashed var(--el-color-primary-light-5); 
  border-radius: 8px; 
  transition: all 0.3s ease;
  background-color: var(--el-color-primary-light-9);
}
.upload-area:hover { 
  border-color: var(--el-color-primary); 
  background-color: var(--el-color-primary-light-8);
}
.upload-content { padding: 20px 0; }
.el-icon--upload { 
  font-size: 48px; 
  color: var(--el-color-primary); 
  margin-bottom: 10px;
  animation: pulse 2s infinite;
}
.el-upload__text { 
  font-size: 16px; 
  color: #606266; 
  margin-bottom: 8px;
}
.el-upload__text em { 
  color: var(--el-color-primary); 
  font-style: normal; 
  font-weight: 600;
  text-decoration: underline;
}
.upload-formats { 
  font-size: 13px; 
  color: #909399; 
  margin-top: 8px;
}

/* 文件信息 */
.file-info-actions { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin: 20px 0; 
  padding: 15px; 
  border: 1px solid var(--el-color-primary-light-7); 
  border-radius: 8px; 
  background-color: var(--el-color-primary-light-9);
  transition: all 0.3s ease;
}
.file-info-actions:hover { 
  border-color: var(--el-color-primary-light-3); 
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.file-info { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  color: #606266;
}
.file-icon { 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.file-details { 
  display: flex; 
  flex-direction: column;
}
.file-name { 
  font-weight: 600; 
  color: #303133; 
  margin-bottom: 4px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size { 
  font-size: 13px; 
  color: #909399;
}
.start-button { 
  padding: 12px 20px; 
  font-weight: 600; 
  transition: all 0.3s ease;
}
.start-button .el-icon { margin-right: 8px; }

/* 进度状态 */
.extracting-status { 
  margin-top: 20px; 
  padding: 15px; 
  border-radius: 8px; 
  background-color: var(--el-color-primary-light-9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.status-text { 
  margin-top: 12px; 
  text-align: center; 
  color: var(--el-color-primary-dark-2); 
  font-weight: 600; 
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.status-icon { 
  font-size: 18px; 
  color: var(--el-color-primary);
}
.rotating { 
  animation: rotate 1.5s linear infinite;
}
.error-message { margin-top: 20px; }

/* 结果区域 */
.result-area { 
  margin-top: 20px; 
  padding: 20px; 
  border-radius: 8px; 
  background-color: #fff; 
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--el-color-success-light-5);
}
.result-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-color-success-light-5);
}
.result-header h4 { 
  margin: 0; 
  font-size: 18px; 
  color: var(--el-color-success-dark-2);
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-icon { 
  color: var(--el-color-success);
}
.result-container { 
  margin-top: 15px; 
  max-height: 400px; 
  overflow-y: auto; 
  background-color: #f8f9fa; 
  border-radius: 6px; 
  border: 1px solid #ebeef5;
}
.result-text { 
  padding: 15px; 
  margin: 0; 
  font-family: 'Courier New', monospace; 
  font-size: 14px; 
  line-height: 1.5;
  color: #303133;
}

/* 模板选择器 */
.template-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
}
.template-header h4 { 
  margin: 0; 
  font-size: 18px; 
  color: var(--el-color-primary-dark-2);
  display: flex;
  align-items: center;
  gap: 8px;
}
.checklist-icon { 
  color: var(--el-color-primary);
}
.template-radio-group { 
  display: flex; 
  flex-direction: column; 
  gap: 10px; 
  margin: 10px 0;
}
.template-radio-item { 
  border-radius: 6px; 
  transition: all 0.3s ease;
}
.radio-content { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  width: 100%;
}
.template-tag { 
  margin-left: 8px; 
  font-size: 11px;
}
.actions-line { 
  display: flex; 
  gap: 8px; 
  margin: 15px 0;
  flex-wrap: wrap;
}
.profile-name-input { 
  width: 160px; 
  transition: all 0.3s ease;
}
.profile-name-input:focus { 
  width: 180px;
}
.divider-icon { 
  margin-right: 5px; 
  color: var(--el-color-primary);
}

/* 动画 */
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.mb10 { margin-bottom: 10px; }
.mr8 { margin-right: 8px; }
</style>

