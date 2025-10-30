<template>
  <div class="compose-result">
    <div class="result-card">
      <div class="success-icon">
        <el-icon color="#67c23a" :size="64"><CircleCheck /></el-icon>
      </div>
      <h2 class="title">合同已保存</h2>
      <p class="desc">文档已保存到服务器，您可以下载查看最终版本</p>
      <el-alert 
        type="info" 
        :closable="false" 
        show-icon
        style="margin-bottom: 24px; text-align: left;"
      >
        <template #title>
          <span style="font-size: 13px;">文档处理说明</span>
        </template>
        <div style="font-size: 12px; line-height: 1.6;">
          文档保存需要服务器处理时间（约5-10秒）。如果下载的文档内容不是最新的，请稍等片刻后重新下载。
        </div>
      </el-alert>
      
      <div class="actions">
        <el-button type="primary" size="large" @click="downloadFile">
          <el-icon style="margin-right: 4px;"><Download /></el-icon>
          下载合同
        </el-button>
        <el-button size="large" @click="goBack">
          <el-icon style="margin-right: 4px;"><ArrowLeft /></el-icon>
          返回继续编辑
        </el-button>
      </div>
      
      <div class="file-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件ID">{{ fileId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag type="success">已完成</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Download, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const fileId = computed(() => String(route.params.fileId || ''))

function downloadFile() {
  if (!fileId.value) {
    ElMessage.error('文件ID不存在')
    return
  }
  // 使用文件下载API
  const downloadUrl = `/api/file/download/${fileId.value}`
  window.open(downloadUrl, '_blank')
  ElMessage.success('开始下载')
}

function goBack() {
  const id = String(route.query.id || '')
  const templateId = String(route.query.templateId || 'demo')
  const templateFileId = String(route.query.fileId || '9999')
  router.push({ 
    path: '/contract-compose-frontend', 
    query: { 
      id, 
      templateId, 
      fileId: templateFileId 
    } 
  })
}
</script>

<style scoped>
.compose-result {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.result-card {
  background: white;
  border-radius: 8px;
  padding: 60px 48px;
  max-width: 680px;
  width: 100%;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  text-align: center;
  border: 1px solid #e4e7ed;
}

.success-icon {
  margin-bottom: 32px;
  animation: scaleIn 0.4s ease-out;
}

@keyframes scaleIn {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.desc {
  font-size: 15px;
  color: #606266;
  margin-bottom: 40px;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.actions .el-button {
  min-width: 140px;
  flex: 0 1 auto;
}

.file-info {
  margin-top: 32px;
  text-align: left;
}

.file-info :deep(.el-descriptions) {
  border-radius: 4px;
  overflow: hidden;
}

.file-info :deep(.el-descriptions__label) {
  background-color: #fafafa;
  font-weight: 500;
}
</style>



