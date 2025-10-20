<template>
  <div class="concentric-loader">
    <div class="loader-container">
      <svg :width="size" :height="size" viewBox="0 0 100 100" class="loader-svg">
        <!-- 外圈 -->
        <circle
          cx="50"
          cy="50"
          r="45"
          fill="none"
          :stroke="color"
          stroke-width="2"
          opacity="0.2"
        />
        <!-- 中圈 -->
        <circle
          cx="50"
          cy="50"
          r="35"
          fill="none"
          :stroke="color"
          stroke-width="2"
          opacity="0.4"
        />
        <!-- 内圈 -->
        <circle
          cx="50"
          cy="50"
          r="25"
          fill="none"
          :stroke="color"
          stroke-width="2"
          opacity="0.6"
        />
        <!-- 旋转的外圈 -->
        <circle
          cx="50"
          cy="50"
          r="45"
          fill="none"
          :stroke="color"
          stroke-width="3"
          stroke-dasharray="70 213"
          stroke-linecap="round"
          class="rotating-circle outer"
        />
        <!-- 旋转的中圈 -->
        <circle
          cx="50"
          cy="50"
          r="35"
          fill="none"
          :stroke="color"
          stroke-width="3"
          stroke-dasharray="55 165"
          stroke-linecap="round"
          class="rotating-circle middle"
        />
        <!-- 旋转的内圈 -->
        <circle
          cx="50"
          cy="50"
          r="25"
          fill="none"
          :stroke="color"
          stroke-width="3"
          stroke-dasharray="40 117"
          stroke-linecap="round"
          class="rotating-circle inner"
        />
      </svg>
    </div>
    <div v-if="text" class="loader-text" :style="{ color: color }">{{ text }}</div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, withDefaults } from 'vue'

interface Props {
  color?: string
  size?: number
  text?: string
}

withDefaults(defineProps<Props>(), {
  color: '#1677ff',
  size: 52,
  text: ''
})
</script>

<style scoped>
.concentric-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.loader-container {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loader-svg {
  display: block;
}

.rotating-circle {
  transform-origin: center;
  animation: rotate 2s linear infinite;
}

.rotating-circle.outer {
  animation-duration: 2s;
}

.rotating-circle.middle {
  animation-duration: 1.5s;
  animation-direction: reverse;
}

.rotating-circle.inner {
  animation-duration: 1s;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loader-text {
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}
</style>

