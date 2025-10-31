<template>
  <div class="template-list-page">
    <el-card>
      <template #header>
        <div class="header">
          <div class="header-left">
            <el-button 
              text 
              @click="$router.push('/rule-extract')"
              style="margin-right: 12px;"
            >
              <el-icon><Back /></el-icon>
              返回
            </el-button>
            <h3>规则模板管理</h3>
          </div>
          <div style="display: flex; gap: 10px;">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新建模板
            </el-button>
            <el-button type="success" @click="goAIGenerator">
              <el-icon><MagicStick /></el-icon>
              AI生成模板
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="searchStatus" placeholder="请选择" clearable style="width: 120px;">
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 模板列表 -->
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="templateCode" label="模板编号" width="150" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="goDesign(row)">设计</el-button>
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" @click="handleCopy(row)">复制</el-button>
            <el-dropdown @command="(cmd: string) => handleCommand(cmd, row)" style="margin-left: 8px;">
              <el-button size="small">
                更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="enable" v-if="row.status !== 'active'">启用</el-dropdown-item>
                  <el-dropdown-item command="disable" v-if="row.status === 'active'">禁用</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="模板编号" prop="templateCode">
          <el-input 
            v-model="form.templateCode" 
            placeholder="请输入模板编号（唯一标识）" 
            maxlength="30" 
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown, Back, MagicStick } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  enableTemplate,
  disableTemplate,
  copyTemplate as apiCopyTemplate
} from '@/api/rule-extract'
import { extractArrayData } from '@/utils/response-helper'

const router = useRouter()

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const submitting = ref(false)
const formRef = ref()
const searchStatus = ref('')

const form = ref({
  id: null as string | null,
  templateName: '',
  templateCode: '',
  description: '',
  status: 'draft'
})

const rules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  templateCode: [
    { required: true, message: '请输入模板编号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '只能包含字母、数字、下划线和横线', trigger: 'blur' },
    { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
  ]
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchStatus.value) {
      params.status = searchStatus.value
    }
    const res: any = await listTemplates(params)
    list.value = extractArrayData(res)
  } catch (error) {
    console.error('加载模板列表失败:', error)
    ElMessage.error('加载模板列表失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchStatus.value = ''
  loadList()
}

const showCreateDialog = () => {
  dialogTitle.value = '新建模板'
  resetForm()
  dialogVisible.value = true
}

const showEditDialog = (row: any) => {
  dialogTitle.value = '编辑模板'
  form.value = {
    id: row.id,
    templateName: row.templateName,
    templateCode: row.templateCode || '',
    description: row.description || '',
    status: row.status
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.value = {
    id: null,
    templateName: '',
    templateCode: '',
    description: '',
    status: 'draft'
  }
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (form.value.id) {
      await updateTemplate(form.value.id as any, form.value)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(form.value)
      ElMessage.success('创建成功')
      searchStatus.value = ''
    }

    dialogVisible.value = false
    loadList()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const goDesign = (row: any) => {
  router.push(`/rule-extract/template/${row.id}`)
}

const goAIGenerator = () => {
  router.push('/rule-extract/ai-generator')
}

const handleCopy = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新模板名称', '复制模板', {
      inputValue: `${row.templateName} - 副本`
    })
    
    if (value) {
      await apiCopyTemplate(row.id, value)
      ElMessage.success('复制成功')
      loadList()
    }
  } catch (error) {
    // 用户取消
  }
}

const handleCommand = async (command: string, row: any) => {
  try {
    switch (command) {
      case 'enable':
        await enableTemplate(row.id)
        ElMessage.success('启用成功')
        loadList()
        break
      case 'disable':
        await disableTemplate(row.id)
        ElMessage.success('禁用成功')
        loadList()
        break
      case 'delete':
        await ElMessageBox.confirm('确定要删除该模板吗？', '提示', {
          type: 'warning'
        })
        await deleteTemplate(row.id)
        ElMessage.success('删除成功')
        loadList()
        break
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'active': '启用',
    'inactive': '禁用',
    'draft': '草稿'
  }
  return map[status] || status
}

const getStatusType = (status: string): any => {
  const map: Record<string, string> = {
    'active': 'success',
    'inactive': 'danger',
    'draft': 'info'
  }
  return map[status] || ''
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    if (isNaN(date.getTime())) return dateStr
    
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch (error) {
    return dateStr
  }
}

onMounted(() => {
  loadList()
})

onActivated(() => {
  loadList()
})
</script>

<style scoped lang="scss">
.template-list-page {
  padding: 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header h3 {
  margin: 0;
}

.search-form {
  margin-bottom: 16px;
}
</style>

