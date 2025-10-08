<template>
  <div class="new-template">
    <PageHeader 
      title="新建模板"
      description="上传Word文档模板，设置模板信息后即可进入设计"
      :icon="DocumentAdd"
    />

    <el-card>
      <div class="body">
        <el-form label-width="100px" :inline="false" @submit.prevent>
          <el-form-item label="上传docx">
            <el-upload class="uploader" drag :auto-upload="false" :show-file-list="false" accept=".docx" :on-change="onFileChange">
              <i class="el-icon-upload"></i>
              <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
            </el-upload>
            <div v-if="file" class="file-tip">{{ file.name }}</div>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.name" placeholder="请输入模板名称" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="form.category" placeholder="请选择分类" style="width: 240px;">
              <el-option label="销售合同" value="sale" />
              <el-option label="服务合同" value="service" />
              <el-option label="租赁合同" value="lease" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板ID">
            <el-input v-model="form.templateId" placeholder="上传成功后生成，也可手动调整" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!canUpload" :loading="loading" @click="doUpload">上传并开始设计</el-button>
            <el-button @click="reset">清空</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentAdd } from '@element-plus/icons-vue'
import { uploadTemplateDocx } from '@/api/templateDesign'
import { PageHeader } from '@/components/common'

const router = useRouter()
const loading = ref(false)
const file = ref<File | null>(null)
const form = reactive({ name: '', category: '', templateId: '' })

const canUpload = computed(() => !!file.value && !!form.templateId)

function onFileChange(f: any) {
  const raw = f?.raw as File
  if (!raw) return
  if (!raw.name.toLowerCase().endsWith('.docx')) {
    ElMessage.warning('仅支持docx；如为doc请先转换为docx')
    return
  }
  file.value = raw
}

async function doUpload() {
  if (!canUpload.value) return
  try {
    loading.value = true
    const res = await uploadTemplateDocx({ templateId: form.templateId, file: file.value as File }) as any
    if (res?.code !== 200) throw new Error(res?.message || '上传失败')
    const fileId = res?.data?.fileId || ''
    ElMessage.success('上传成功，进入模板设计')
    router.push({ path: '/template-design', query: { id: form.templateId, fileId } })
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  file.value = null
  form.name = ''
  form.category = ''
  form.templateId = ''
}
</script>

<style scoped>
.new-template { padding: 16px; }
.card-header { font-weight: 600; }
.uploader { width: 420px; }
.file-tip { margin-top: 8px; color:#606266; }
</style>


