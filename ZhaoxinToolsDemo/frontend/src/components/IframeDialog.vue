<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :fullscreen="fullscreen"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    destroy-on-close
    @close="handleClose"
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
.iframe-content {
  width: 100%;
  height: calc(80vh);
  border: none;
  display: block;
}

/* 深度选择器修改Dialog body样式 */
:deep(.el-dialog__body) {
  padding: 0;
  height: calc(80vh);
  overflow: hidden;
}

/* 全屏模式下的样式 */
:deep(.el-dialog.is-fullscreen .el-dialog__body) {
  height: calc(100vh - 60px); /* 减去header高度 */
}

:deep(.el-dialog.is-fullscreen) .iframe-content {
  height: calc(100vh - 60px);
}
</style>

