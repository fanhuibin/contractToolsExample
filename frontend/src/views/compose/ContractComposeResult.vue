<template>
  <section class="compose-result">
    <div class="bg-anim" aria-hidden="true"></div>
    <el-card class="card" shadow="never">
      <div class="hero">
        <div class="hero-badge">
          <span class="dot"></span>
          <span class="text">合成成功</span>
        </div>
        <h1>文件已生成</h1>
        <p class="sub">请选择需要下载的版本</p>
      </div>
      <el-row class="grid" :gutter="32">
        <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
          <el-button size="large" type="primary" plain :disabled="!docxPath" @click="download(docxPath)">下载 DOCX 版</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
          <el-button size="large" type="primary" plain :disabled="!pdfPath" @click="download(pdfPath)">下载 PDF 版</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
          <el-button size="large" type="success" plain :disabled="!stampedPdfPath" @click="download(stampedPdfPath)">下载盖章 PDF</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
          <el-button size="large" type="warning" plain :disabled="!ridingStampPdfPath" @click="download(ridingStampPdfPath)">下载骑缝章 PDF</el-button>
        </el-col>
      </el-row>
    </el-card>
  </section>
  
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

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
  if (!path) { ElMessage.warning('该版本尚未生成'); return }
  const backendBase = window.location.port === '8080' ? '' : 'http://localhost:8080'
  const url = `${backendBase}/api/download/temp?path=${encodeURIComponent(path)}`
  window.open(url, '_blank')
}
</script>

<style scoped>
.compose-result {
  position: relative;
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.bg-anim {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(800px 400px at 15% 20%, rgba(64,158,255,0.15), transparent 60%),
    radial-gradient(700px 380px at 85% 25%, rgba(103,194,58,0.12), transparent 60%),
    linear-gradient(180deg, rgba(64,158,255,0.06), rgba(64,158,255,0.02));
  overflow: hidden;
}
.bg-anim::before, .bg-anim::after {
  content: "";
  position: absolute;
  width: 1400px;
  height: 1400px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(64,158,255,0.18), transparent 60%);
  filter: blur(40px);
  animation: float 18s ease-in-out infinite;
}
.bg-anim::after {
  left: auto; right: -30%; top: -20%;
  background: radial-gradient(circle at 70% 70%, rgba(103,194,58,0.16), transparent 60%);
  animation-duration: 22s;
}
@keyframes float {
  0% { transform: translate3d(-10%, -6%, 0) scale(1); }
  50% { transform: translate3d(6%, 8%, 0) scale(1.05); }
  100% { transform: translate3d(-10%, -6%, 0) scale(1); }
}

.card {
  width: clamp(1080px, 88vw, 1360px);
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
}
.card :deep(.el-card__body) {
  padding: 56px 48px 36px;
}

.hero {
  text-align: center;
  margin-bottom: 12px;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(64,158,255,0.08);
  border: 1px solid rgba(64,158,255,0.18);
}
.hero-badge .dot {
  width: 8px; height: 8px; border-radius: 50%; background: #67C23A;
  box-shadow: 0 0 0 0 rgba(103,194,58,0.4);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(103,194,58,0.4); }
  70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(103,194,58,0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(103,194,58,0); }
}
.hero h1 {
  margin: 12px 0 6px;
  font-size: 28px;
  font-weight: 700;
}
.hero .sub {
  color: var(--el-text-color-secondary);
}

.grid { margin-top: 26px; }
.grid :deep(.el-button) {
  width: 100%;
  height: 50px;
}
</style>


