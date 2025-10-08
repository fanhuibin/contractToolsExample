<template>
  <div class="loading-container" :class="{ fullscreen, compact: size === 'small' }">
    <div class="spinner-wrapper">
      <div class="spinner" :class="spinnerSize">
        <div class="ring"></div>
        <div class="ring"></div>
        <div class="ring"></div>
      </div>
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  text?: string
  fullscreen?: boolean
  size?: 'small' | 'normal' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  fullscreen: false,
  size: 'normal'
})

const spinnerSize = computed(() => {
  return `spinner-${props.size}`
})
</script>

<style scoped>
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--zx-spacing-3xl);
}

.loading-container.compact {
  padding: var(--zx-spacing-xl);
}

.loading-container.fullscreen {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  z-index: var(--zx-z-modal);
  backdrop-filter: blur(4px);
}

.spinner-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zx-spacing-lg);
}

.spinner {
  position: relative;
  width: 80px;
  height: 80px;
}

.spinner-small {
  width: 40px;
  height: 40px;
}

.spinner-large {
  width: 120px;
  height: 120px;
}

.ring {
  position: absolute;
  border: 3px solid transparent;
  border-top-color: var(--zx-primary);
  border-radius: 50%;
  animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-normal .ring:nth-child(1) {
  width: 64px;
  height: 64px;
  top: 8px;
  left: 8px;
  animation-delay: -0.45s;
}

.spinner-normal .ring:nth-child(2) {
  width: 48px;
  height: 48px;
  top: 16px;
  left: 16px;
  animation-delay: -0.3s;
  border-top-color: var(--zx-primary-light-2);
}

.spinner-normal .ring:nth-child(3) {
  width: 32px;
  height: 32px;
  top: 24px;
  left: 24px;
  animation-delay: -0.15s;
  border-top-color: var(--zx-primary-light-4);
}

.spinner-small .ring:nth-child(1) {
  width: 32px;
  height: 32px;
  top: 4px;
  left: 4px;
  animation-delay: -0.45s;
}

.spinner-small .ring:nth-child(2) {
  width: 24px;
  height: 24px;
  top: 8px;
  left: 8px;
  animation-delay: -0.3s;
  border-top-color: var(--zx-primary-light-2);
}

.spinner-small .ring:nth-child(3) {
  width: 16px;
  height: 16px;
  top: 12px;
  left: 12px;
  animation-delay: -0.15s;
  border-top-color: var(--zx-primary-light-4);
}

.spinner-large .ring:nth-child(1) {
  width: 96px;
  height: 96px;
  top: 12px;
  left: 12px;
  animation-delay: -0.45s;
}

.spinner-large .ring:nth-child(2) {
  width: 72px;
  height: 72px;
  top: 24px;
  left: 24px;
  animation-delay: -0.3s;
  border-top-color: var(--zx-primary-light-2);
}

.spinner-large .ring:nth-child(3) {
  width: 48px;
  height: 48px;
  top: 36px;
  left: 36px;
  animation-delay: -0.15s;
  border-top-color: var(--zx-primary-light-4);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: var(--zx-font-base);
  color: var(--zx-text-secondary);
  margin: 0;
  animation: pulse 2s ease-in-out infinite;
}

.compact .loading-text {
  font-size: var(--zx-font-sm);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>

