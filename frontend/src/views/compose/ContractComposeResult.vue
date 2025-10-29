<template>
  <div class="compose-result">
    <el-card class="result-card">
      <div class="header">
        <el-icon class="success-icon" color="#67C23A" :size="48">
          <CircleCheck />
        </el-icon>
        <h2>合成成功</h2>
        <p class="desc">文件已生成，请选择需要下载的版本</p>
      </div>
      
      <div class="download-links">
        <a 
          v-if="docxPath" 
          href="javascript:;" 
          @click="download(docxPath)"
          class="download-link"
        >
          下载 DOCX 版
        </a>
        <span v-else class="download-link disabled">下载 DOCX 版</span>
        
        <span class="separator">|</span>
        
        <a 
          v-if="pdfPath" 
          href="javascript:;" 
          @click="download(pdfPath)"
          class="download-link"
        >
          下载 PDF 版
        </a>
        <span v-else class="download-link disabled">下载 PDF 版</span>
        
        <span class="separator">|</span>
        
        <a 
          v-if="stampedPdfPath" 
          href="javascript:;" 
          @click="download(stampedPdfPath)"
          class="download-link"
        >
          下载盖章 PDF
        </a>
        <span v-else class="download-link disabled">下载盖章 PDF</span>
        
        <span class="separator">|</span>
        
        <a 
          v-if="ridingStampPdfPath" 
          href="javascript:;" 
          @click="download(ridingStampPdfPath)"
          class="download-link"
        >
          下载骑缝章 PDF
        </a>
        <span v-else class="download-link disabled">下载骑缝章 PDF</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'

const route = useRoute()
const docxPath = ref<string>('')
const pdfPath = ref<string>('')
const stampedPdfPath = ref<string>('')
const ridingStampPdfPath = ref<string>('')

onMounted(() => {
  // 从路由query读取相对路径
  docxPath.value = String(route.query.docxPath || '')
  pdfPath.value = String(route.query.pdfPath || '')
  stampedPdfPath.value = String(route.query.stampedPdfPath || '')
  ridingStampPdfPath.value = String(route.query.ridingStampPdfPath || '')
})

function download(path: string) {
  if (!path) { 
    ElMessage.warning('该版本尚未生成')
    return 
  }
  // 使用相对路径，由 Nginx 或反向代理处理路由
  const url = `/api/download/temp?path=${encodeURIComponent(path)}`
  window.open(url, '_blank')
}
</script>

<style scoped>
.compose-result {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #ffffff;
}

.result-card {
  max-width: 600px;
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.success-icon {
  margin-bottom: 16px;
}

.header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header .desc {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.download-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.download-link {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
}

.download-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.download-link.disabled {
  color: #C0C4CC;
  cursor: not-allowed;
}

.separator {
  color: #DCDFE6;
  user-select: none;
}
</style>


