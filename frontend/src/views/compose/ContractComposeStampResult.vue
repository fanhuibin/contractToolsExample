<template>
  <div class="compose-result-page">
    <div class="toolbar">
      <el-button @click="goBack" icon="ArrowLeft">返回合成</el-button>
      <div class="title">合成盖章结果预览</div>
      <div class="version-switcher">
        <el-radio-group v-model="currentVersion" size="small">
          <el-radio-button label="pdf">PDF版</el-radio-button>
          <el-radio-button label="stamped">盖章版</el-radio-button>
          <el-radio-button label="riding">骑缝章版</el-radio-button>
        </el-radio-group>
      </div>
      <div class="spacer"></div>
      <div class="download-buttons">
        <el-button @click="downloadDocx" type="primary" size="small">下载DOCX版</el-button>
        <el-button @click="downloadPdf" type="success" size="small">下载PDF版</el-button>
        <el-button @click="downloadStampedPdf" type="warning" size="small">下载PDF盖章版</el-button>
        <el-button @click="downloadRidingStamp" type="danger" size="small">下载PDF骑缝章版</el-button>
      </div>
    </div>
    
    <div class="viewer-container">
      <iframe v-if="currentPdfUrl" :src="viewerUrl(currentPdfUrl)" class="pdf-viewer"></iframe>
      <div v-else class="empty-state">
        <p>没有可预览的文件。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { downloadTempFile } from '@/api/contract-compose'

const route = useRoute()
const router = useRouter()

const docxPath = ref('')
const pdfPath = ref('')
const stampedPdfPath = ref('')
const ridingStampPdfPath = ref('')

const currentVersion = ref<'pdf' | 'stamped' | 'riding'>('pdf')

onMounted(() => {
  docxPath.value = String(route.query.docxPath || '')
  pdfPath.value = String(route.query.pdfPath || '')
  stampedPdfPath.value = String(route.query.stampedPdfPath || '')
  ridingStampPdfPath.value = String(route.query.ridingStampPdfPath || '')
})

const currentPdfUrl = computed(() => {
  switch (currentVersion.value) {
    case 'pdf':
      return pdfPath.value
    case 'stamped':
      return stampedPdfPath.value
    case 'riding':
      return ridingStampPdfPath.value
    default:
      return ''
  }
})

const viewerUrl = (fileUrl: string) => {
    if (!fileUrl) return ''
    // Assuming files are served from /uploads/compose/...
    const fullFileUrl = `${window.location.origin}/api/uploads/${fileUrl}`
    return `/pdfviewer/web/viewer.html?file=${encodeURIComponent(fullFileUrl)}#page=1`
}


function goBack() {
  router.push({ path: '/contract-compose' })
}

function downloadDocx() {
  if (docxPath.value) {
    downloadTempFile(docxPath.value)
  }
}

function downloadPdf() {
  if (pdfPath.value) {
    downloadTempFile(pdfPath.value)
  }
}

function downloadStampedPdf() {
  if (stampedPdfPath.value) {
    downloadTempFile(stampedPdfPath.value)
  }
}

function downloadRidingStamp() {
  if (ridingStampPdfPath.value) {
    downloadTempFile(ridingStampPdfPath.value)
  }
}

</script>

<style scoped>
.compose-result-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 50px);
  background-color: #f0f2f5;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #dcdfe6;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
.version-switcher {
  margin-left: 24px;
}
.spacer {
  flex-grow: 1;
}
.viewer-container {
  flex-grow: 1;
  padding: 16px;
  overflow: hidden;
}
.pdf-viewer {
  width: 100%;
  height: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}
.empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #909399;
}
</style>
