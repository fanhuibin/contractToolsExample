<template>
  <div class="contract-extractor">
    <el-row :gutter="20">
      <!-- Main Content -->
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
  <div class="card-header-left">
    <h3>合同信息提取</h3>
    <span class="subtitle">请先从右侧选择提取模板，然后上传合同文件</span>
  </div>
  <el-button type="primary" link @click="showHistoryDialog">提取记录</el-button>
</div>
</template>

<!-- File Upload Area -->
<el-upload
  class="upload-area"
  drag
  action="#"
  :auto-upload="false"
  :show-file-list="false"
  :on-change="handleFileChange"
  accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
>
  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
  <div class="el-upload__text">
    拖拽文件到此处，或 <em>点击上传</em>
  </div>
</el-upload>

<div v-if="selectedFile" class="file-info-actions">
  <div class="file-info">
    <el-icon><Document /></el-icon>
    <span>{{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})</span>
  </div>
  <el-button type="primary" @click="uploadFile" :loading="uploading" :disabled="!canStartExtraction">
    <el-icon style="margin-right: 5px;"><CaretRight /></el-icon>
    开始提取
  </el-button>
</div>

<!-- Fields Display Area -->
<div v-if="selectedTemplateId" class="fields-display-area">
  <el-divider content-position="left">提取字段</el-divider>
  
  <!-- Pre-defined template fields -->
  <div v-if="selectedTemplateId !== 'custom' && selectedTemplateFields.length > 0">
    <el-tag v-for="field in selectedTemplateFields" :key="field" class="field-tag" size="large">
      {{ field }}
    </el-tag>
  </div>
  
  <!-- Custom template fields editor -->
  <div v-if="selectedTemplateId === 'custom'" class="dynamic-prompts">
    <div v-for="(item, index) in extractionFields" :key="index" class="prompt-item">
      <el-input v-model="item.value" placeholder="请输入要提取的字段名称" clearable />
      <el-button type="danger" :icon="Delete" @click="removeField(index)" circle :disabled="extractionFields.length <= 1" />
    </div>
    <el-button type="primary" :icon="Plus" @click="addField" plain>添加字段</el-button>
    
    <!-- Save as Template -->
    <div class="save-as-template" v-if="extractionFields.length > 0 && extractionFields[0].value">
      <el-divider content-position="left">另存为模板</el-divider>
      <div class="save-template-form">
        <el-input v-model="newTemplateName" placeholder="输入新模板名称" style="width: 200px; margin-right: 10px;" />
        <el-select v-model="newTemplateType" placeholder="选择合同类型" style="width: 150px; margin-right: 10px;">
          <el-option v-for="(label, value) in contractTypes" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="success" :disabled="!canSaveTemplate" @click="saveAsTemplate">保存模板</el-button>
      </div>
    </div>
  </div>
</div>

<!-- Extraction Status -->
<div v-if="extracting" class="extracting-status">
  <el-progress :percentage="extractingProgress" :status="extractingStatus" :stroke-width="10" />
  <div class="status-text">{{ statusText }}</div>
</div>

<!-- Extraction Result -->
<div v-if="extractedText" class="result-area">
  <div class="result-header">
    <h4>提取结果</h4>
    <div class="result-actions">
      <el-button type="primary" size="small" @click="copyText">复制JSON</el-button>
      <el-button type="success" size="small" @click="exportText">导出TXT</el-button>
    </div>
  </div>
  <pre class="result-text">{{ formattedJsonResult }}</pre>
</div>

<!-- Error Message -->
<div v-if="error" class="error-message">
  <el-alert :title="error" type="error" :closable="false" show-icon />
</div>
</el-card>
</el-col>

<!-- Right Sidebar for Template Selection -->
<el-col :span="8">
<el-card class="template-selector-card">
  <template #header>
    <h4>选择提取模板</h4>
  </template>
  <el-radio-group v-model="selectedTemplateId" class="template-radio-group">
    <el-collapse v-model="activeCollapse" accordion>
      <el-collapse-item v-for="(label, type) in contractTypes" :key="type" :title="label" :name="type">
        <div v-if="getTemplatesByType(type).length > 0">
          <div v-for="template in getTemplatesByType(type)" :key="template.id" class="template-item-wrapper">
            <el-radio 
              :label="template.id"
              class="template-radio-item"
            >
              {{ template.name }}
              <el-tag v-if="template.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
              <el-tag v-if="template.type === 'system'" size="small" class="template-tag">系统</el-tag>
            </el-radio>
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click.stop="goRuleSettings(template.id)" 
              class="copy-button"
            >
              编辑该模板规则
            </el-button>
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click.stop="copyTemplateForEditing(template)" 
              class="copy-button"
            >
              复制并编辑
            </el-button>
            <el-button 
              type="danger" 
              link 
              size="small" 
              @click.stop="deleteTemplate(template)" 
              class="copy-button"
            >
              删除
            </el-button>
          </div>
        </div>
        <div v-else class="no-template-tip">暂无该类型模板</div>
      </el-collapse-item>
    </el-collapse>
  </el-radio-group>
  <el-divider />
  <el-radio v-model="selectedTemplateId" label="custom" class="template-radio-item custom-radio">
    自定义提取字段
  </el-radio>
</el-card>
</el-col>
</el-row>

<!-- History Dialog -->
<el-dialog v-model="historyDialogVisible" title="提取历史记录" width="60%">
<el-table :data="historyList" style="width: 100%" height="400">
  <el-table-column prop="fileName" label="文件名" />
  <el-table-column prop="extractTime" label="提取时间" width="200">
    <template #default="{ row }">
      {{ new Date(row.extractTime).toLocaleString() }}
    </template>
  </el-table-column>
  <el-table-column label="操作" width="120">
    <template #default="{ row }">
      <el-button type="primary" link @click="viewHistoryDetail(row)">查看详情</el-button>
    </template>
  </el-table-column>
</el-table>
</el-dialog>

<!-- History Detail Dialog -->
<el-dialog v-model="historyDetailDialogVisible" title="历史记录详情" width="50%">
<pre class="result-text">{{ formattedHistoryDetail }}</pre>
<template #footer>
  <el-button @click="historyDetailDialogVisible = false">关闭</el-button>
</template>
</el-dialog>
</div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import { UploadFilled, Delete, Plus, Document, CaretRight } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { aiContract } from '@/api/ai';
import { useRouter } from 'vue-router'
const router = useRouter()

// --- Reactive State ---
// File handling
const selectedFile = ref<File | null>(null);
const uploading = ref(false);
const extracting = ref(false);
const extractingProgress = ref(0);
const extractingStatus = ref < '' | 'success' | 'warning' | 'exception' > ('');
const statusText = ref('');
const extractedText = ref('');
const error = ref('');
const taskId = ref('');
const statusCheckInterval = ref<number | null>(null);

// Template handling
const contractTypes = ref<Record<string, string>>({});
const templates = ref<any[]>([]);
const selectedTemplateId = ref<number | 'custom' | null>(null);
const activeCollapse = ref<string>('common');
const selectedTemplateFields = ref<string[]>([]);

// Custom fields & new template
const extractionFields = ref([{ value: '' }]);
const newTemplateName = ref('');
const newTemplateType = ref('');
const userId = ref('default-user'); // Should be fetched from user session in a real app

// History
const historyDialogVisible = ref(false);
const historyDetailDialogVisible = ref(false);
const historyList = ref([]);
const selectedHistoryDetail = ref('');

// --- Computed Properties ---
const canStartExtraction = computed(() => {
  if (!selectedFile.value || uploading.value) return false;
  if (selectedTemplateId.value === 'custom') {
    return extractionFields.value.every(field => field.value.trim() !== '');
  }
  return selectedTemplateId.value !== null;
});

const canSaveTemplate = computed(() =>
  newTemplateName.value.trim() !== '' &&
  newTemplateType.value !== '' &&
  extractionFields.value.length > 0 &&
  extractionFields.value.every(field => field.value.trim() !== '')
);

const formattedJsonResult = computed(() => {
  try {
    const jsonObj = JSON.parse(extractedText.value);
    return JSON.stringify(jsonObj, null, 2);
  } catch (e) {
    return extractedText.value;
  }
});

const formattedHistoryDetail = computed(() => {
  try {
    const jsonObj = JSON.parse(selectedHistoryDetail.value);
    return JSON.stringify(jsonObj, null, 2);
  } catch (e) {
    return selectedHistoryDetail.value;
  }
});

// --- Functions ---
const getTemplatesByType = (type: string) => {
  return templates.value.filter(t => t.contractType === type);
};

const loadInitialData = async () => {
  try {
    const [typesResult, templatesResult] = await Promise.all([
      aiContract.getContractTypes(),
      aiContract.getTemplates(userId.value)
    ]);

    contractTypes.value = (typesResult as any)?.data || {};
    templates.value = (templatesResult as any)?.data || [];

    // Set default selection
    const defaultTemplate = templates.value.find(t => t.contractType === 'common' && t.isDefault);
    if (defaultTemplate) {
      selectedTemplateId.value = defaultTemplate.id;
      activeCollapse.value = 'common';
    }

  } catch (err) {
    ElMessage.error('加载模板数据失败');
    console.error(err);
  }
};
function goRuleSettings(id: number) {
  router.push(`/rule-settings?templateId=${id}`)
}

// 供父组件调用：根据当前选择跳转到规则页
function openRuleSettingsForSelectedTemplate() {
  if (typeof selectedTemplateId.value === 'number') {
    router.push(`/rule-settings?templateId=${selectedTemplateId.value}`)
  }
}

defineExpose({ openRuleSettingsForSelectedTemplate })

// History methods
const showHistoryDialog = async () => {
  try {
    const result = await aiContract.getHistory(userId.value);
    historyList.value = (result as any)?.data || [];
    historyDialogVisible.value = true;
  } catch (err: any) {
    ElMessage.error(err.message || '获取历史记录出错');
  }
};

const viewHistoryDetail = (historyItem: any) => {
  selectedHistoryDetail.value = historyItem.extractedContent;
  historyDetailDialogVisible.value = true;
};


// File handling methods
const handleFileChange = (file: any) => {
  const isLt100M = file.size / 1024 / 1024 < 100;
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过100MB');
    return false;
  }
  selectedFile.value = file.raw;
  error.value = '';
  extractedText.value = '';
  return false;
};

const uploadFile = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请先选择文件');
    return;
  }
  if (!canStartExtraction.value) {
    ElMessage.error('请选择一个模板或填写自定义字段');
    return;
  }

  uploading.value = true;
  error.value = '';
  extractedText.value = '';
  extracting.value = true;
  extractingProgress.value = 10;
  extractingStatus.value = '';
  statusText.value = '正在上传文件...';

  try {
    const prompt = selectedTemplateId.value === 'custom'
      ? `请提取以下信息：${extractionFields.value.map(f => f.value).join('、')}`
      : undefined;
    
    const templateId = typeof selectedTemplateId.value === 'number' ? selectedTemplateId.value : undefined;

    const resp: any = await aiContract.extractInfo(selectedFile.value, prompt, templateId);

    if (resp?.data?.taskId) {
      taskId.value = resp.data.taskId;
      statusText.value = '正在处理文件...';
      extractingProgress.value = 30;
      startStatusCheck();
    } else {
      error.value = resp?.message || '上传失败';
      extracting.value = false;
      extractingStatus.value = 'exception';
    }
  } catch (err: any) {
    error.value = err.message || '上传过程中发生错误';
    extracting.value = false;
    extractingStatus.value = 'exception';
  } finally {
    uploading.value = false;
  }
};

const startStatusCheck = () => {
  if (statusCheckInterval.value) clearInterval(statusCheckInterval.value);
  statusCheckInterval.value = window.setInterval(async () => {
    if (!taskId.value) return clearStatusCheck();
    try {
      const resp: any = await aiContract.getTaskStatus(taskId.value);
      const task = resp?.data?.task;
      if (!task) throw new Error('获取任务状态失败');
      if (task.status === 'processing') {
        extractingProgress.value = Math.min(70, extractingProgress.value + 5);
        statusText.value = '正在提取信息...';
      } else if (task.status === 'completed') {
        extractingProgress.value = 100;
        extractingStatus.value = 'success';
        statusText.value = '提取完成';
        extractedText.value = task.result;
        clearStatusCheck();
      } else if (task.status === 'failed') {
        extractingStatus.value = 'exception';
        statusText.value = '提取失败';
        error.value = task.error || '处理失败';
        clearStatusCheck();
      }
    } catch (err: any) {
      error.value = err.message || '检查任务状态时发生错误';
      extractingStatus.value = 'exception';
      clearStatusCheck();
    }
  }, 2000);
};

const clearStatusCheck = () => {
  if (statusCheckInterval.value) {
    clearInterval(statusCheckInterval.value);
    statusCheckInterval.value = null;
  }
};

const copyText = () => {
  if (!extractedText.value) return;
  navigator.clipboard.writeText(formattedJsonResult.value)
    .then(() => ElMessage.success('结果已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'));
};

const exportText = () => {
  if (!extractedText.value || !selectedFile.value) return;
  const fileName = selectedFile.value.name.split('.')[0];
  const blob = new Blob([extractedText.value], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${fileName}_提取结果.txt`;
  link.click();
  URL.revokeObjectURL(url);
};

const formatFileSize = (size: number) => {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
};

// Custom field methods
const addField = () => {
  extractionFields.value.push({ value: '' });
};

const removeField = (index: number) => {
  if (extractionFields.value.length > 1) {
    extractionFields.value.splice(index, 1);
  }
};

const copyTemplateForEditing = (template: any) => {
  // Switch to custom mode
  selectedTemplateId.value = 'custom';
  
  // Load fields into the editor
  try {
    const fields = JSON.parse(template.fields);
    extractionFields.value = fields.map((field: string) => ({ value: field }));
  } catch (e) {
    ElMessage.error('模板字段解析失败');
    extractionFields.value = [{ value: '' }];
  }
  
  // Pre-fill the save-as form
  newTemplateName.value = `${template.name} - 副本`;
  newTemplateType.value = template.contractType;

  ElMessage.success(`“${template.name}”已加载到编辑区`);
};

const saveAsTemplate = async () => {
  if (!canSaveTemplate.value) return;
  try {
    const fields = extractionFields.value.map(field => field.value.trim());
    const template = {
      name: newTemplateName.value,
      contractType: newTemplateType.value,
      fields: JSON.stringify(fields),
      type: 'user',
      creatorId: userId.value,
      isDefault: false,
      description: `用户自定义模板`
    };
    await aiContract.createTemplate(template);
    ElMessage.success('模板保存成功');
    newTemplateName.value = '';
    loadInitialData(); // Refresh templates
  } catch (err: any) {
    ElMessage.error(err.message || '保存模板出错');
  }
};

async function deleteTemplate(template: any) {
  try {
    await ElMessageBox.confirm(`确定删除模板“${template.name}”吗？此操作不可恢复。`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await aiContract.deleteTemplate(template.id)
    ElMessage.success('删除成功')
    await loadInitialData()
    if (typeof selectedTemplateId.value === 'number' && selectedTemplateId.value === template.id) {
      selectedTemplateId.value = null
      selectedTemplateFields.value = []
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

// --- Lifecycle & Watchers ---
onMounted(loadInitialData);

watch(selectedTemplateId, (newId) => {
  if (typeof newId === 'number') {
    const template = templates.value.find(t => t.id === newId);
    if (template) {
      selectedTemplateFields.value = JSON.parse(template.fields);
    }
  } else {
    selectedTemplateFields.value = [];
    if (extractionFields.value.length === 0 || (extractionFields.value.length === 1 && !extractionFields.value[0].value)) {
      extractionFields.value = [{ value: '' }];
    }
  }
});
</script>

<style scoped>
.contract-extractor { padding: 0; }

/* 对齐审核页卡片与阴影风格 */
.main-content-card, .template-selector-card { 
  height: 100%; 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  transition: all 0.3s ease;
}
.main-content-card:hover, .template-selector-card:hover { 
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); 
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header-left h3 { margin: 0; font-weight: 600; color: var(--el-color-primary-dark-2); }
.subtitle { font-size: 14px; color: #909399; margin-top: 5px; }

/* 上传区域对齐审核页 */
.upload-area { 
  margin-bottom: 20px; 
  border: 2px dashed var(--el-color-primary-light-5); 
  border-radius: 8px; 
  transition: all 0.3s ease; 
  background-color: var(--el-color-primary-light-9);
}
.upload-area:hover { border-color: var(--el-color-primary); background-color: var(--el-color-primary-light-8); }

.file-info-actions { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin: 20px 0; 
  padding: 15px; 
  border: 1px solid var(--el-color-primary-light-7); 
  border-radius: 8px; 
  background-color: var(--el-color-primary-light-9); 
  transition: all 0.3s ease;
}
.file-info-actions:hover { border-color: var(--el-color-primary-light-3); box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
.file-info { display: flex; align-items: center; gap: 12px; color: #606266; }

.fields-display-area { margin-top: 20px; }
.field-tag { margin: 4px; }
.dynamic-prompts { margin-top: 10px; }
.prompt-item { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.save-as-template { margin-top: 20px; }
.save-template-form { display: flex; align-items: center; margin-top: 10px; }
.extracting-status, .result-area, .error-message { margin-top: 20px; }
.status-text { margin-top: 10px; text-align: center; color: #606266; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.result-text { background-color: #f8f9fa; padding: 15px; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word; font-family: monospace; border: 1px solid #ebeef5; }

/* 右侧模板选择器对齐审核页 */
.template-radio-group { display: flex; flex-direction: column; gap: 10px; margin: 10px 0; }
.template-radio-item { display: flex; height: 32px; align-items: center; }
.template-radio-item { flex-grow: 1; }
.template-item-wrapper { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.copy-button { margin-left: 10px; flex-shrink: 0; }
.template-tag { margin-left: 8px; }
.custom-radio { margin-top: 10px; }
.no-template-tip { color: #909399; font-size: 14px; padding: 10px; }
::deep(.el-collapse-item__header) { font-size: 16px; font-weight: 500; }
::deep(.el-collapse-item__content) { padding-bottom: 0; }
</style>