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

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  icon: {
    type: Object,
    default: null
  },
  tag: {
    type: String,
    default: ''
  },
  tagType: {
    type: String,
    default: 'info',
    validator: (value) => ['success', 'info', 'warning', 'danger', ''].includes(value)
  }
})
</script>

<style scoped>
.page-header-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.page-header-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.page-header {
  padding: 32px 40px;
  position: relative;
  background: linear-gradient(135deg, #c6e2ff, #ecf5ff);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.header-content {
  position: relative;
  z-index: 2;
  flex: 1;
  text-align: left;
}

.header-title {
  margin: 0;
  font-size: 28px;
  color: #337ecc;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  text-align: left;
}

.header-icon {
  font-size: 28px;
  color: #409EFF;
}

.header-tag {
  margin-left: 8px;
}

.header-description {
  margin: 12px 0 0;
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
  text-align: left;
}

.header-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 150px;
  height: 100%;
  background: linear-gradient(135deg, transparent, rgba(64, 158, 255, 0.05));
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}

.header-actions {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-title {
    font-size: 22px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>

