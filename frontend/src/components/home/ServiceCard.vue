<template>
  <div class="service-card" @click="handleCardClick">
    <div class="service-image">
      <img :src="service.image" :alt="service.title" @error="handleImageError" />
    </div>
    <div class="service-content">
      <h3 class="service-title">{{ service.title }}</h3>
      <p class="service-description">{{ service.description }}</p>
      <el-button 
        type="primary" 
        class="service-button"
        @click.stop="handleButtonClick"
      >
        {{ service.button_text }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineEmits, defineProps } from 'vue'

interface Service {
  title: string
  description: string
  image: string
  button_text: string
  link: string
}

interface Props {
  service: Service
}

const props = defineProps<Props>()

const emit = defineEmits<{
  cardClick: [service: Service]
  buttonClick: [service: Service]
}>()

const handleCardClick = () => {
  emit('cardClick', props.service)
}

const handleButtonClick = () => {
  emit('buttonClick', props.service)
}

const handleImageError = (event: Event) => {
  // 图片加载失败时显示默认图片
  const target = event.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIiBmaWxsPSIjRjVGN0ZBIi8+CjxwYXRoIGQ9Ik02MCA2MEgxNDBWMTQwSDYwVjYwWiIgZmlsbD0iIzAwQTRCNyIgZmlsbC1vcGFjaXR5PSIwLjIiLz4KPHN2ZyB4PSI4NSIgeT0iODUiIHdpZHRoPSIzMCIgaGVpZ2h0PSIzMCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjMDBBNEI3Ij4KPHA+PHBhdGggZD0iTTEyIDJDMTMuMSAyIDE0IDIuOSAxNCA0VjhIMTBWNEM5IDIuOSA5LjkgMiAxMiAyWk0yMSAxMUgxNUwxMy41IDkuNUMxMy4xIDkuMSAxMi42IDkgMTIgOUMxMS40IDkgMTAuOSA5LjEgMTAuNSA5LjVMOSAxMUgzQzIuNDUgMTEgMiAxMS40NSAyIDEyUzIuNDUgMTMgMyAxM0g5TDEwLjUgMTQuNUMxMC45IDE0LjkgMTEuNCAxNSAxMiAxNUMxMi42IDE1IDEzLjEgMTQuOSAxMy41IDE0LjVMMTUgMTNIMjFDMjEuNTUgMTMgMjIgMTIuNTUgMjIgMTJTMjEuNTUgMTEgMjEgMTFaIi8+PC9wPgo8L3N2Zz4KPC9zdmc+'
}
</script>

<style scoped>
/* 使用设计系统的样式变量 */
.service-card {
  background: var(--zx-bg-white);
  border-radius: var(--zx-radius-lg);
  box-shadow: var(--zx-shadow-base);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--zx-transition-base);
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--zx-border-lighter);
}

.service-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: var(--zx-shadow-lg);
  border-color: var(--zx-primary);
}

.service-image {
  height: 200px;
  overflow: hidden;
  background: var(--zx-bg-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.service-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--zx-transition-base);
}

.service-card:hover .service-image img {
  transform: scale(1.1);
}

.service-content {
  padding: var(--zx-spacing-2xl);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.service-title {
  font-size: var(--zx-font-xl);
  font-weight: var(--zx-font-medium);
  margin-bottom: var(--zx-spacing-md);
  color: var(--zx-text-primary);
}

.service-description {
  color: var(--zx-text-regular);
  line-height: var(--zx-leading-relaxed);
  margin-bottom: var(--zx-spacing-xl);
  flex: 1;
  font-size: var(--zx-font-base);
}

.service-button {
  align-self: flex-start;
  background-color: var(--zx-primary);
  border-color: var(--zx-primary);
  color: #FFFFFF;
  border-radius: var(--zx-radius-base);
  padding: 10px 20px;
  font-size: var(--zx-font-base);
  transition: all var(--zx-transition-base);
}

.service-button:hover {
  background-color: var(--zx-primary-light-2);
  border-color: var(--zx-primary-light-2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}
</style>
