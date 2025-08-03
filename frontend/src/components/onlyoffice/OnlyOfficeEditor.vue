<template>
  <div class="onlyoffice-editor">
    <!-- 加载状态 -->
    <el-loading 
      v-loading="loading" 
      :text="loadingText"
      element-loading-background="rgba(255, 255, 255, 0.9)"
      class="loading-container"
    >
      <!-- 编辑器容器 -->
      <div
        id="onlyoffice-editor-container"
        :style="containerStyle"
        class="editor-container"
      />
    </el-loading>

    <!-- 工具栏 -->
    <div v-if="showToolbar" class="toolbar">
      <el-space>
        <el-button 
          v-if="canEdit" 
          type="primary" 
          icon="DocumentAdd"
          @click="forceSave"
        >
          强制保存
        </el-button>
        <el-button 
          v-if="!isEditMode" 
          icon="View"
          @click="addWatermark"
        >
          添加水印
        </el-button>
        <el-button 
          icon="Refresh"
          @click="refreshEditor"
        >
          刷新
        </el-button>
      </el-space>
    </div>

    <!-- 状态信息 -->
    <div v-if="showStatus" class="status-bar">
      <el-tag v-if="isEditMode" type="success" size="small">编辑模式</el-tag>
      <el-tag v-else type="info" size="small">预览模式</el-tag>
      <el-tag v-if="canReview" type="warning" size="small">可审阅</el-tag>
      <span class="file-info">{{ fileInfo.originalName }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { ElLoading, ElButton, ElSpace, ElTag, ElMessage, ElMessageBox } from 'element-plus'
import { DocumentAdd, View, Refresh } from '@element-plus/icons-vue'
import { getEditorConfig, getServerInfo } from '@/api/onlyoffice'

// Props定义
const props = defineProps({
  fileId: {
    type: [String, Number],
    required: true
  },
  canEdit: {
    type: Boolean,
    default: false
  },
  canReview: {
    type: Boolean,
    default: false
  },
  height: {
    type: String,
    default: 'calc(100vh - 200px)'
  },
  width: {
    type: String,
    default: '100%'
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  showStatus: {
    type: Boolean,
    default: true
  },
  watermarkText: {
    type: String,
    default: '机密文档'
  }
})

// Emits定义
const emit = defineEmits([
  'ready',
  'documentStateChange', 
  'error',
  'save',
  'warning'
])

// 响应式数据
const loading = ref(true)
const loadingText = ref('正在加载编辑器...')
const onlyofficeLoaded = ref(false)
const editorReady = ref(false)
const docEditor = ref(null)
const serverInfo = ref(null)

const fileInfo = reactive({
  originalName: '',
  fileType: '',
  size: 0
})

// 计算属性
const isEditMode = computed(() => props.canEdit)
const containerStyle = computed(() => ({
  height: props.height,
  width: props.width
}))

// 生命周期
onMounted(() => {
  initEditor()
  window.addEventListener('message', handleMessage)
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  if (docEditor.value) {
    docEditor.value.destroyEditor()
  }
})

// 方法定义
const initEditor = async () => {
  try {
    loadingText.value = '获取服务器信息...'
    // 获取OnlyOffice服务器信息
    const serverResponse = await getServerInfo()
    serverInfo.value = serverResponse.data
    
    loadingText.value = '获取文档配置...'
    // 获取文档编辑器配置
    const configResponse = await getEditorConfig({
      fileId: props.fileId,
      canEdit: props.canEdit,
      canReview: props.canReview
    })
    
    const editorConfig = configResponse.data
    
    // 保存文件信息
    Object.assign(fileInfo, {
      originalName: editorConfig.document.title,
      fileType: editorConfig.document.fileType,
      size: editorConfig.document.info?.size || 0
    })
    
    loadingText.value = '加载OnlyOffice脚本...'
    // 加载OnlyOffice API脚本
    await loadOnlyOfficeScript()
    
    loadingText.value = '初始化编辑器...'
    // 初始化编辑器
    await initOnlyOfficeEditor(editorConfig)
    
  } catch (error) {
    console.error('初始化编辑器失败:', error)
    ElMessage.error('加载文档编辑器失败: ' + error.message)
    emit('error', error)
    loading.value = false
  }
}

const loadOnlyOfficeScript = () => {
  return new Promise((resolve, reject) => {
    // 检查是否已经加载
    if (window.DocsAPI) {
      resolve()
      return
    }
    
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `${serverInfo.value.fullUrl}/web-apps/apps/api/documents/api.js`
    
    script.onload = () => {
      onlyofficeLoaded.value = true
      resolve()
    }
    
    script.onerror = () => {
      reject(new Error('无法加载OnlyOffice API脚本'))
    }
    
    document.head.appendChild(script)
  })
}

const initOnlyOfficeEditor = (config) => {
  return new Promise((resolve, reject) => {
    try {
      // 清空容器
      const container = document.getElementById('onlyoffice-editor-container')
      container.innerHTML = '<div id="onlyoffice-editor" style="height: 100%; width: 100%;"></div>'
      
      // 添加事件处理
      const editorConfig = {
        ...config,
        events: {
          onAppReady: () => {
            console.log('OnlyOffice应用已准备就绪')
            loading.value = false
            editorReady.value = true
            emit('ready')
            resolve()
          },
          onDocumentStateChange: (event) => {
            console.log('文档状态改变:', event)
            emit('documentStateChange', event)
          },
          onError: (event) => {
            console.error('OnlyOffice错误:', event)
            ElMessage.error('文档编辑器错误: ' + event.data)
            emit('error', event)
            reject(new Error(event.data))
          },
          onWarning: (event) => {
            console.warn('OnlyOffice警告:', event)
            ElMessage.warning('编辑器警告: ' + event.data)
            emit('warning', event)
          },
          onRequestSaveAs: (event) => {
            console.log('保存文档:', event)
            emit('save', event)
          }
        }
      }
      
      // 创建编辑器实例
      docEditor.value = new window.DocsAPI.DocEditor('onlyoffice-editor', editorConfig)
      
    } catch (error) {
      reject(error)
    }
  })
}

const handleMessage = (event) => {
  // 处理来自编辑器的消息
  try {
    const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
    
    switch (data.action) {
      case 'loaded':
        console.log('编辑器插件已加载')
        break
      case 'ready':
        console.log('编辑器已就绪')
        break
      default:
        console.log('收到编辑器消息:', data)
    }
  } catch (error) {
    console.warn('处理编辑器消息失败:', error)
  }
}

// 工具方法
const forceSave = () => {
  if (!docEditor.value) {
    ElMessage.warning('编辑器未准备就绪')
    return
  }
  
  try {
    docEditor.value.processSaveResult(true)
    ElMessage.success('保存命令已发送')
  } catch (error) {
    console.error('强制保存失败:', error)
    ElMessage.error('保存失败: ' + error.message)
  }
}

const addWatermark = () => {
  if (!docEditor.value) {
    ElMessage.warning('编辑器未准备就绪')
    return
  }
  
  try {
    const watermarkData = {
      action: 'addWatermark',
      text: props.watermarkText
    }
    
    // 向编辑器发送水印命令
    window.frames['onlyoffice-editor']?.postMessage(JSON.stringify(watermarkData), '*')
    ElMessage.success('水印已添加')
  } catch (error) {
    console.error('添加水印失败:', error)
    ElMessage.error('添加水印失败: ' + error.message)
  }
}

const refreshEditor = async () => {
  try {
    await ElMessageBox.confirm('刷新会丢失未保存的更改，确定要继续吗？', '确认刷新', {
      type: 'warning'
    })
    
    loading.value = true
    loadingText.value = '正在刷新...'
    
    // 重新初始化编辑器
    await initEditor()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('刷新编辑器失败:', error)
      ElMessage.error('刷新失败: ' + error.message)
    }
  }
}

// 暴露方法给父组件
defineExpose({
  forceSave,
  addWatermark,
  refreshEditor,
  getFileInfo: () => fileInfo,
  isReady: () => editorReady.value
})
</script>

<style scoped>
.onlyoffice-editor {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.loading-container {
  flex: 1;
  min-height: 400px;
}

.editor-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.toolbar {
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fafafa;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #606266;
}

.file-info {
  margin-left: auto;
}

.el-loading-mask {
  border-radius: 4px;
}
</style>