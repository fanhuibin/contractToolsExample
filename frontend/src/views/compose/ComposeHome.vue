<template>
  <div class="compose-home">
    <PageHeader 
      title="智能合同合成"
      description="选择已发布的模板进行合同合成"
      :icon="Document"
    >
      <template #actions>
        <div class="header-tools">
          <el-input v-model="keyword" placeholder="搜索模板名称" clearable style="width: 220px;" @keyup.enter="fetchList" />
          <el-button @click="fetchList">搜索</el-button>
          <el-button type="primary" @click="goToTemplateManagement">模板管理</el-button>
        </div>
      </template>
    </PageHeader>

    <el-card>
      <el-table :data="filteredTemplates" v-loading="loading" style="width:100%" row-key="id">
        <el-table-column prop="templateCode" label="模板编码" width="140" />
        <el-table-column prop="templateName" label="模板名称" width="200" />
        <el-table-column prop="version" label="版本" width="80">
          <template #default="{ row }">
            v{{ row.version || '1.0' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" @click="backendCompose(row)">
              <el-icon><EditPen /></el-icon>
              开始合成
            </el-button>
            <el-button link @click="viewElements(row)">查看元素</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <EmptyState 
        v-if="!loading && filteredTemplates.length === 0"
        title="暂无可用模板"
        description="当前没有已发布的模板，请先在模板管理中发布模板"
      >
        <template #actions>
          <el-button type="primary" @click="goToTemplateManagement">前往模板管理</el-button>
        </template>
      </EmptyState>
    </el-card>

    <!-- 查看元素对话框 -->
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
import { Document, EditPen } from '@element-plus/icons-vue'
import { PageHeader, EmptyState } from '@/components/common'
import { listTemplateDesigns, getTemplateDesignDetail } from '@/api/templateDesign'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const templates = ref<any[]>([])
const viewVisible = ref(false)
const viewing = ref(false)
const elementRows = ref<any[]>([])

// 只显示已发布的模板
const filteredTemplates = computed(() => {
  let result = templates.value.filter(t => t.status === 'PUBLISHED')
  
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    result = result.filter(t => 
      (t.templateName && t.templateName.toLowerCase().includes(kw)) ||
      (t.templateCode && t.templateCode.toLowerCase().includes(kw))
    )
  }
  
  return result
})

async function fetchList() {
  loading.value = true
  try {
    const res: any = await listTemplateDesigns()
    if (res?.data?.code === 200) {
      templates.value = res.data.data || []
    } else {
      throw new Error(res?.data?.message || '加载失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载模板列表失败')
  } finally {
    loading.value = false
  }
}

function goToTemplateManagement() {
  router.push('/templates')
}

async function viewElements(row: any) {
  viewVisible.value = true
  viewing.value = true
  elementRows.value = []
  
  try {
    const res: any = await getTemplateDesignDetail(row.id)
    if (res?.data?.code !== 200) {
      throw new Error(res?.data?.message || '获取模板详情失败')
    }
    
    const detail = res.data.data
    let elements: any[] = []
    
    if (detail.elementsJson) {
      try {
        const parsed = JSON.parse(detail.elementsJson)
        elements = parsed.elements || []
      } catch {
        elements = []
      }
    }
    
    elementRows.value = elements.map((el: any, idx: number) => ({
      index: idx + 1,
      displayName: el.displayName || el.key || '-',
      tag: el.tag || '-',
      dataType: el.type || el.dataType || 'string',
      example: el.tag || el.key || ''
    }))
  } catch (e: any) {
    ElMessage.error(e?.message || '获取模板元素失败')
  } finally {
    viewing.value = false
  }
}

async function backendCompose(row: any) {
  router.push({ 
    path: '/contract-compose', 
    query: { 
      id: row.id,           // 设计记录ID
      templateId: row.templateId,  // 旧版模板ID（向后兼容）
      fileId: row.fileId     // 模板文件ID
    } 
  })
}

function copyText(text: string) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.compose-home {
  padding: 16px;
}

.header-tools {
  display: flex;
  gap: 8px;
  align-items: center;
}

code {
  padding: 2px 6px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>

