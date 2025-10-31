<template>
  <el-card class="page-header-card" :class="{ 'has-actions': $slots.actions }">
    <div class="page-header">
      <div class="header-content">
        <h2 class="header-title">
          <el-icon v-if="icon" class="header-icon">
            <component :is="icon" />
          </el-icon>
          {{ title }}
          <el-tag v-if="tag" :type="tagType" size="small" class="header-tag">
            {{ tag }}
          </el-tag>
        </h2>
        <p v-if="description" class="header-description">{{ description }}</p>
      </div>
      <div class="header-decoration"></div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

export interface PageHeaderProps {
  title: string
  description?: string
  icon?: Component
  tag?: string
  tagType?: 'success' | 'info' | 'warning' | 'danger' | ''
}

withDefaults(defineProps<PageHeaderProps>(), {
  description: '',
  tagType: 'info'
})
</script>

<style scoped>
.page-header-card {
  border-radius: var(--zx-radius-lg);
  box-shadow: var(--zx-shadow-sm);
  overflow: hidden;
  transition: all var(--zx-transition-base);
  margin-bottom: var(--zx-spacing-md);
}

.page-header-card:hover {
  box-shadow: var(--zx-shadow-md);
}

.page-header {
  padding: var(--zx-spacing-lg) var(--zx-spacing-xl);
  position: relative;
  background: var(--zx-gradient-page-header);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zx-spacing-xl);
}

.header-content {
  position: relative;
  z-index: 2;
  flex: 1;
}

.header-title {
  margin: 0;
  font-size: var(--zx-font-3xl);
  color: var(--zx-primary-dark-2);
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-md);
  font-weight: var(--zx-font-semibold);
}

.header-icon {
  font-size: var(--zx-font-3xl);
  color: var(--zx-primary);
}

.header-tag {
  margin-left: var(--zx-spacing-sm);
}

.header-description {
  margin: var(--zx-spacing-md) 0 0;
  color: var(--zx-text-regular);
  font-size: var(--zx-font-base);
  line-height: var(--zx-leading-relaxed);
  max-width: 80%;
}

.header-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 150px;
  height: 100%;
  background: var(--zx-gradient-card-decoration);
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}

.header-actions {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-md);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-title {
    font-size: var(--zx-font-2xl);
  }
  
  .header-description {
    max-width: 100%;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>

