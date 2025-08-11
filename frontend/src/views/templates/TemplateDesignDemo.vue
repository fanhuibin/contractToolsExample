<template>
  <div class="td-demo-page">
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>模板设计 Demo</span>
        </div>
      </template>
      <el-form :model="form" label-width="100px" style="max-width: 720px;">
        <el-form-item label="模板ID">
          <el-input v-model="form.templateId" placeholder="例如: demo" />
        </el-form-item>
        <el-form-item label="文件ID">
          <el-input v-model="form.fileId" placeholder="例如: 9999 (示例文件)" />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="form.callbackUrl" placeholder="http://localhost:3000/callback" />
        </el-form-item>
        <el-form-item label="后端地址">
          <el-input v-model="form.backendUrl" placeholder="http://localhost:8080" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="openDesigner">打开模板设计</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { startTemplateDesign } from '@/api/templateDesign'

const form = reactive({
  templateId: 'demo',
  fileId: '9999',
  callbackUrl: 'http://localhost:3000/callback',
  backendUrl: 'http://localhost:8080'
})

const openDesigner = async () => {
  try {
    const res = await startTemplateDesign({
      templateId: form.templateId,
      callbackUrl: form.callbackUrl,
      backendUrl: form.backendUrl
    })
    const editUrl = res?.data?.editUrl
    if (editUrl) {
      window.open(editUrl + `&fileId=${encodeURIComponent(form.fileId)}`, '_blank')
    } else {
      ElMessage.error('后端未返回编辑地址')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '发起模板设计失败')
  }
}
</script>

<style scoped>
.td-demo-page { padding: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>



