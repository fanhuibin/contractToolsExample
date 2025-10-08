<template>
  <div class="templates-lib">
    <PageHeader 
      title="模板库"
      description="管理合同模板，设计模板元素并用于合同合成"
      :icon="FolderOpened"
    >
      <template #actions>
        <div class="header-tools">
          <el-input v-model="keyword" placeholder="搜索名称/分类" clearable style="width: 220px;" @keyup.enter="fetchList" />
          <el-button @click="fetchList">搜索</el-button>
          <el-button type="primary" @click="goNew">新建模板</el-button>
        </div>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="filtered" v-loading="loading" style="width:100%">
        <el-table-column prop="templateId" label="模板ID" width="180" />
        <el-table-column prop="fileId" label="文件ID" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="200" />
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDesigner(row)">设计模板</el-button>
            <el-button link @click="viewElements(row)">查看元素</el-button>
            <el-button type="success" size="small" @click="frontendCompose(row)">前端合成</el-button>
            <el-button type="primary" size="small" @click="backendCompose(row)">后端合成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState 
        v-if="!loading && filtered.length === 0"
        title="暂无模板"
        description="还没有创建任何模板，点击上方"新建模板"开始创建"
      />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened } from '@element-plus/icons-vue'
import { listTemplateDesigns, getTemplateDesignByTemplateId } from '@/api/templateDesign'
import { PageHeader, EmptyState } from '@/components/common'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const records = ref<any[]>([])
const viewVisible = ref(false)
const elementRows = ref<any[]>([])
const viewing = ref(false)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return records.value
  return records.value.filter((r: any) => String(r.templateId || '').toLowerCase().includes(kw))
})

function goNew() { router.push('/templates/new') }
function openDesigner(row: any) { router.push({ path: '/template-design', query: { id: row.templateId, fileId: row.fileId } }) }
function frontendCompose(row: any) { router.push({ path: '/contract-compose-frontend', query: { templateId: row.templateId, fileId: row.fileId } }) }
function backendCompose(row: any) { router.push({ path: '/contract-compose', query: { templateId: row.templateId, fileId: row.fileId } }) }
function viewElements(row: any) { viewVisible.value = true; loadElements(row.templateId) }

async function fetchList() {
  loading.value = true
  try {
    const res = await listTemplateDesigns() as any
    if (res?.code !== 200) throw new Error(res?.message || '查询失败')
    records.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally { loading.value = false }
}

onMounted(fetchList)

async function loadElements(templateId: string) {
  try {
    viewing.value = true
    elementRows.value = []
    const res = await getTemplateDesignByTemplateId(templateId) as any
    if (res?.code !== 200) throw new Error(res?.message || '查询失败')
    const record = res.data || {}
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
.templates-lib { padding: 16px; }
.header { display:flex; justify-content: space-between; align-items:center; }
.title { font-weight: 600; }
</style>


