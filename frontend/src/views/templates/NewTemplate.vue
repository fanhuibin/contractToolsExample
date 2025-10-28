<template>
  <div class="new-template">
    <PageHeader 
      title="新建模板"
      description="上传Word文档模板，设置模板信息后即可进入设计"
      :icon="DocumentAdd"
    />

    <el-card>
      <div class="body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :inline="false" @submit.prevent>
          <el-form-item label="模板名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入模板名称" maxlength="50" show-word-limit style="width: 400px;" />
          </el-form-item>
          <el-form-item label="模板编码" prop="templateCode">
            <el-input v-model="form.templateCode" placeholder="请输入模板编码，如：ht-sale-001" maxlength="50" style="width: 400px;" />
            <div class="form-tip">模板编码用于标识同一合同的不同版本，支持字母、数字、中划线、下划线，至少3个字符</div>
          </el-form-item>
          <el-form-item label="版本描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入版本描述或更新说明（可选）" maxlength="200" show-word-limit style="width: 400px;" />
          </el-form-item>
          <el-form-item label="上传docx" prop="file">
            <el-upload class="uploader" drag :auto-upload="false" :show-file-list="false" accept=".docx" :on-change="onFileChange">
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">仅支持.docx格式文件</div>
              </template>
            </el-upload>
            <div v-if="file" class="file-tip">已选择：{{ file.name }}</div>
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
import { DocumentAdd, UploadFilled } from '@element-plus/icons-vue'
import { uploadTemplateDocx, saveTemplateDesign } from '@/api/templateDesign'
import { PageHeader } from '@/components/common'

const router = useRouter()
const loading = ref(false)
const file = ref<File | null>(null)
const formRef = ref()
const form = reactive({ 
  name: '', 
  templateCode: '', 
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
    { min: 3, message: '模板编码至少3个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9\-_]+$/, message: '模板编码只能包含字母、数字、中划线和下划线，不能使用中文', trigger: 'blur' }
  ]
}

const canUpload = computed(() => !!file.value && !!form.name && !!form.templateCode)

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
  if (!formRef.value) return
  
  // 先检查文件是否上传
  if (!file.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  
  try {
    // 表单验证
    await formRef.value.validate()
  } catch {
    return
  }

  if (!canUpload.value) return
  
  try {
    loading.value = true
    
    // 上传文件（使用临时ID）
    const tempId = `TEMP_${Date.now()}`
    const uploadRes = await uploadTemplateDocx({ templateId: tempId, file: file.value as File }) as any
    
    if (uploadRes?.data?.code !== 200) {
      throw new Error(uploadRes?.data?.message || '上传失败')
    }
    
    const uploadedData = uploadRes?.data?.data || {}
    const fileId = uploadedData?.fileId || ''
    const recordId = uploadedData?.id || ''
    
    // 更新模板信息
    const saveRes = await saveTemplateDesign({
      id: recordId,
      templateCode: form.templateCode,
      templateName: form.name,
      description: form.description,
      fileId: fileId,
      version: '1.0',
      status: 'DRAFT',
      elementsJson: JSON.stringify({ elements: [] })
    }) as any
    
    if (saveRes?.data?.code !== 200) {
      throw new Error(saveRes?.data?.message || '保存失败')
    }
    
    const savedData = saveRes?.data?.data || {}
    const finalId = savedData?.id || recordId
    
    ElMessage.success('模板创建成功，进入设计页面')
    router.push({ path: '/template-design', query: { id: finalId, fileId, returnUrl: '/templates' } })
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  file.value = null
  form.name = ''
  form.templateCode = ''
  form.description = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.new-template { padding: 16px; }
.card-header { font-weight: 600; }
.uploader { width: 420px; }
.file-tip { margin-top: 8px; color: #67c23a; font-weight: 500; }
.form-tip { 
  margin-top: 4px; 
  font-size: 12px; 
  color: #909399; 
  line-height: 1.5;
}
.el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 7px;
}
</style>


