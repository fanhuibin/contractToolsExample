<template>
  <div class="compose-result">
    <div class="toolbar">
      <el-button @click="goBack" icon="ArrowLeft">返回前端合成</el-button>
      <div class="title">前端合成结果预览（只读）</div>
    </div>
    <div class="viewer">
      <OnlyOfficeEditor
        v-if="fileId"
        :file-id="fileId"
        :can-edit="false"
        :can-review="false"
        :height="'calc(100vh - 140px)'"
        :show-toolbar="false"
        :show-status="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

const route = useRoute()
const router = useRouter()

const fileId = computed(() => String(route.params.fileId || ''))

function goBack() {
  const templateId = String(route.query.templateId || 'demo')
  const fileId = String(route.query.fileId || '9999')
  router.push({ path: '/contract-compose-frontend', query: { templateId, fileId } })
}
</script>

<style scoped>
.compose-result { display: flex; flex-direction: column; height: 100%; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 8px 12px; border-bottom: 1px solid #eee; }
.title { font-weight: 600; }
.viewer { padding: 12px; height: calc(100vh - 120px); }
.viewer :deep(.onlyoffice-editor),
.viewer :deep(#onlyoffice-editor-container) { width: 100%; height: 100%; }
</style>



