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
  // Grid breathing effect based on 04-concentric-rotations.html implementation
  const base = 180
  const scale = Math.max(0.1, w / base)
  ctx.clearRect(0, 0, w, h)

  // Grid breathing effect
  const gridSize = 9
  const spacing = 18 * scale
  const dots = []
  const gridOffsetX = centerX - ((gridSize - 1) * spacing) / 2
  const gridOffsetY = centerY - ((gridSize - 1) * spacing) / 2

  for (let r = 0; r < gridSize; r++) {
    for (let c = 0; c < gridSize; c++) {
      dots.push({
        x: gridOffsetX + c * spacing,
        y: gridOffsetY + r * spacing
      })
    }
  }

  // Wave speed scaled proportionally
  const waveSpeed = 60 * speed * scale
  const waveThickness = 40 * scale
  const maxDist = Math.sqrt(centerX * centerX + centerY * centerY) + waveThickness

  const currentWaveCenterDist = (t * waveSpeed) % maxDist

  const toRgba = (hex: string, a: number) => {
    const h = hex.replace('#', '')
    const r = parseInt(h.substring(0, 2), 16)
    const g = parseInt(h.substring(2, 4), 16)
    const b = parseInt(h.substring(4, 6), 16)
    return `rgba(${r},${g},${b},${a})`
  }

  dots.forEach((dot) => {
    const distFromCanvasCenter = Math.hypot(
      dot.x - centerX,
      dot.y - centerY
    )
    const distToWave = Math.abs(
      distFromCanvasCenter - currentWaveCenterDist
    )
    let pulseFactor = 0

    if (distToWave < waveThickness / 2) {
      pulseFactor = 1 - distToWave / (waveThickness / 2)
      pulseFactor = Math.sin((pulseFactor * Math.PI) / 2)
    }

    const dotSize = (1.5 + pulseFactor * 2.5) * scale
    const opacity = 0.2 + pulseFactor * 0.8

    ctx.beginPath()
    ctx.arc(dot.x, dot.y, dotSize, 0, Math.PI * 2)
    ctx.fillStyle = toRgba(colorHex, opacity)
    ctx.fill()
  })

  rafId = requestAnimationFrame(drawFrame)
}

function onResize() {
  if (!canvas.value) return
  const s = Math.max(40, Math.min(400, props.size ?? 180))
  canvas.value.width = s
  canvas.value.height = s
}

onMounted(() => {
  // Debug: component mounted
  // eslint-disable-next-line no-console
  console.log('[ConcentricLoader] mounted')
  onResize()
  window.addEventListener('resize', onResize)
  rafId = requestAnimationFrame(drawFrame)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
})

watch(() => props.size, () => onResize())
</script>

<style scoped>
.concentric-loader {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  pointer-events: none;
}

/* Style for PDF loader effect */
.concentric-loader.pdf-loader {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

canvas { 
  display: block; 
  filter: drop-shadow(0 0 10px rgba(0,0,0,0.15)); 
}

.loading-text {
  margin-top: 35px;
  color: #1677ff;
  font-size: 15px;
  font-weight: 500;
  text-align: center;
  opacity: 0.9;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
</style>

