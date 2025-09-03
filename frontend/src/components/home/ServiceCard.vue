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
/* 基于设计系统的样式规范 */
.service-card {
  background: #FFFFFF; /* colors.background.component */
  border-radius: 8px; /* borderRadius.large */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); /* shadows.base */
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #EBEEF5; /* colors.border.lighter */
}

.service-card:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, .15); /* shadows.dark */
  border-color: #409EFF; /* colors.primary.main */
}

.service-image {
  height: 200px;
  overflow: hidden;
  background: #F5F7FA; /* colors.background.hover */
  display: flex;
  align-items: center;
  justify-content: center;
}

.service-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.service-card:hover .service-image img {
  transform: scale(1.1);
}

.service-content {
  padding: 24px; /* spacing.lg */
  flex: 1;
  display: flex;
  flex-direction: column;
}

.service-title {
  font-size: 18px; /* typography.fontSize.large */
  font-weight: 500; /* typography.fontWeight.medium */
  margin-bottom: 12px; /* spacing.sm */
  color: #303133; /* colors.text.primary */
}

.service-description {
  color: #606266; /* colors.text.regular */
  line-height: 1.6;
  margin-bottom: 20px;
  flex: 1;
  font-size: 14px; /* typography.fontSize.base */
}

.service-button {
  align-self: flex-start;
  background-color: #409EFF; /* colors.primary.main */
  border-color: #409EFF; /* colors.primary.main */
  color: #FFFFFF; /* colors.primary.text */
  border-radius: 4px; /* borderRadius.base */
  padding: 10px 20px; /* button.sizes.padding.medium */
  font-size: 14px; /* typography.fontSize.base */
  transition: all 0.3s ease;
}

.service-button:hover {
  background-color: #66B1FF; /* colors.primary.light */
  border-color: #66B1FF; /* colors.primary.light */
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}
</style>
