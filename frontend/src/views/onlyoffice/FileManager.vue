<template>
  <div class="file-manager">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span>文档文件管理</span>
          <el-space>
            <el-button @click="goBack">
              <el-icon><Back /></el-icon>
              返回编辑页面
            </el-button>
            <el-tooltip content="刷新列表">
              <el-button 
                circle 
                :icon="Refresh" 
                @click="refreshFileList"
              />
            </el-tooltip>
          </el-space>
        </div>
      </template>

      <!-- 文件上传区域 -->
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-area"
          drag
          :action="uploadAction"
          :headers="uploadHeaders"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :before-upload="beforeUpload"
          :show-file-list="false"
          accept=".docx"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              仅支持 Word (.docx) 格式文件，单个文件不超过 50MB
              <br>
              <span style="color: #f56c6c;">注意：不支持 .doc 和 .wps 格式，请先转换为 .docx 格式</span>
            </div>
          </template>
        </el-upload>
      </div>
    </el-card>

    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <span>文件列表 ({{ fileList.length }})</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名..."
            :prefix-icon="Search"
            style="width: 300px"
            clearable
          />
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="filteredFileList"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column type="index" label="序号" width="60" />
        
        <el-table-column prop="originalName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon :size="20" style="margin-right: 8px">
                <Document v-if="isWordFile(row.fileExtension)" />
                <DocumentCopy v-else />
              </el-icon>
              <span>{{ row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileExtension" label="文件类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getFileTypeTagType(row.fileExtension)" size="small">
              {{ getFileTypeLabel(row.fileExtension) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button 
                type="primary" 
                size="small" 
                :icon="Edit"
                @click.stop="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="success" 
                size="small" 
                :icon="View"
                @click.stop="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button 
                type="info" 
                size="small" 
                :icon="Download"
                @click.stop="handleDownload(row)"
              >
                下载
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                :icon="Delete"
                @click.stop="handleDelete(row)"
              >
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredFileList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无文件，请上传文件" />
      </div>
    </el-card>

    <!-- 编辑器对话框 -->
    <el-dialog
      v-model="editorDialogVisible"
      :title="editorTitle"
      width="90%"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      destroy-on-close
      fullscreen
    >
      <div class="editor-dialog-content">
        <div class="editor-controls">
          <el-space>
            <el-switch
              v-model="canEdit"
              active-text="可编辑"
              inactive-text="只读"
              @change="handlePermissionChange"
            />
            <el-switch
              v-model="canReview"
              active-text="可审阅"
              inactive-text="不可审阅"
              :disabled="!canEdit"
              @change="handlePermissionChange"
            />
          </el-space>
        </div>
        
        <OnlyOfficeEditor
          v-if="editorDialogVisible && currentEditFileId"
          ref="editorRef"
          :file-id="currentEditFileId"
          :can-edit="canEdit"
          :can-review="canReview"
          :height="'calc(100vh - 200px)'"
          :show-toolbar="true"
          :show-status="true"
          @ready="handleEditorReady"
          @error="handleEditorError"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  UploadFilled, 
  Search, 
  Edit, 
  View, 
  Download, 
  Delete,
  Document,
  DocumentCopy,
  Back
} from '@element-plus/icons-vue'
import { getFileList, deleteFile, type FileInfo } from '@/api/file'
import { downloadFile } from '@/api/onlyoffice'
import { getToken } from '@/utils/auth'
import { formatDateTime } from '@/utils/dateFormat'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const fileList = ref<FileInfo[]>([])
const searchKeyword = ref('')
const uploadRef = ref()
const editorDialogVisible = ref(false)
const currentEditFileId = ref<string | number>('')
const editorTitle = ref('')
const canEdit = ref(true)
const canReview = ref(false)
const editorRef = ref()

// 上传配置
const uploadAction = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL || ''}/api/onlyoffice/upload`
})

const uploadHeaders = computed(() => ({
  Authorization: getToken()
}))

// 计算属性
const filteredFileList = computed(() => {
  if (!searchKeyword.value) {
    return fileList.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return fileList.value.filter(file => 
    file.originalName?.toLowerCase().includes(keyword) ||
    file.fileExtension?.toLowerCase().includes(keyword)
  )
})

// 生命周期
onMounted(() => {
  loadFileList()
})

// 方法定义
const loadFileList = async () => {
  loading.value = true
  try {
    // 只获取 onlyoffice-demo 模块的文件
    const response = await getFileList({ module: 'onlyoffice-demo' })
    if (response.data.code === 200) {
      fileList.value = response.data.data || []
    } else {
      ElMessage.error('获取文件列表失败：' + response.data.message)
    }
  } catch (error: any) {
    ElMessage.error('获取文件列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const refreshFileList = () => {
  searchKeyword.value = ''
  loadFileList()
}

const goBack = () => {
  router.push('/onlyoffice')
}

const beforeUpload = (file: File) => {
  // 检查是否是不支持的格式
  const isDocOrWps = /\.(doc|wps)$/i.test(file.name)
  if (isDocOrWps) {
    ElMessage.warning({
      message: '不支持 .doc 和 .wps 格式，请先转换为 .docx 格式后再上传！',
      duration: 5000,
      showClose: true
    })
    return false
  }
  
  // 检查是否是docx格式
  const isDocx = /\.docx$/i.test(file.name)
  if (!isDocx) {
    ElMessage.error('只支持上传 Word (.docx) 格式的文件！')
    return false
  }
  
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB！')
    return false
  }
  
  loading.value = true
  return true
}

const handleUploadSuccess = (response: any) => {
  loading.value = false
  if (response.code === 200) {
    ElMessage.success('文件上传成功！')
    loadFileList()
  } else {
    ElMessage.error('文件上传失败：' + response.message)
  }
}

const handleUploadError = (error: any) => {
  loading.value = false
  ElMessage.error('文件上传失败：' + error.message)
}

const handleRowClick = (row: FileInfo) => {
  handleEdit(row)
}

const handleEdit = (row: FileInfo) => {
  currentEditFileId.value = row.id
  editorTitle.value = `编辑文档：${row.originalName}`
  canEdit.value = true
  canReview.value = false
  editorDialogVisible.value = true
}

const handlePreview = (row: FileInfo) => {
  currentEditFileId.value = row.id
  editorTitle.value = `预览文档：${row.originalName}`
  canEdit.value = false
  canReview.value = false
  editorDialogVisible.value = true
}

const handleDownload = async (row: FileInfo) => {
  try {
    const response = await downloadFile(row.id)
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.originalName
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('文件下载成功')
  } catch (error: any) {
    ElMessage.error('文件下载失败：' + error.message)
  }
}

const handleDelete = async (row: FileInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件"${row.originalName}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await deleteFile(row.id)
    ElMessage.success('文件删除成功')
    loadFileList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('文件删除失败：' + error.message)
    }
  } finally {
    loading.value = false
  }
}

const handlePermissionChange = () => {
  if (!canEdit.value) {
    canReview.value = false
  }
  // 关闭并重新打开编辑器以应用新权限
  const fileId = currentEditFileId.value
  const title = editorTitle.value
  editorDialogVisible.value = false
  setTimeout(() => {
    currentEditFileId.value = fileId
    editorTitle.value = title
    editorDialogVisible.value = true
  }, 100)
}

const handleEditorReady = () => {
  console.log('编辑器已就绪')
}

const handleEditorError = (error: any) => {
  ElMessage.error('编辑器错误：' + error.message)
}

// 工具方法
const isWordFile = (ext: string) => {
  return ext?.toLowerCase() === 'docx'
}

const getFileTypeTagType = (ext: string) => {
  const extension = ext?.toLowerCase()
  if (extension === 'docx') return 'primary'
  // 不支持的格式显示为灰色
  return 'info'
}

const getFileTypeLabel = (ext: string) => {
  const extension = ext?.toLowerCase()
  if (extension === 'docx') return 'DOCX'
  if (extension === 'doc') return 'DOC (不支持)'
  if (extension === 'wps') return 'WPS (不支持)'
  return ext?.toUpperCase() || '未知'
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}
</script>

<style scoped lang="scss">
.file-manager {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.upload-section {
  .upload-area {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 40px 20px;
  }

  .el-icon--upload {
    font-size: 67px;
    color: #409eff;
    margin-bottom: 16px;
  }

  .el-upload__text {
    font-size: 14px;
    color: #606266;
    
    em {
      color: #409eff;
      font-style: normal;
    }
  }

  .el-upload__tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }
}

.file-list-card {
  .file-name {
    display: flex;
    align-items: center;
    cursor: pointer;
    
    &:hover {
      color: #409eff;
    }
  }

  :deep(.el-table__row) {
    cursor: pointer;
    
    &:hover {
      background-color: #f5f7fa;
    }
  }

  .empty-state {
    padding: 40px 0;
    text-align: center;
  }
}

.editor-dialog-content {
  .editor-controls {
    margin-bottom: 16px;
    padding: 12px;
    background-color: #f5f7fa;
    border-radius: 4px;
  }
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>

