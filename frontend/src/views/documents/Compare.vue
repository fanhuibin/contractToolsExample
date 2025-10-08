<template>
  <div class="compare-page">
    <PageHeader 
      title="PDF合同比对"
      description="上传两个版本或提供两个URL，系统将对比差异并给出结果。"
      :icon="DocumentCopy"
    />
    <el-card class="mb12">
      <template #header>上传文件比对</template>
      <el-form :inline="true" class="form-inline">
        <el-form-item label="原始文件">
          <input
            ref="oldInput"
            type="file"
            accept=".pdf,.doc,.docx,.xls,.xlsx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          />
        </el-form-item>
        <el-form-item label="新文件">
          <input
            ref="newInput"
            type="file"
            accept=".pdf,.doc,.docx,.xls,.xlsx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          />
        </el-form-item>
        <el-button type="primary" @click="doUploadCompare" :loading="loading">开始比对</el-button>
        <el-button text @click="settingsOpen = true">比对设置</el-button>
      </el-form>
    </el-card>

    <el-card class="mb12">
      <template #header>URL 比对</template>
      <el-form :inline="true" class="form-inline">
        <el-form-item label="原始URL">
          <el-input v-model="oldUrl" placeholder="http(s)://... (支持 pdf/doc/docx/xls/xlsx)" style="width:360px" />
        </el-form-item>
        <el-form-item label="新URL">
          <el-input v-model="newUrl" placeholder="http(s)://... (支持 pdf/doc/docx/xls/xlsx)" style="width:360px" />
        </el-form-item>
        <el-button @click="doUrlCompare" :loading="loading">开始比对</el-button>
        <el-button text @click="settingsOpen = true">比对设置</el-button>
      </el-form>
    </el-card>

    <el-drawer v-model="settingsOpen" title="比对设置" size="380px">
      <el-form label-width="120px">
        <el-form-item label="忽略页眉页脚">
          <el-switch v-model="settings.ignoreHeaderFooter" />
        </el-form-item>
        <el-form-item label="页眉高度(mm)">
          <el-input-number v-model="settings.headerHeightMm" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="页脚高度(mm)">
          <el-input-number v-model="settings.footerHeightMm" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="忽略大小写">
          <el-switch v-model="settings.ignoreCase" />
        </el-form-item>
        <el-form-item label="忽略符号集">
          <el-input v-model="settings.ignoredSymbols" placeholder="例如：_＿-·—" />
        </el-form-item>
        <el-alert title="说明：这些设置仅影响结果过滤与页眉/页脚忽略，不影响坐标计算。" type="info" show-icon />
      </el-form>
    </el-drawer>

    <!-- 结果展示移至 CompareResult.vue，此处不再内嵌结果页 -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { uploadCompare, compareByUrls } from '@/api/compare'
import { PageHeader } from '@/components/common'

const oldInput = ref<HTMLInputElement | null>(null)
const newInput = ref<HTMLInputElement | null>(null)
const oldUrl = ref('')
const newUrl = ref('')
const loading = ref(false)
const router = useRouter()

// 设置项（与后端 CompareOptions 对应）
const settingsOpen = ref(false)
const settings = reactive({
  ignoreHeaderFooter: true,
  headerHeightMm: 20,
  footerHeightMm: 20,
  ignoreCase: true,
  ignoredSymbols: '_＿'
})

const doUploadCompare = async () => {
  const f1 = oldInput.value?.files?.[0]
  const f2 = newInput.value?.files?.[0]
  if (!f1 || !f2) {
    ElMessage.warning('请先选择两个文件（支持 PDF/Word/Excel）')
    return
  }
  const fd = new FormData()
  fd.append('oldFile', f1)
  fd.append('newFile', f2)
  // 添加原始文件名
  fd.append('oldFileName', f1.name)
  fd.append('newFileName', f2.name)
  fd.append('ignoreHeaderFooter', String(settings.ignoreHeaderFooter))
  fd.append('headerHeightMm', String(settings.headerHeightMm))
  fd.append('footerHeightMm', String(settings.footerHeightMm))
  fd.append('ignoreCase', String(settings.ignoreCase))
  fd.append('ignoredSymbols', settings.ignoredSymbols || '')
  loading.value = true
  try {
    // 先进入结果页占位，显示等待动效
    router.push({ 
      name: 'CompareResult', 
      params: { id: 'pending' },
      query: { oldFileName: f1.name, newFileName: f2.name }
    }).catch(() => {})

    const res = await uploadCompare(fd)
    const id = res.data.id || ''
    if (id) {
      sessionStorage.setItem('lastCompareId', id)
      // 使用 replace 替换为真实 id，避免历史多一条
      router.replace({ 
        name: 'CompareResult', 
        params: { id },
        query: {
          oldFileName: f1.name,
          newFileName: f2.name
        }
      }).catch(() => {})
    }
    ElMessage.success('比对完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '比对失败')
  } finally {
    loading.value = false
  }
}

const doUrlCompare = async () => {
  if (!oldUrl.value || !newUrl.value) {
    ElMessage.warning('请填写两个URL')
    return
  }
  loading.value = true
  try {
    // 先进入结果页占位，显示等待动效
    router.push({ 
      name: 'CompareResult', 
      params: { id: 'pending' },
      query: { 
        oldFileName: oldUrl.value,
        newFileName: newUrl.value
      }
    }).catch(() => {})

    const res = await compareByUrls({
      oldUrl: oldUrl.value,
      newUrl: newUrl.value,
      ignoreHeaderFooter: settings.ignoreHeaderFooter,
      headerHeightMm: settings.headerHeightMm,
      footerHeightMm: settings.footerHeightMm,
      ignoreCase: settings.ignoreCase,
      ignoredSymbols: settings.ignoredSymbols
    })
    const id = res.data.id || ''
    if (id) {
      sessionStorage.setItem('lastCompareId', id)
      // 从URL中提取文件名
      const getFileNameFromUrl = (url: string) => {
        try {
          const fileName = url.split('/').pop()?.split('?')[0] || url
          return decodeURIComponent(fileName)
        } catch {
          return url
        }
      }
      
      router.replace({ 
        name: 'CompareResult', 
        params: { id },
        query: {
          oldFileName: getFileNameFromUrl(oldUrl.value),
          newFileName: getFileNameFromUrl(newUrl.value)
        }
      }).catch(() => {})
    }
    ElMessage.success('比对完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '比对失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.compare-page { padding: 16px; }
.mb12 { margin-bottom: 12px; }
/* 统一头部样式 */
.page-header-card { 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  overflow: hidden;
  transition: all 0.3s ease;
}
.page-header-card:hover { box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); }
.page-header { 
  padding: 16px 20px; 
  position: relative; 
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
}
.header-content { position: relative; z-index: 2; }
.header-decoration { 
  position: absolute; 
  top: 0; 
  right: 0; 
  width: 150px; 
  height: 100%; 
  background: linear-gradient(135deg, transparent, var(--el-color-primary-light-5)); 
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}
.page-header h2 { 
  margin: 0; 
  font-size: 26px; 
  color: var(--el-color-primary-dark-2); 
  display: flex; 
  align-items: center;
  font-weight: 600;
}
.header-icon { 
  margin-right: 10px; 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.page-header p { 
  margin: 10px 0 0; 
  color: #606266; 
  font-size: 15px; 
  max-width: 80%;
}
</style>


