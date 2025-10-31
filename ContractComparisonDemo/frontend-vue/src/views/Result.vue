<template>
  <iframe :src="iframeUrl" class="result-iframe" />
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ZHAOXIN_CONFIG } from '@/config'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.params.taskId)

// iframe URL - 添加 ?embed=true 隐藏肇新前端的侧边栏和头部
// 注意：肇新前端使用 HTML5 History 模式，不需要 # 符号
const iframeUrl = computed(() => 
  `${ZHAOXIN_CONFIG.frontendUrl}/gpu-ocr-compare/canvas-result/${taskId.value}?embed=true`
)

// 调试日志
console.log('📊 Result 页面，taskId:', taskId.value)
console.log('🌐 iframe URL:', iframeUrl.value)

// postMessage 消息处理
const handleMessage = (event) => {
  // 验证来源
  if (event.origin !== ZHAOXIN_CONFIG.frontendUrl) {
    console.warn('⚠️ 忽略来自未知来源的消息:', event.origin)
    return
  }
  
  // 处理导航消息
  if (event.data?.type === 'NAVIGATE_BACK' && 
      event.data?.source === 'zhaoxin-sdk') {
    console.log('✅ 收到返回消息，导航到首页')
    router.push('/')
  }
}

// 添加和移除事件监听器
onMounted(() => {
  window.addEventListener('message', handleMessage)
  console.log('📡 已添加 postMessage 监听器')
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  console.log('🔌 已移除 postMessage 监听器')
})
</script>

<style scoped>
.result-iframe {
  width: 100%;
  height: 100vh;
  border: none;
  display: block;
}
</style>
