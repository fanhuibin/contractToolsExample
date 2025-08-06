<template>
  <div class="contract-extractor">
    <el-card class="contract-extractor-card">
      <template #header>
        <div class="card-header">
          <h3>合同信息提取</h3>
          <span class="subtitle">上传合同文件，可自定义提取字段</span>
        </div>
      </template>

      <!-- 文件上传区域 -->
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
        <template #tip>
          <div class="el-upload__tip">
            支持PDF、Word、Excel、图片等格式，大小不超过30MB
          </div>
        </template>
      </el-upload>
      
      <!-- 文件和提取操作 -->
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

      <!-- 动态提取字段区域 -->
      <div v-if="showPromptInput" class="dynamic-prompts">
        <h4>自定义提取字段</h4>
        <div v-for="(item, index) in extractionFields" :key="index" class="prompt-item">
          <el-input
            v-model="item.value"
            placeholder="请输入要提取的字段名称"
            clearable
          ></el-input>
          <el-button 
            type="danger" 
            :icon="Delete" 
            @click="removeField(index)" 
            circle 
            :disabled="extractionFields.length <= 1"
          />
        </div>
        <el-button type="primary" :icon="Plus" @click="addField" plain>添加字段</el-button>
        
        <div class="recommended-tags">
          <span>推荐字段：</span>
          <el-tag 
            v-for="tag in recommendedTags" 
            :key="tag" 
            @click="addTagAsField(tag)"
            class="tag-item"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 提取状态 -->
      <div v-if="extracting" class="extracting-status">
        <el-progress 
          :percentage="extractingProgress" 
          :status="extractingStatus"
          :stroke-width="10"
        ></el-progress>
        <div class="status-text">{{ statusText }}</div>
      </div>
      
      <!-- 提取结果 -->
      <div v-if="extractedText" class="result-area">
        <div class="result-header">
          <h4>提取结果</h4>
          <div class="result-actions">
            <el-button type="primary" size="small" @click="copyText">
              复制文本
            </el-button>
            <el-button type="success" size="small" @click="exportText">
              导出结果
            </el-button>
          </div>
        </div>
        <el-input
          v-model="extractedText"
          type="textarea"
          :rows="12"
          readonly
          resize="none"
          class="result-text"
        ></el-input>
      </div>
      
      <!-- 错误信息 -->
      <div v-if="error" class="error-message">
        <el-alert
          :title="error"
          type="error"
          :closable="false"
          show-icon
        ></el-alert>
      </div>
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed } from 'vue';
import { 
  UploadFilled, 
  Delete, 
  Plus, 
  Document,
  CaretRight
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { aiContract } from '@/api/ai';
// 文件相关
const selectedFile = ref<File | null>(null);
const uploading = ref(false);
const extracting = ref(false);
const extractingProgress = ref(0);
const extractingStatus = ref('');
const statusText = ref('');
const extractedText = ref('');
const error = ref('');
const taskId = ref('');
const statusCheckInterval = ref<number | null>(null);
const showPromptInput = ref(false);
// 动态提取字段
const extractionFields = ref([{ value: '合同名称' }]);
const recommendedTags = ref([
  '合同双方信息（甲方、乙方）',
  '合同金额',
  '签订日期',
  '合同期限',
  '付款方式',
  '其他关键条款'
]);
const canStartExtraction = computed(() => {
  return extractionFields.value.every(field => field.value.trim() !== '') && !uploading.value;
});
const addField = () => {
  extractionFields.value.push({ value: '' });
};
const removeField = (index: number) => {
  if (extractionFields.value.length > 1) {
    extractionFields.value.splice(index, 1);
  }
};
const addTagAsField = (tag: string) => {
  // 避免添加重复字段
  if (!extractionFields.value.some(field => field.value === tag)) {
    // 如果第一个字段是默认的空字段，则直接替换
    if (extractionFields.value.length === 1 && extractionFields.value[0].value === '') {
      extractionFields.value[0].value = tag;
    } else {
      extractionFields.value.push({ value: tag });
    }
  }
};
// 组合最终的提取提示
const finalExtractPrompt = computed(() => {
  const fields = extractionFields.value
    .map(field => field.value.trim())
    .filter(value => value);
  if (fields.length === 0) return '';
  return `请提取以下信息：${fields.join('、')}`;
});
// 文件选择处理
const handleFileChange = (file: any) => {
  // 检查文件类型
  const supportedTypes = [
    'application/pdf', 
    'application/msword', 
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'image/jpeg',
    'image/png'
  ];
  
  const fileName = file.name.toLowerCase();
  const fileExt = fileName.substring(fileName.lastIndexOf('.'));
  const supportedExts = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.jpg', '.jpeg', '.png'];
  
  if (!supportedExts.some(ext => fileExt === ext)) {
    ElMessage.error('不支持的文件格式');
    return false;
  }
  
  // 检查文件大小
  const isLt30M = file.size / 1024 / 1024 < 30;
  if (!isLt30M) {
    ElMessage.error('文件大小不能超过30MB');
    return false;
  }
  
  selectedFile.value = file.raw;
  error.value = '';
  extractedText.value = '';
  showPromptInput.value = true;
  return false;
};
// 上传文件
const uploadFile = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请先选择文件');
    return;
  }
  
  if (!canStartExtraction.value) {
    ElMessage.error('请确保所有提取字段都已填写');
    return;
  }
  try {
    uploading.value = true;
    error.value = '';
    extractedText.value = '';
    extracting.value = true;
    extractingProgress.value = 10;
    extractingStatus.value = '';
    statusText.value = '正在上传文件...';
    
    // 上传文件
    const response = await aiContract.extractInfo(selectedFile.value, finalExtractPrompt.value);
    
    if (response.success) {
      taskId.value = response.taskId;
      statusText.value = '正在处理文件...';
      extractingProgress.value = 30;
      
      // 开始轮询任务状态
      startStatusCheck();
    } else {
      error.value = response.message || '上传失败';
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
// 开始轮询任务状态
const startStatusCheck = () => {
  if (statusCheckInterval.value) {
    clearInterval(statusCheckInterval.value);
  }
  
  statusCheckInterval.value = window.setInterval(async () => {
    try {
      if (!taskId.value) {
        clearStatusCheck();
        return;
      }
      
      const response = await aiContract.getTaskStatus(taskId.value);
      
      if (response.success && response.task) {
        const task = response.task;
        
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
      } else {
        error.value = response.message || '获取任务状态失败';
        extractingStatus.value = 'exception';
        clearStatusCheck();
      }
    } catch (err: any) {
      error.value = err.message || '检查任务状态时发生错误';
      extractingStatus.value = 'exception';
      clearStatusCheck();
    }
  }, 2000);
};
// 清除状态检查
const clearStatusCheck = () => {
  if (statusCheckInterval.value) {
    clearInterval(statusCheckInterval.value);
    statusCheckInterval.value = null;
  }
};
// 复制文本
const copyText = () => {
  if (!extractedText.value) {
    return;
  }
  
  navigator.clipboard.writeText(extractedText.value)
    .then(() => {
      ElMessage.success('文本已复制到剪贴板');
    })
    .catch(() => {
      ElMessage.error('复制失败，请手动复制');
    });
};
// 导出文本
const exportText = () => {
  if (!extractedText.value || !selectedFile.value) {
    return;
  }
  
  const fileName = selectedFile.value.name.split('.')[0];
  const blob = new Blob([extractedText.value], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${fileName}_提取结果.txt`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  ElMessage.success('导出成功');
};
// 格式化文件大小
const formatFileSize = (size: number) => {
  if (size < 1024) {
    return size + ' B';
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB';
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
  }
};
</script>

<style scoped>
.contract-extractor {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}
.contract-extractor-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  flex-direction: column;
}
.card-header h3 {
  margin: 0;
  font-size: 18px;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
.upload-area {
  margin: 20px 0;
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
.dynamic-prompts {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
.dynamic-prompts h4 {
  margin: 0 0 15px 0;
}
.prompt-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.recommended-tags {
  margin-top: 15px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.tag-item {
  cursor: pointer;
  transition: all 0.2s;
}
.tag-item:hover {
  opacity: 0.8;
}
.extracting-status {
  margin: 20px 0;
}
.status-text {
  margin-top: 10px;
  text-align: center;
  color: #606266;
}
.result-area {
  margin-top: 20px;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.result-header h4 {
  margin: 0;
  font-size: 16px;
}
.result-actions {
  display: flex;
  gap: 10px;
}
.result-text {
  margin-top: 10px;
  font-family: monospace;
}
.error-message {
  margin-top: 20px;
}
</style>
