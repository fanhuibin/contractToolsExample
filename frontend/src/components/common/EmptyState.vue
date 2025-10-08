<template>
  <div class="empty-state" :class="{ compact: size === 'small' }">
    <div class="empty-icon">
      <component :is="icon || DocumentIcon" />
    </div>
    <h3 class="empty-title">{{ title || '暂无数据' }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <div v-if="$slots.action || actionText" class="empty-action">
      <slot name="action">
        <el-button 
          v-if="actionText" 
          :type="actionType" 
          @click="$emit('action')"
        >
          {{ actionText }}
        </el-button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document as DocumentIcon } from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface Props {
  title?: string
  description?: string
  icon?: Component
  actionText?: string
  actionType?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
  size?: 'normal' | 'small'
}

withDefaults(defineProps<Props>(), {
  actionType: 'primary',
  size: 'normal'
})

defineEmits<{
  action: []
}>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--zx-spacing-5xl) var(--zx-spacing-xl);
  text-align: center;
  min-height: 300px;
}

.empty-state.compact {
  padding: var(--zx-spacing-3xl) var(--zx-spacing-lg);
  min-height: 200px;
}

.empty-icon {
  font-size: 80px;
  color: var(--zx-text-placeholder);
  margin-bottom: var(--zx-spacing-2xl);
  opacity: 0.6;
  animation: float 3s ease-in-out infinite;
}

.empty-state.compact .empty-icon {
  font-size: 56px;
  margin-bottom: var(--zx-spacing-lg);
}

.empty-title {
  font-size: var(--zx-font-xl);
  font-weight: var(--zx-font-semibold);
  color: var(--zx-text-primary);
  margin: 0 0 var(--zx-spacing-md);
}

.empty-state.compact .empty-title {
  font-size: var(--zx-font-lg);
}

.empty-description {
  font-size: var(--zx-font-base);
  color: var(--zx-text-secondary);
  margin: 0 0 var(--zx-spacing-2xl);
  max-width: 400px;
  line-height: var(--zx-leading-relaxed);
}

.empty-state.compact .empty-description {
  font-size: var(--zx-font-sm);
  margin-bottom: var(--zx-spacing-lg);
}

.empty-action {
  margin-top: var(--zx-spacing-lg);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .empty-state {
    padding: var(--zx-spacing-3xl) var(--zx-spacing-lg);
  }
  
  .empty-icon {
    font-size: 64px;
  }
  
  .empty-title {
    font-size: var(--zx-font-lg);
  }
  
  .empty-description {
    font-size: var(--zx-font-sm);
  }
}
</style>

