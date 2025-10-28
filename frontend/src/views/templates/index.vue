<template>
  <div class="templates-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模板管理</span>
        </div>
      </template>
      <div class="content">
        <div class="upload-row">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="模板ID">
              <el-input v-model="form.templateId" placeholder="请输入模板ID，如 demo" />
            </el-form-item>
            <el-form-item label="上传docx">
              <el-upload :auto-upload="false" :show-file-list="false" accept=".docx" :on-change="onFileChange">
                <el-button>选择文件</el-button>
              </el-upload>
              <span class="file-name" v-if="file">{{ file.name }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!canUpload" @click="doUpload">开始模板设计</el-button>
              <span class="tip">仅支持docx；如为doc请先转换为docx</span>
            </el-form-item>
          </el-form>
        </div>

        <el-divider />

        <div class="list-row">
          <el-table :data="records" style="width: 100%" v-loading="loading">
            <el-table-column prop="templateId" label="模板ID" width="160" />
            <el-table-column prop="fileId" label="文件ID" width="160" />
            <el-table-column prop="updatedAt" label="更新时间" width="200" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" @click="openDesigner(row)">设计模板</el-button>
                <el-button size="small" @click="viewElements(row)">查看元素</el-button>
                <!-- 前端合成功能已禁用 -->
                <!-- <el-button size="small" @click="frontendCompose(row)">前端合成</el-button> -->
                <el-button size="small" @click="backendCompose(row)">后端合成</el-button>
                <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
  
  <!-- 查看元素弹窗（无分页） -->
  <el-dialog v-model="viewVisible" title="查看元素" width="860px" append-to-body class="view-elements-dialog" :lock-scroll="false">
    <div style="margin-bottom:8px; display:flex; justify-content: space-between; align-items:center;">
      <div>
        <span>模板ID：</span><b>{{ currentView?.templateId }}</b>
        <span style="margin-left:16px;">文件ID：</span><b>{{ currentView?.fileId }}</b>
      </div>
      <div>
        <el-button size="small" @click="copyText(allExampleJson)">复制全部示例JSON</el-button>
      </div>
    </div>
    <el-table :data="elementRows" v-loading="viewing" border style="width:100%">
      <el-table-column prop="index" label="序号" width="70" />
      <el-table-column prop="displayName" label="要素名称" width="120" />
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
      <el-button @click="viewVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadTemplateDocx, listTemplateDesigns, deleteTemplateDesign, getTemplateDesignByTemplateId } from '@/api/templateDesign'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const form = reactive({ templateId: '' })
const file = ref<File | null>(null)
const records = ref<Array<any>>([])
const viewVisible = ref(false)
const elementRows = ref<Array<any>>([])
const currentView = ref<{ templateId?: string; fileId?: string } | null>(null)
const viewing = ref(false)

const canUpload = computed(() => !!form.templateId && !!file.value)

function onFileChange(f: any) {
  const raw = f?.raw as File
  if (!raw) return
  if (!raw.name.toLowerCase().endsWith('.docx')) {
    ElMessage.warning('仅支持docx；如为doc请先转换为docx')
    return
  }
  file.value = raw
}

async function doUpload() {
  if (!canUpload.value) return
  try {
    loading.value = true
    const res = await uploadTemplateDocx({ templateId: form.templateId, file: file.value as File }) as any
    if (res?.code !== 200) throw new Error(res?.message || '上传失败')
    ElMessage.success('上传成功')
    await fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    loading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listTemplateDesigns() as any
    if (res?.code !== 200) throw new Error(res?.message || '查询失败')
    records.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function openDesigner(row: any) {
  router.push({ path: '/template-design', query: { id: row.templateId, fileId: row.fileId, returnUrl: '/templates-old' } })
}

function viewElements(row: any) {
  // 打开无分页查看元素弹窗
  currentView.value = { templateId: row.templateId, fileId: row.fileId }
  viewVisible.value = true
  loadElements(row.templateId)
}

function frontendCompose(row: any) {
  router.push({ path: '/contract-compose-frontend', query: { id: row.id, templateId: row.templateId, fileId: row.fileId } })
}

function backendCompose(row: any) {
  router.push({ path: '/contract-compose', query: { id: row.id, templateId: row.templateId, fileId: row.fileId } })
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除模板 ${row.templateId} ?`, '提示', { type: 'warning' })
  } catch { return }
  try {
    const res = await deleteTemplateDesign(row.id) as any
    if (res?.code !== 200) throw new Error(res?.message || '删除失败')
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(fetchList)

async function loadElements(templateId: string) {
  try {
    viewing.value = true
    elementRows.value = []
    const res = await getTemplateDesignByTemplateId(templateId) as any
    if (res?.code !== 200) throw new Error(res?.message || '查询失败')
    const record = res.data || {}
    const parsed = (() => {
      try {
        const val = record.elementsJson || '{}'
        return typeof val === 'string' ? JSON.parse(val) : val
      } catch { return {} }
    })()
    const arr = Array.isArray(parsed) ? parsed : (parsed.elements || [])
    elementRows.value = arr.map((it: any, idx: number) => ({
      index: idx + 1,
      displayName: it?.customName || it?.name || it?.tag,
      tag: it?.tag,
      dataType: (it?.meta?.richText || it?.meta?.isRichText || it?.richText) ? 'html' : 'varchar(255)',
      example: `{"${it?.tag}": ""}`
    }))
  } catch (e: any) {
    ElMessage.error(e?.message || '加载元素失败')
  } finally {
    viewing.value = false
  }
}

function copyText(text: string) {
  if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制'))
    return
  }
  const ta = document.createElement('textarea')
  ta.value = text
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy'); ElMessage.success('已复制') } catch {}
  document.body.removeChild(ta)
}

const allExampleJson = computed(() => {
  const obj: Record<string, string> = {}
  elementRows.value.forEach(r => { if (r.tag) obj[r.tag] = '' })
  return JSON.stringify(obj)
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.content { min-height: 400px; }
.upload-row { display: flex; align-items: center; gap: 8px; }
.file-name { margin-left: 8px; color: #666; }
/* 查看元素弹窗固定高度与仅body滚动 */
:deep(.view-elements-dialog .el-dialog__body) {
  max-height: calc(80vh - 120px); /* 扣除标题与底部 */
  overflow: auto;
}
/* 处理内部表格宽度导致溢出时的横向滚动 */
:deep(.view-elements-dialog .el-dialog__body) .el-table {
  overflow: auto;
}
</style> 