<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :fullscreen="fullscreen"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    destroy-on-close
    align-center
    @close="handleClose"
    class="iframe-dialog"
  >
    <iframe 
      v-if="visible"
      :src="iframeUrl" 
      class="iframe-content"
      @load="onIframeLoad"
    />
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { ZHAOXIN_CONFIG } from '@/config'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  url: {
    type: String,
    required: true
  },
  title: {
    type: String,
    default: '详情'
  },
  width: {
    type: String,
    default: '90%'
  },
  fullscreen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'close'])

// 双向绑定的可见性状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 构建iframe URL，自动添加embed和hideBack参数
 */
const iframeUrl = computed(() => {
  if (!props.url) return ''
  
  try {
    // 构建完整URL
    const url = new URL(props.url, ZHAOXIN_CONFIG.frontendUrl)
    
    // 自动添加嵌入模式参数
    url.searchParams.set('embed', 'true')
    url.searchParams.set('hideBack', 'true')
    
    console.log('🌐 构建iframe URL:', url.toString())
    return url.toString()
  } catch (error) {
    console.error('❌ 构建iframe URL失败:', error)
    return props.url
  }
})

/**
 * 处理来自iframe的postMessage消息
 */
const handleMessage = (event) => {
  // 验证消息来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
    return
  }
  
  // 处理导航返回消息
  if (event.data?.type === 'NAVIGATE_BACK' && 
      event.data?.source === 'zhaoxin-sdk') {
    console.log('✅ 收到返回消息，关闭弹窗', event.data.payload)
    handleClose()
  }
}

/**
 * 处理弹窗关闭
 */
const handleClose = () => {
  visible.value = false
  emit('close')
}

/**
 * iframe加载完成事件
 */
const onIframeLoad = () => {
  console.log('📡 iframe 加载完成')
}

// 生命周期：添加事件监听器
onMounted(() => {
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

// 生命周期：移除事件监听器
onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>

<style scoped>
/* 弹窗容器样式 */
.iframe-dialog {
  :deep(.el-dialog) {
    margin-top: 5vh !important;
    margin-bottom: 5vh !important;
    max-height: 90vh;
    display: flex;
    flex-direction: column;
  }
  
  :deep(.el-dialog__header) {
    flex-shrink: 0;
  }
  
  :deep(.el-dialog__body) {
    padding: 0;
    flex: 1;
    overflow: hidden;
    min-height: 0;
  }
}

/* iframe内容样式 */
.iframe-content {
  width: 100%;
  height: 70vh;
  max-height: calc(90vh - 80px); /* 减去header和padding */
  border: none;
  display: block;
}

/* 全屏模式下的样式 */
.iframe-dialog :deep(.el-dialog.is-fullscreen) {
  margin: 0 !important;
  max-height: 100vh;
}

.iframe-dialog :deep(.el-dialog.is-fullscreen .el-dialog__body) {
  height: calc(100vh - 60px); /* 减去header高度 */
}

.iframe-dialog :deep(.el-dialog.is-fullscreen) .iframe-content {
  height: calc(100vh - 60px);
  max-height: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .iframe-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin-top: 2vh !important;
    margin-bottom: 2vh !important;
    max-height: 96vh;
  }
  
  .iframe-content {
    height: 75vh;
    max-height: calc(96vh - 80px);
  }
}
</style>

