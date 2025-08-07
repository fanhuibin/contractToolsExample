<template>
  <div class="fulfillment-task">
    <el-row :gutter="20">
      <!-- Main Content -->
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <h3>合同履约任务识别</h3>
                <span class="subtitle">请先从右侧选择模板，然后上传合同文件</span>
              </div>
              <el-button type="primary" link @click="showHistoryDialog">识别记录</el-button>
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
            <el-button type="primary" @click="extractTask" :loading="loading" :disabled="!canStartExtraction">
              <el-icon style="margin-right: 5px;"><CaretRight /></el-icon>
              开始识别
            </el-button>
          </div>

          <!-- Extraction Status -->
          <div v-if="loading" class="extracting-status">
            <el-progress :percentage="extractingProgress" :status="extractingStatus" :stroke-width="10" />
            <div class="status-text">{{ statusText }}</div>
          </div>

          <!-- Extraction Result -->
          <div v-if="result.length > 0" class="result-area">
            <div class="result-header">
              <h4>识别结果</h4>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="exportTable">导出Excel</el-button>
              </div>
            </div>
            <el-table :data="result" style="width: 100%">
              <el-table-column prop="contractName" label="合同名称" />
              <el-table-column prop="fulfillmentName" label="履约名称" />
              <el-table-column prop="dueDate" label="完成履约时间" />
              <el-table-column prop="fulfillmentMethod" label="履约方式" />
            </el-table>
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
            <h4>选择识别模板</h4>
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
                      @click.stop="copyTemplateForEditing(template)" 
                      class="copy-button"
                    >
                      复制并编辑
                    </el-button>
                  </div>
                </div>
                <div v-else class="no-template-tip">暂无该类型模板</div>
              </el-collapse-item>
            </el-collapse>
          </el-radio-group>
          <el-divider />
          <el-radio v-model="selectedTemplateId" label="custom" class="template-radio-item custom-radio">
            自定义配置
          </el-radio>
        </el-card>
      </el-col>
    </el-row>

    <!-- History Dialog -->
    <el-dialog v-model="historyDialogVisible" title="识别历史记录" width="60%">
      <el-table :data="historyList" style="width: 100%" height="400">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="extractTime" label="识别时间" width="200">
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
      <el-table :data="selectedHistoryResult" style="width: 100%">
        <el-table-column prop="contractName" label="合同名称" />
        <el-table-column prop="fulfillmentName" label="履约名称" />
        <el-table-column prop="dueDate" label="完成履约时间" />
        <el-table-column prop="fulfillmentMethod" label="履约方式" />
      </el-table>
      <template #footer>
        <el-button @click="historyDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Custom Edit Dialog -->
    <el-dialog v-model="customEditVisible" title="自定义配置编辑" width="50%">
      <el-form label-width="120px">
        <el-form-item label="模板名称">
          <el-input v-model="customTemplate.name" placeholder="输入模板名称" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="customTemplate.contractType" placeholder="选择合同类型">
            <el-option v-for="(label, type) in contractTypes" :key="type" :label="label" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-cascader
            v-model="customTemplate.taskTypes"
            :options="taskTypeTree"
            :props="{ multiple: true, checkStrictly: true, emitPath: false }"
            placeholder="选择任务类型"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-select v-model="customTemplate.keywords" multiple filterable allow-create placeholder="选择或输入关键词" style="width: 100%">
            <el-option v-for="item in keywordOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间规则关键字">
          <el-select v-model="customTemplate.timeRules" multiple filterable allow-create placeholder="选择或输入时间规则" style="width: 100%">
            <el-option-group v-for="group in timeRuleGroups" :key="group.label" :label="group.label">
              <el-option v-for="item in group.options" :key="item" :label="item" :value="item" />
            </el-option-group>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled, Document, CaretRight } from '@element-plus/icons-vue';
import ai from '@/api/ai';

const selectedFile = ref<File | null>(null);
const loading = ref(false);
const extractingProgress = ref(0);
const extractingStatus = ref('');
const statusText = ref('');
const error = ref('');
const result = ref<any[]>([]);
const historyDialogVisible = ref(false);
const historyDetailDialogVisible = ref(false);
const historyList = ref([]);
const selectedHistoryResult = ref<any[]>([]);
const customEditVisible = ref(false);
const customTemplate = ref({
  name: '',
  contractType: '',
  taskTypes: [],
  keywords: [],
  timeRules: []
});

const contractTypes = ref<Record<string, string>>({});
const templates = ref<any[]>([]);
const selectedTemplateId = ref<number | 'custom' | null>(null);
const activeCollapse = ref<string>('common');
const taskTypeTree = ref<any[]>([]);  // 树形任务类型数据
const keywordOptions = ref<string[]>([]);
const timeRuleGroups = ref<any[]>([]);  // 分组时间规则

onMounted(async () => {
  await loadInitialData();
});

async function loadInitialData() {
  const [typesRes, templatesRes, historyRes] = await Promise.all([
    ai.getFulfillmentContractTypes(),
    ai.getFulfillmentTemplates(),
    ai.getFulfillmentHistory()
  ]);
  contractTypes.value = typesRes.data || {};
  templates.value = templatesRes.data || [];
  historyList.value = historyRes.data || [];

  // 初始化树形任务类型（基于用户需求的分层结构）
  taskTypeTree.value = [
    { value: '开票履约', label: '开票履约', children: [
      { value: '预付款开票', label: '预付款开票' },
      { value: '进度款开票', label: '进度款开票' },
      { value: '验收款开票', label: '验收款开票' }
    ] },
    { value: '付款履约', label: '付款履约', children: [
      { value: '应付任务-预付款', label: '应付任务-预付款' },
      { value: '应付任务-进度款', label: '应付任务-进度款' },
      { value: '应付任务-尾款', label: '应付任务-尾款' }
    ] },
    { value: '收款履约', label: '收款履约', children: [
      { value: '应收任务-预收款', label: '应收任务-预收款' },
      { value: '应收任务-尾款', label: '应收任务-尾款' }
    ] },
    { value: '到期提醒', label: '到期提醒', children: [
      { value: '合同到期提醒', label: '合同到期提醒' },
      { value: '服务到期提醒', label: '服务到期提醒' }
    ] },
    { value: '事件触发', label: '事件触发', children: [
      { value: '验收合格提醒', label: '验收合格提醒' },
      { value: '货物送达提醒', label: '货物送达提醒' }
    ] }
  ];

  // 初始化关键词选项（基于任务类型动态）
  keywordOptions.value = ['预付款', '首笔款', '合同生效', '验收报告', '交付完成', '支付', '付款', '期限届满', '到期日', '当...时', '经确认后'];

  // 初始化时间规则分组
  timeRuleGroups.value = [
    { label: '绝对时间规则', options: ['截止至', '不晚于', '在...之前', '于...日前'] },
    { label: '相对时间规则', options: ['签署后', '生效后', '之日起', '验收合格后', '收到款项后', '个自然日', '个工作日内', '个月内'] },
    { label: '复合规则', options: ['且不得晚于', '或最迟不超过', '以较早者为准'] },
    { label: '特殊规则', options: ['遇节假日顺延', '包含法定节假日', '按自然月计算'] }
  ];

  // 设置默认模板
  const defaultTemplate = templates.value.find(t => t.isDefault);
  if (defaultTemplate) {
    selectedTemplateId.value = defaultTemplate.id;
  }
}

const canStartExtraction = computed(() => selectedFile.value && selectedTemplateId.value !== null);

function getTemplatesByType(type: string) {
  return templates.value.filter(t => t.contractType === type);
}

function handleFileChange(file: any) {
  selectedFile.value = file.raw;
  error.value = '';
  result.value = [];
}

function copyTemplateForEditing(template: any) {
  customTemplate.value = { ...template, id: undefined };  // 复制为新模板
  customEditVisible.value = true;
}

async function saveCustomTemplate() {
  try {
    const res = await ai.createFulfillmentTemplate(customTemplate.value);
    if (res.success) {
      ElMessage.success('模板保存成功');
      customEditVisible.value = false;
      await loadInitialData();
    }
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败');
  }
}

async function extractTask() {
  if (!canStartExtraction.value) return;
  loading.value = true;
  error.value = '';
  result.value = [];
  extractingProgress.value = 0;
  extractingStatus.value = '';
  statusText.value = '正在上传文件...';

  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value!);
    formData.append('templateId', selectedTemplateId.value?.toString() || 'custom');  // 修复null

    const res = await ai.extractFulfillmentTask(formData);
    if (res.success) {
      result.value = res.tasks || [];
      extractingProgress.value = 100;
      extractingStatus.value = 'success';
      statusText.value = '识别完成';
    } else {
      error.value = res.message || '识别失败';
      extractingStatus.value = 'exception';
    }
  } catch (e: any) {
    error.value = e.message || '识别失败';
    extractingStatus.value = 'exception';
  } finally {
    loading.value = false;
  }
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
}

function showHistoryDialog() {
  // 实现类似合同提取的历史记录
  historyDialogVisible.value = true;
}

function viewHistoryDetail(row: any) {
  selectedHistoryResult.value = row.tasks || [];
  historyDetailDialogVisible.value = true;
}

function exportTable() {
  // 实现表格导出为Excel
  ElMessage.info('导出功能待实现');
}

watch(selectedTemplateId, (newId) => {
  if (newId === 'custom') {
    customTemplate.value = { name: '', contractType: '', taskTypes: [], keywords: [], timeRules: [] };
    customEditVisible.value = true;
  }
});
</script>

<style scoped>
/* 复用合同提取页面的样式，保持一致 */
.fulfillment-task {
  padding: 20px;
}
.main-content-card, .template-selector-card {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header-left h3 {
  margin: 0;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
.upload-area {
  margin-bottom: 20px;
}
.file-info-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}
.extracting-status, .result-area, .error-message {
  margin-top: 20px;
}
.status-text {
  margin-top: 10px;
  text-align: center;
  color: #606266;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.template-radio-group {
  display: flex;
  flex-direction: column;
}
.template-radio-item {
  display: flex;
  height: 32px;
  align-items: center;
}
.template-item-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.copy-button {
  margin-left: 10px;
  flex-shrink: 0;
}
.template-tag {
  margin-left: 8px;
}
.custom-radio {
  margin-top: 10px;
}
.no-template-tip {
  color: #909399;
  font-size: 14px;
  padding: 10px;
}
:deep(.el-collapse-item__header) {
  font-size: 16px;
  font-weight: 500;
}
:deep(.el-collapse-item__content) {
  padding-bottom: 0;
}
</style>

