<template>
  <iframe :src="iframeUrl" class="result-iframe" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ZHAOXIN_CONFIG } from '@/config'

const route = useRoute()
const taskId = computed(() => route.params.taskId)

// iframe URL - 添加 ?embed=true 隐藏肇新前端的侧边栏和头部
// 注意：肇新前端使用 HTML5 History 模式，不需要 # 符号
const iframeUrl = computed(() => 
  `${ZHAOXIN_CONFIG.frontendUrl}/gpu-ocr-compare/canvas-result/${taskId.value}?embed=true`
)

// 调试日志
console.log('📊 Result 页面，taskId:', taskId.value)
console.log('🌐 iframe URL:', iframeUrl.value)
</script>

<style scoped>
.result-iframe {
  width: 100%;
  height: 100vh;
  border: none;
  display: block;
}
</style>
