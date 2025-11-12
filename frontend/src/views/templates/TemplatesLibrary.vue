<template>
  <div class="templates-lib">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-content">
          <div class="card-title">
            <el-button 
              text 
              @click="goBack"
              style="margin-right: 12px;"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            模板管理
          </div>
          <div class="header-actions">
            <el-input 
              v-model="keyword" 
              placeholder="搜索名称/分类" 
              clearable 
              style="width: 220px; margin-right: 8px;" 
              @keyup.enter="fetchList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="fetchList">搜索</el-button>
            <el-button type="primary" @click="goNew" :disabled="isDemoMode" style="margin-left: 8px;">
              <el-icon><Plus /></el-icon>
              新建模板
            </el-button>
          </div>
        </div>
      </template>

    <div class="table-container">
      <el-table :data="filtered" v-loading="loading" style="width:100%" row-key="id">
        <el-table-column prop="templateCode" label="模板编码" width="140" />
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="version" label="版本" width="80">
          <template #default="{ row }">
            v{{ row.version || 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success" size="small">已发布</el-tag>
            <el-tag v-else-if="row.status === 'DRAFT'" type="info" size="small">草稿</el-tag>
            <el-tag v-else-if="row.status === 'DISABLED'" type="warning" size="small">已禁用</el-tag>
            <el-tag v-else-if="row.status === 'DELETED'" type="danger" size="small">已删除</el-tag>
            <el-tag v-else type="info" size="small">{{ row.status || '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="480" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDesigner(row)">设计模板</el-button>
            <el-button link @click="viewElements(row)">查看元素</el-button>
            <el-button link @click="viewVersions(row)">版本管理</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleStatusChange(row, cmd)" :disabled="isDemoMode">
              <el-button link :disabled="isDemoMode">状态<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="publish" :disabled="isDemoMode || row.status === 'PUBLISHED'">发布</el-dropdown-item>
                  <el-dropdown-item command="draft" :disabled="isDemoMode || row.status === 'DRAFT'">设为草稿</el-dropdown-item>
                  <el-dropdown-item command="disable" :disabled="isDemoMode || row.status === 'DISABLED'">禁用</el-dropdown-item>
                  <el-dropdown-item command="delete" :disabled="isDemoMode" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 前端合成功能已禁用 -->
            <!-- <el-button type="success" size="small" @click="frontendCompose(row)">前端合成</el-button> -->
            <el-button type="primary" size="small" @click="backendCompose(row)">后端合成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState 
        v-if="!loading && filtered.length === 0"
        title="暂无模板"
        description="还没有创建任何模板，点击上方【新建模板】按钮开始创建"
      />
    </div>
    </el-card>

    <el-dialog v-model="viewVisible" title="查看元素" width="860px" append-to-body :lock-scroll="false">
      <el-table :data="elementRows" v-loading="viewing" border style="width:100%">
        <el-table-column prop="index" label="序号" width="70" />
        <el-table-column prop="displayName" label="要素名称" width="140" />
        <el-table-column prop="tag" label="ObjectCode" width="160" />
        <el-table-column prop="dataType" label="数据类型" width="120" />
        <el-table-column label="使用示例">
          <template #default="{ row }">
            <code>{{ row.example }}</code>
            <el-button size="small" text style="margin-left:8px;" @click="copyText(row.example)">复制</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="viewVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionsVisible" title="版本管理" width="900px" append-to-body :lock-scroll="false">
      <div class="versions-header">
        <el-button type="primary" size="small" @click="showCreateVersionDialog" :disabled="isDemoMode">创建新版本</el-button>
      </div>
      <el-table :data="versionsList" v-loading="versionsLoading" border style="width:100%; margin-top: 16px;">
        <el-table-column prop="version" label="版本号" width="80">
          <template #default="{ row }">
            v{{ row.version }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success" size="small">已发布</el-tag>
            <el-tag v-else-if="row.status === 'DRAFT'" type="info" size="small">草稿</el-tag>
            <el-tag v-else-if="row.status === 'DISABLED'" type="warning" size="small">已禁用</el-tag>
            <el-tag v-else type="info" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="版本说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="publishVersionAction(row)" :disabled="isDemoMode">发布</el-button>
            <el-button link size="small" @click="openDesigner(row)">设计</el-button>
            <el-button link size="small" @click="viewElements(row)">查看元素</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="versionsVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVersionVisible" title="创建新版本" width="500px" append-to-body>
      <el-alert 
        title="说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px;">
        新版本将复制当前版本的文件和设计元素，您可以在此基础上继续修改
      </el-alert>
      <el-form ref="versionFormRef" :model="versionForm" :rules="versionRules" label-width="100px">
        <el-form-item label="当前版本">
          <el-input :value="currentVersionDisplay" disabled />
        </el-form-item>
        <el-form-item label="新版本号" prop="newVersion">
          <el-input 
            v-model="versionForm.newVersion" 
            placeholder="如：1.1 或 2.0"
            maxlength="10"
            style="width: 200px;">
            <template #prepend>v</template>
          </el-input>
          <div class="form-tip">版本号格式：主版本号.小版本号（如：1.0, 1.1, 2.0）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVersionVisible=false">取消</el-button>
        <el-button type="primary" :loading="createVersionLoading" @click="doCreateVersion">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowDown, UploadFilled, Search, Plus } from '@element-plus/icons-vue'
import { 
  listTemplateDesigns, 
  getTemplateDesignByTemplateId, 
  getTemplateVersions,
  publishVersion,
  updateTemplateStatus,
  uploadTemplateDocx,
  createNewVersion
} from '@/api/templateDesign'
import { EmptyState } from '@/components/common'
import { getSystemConfig } from '@/api/system'

const router = useRouter()
const route = useRoute()

// Embed 模式检测
const isEmbedMode = computed(() => route.query.embed === 'true')
// 自定义字段配置URL（从URL参数中读取）
const fieldsConfigUrl = computed(() => route.query.fieldsConfigUrl as string)
const loading = ref(false)
const keyword = ref('')
const records = ref<any[]>([])
const viewVisible = ref(false)
const elementRows = ref<any[]>([])
const viewing = ref(false)
const isDemoMode = ref(false)

const versionsVisible = ref(false)
const versionsList = ref<any[]>([])
const versionsLoading = ref(false)
const currentTemplateCode = ref('')

const createVersionVisible = ref(false)
const createVersionLoading = ref(false)
const versionFile = ref<File | null>(null)
const versionForm = reactive({ 
  templateCode: '', 
  newVersion: '',
  sourceId: '',
  currentVersion: ''
})
const versionFormRef = ref()
const currentVersionDisplay = computed(() => versionForm.currentVersion ? `v${versionForm.currentVersion}` : '')

// 版本号验证规则
const versionRules = {
  newVersion: [
    { required: true, message: '请输入新版本号', trigger: 'blur' },
    { 
      pattern: /^\d+\.\d{1,2}$/, 
      message: '版本号格式：主版本号.小版本号（小版本号最多2位，如：1.0, 1.10, 2.5）', 
      trigger: 'blur' 
    },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (!value) {
          callback()
          return
        }
        if (!versionForm.currentVersion) {
          callback()
          return
        }
        if (!compareVersion(value, versionForm.currentVersion)) {
          callback(new Error(`新版本号必须大于当前版本v${versionForm.currentVersion}`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 版本号比较函数：version1 > version2 返回true
function compareVersion(version1: string, version2: string): boolean {
  const v1Parts = version1.split('.').map(Number)
  const v2Parts = version2.split('.').map(Number)
  
  // 比较主版本号
  if (v1Parts[0] > v2Parts[0]) return true
  if (v1Parts[0] < v2Parts[0]) return false
  
  // 主版本号相同，比较小版本号
  const minor1 = v1Parts[1] || 0
  const minor2 = v2Parts[1] || 0
  return minor1 > minor2
}

// 计算下一个版本号（小版本号+1）
function getNextVersion(currentVersion: string): string {
  const parts = currentVersion.split('.')
  const major = parseInt(parts[0] || '1')
  const minor = parseInt(parts[1] || '0')
  return `${major}.${minor + 1}`
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return records.value
  return records.value.filter((r: any) => {
    const code = String(r.templateCode || '').toLowerCase()
    const name = String(r.templateName || '').toLowerCase()
    const desc = String(r.description || '').toLowerCase()
    return code.includes(kw) || name.includes(kw) || desc.includes(kw)
  })
})

function goNew() { 
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许新建模板')
    return
  }
  router.push('/templates/new') 
}

function goBack() {
  if (isEmbedMode.value) {
    // Embed 模式：发送关闭消息给父窗口
    console.log('🔙 [嵌入模式] 关闭模板管理 iframe')
    window.parent.postMessage({
      type: 'NAVIGATE_BACK',
      source: 'zhaoxin-sdk',
      payload: {
        from: '/templates',
        timestamp: Date.now()
      }
    }, '*')
  } else {
    // 独立模式：跳转到智能合同合成页面
    router.push('/smart-compose')
  }
}

function openDesigner(row: any) {
  const query: any = { 
    id: row.id || row.templateId, 
    fileId: row.fileId,
    returnUrl: '/templates'
  }
  
  // Embed 模式下，保持 embed 参数
  if (isEmbedMode.value) {
    query.embed = 'true'
    query.hideBack = 'true'
  }
  
  // 传递自定义字段配置URL（如果存在）
  if (fieldsConfigUrl.value) {
    query.fieldsConfigUrl = fieldsConfigUrl.value
    console.log('📋 传递自定义字段配置URL:', fieldsConfigUrl.value)
  }
  
  router.push({ 
    path: '/template-design', 
    query 
  }) 
}

function frontendCompose(row: any) { 
  router.push({ 
    path: '/contract-compose-frontend', 
    query: { 
      id: row.id,
      templateId: row.templateId,
      fileId: row.fileId 
    } 
  }) 
}

function backendCompose(row: any) { 
  router.push({ 
    path: '/contract-compose', 
    query: { 
      id: row.id,
      templateId: row.templateId,
      fileId: row.fileId 
    } 
  }) 
}

function viewElements(row: any) { 
  viewVisible.value = true
  loadElements(row.templateId || row.id) 
}

async function viewVersions(row: any) {
  const code = row.templateCode
  if (!code) {
    ElMessage.warning('该模板没有编码，无法查看版本')
    return
  }
  currentTemplateCode.value = code
  versionsVisible.value = true
  await loadVersions(code)
}

async function loadVersions(templateCode: string) {
  try {
    versionsLoading.value = true
    const res = await getTemplateVersions(templateCode) as any
    if (res?.data?.code !== 200) throw new Error(res?.data?.message || '查询失败')
    versionsList.value = res.data?.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载版本失败')
  } finally {
    versionsLoading.value = false
  }
}

function showCreateVersionDialog() {
  // 演示模式检查
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许创建新版本')
    return
  }
  
  // 获取最新版本作为源版本
  if (versionsList.value.length === 0) {
    ElMessage.warning('没有可用的源版本')
    return
  }
  
  const latestVersion = versionsList.value[0] // 列表已按版本号倒序排列
  versionForm.templateCode = currentTemplateCode.value
  versionForm.sourceId = latestVersion.id
  versionForm.currentVersion = latestVersion.version || '1.0'
  versionForm.newVersion = getNextVersion(versionForm.currentVersion)
  
  createVersionVisible.value = true
}

async function doCreateVersion() {
  // 演示模式检查
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许创建新版本')
    return
  }
  
  if (!versionFormRef.value) return
  
  try {
    // 验证表单
    await versionFormRef.value.validate()
  } catch {
    return
  }
  
  try {
    createVersionLoading.value = true
    
    // 创建新版本（基于源版本复制）
    const res = await createNewVersion(versionForm.sourceId, versionForm.newVersion) as any
    if (res?.data?.code !== 200) {
      throw new Error(res?.data?.message || '创建版本失败')
    }
    
    ElMessage.success('新版本创建成功，已复制文件和设计元素')
    createVersionVisible.value = false
    await loadVersions(versionForm.templateCode)
    await fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    createVersionLoading.value = false
  }
}

async function publishVersionAction(row: any) {
  // 演示模式检查
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许发布版本')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要发布版本 v${row.version} 吗？发布后，同一编码的其他已发布版本将被设为草稿状态。`, '确认发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await publishVersion(row.id) as any
    if (res?.data?.code !== 200) {
      throw new Error(res?.data?.message || '发布失败')
    }
    
    ElMessage.success('发布成功')
    // 如果当前在版本管理对话框中，则刷新版本列表
    if (currentTemplateCode.value && versionsVisible.value) {
      await loadVersions(currentTemplateCode.value)
    }
    await fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '发布失败')
    }
  }
}

async function handleStatusChange(row: any, command: string) {
  // 演示模式检查
  if (isDemoMode.value) {
    ElMessage.warning('演示环境不允许修改模板状态')
    return
  }
  
  try {
    let statusValue = ''
    let confirmMsg = ''
    
    switch (command) {
      case 'publish':
        await publishVersionAction(row)
        return
      case 'draft':
        statusValue = 'DRAFT'
        confirmMsg = '确定要将此模板设为草稿状态吗？'
        break
      case 'disable':
        statusValue = 'DISABLED'
        confirmMsg = '确定要禁用此模板吗？'
        break
      case 'delete':
        statusValue = 'DELETED'
        confirmMsg = '确定要删除此模板吗？（软删除）'
        break
      default:
        return
    }
    
    await ElMessageBox.confirm(confirmMsg, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await updateTemplateStatus(row.id, statusValue) as any
    if (res?.data?.code !== 200) {
      throw new Error(res?.data?.message || '操作失败')
    }
    
    ElMessage.success('操作成功')
    await fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listTemplateDesigns() as any
    if (res?.data?.code !== 200) throw new Error(res?.data?.message || '查询失败')
    records.value = res.data?.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally { loading.value = false }
}

onMounted(async () => {
  // 加载系统配置（演示模式）
  try {
    const configRes = await getSystemConfig() as any
    isDemoMode.value = configRes?.data?.data?.demoMode || false
  } catch (e) {
    console.error('获取系统配置失败', e)
  }
  
  await fetchList()
})

async function loadElements(templateId: string) {
  try {
    viewing.value = true
    elementRows.value = []
    const res = await getTemplateDesignByTemplateId(templateId) as any
    if (res?.data?.code !== 200) throw new Error(res?.data?.message || '查询失败')
    const record = res.data?.data || {}
    const parsed = (() => { try { const v = record.elementsJson || '{}'; return typeof v === 'string' ? JSON.parse(v) : v } catch { return {} } })()
    const arr = Array.isArray(parsed) ? parsed : (parsed.elements || [])
    elementRows.value = arr.map((it: any, idx: number) => ({ index: idx + 1, displayName: it?.customName || it?.name || it?.tag, tag: it?.tag, dataType: (it?.meta?.richText || it?.meta?.isRichText || it?.richText) ? 'html' : 'varchar(255)', example: `{"${it?.tag}": ""}` }))
  } catch (e: any) {
    ElMessage.error(e?.message || '加载元素失败')
  } finally { viewing.value = false }
}

function copyText(text: string) {
  try { navigator.clipboard?.writeText(text); ElMessage.success('已复制') } catch {}
}
</script>

<style scoped>
.templates-lib { padding: 20px; }

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
}

.versions-header { 
  display: flex; 
  justify-content: flex-end; 
  align-items: center; 
}
.file-tip { 
  margin-top: 8px; 
  color: #67c23a; 
  font-weight: 500; 
}
</style>


