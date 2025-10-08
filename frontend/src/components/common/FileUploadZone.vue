<template>
  <el-upload
    class="upload-zone"
    drag
    :action="action"
    :auto-upload="autoUpload"
    :show-file-list="showFileList"
    :on-change="handleChange"
    :accept="accept"
    :multiple="multiple"
    :limit="limit"
  >
    <div class="upload-content">
      <el-icon class="upload-icon"><upload-filled /></el-icon>
      <div class="upload-text">
        拖拽文件到此处，或 <em>点击上传</em>
      </div>
      <div class="upload-tip">{{ tip || `支持格式: ${accept}` }}</div>
      <div v-if="maxSize" class="upload-tip">{{ `最大文件大小: ${maxSize}MB` }}</div>
    </div>
  </el-upload>
</template>

<script setup lang="ts">
import { UploadFilled } from '@element-plus/icons-vue'

interface Props {
  accept?: string
  tip?: string
  action?: string
  autoUpload?: boolean
  showFileList?: boolean
  multiple?: boolean
  limit?: number
  maxSize?: number
}

withDefaults(defineProps<Props>(), {
  accept: '.pdf,.doc,.docx,.xls,.xlsx',
  action: '#',
  autoUpload: false,
  showFileList: false,
  multiple: false,
  limit: 1
})

const emit = defineEmits<{
  change: [file: File]
}>()

const handleChange = (uploadFile: any) => {
  if (uploadFile.raw) {
    emit('change', uploadFile.raw)
  }
}
</script>

<style scoped>
.upload-zone {
  width: 100%;
}

.upload-content {
  padding: var(--zx-spacing-3xl) var(--zx-spacing-xl);
  text-align: center;
}

.upload-icon {
  font-size: 64px;
  color: var(--zx-primary);
  margin-bottom: var(--zx-spacing-lg);
  transition: transform var(--zx-transition-base);
}

.upload-content:hover .upload-icon {
  transform: scale(1.1);
  color: var(--zx-primary-light-2);
}

.upload-text {
  font-size: var(--zx-font-lg);
  color: var(--zx-text-regular);
  margin-bottom: var(--zx-spacing-sm);
  line-height: var(--zx-leading-relaxed);
}

.upload-text em {
  color: var(--zx-primary);
  font-style: normal;
  font-weight: var(--zx-font-medium);
  cursor: pointer;
  text-decoration: underline;
}

.upload-tip {
  font-size: var(--zx-font-sm);
  color: var(--zx-text-secondary);
  margin-top: var(--zx-spacing-xs);
}

:deep(.el-upload-dragger) {
  border: 2px dashed var(--zx-border-base);
  border-radius: var(--zx-radius-lg);
  transition: all var(--zx-transition-base);
  background-color: var(--zx-bg-white);
}

:deep(.el-upload-dragger:hover) {
  border-color: var(--zx-primary);
  background-color: var(--zx-primary-light-9);
}

:deep(.el-upload-dragger.is-dragover) {
  border-color: var(--zx-primary);
  background-color: var(--zx-primary-light-8);
}

/* 响应式 */
@media (max-width: 768px) {
  .upload-content {
    padding: var(--zx-spacing-2xl) var(--zx-spacing-lg);
  }
  
  .upload-icon {
    font-size: 48px;
  }
  
  .upload-text {
    font-size: var(--zx-font-base);
  }
}
</style>

