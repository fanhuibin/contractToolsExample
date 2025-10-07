<template>
  <div class="concentric-loader" ref="wrap">
    <canvas ref="canvas" :width="size" :height="size"></canvas>
    <div class="loading-text">{{ props.text || '文件解析中...' }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  size?: number
  speed?: number
  color?: string
  text?: string
}>()

const size = props.size ?? 180
const speed = props.speed ?? 1
const colorHex = props.color ?? '#1677ff'

const canvas = ref<HTMLCanvasElement | null>(null)
const wrap = ref<HTMLDivElement | null>(null)
let rafId = 0
let start = 0

function drawFrame(ts: number) {
  if (!canvas.value) return
  const ctx = canvas.value.getContext('2d')!
  if (!start) start = ts
  const t = (ts - start) / 1000

  const w = canvas.value.width
  const h = canvas.value.height
  const centerX = w / 2
  const centerY = h / 2
  // 基于180的基准尺寸做整体缩放，保证粒子数量不变，仅尺寸与间距同比例缩放
  const base = 180
  const scale = Math.max(0.1, w / base)
  ctx.clearRect(0, 0, w, h)

  // 网格呼吸效果 - 按照 04-concentric-rotations.html 的实现
  const gridSize = 9
  const spacing = 18 * scale
  const dots = []
  const gridOffsetX = centerX - ((gridSize - 1) * spacing) / 2
  const gridOffsetY = centerY - ((gridSize - 1) * spacing) / 2

  for (let r = 0; r < gridSize; r++) {
    for (let c = 0; c < gridSize; c++) {
      const x = gridOffsetX + c * spacing
      const y = gridOffsetY + r * spacing
      const distFromCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2))
      const maxDist = Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2))
      const normalizedDist = distFromCenter / maxDist

      // 呼吸波动效果：从中心向外扩散的波纹
      const wave = Math.sin(t * 2 * speed - normalizedDist * 8) * 0.5 + 0.5
      
      // 基础大小和波动大小
      const baseSize = 2.5 * scale
      const waveSize = 1.5 * scale * wave
      const finalSize = Math.max(0.5 * scale, baseSize + waveSize)

      // 透明度也随波动变化
      const baseOpacity = 0.3
      const waveOpacity = 0.7 * wave
      const finalOpacity = Math.min(1, baseOpacity + waveOpacity)

      dots.push({ x, y, size: finalSize, opacity: finalOpacity })
    }
  }

  // 绘制所有点
  ctx.fillStyle = colorHex
  for (const dot of dots) {
    ctx.globalAlpha = dot.opacity
    ctx.beginPath()
    ctx.arc(dot.x, dot.y, dot.size, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.globalAlpha = 1

  rafId = requestAnimationFrame(drawFrame)
}

function startAnimation() {
  if (rafId) cancelAnimationFrame(rafId)
  start = 0
  rafId = requestAnimationFrame(drawFrame)
}

function stopAnimation() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

onMounted(() => {
  startAnimation()
})

onBeforeUnmount(() => {
  stopAnimation()
})

// 监听props变化，重新开始动画
watch([() => props.size, () => props.speed, () => props.color], () => {
  startAnimation()
})
</script>

<style scoped>
.concentric-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.loading-text {
  font-size: 14px;
  color: #666;
  text-align: center;
  line-height: 1.4;
  white-space: pre-line;
}

canvas {
  display: block;
}
</style>