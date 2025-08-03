<template>
  <div class="pdf-extractor">
    <el-card class="pdf-extractor-card">
      <template #header>
        <div class="card-header">
          <h3>PDF文本抽取</h3>
          <span class="subtitle">上传PDF文件，自动提取文本内容</span>
        </div>
      </template>
      
      <el-upload
        class="upload-area"
        drag
        action="#"
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept=".pdf"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽PDF文件到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持PDF文件，大小不超过10MB
          </div>
        </template>
      </el-upload>
      
      <div v-if="selectedFile" class="file-info">
        <span>已选择文件: {{ selectedFile.name }}</span>
        <span>({{ formatFileSize(selectedFile.size) }})</span>
        <el-button type="primary" @click="uploadFile" :loading="uploading">
          开始抽取
        </el-button>
      </div>
      
      <div v-if="extracting" class="extracting-status">
        <el-progress 
          :percentage="extractingProgress" 
          :status="extractingStatus"
          :stroke-width="10"
        ></el-progress>
        <div class="status-text">{{ statusText }}</div>
      </div>
      
      <div v-if="extractedText" class="result-area">
        <div class="result-header">
          <h4>抽取结果</h4>
          <el-button type="primary" size="small" @click="copyText">
            复制文本
          </el-button>
        </div>
        <el-input
          v-model="extractedText"
          type="textarea"
          :rows="10"
          readonly
          resize="none"
          class="result-text"
        ></el-input>
      </div>
      
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
import { UploadFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { aiPdf } from '@/api/ai';

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

// 文件选择处理
const handleFileChange = (file: any) => {
  // 检查文件类型
  if (file.raw.type !== 'application/pdf') {
    ElMessage.error('只能上传PDF文件');
    return false;
  }
  
  // 检查文件大小
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB');
    return false;
  }
  
  selectedFile.value = file.raw;
  error.value = '';
  extractedText.value = '';
  return false;
};

// 上传文件
const uploadFile = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请先选择文件');
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
    const response = await aiPdf.extractText(selectedFile.value);
    
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
      
      const response = await aiPdf.getTaskStatus(taskId.value);
      
      if (response.success && response.task) {
        const task = response.task;
        
        if (task.status === 'processing') {
          extractingProgress.value = Math.min(70, extractingProgress.value + 5);
          statusText.value = '正在抽取文本...';
        } else if (task.status === 'completed') {
          extractingProgress.value = 100;
          extractingStatus.value = 'success';
          statusText.value = '抽取完成';
          extractedText.value = task.result;
          clearStatusCheck();
        } else if (task.status === 'failed') {
          extractingStatus.value = 'exception';
          statusText.value = '抽取失败';
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
.pdf-extractor {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.pdf-extractor-card {
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

.file-info {
  display: flex;
  align-items: center;
  margin: 15px 0;
  gap: 10px;
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

.result-text {
  margin-top: 10px;
  font-family: monospace;
}

.error-message {
  margin-top: 20px;
}
</style>