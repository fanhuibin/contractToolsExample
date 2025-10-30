<template>
  <div class="document-editor">
    <el-card class="editor-header">
      <template #header>
        <div class="card-header">
          <el-space>
            <el-button 
              :icon="ArrowLeft" 
              @click="goBack"
            >
              返回文件列表
            </el-button>
            <span class="file-title">{{ fileInfo.originalName || '文档编辑器' }}</span>
          </el-space>
          <el-space>
            <el-tooltip content="刷新编辑器">
              <el-button 
                circle 
                :icon="Refresh" 
                @click="refreshEditor"
              />
            </el-tooltip>
          </el-space>
        </div>
      </template>

      <!-- 编辑器控制区域 -->
      <div class="editor-controls">
        <el-row :gutter="16" align="middle">
          <el-col :span="6">
            <el-form-item label="编辑权限:">
              <el-switch
                v-model="canEdit"
                active-text="可编辑"
                inactive-text="只读"
                @change="handlePermissionChange"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="6">
            <el-form-item label="审阅权限:">
              <el-switch
                v-model="canReview"
                active-text="可审阅"
                inactive-text="不可审阅"
                :disabled="!canEdit"
                @change="handlePermissionChange"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="6">
            <el-form-item label="更新文档密钥:">
              <el-tooltip 
                content="文档编辑器有缓存机制，开启后会重新生成文档的唯一标识符，确保获取最新内容。"
                placement="top"
              >
                <el-switch
                  v-model="updateOnlyofficeKey"
                  active-text="更新"
                  inactive-text="不更新"
                  @change="handleKeyUpdateChange"
                />
              </el-tooltip>
            </el-form-item>
          </el-col>
          
          <el-col :span="6">
            <el-button 
              type="primary" 
              :loading="loading"
              @click="reloadEditor"
            >
              应用设置并重新加载
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 编辑器区域 -->
    <el-card v-if="fileId" class="editor-card">
      <OnlyOfficeEditor
        ref="editorRef"
        :file-id="fileId"
        :can-edit="canEdit"
        :can-review="canReview"
        :height="editorHeight"
        :show-toolbar="true"
        :show-status="true"
        :update-onlyoffice-key="updateOnlyofficeKey"
        @ready="handleEditorReady"
        @documentStateChange="handleDocumentStateChange"
        @error="handleEditorError"
        @save="handleEditorSave"
      />
    </el-card>

    <!-- 状态信息 -->
    <el-card v-if="fileId" class="status-card">
      <template #header>
        <span>状态信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件ID">{{ fileId }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ fileInfo.originalName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">
          <el-tag :type="getFileTypeTagType(fileInfo.fileExtension)">
            {{ fileInfo.fileExtension?.toUpperCase() }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(fileInfo.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="编辑模式">
          <el-tag :type="canEdit ? 'success' : 'info'">
            {{ canEdit ? '可编辑' : '只读' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审阅权限">
          <el-tag :type="canReview ? 'warning' : 'info'">
            {{ canReview ? '可审阅' : '不可审阅' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编辑器状态">
          <el-tag :type="editorReady ? 'success' : 'danger'">
            {{ editorReady ? '已就绪' : '未就绪' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文档状态">
          <el-tag v-if="documentState !== null" :type="getDocumentStateType(documentState)">
            {{ getDocumentStateText(documentState) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import { getFileInfo, type FileInfo } from '@/api/file'

// 路由
const route = useRoute()
const router = useRouter()

// 响应式数据
const fileId = ref<string | number>('')
const canEdit = ref(true)
const canReview = ref(false)
const updateOnlyofficeKey = ref(false)
const loading = ref(false)
const editorReady = ref(false)
const documentState = ref<number | null>(null)
const editorRef = ref()
const fileInfo = ref<Partial<FileInfo>>({})

// 计算属性
const editorHeight = computed(() => 'calc(100vh - 450px)')

// 生命周期
onMounted(async () => {
  fileId.value = route.params.fileId as string || route.query.fileId as string
  
  if (!fileId.value) {
    ElMessage.error('缺少文件ID参数')
    goBack()
    return
  }
  
  // 加载文件信息
  await loadFileInfo()
})

// 方法定义
const loadFileInfo = async () => {
  try {
    const response = await getFileInfo(fileId.value)
    if (response.data.code === 200) {
      fileInfo.value = response.data.data
    } else {
      ElMessage.error('获取文件信息失败：' + response.data.message)
    }
  } catch (error: any) {
    ElMessage.error('获取文件信息失败：' + error.message)
  }
}

const goBack = () => {
  router.push({ name: 'FileManager' })
}

const handlePermissionChange = () => {
  if (!canEdit.value) {
    canReview.value = false
  }
}

const handleKeyUpdateChange = () => {
  ElMessage.info(`文档密钥更新已${updateOnlyofficeKey.value ? '开启' : '关闭'}`)
}

const reloadEditor = async () => {
  loading.value = true
  try {
    if (editorRef.value) {
      editorRef.value.destroyEditor?.()
    }
    
    editorReady.value = false
    await nextTick()
    
    if (editorRef.value) {
      await editorRef.value.initEditor()
    }
    
    ElMessage.success('编辑器已重新加载')
  } catch (error: any) {
    ElMessage.error('重新加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const refreshEditor = async () => {
  try {
    await ElMessageBox.confirm('刷新会丢失未保存的更改，确定要继续吗？', '确认刷新', {
      type: 'warning'
    })
    
    await reloadEditor()
  } catch (error) {
    // 用户取消
  }
}

const handleEditorReady = () => {
  editorReady.value = true
  loading.value = false
  ElMessage.success('文档编辑器加载完成')
}

const handleDocumentStateChange = (event: any) => {
  documentState.value = event.data
}

const handleEditorError = (error: any) => {
  loading.value = false
  ElMessage.error('编辑器错误：' + (error.message || error.data))
}

const handleEditorSave = () => {
  ElMessage.success('文档已保存')
}

// 工具方法
const getFileTypeTagType = (ext?: string) => {
  const extension = ext?.toLowerCase()
  if (['doc', 'docx'].includes(extension || '')) return 'primary'
  if (['xls', 'xlsx'].includes(extension || '')) return 'success'
  if (['ppt', 'pptx'].includes(extension || '')) return 'warning'
  if (extension === 'pdf') return 'danger'
  return 'info'
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const getDocumentStateType = (state: number) => {
  switch (state) {
    case 0: return 'info'
    case 1: return 'warning'
    case 2: return 'success'
    case 3: return 'danger'
    default: return 'info'
  }
}

const getDocumentStateText = (state: number) => {
  switch (state) {
    case 0: return '无更改'
    case 1: return '编辑中'
    case 2: return '已保存'
    case 3: return '保存错误'
    default: return '未知状态'
  }
}
</script>

<style scoped lang="scss">
.document-editor {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.editor-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-left: 12px;
}

.editor-controls {
  margin-top: 16px;
}

.editor-card {
  margin-bottom: 20px;
  min-height: 600px;
}

.status-card {
  margin-bottom: 20px;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>

