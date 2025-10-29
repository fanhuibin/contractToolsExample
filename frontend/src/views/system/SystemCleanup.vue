<template>
  <div class="system-cleanup-container">
    <el-card class="warning-card">
      <template #header>
        <div class="card-header">
          <el-icon style="color: #e6a23c; margin-right: 8px"><WarningFilled /></el-icon>
          <span style="color: #e6a23c; font-weight: bold">系统文件清理 - 重要提示</span>
        </div>
      </template>
      <el-alert
        title="本系统仅为组件工具集，不负责业务数据长期保存"
        type="warning"
        :closable="false"
        show-icon>
        <template #default>
          <p style="margin: 8px 0"><strong>使用前必读：</strong></p>
          <ul style="margin: 8px 0; padding-left: 20px">
            <li><strong>数据备份责任：</strong>业务端请自行做好数据保存和备份工作</li>
            <li><strong>不可恢复：</strong>删除操作不可逆，被删除的文件和数据库记录无法恢复</li>
            <li><strong>建议操作：</strong>请先使用"预览清理"功能查看将要删除的数据</li>
            <li><strong>清理目的：</strong>用于定期清理历史数据，释放磁盘空间，避免系统运行时间过长积累过多数据</li>
            <li><strong>✅ 模板安全：</strong>清理功能<strong>不会删除任何用户设计的模板</strong>（规则抽取模板、合同合成模板）</li>
          </ul>
        </template>
      </el-alert>
    </el-card>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>清理配置</span>
        </div>
      </template>

      <el-form :model="cleanupForm" label-width="120px" label-position="left" style="max-width: 600px">
        <!-- 清理模块 -->
        <el-form-item label="清理模块">
          <el-checkbox-group v-model="cleanupForm.modules">
            <el-checkbox label="rule-extract">智能文档抽取</el-checkbox>
            <el-checkbox label="compare-pro">智能文档比对</el-checkbox>
            <el-checkbox label="ocr-extract">智能文档解析</el-checkbox>
            <el-checkbox label="compose">智能合同合成</el-checkbox>
            <el-checkbox label="onlyoffice-demo">文档在线编辑</el-checkbox>
            <el-checkbox label="temp-uploads">临时上传目录</el-checkbox>
          </el-checkbox-group>
          <div style="margin-top: 8px; color: #67c23a; font-size: 12px">
            ✓ 模板目录（rule-extract-data/templates）会被自动保护，不会被清理
          </div>
        </el-form-item>

        <!-- 时间范围 -->
        <el-form-item label="清理时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            class="custom-date-picker">
          </el-date-picker>
          <div style="margin-top: 8px; color: #909399; font-size: 12px">
            只能选择今天之前的日期
          </div>
        </el-form-item>

        <!-- 清理选项 -->
        <el-form-item label="清理选项">
          <el-checkbox v-model="cleanupForm.cleanFileSystem">清理文件系统</el-checkbox>
          <el-checkbox v-model="cleanupForm.cleanDatabase">清理数据库记录</el-checkbox>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" @click="previewCleanup" :loading="previewing">
            <el-icon><View /></el-icon>
            预览清理
          </el-button>
          <el-button 
            type="danger" 
            @click="showExecuteConfirm" 
            :loading="executing"
            :disabled="!hasPreviewResult">
            <el-icon><Delete /></el-icon>
            执行清理
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预览结果 -->
    <el-card v-if="previewResult" class="result-card">
      <template #header>
        <div class="card-header">
          <el-icon style="margin-right: 8px"><Document /></el-icon>
          <span>清理预览结果</span>
          <el-tag :type="previewResult.success ? 'success' : 'danger'" style="margin-left: 12px">
            {{ previewResult.success ? '预览成功' : '预览失败' }}
          </el-tag>
        </div>
      </template>

      <div v-if="previewResult.success">
        <!-- 统计信息 -->
        <div class="stats-grid">
          <el-statistic 
            title="文件数量" 
            :value="previewResult.fileSystemStat.deletedFiles"
            suffix="个">
            <template #prefix>
              <el-icon><Files /></el-icon>
            </template>
          </el-statistic>
          
          <el-statistic 
            title="文件大小" 
            :value="(previewResult.fileSystemStat.deletedSize / 1024 / 1024).toFixed(2)"
            suffix="MB">
            <template #prefix>
              <el-icon><FolderOpened /></el-icon>
            </template>
          </el-statistic>
          
          <el-statistic 
            title="目录数量" 
            :value="previewResult.fileSystemStat.deletedDirs"
            suffix="个">
            <template #prefix>
              <el-icon><Folder /></el-icon>
            </template>
          </el-statistic>
          
          <el-statistic 
            title="数据库记录" 
            :value="previewResult.databaseStat.deletedRecords"
            suffix="条">
            <template #prefix>
              <el-icon><Coin /></el-icon>
            </template>
          </el-statistic>
        </div>

        <!-- 各模块统计 -->
        <el-divider>各模块清理详情</el-divider>
        <el-table :data="moduleStatsList" style="width: 100%">
          <el-table-column prop="moduleName" label="模块名称" width="150">
            <template #default="{ row }">
              <el-tag>{{ getModuleName(row.moduleName) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileCount" label="文件数量" width="120">
            <template #default="{ row }">
              {{ row.fileCount }} 个
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="文件大小" width="120">
            <template #default="{ row }">
              {{ (row.fileSize / 1024 / 1024).toFixed(2) }} MB
            </template>
          </el-table-column>
          <el-table-column prop="dirCount" label="目录数量" width="120">
            <template #default="{ row }">
              {{ row.dirCount }} 个
            </template>
          </el-table-column>
        </el-table>

        <!-- 清理日志 -->
        <el-divider>清理日志</el-divider>
        <div class="log-container">
          <div v-for="(log, index) in previewResult.logs" :key="index" class="log-item">
            <el-icon style="color: #409eff"><InfoFilled /></el-icon>
            {{ log }}
          </div>
        </div>
      </div>

      <!-- 错误信息 -->
      <el-alert
        v-if="!previewResult.success"
        :title="previewResult.message"
        type="error"
        :closable="false">
        <template #default>
          <div v-for="(error, index) in previewResult.errors" :key="index">
            {{ error }}
          </div>
        </template>
      </el-alert>
    </el-card>

    <!-- 执行结果 -->
    <el-card v-if="executeResult" class="result-card">
      <template #header>
        <div class="card-header">
          <el-icon style="margin-right: 8px"><CircleCheck /></el-icon>
          <span>清理执行结果</span>
          <el-tag :type="executeResult.success ? 'success' : 'danger'" style="margin-left: 12px">
            {{ executeResult.success ? '清理成功' : '清理失败' }}
          </el-tag>
        </div>
      </template>

      <el-result
        :icon="executeResult.success ? 'success' : 'error'"
        :title="executeResult.message"
        :sub-title="`耗时: ${executeResult.duration}ms`">
        <template #extra>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="删除文件">
              {{ executeResult.fileSystemStat.deletedFiles }} 个
            </el-descriptions-item>
            <el-descriptions-item label="释放空间">
              {{ (executeResult.fileSystemStat.deletedSize / 1024 / 1024).toFixed(2) }} MB
            </el-descriptions-item>
            <el-descriptions-item label="删除目录">
              {{ executeResult.fileSystemStat.deletedDirs }} 个
            </el-descriptions-item>
            <el-descriptions-item label="数据库记录">
              {{ executeResult.databaseStat.deletedRecords }} 条
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>

      <!-- 清理日志 -->
      <el-divider>清理日志</el-divider>
      <div class="log-container">
        <div v-for="(log, index) in executeResult.logs" :key="index" class="log-item">
          <el-icon style="color: #67c23a"><SuccessFilled /></el-icon>
          {{ log }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  WarningFilled, 
  View, 
  Delete, 
  Document,
  Files,
  FolderOpened,
  Folder,
  Coin,
  InfoFilled,
  CircleCheck,
  SuccessFilled
} from '@element-plus/icons-vue';
import axios from 'axios';

// 表单数据
const cleanupForm = ref({
  modules: [],
  cleanFileSystem: true,
  cleanDatabase: true
});

const dateRange = ref([]);
const previewing = ref(false);
const executing = ref(false);
const previewResult = ref(null);
const executeResult = ref(null);
const hasPreviewResult = ref(false);

// 模块名称映射（与导航栏命名保持一致）
const moduleNameMap = {
  'rule-extract': '智能文档抽取',
  'compare-pro': '智能文档比对',
  'ocr-extract': '智能文档解析',
  'compose': '智能合同合成',
  'onlyoffice-demo': '文档在线编辑',
  'temp-uploads': '临时上传目录'
};

const getModuleName = (module) => {
  return moduleNameMap[module] || module;
};

// 模块统计列表
const moduleStatsList = computed(() => {
  if (!previewResult.value || !previewResult.value.moduleStats) {
    return [];
  }
  return Object.values(previewResult.value.moduleStats);
});

// 禁用未来日期
const disabledDate = (time) => {
  return time.getTime() > Date.now();
};

// 预览清理
const previewCleanup = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择清理时间范围');
    return;
  }

  previewing.value = true;
  previewResult.value = null;
  executeResult.value = null;
  hasPreviewResult.value = false;

  try {
    const response = await axios.post('/api/system/cleanup/preview', {
      modules: cleanupForm.value.modules.length > 0 ? cleanupForm.value.modules : null,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      cleanFileSystem: cleanupForm.value.cleanFileSystem,
      cleanDatabase: cleanupForm.value.cleanDatabase
    });

    if (response.data.success) {
      previewResult.value = response.data.data;
      hasPreviewResult.value = true;
      ElMessage.success('预览完成');
    } else {
      ElMessage.error('预览失败: ' + response.data.message);
    }
  } catch (error) {
    console.error('预览清理失败', error);
    ElMessage.error('预览清理失败: ' + (error.response?.data?.message || error.message));
  } finally {
    previewing.value = false;
  }
};

// 显示执行确认对话框
const showExecuteConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      `即将删除 ${previewResult.value.fileSystemStat.deletedFiles} 个文件 (${(previewResult.value.fileSystemStat.deletedSize / 1024 / 1024).toFixed(2)} MB) 和 ${previewResult.value.databaseStat.deletedRecords} 条数据库记录。此操作不可逆，确定要执行吗？`,
      '确认清理',
      {
        confirmButtonText: '确认清理',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    );
    executeCleanup();
  } catch {
    ElMessage.info('已取消清理');
  }
};

// 执行清理
const executeCleanup = async () => {
  executing.value = true;
  executeResult.value = null;

  try {
    const response = await axios.post('/api/system/cleanup/execute', {
      modules: cleanupForm.value.modules.length > 0 ? cleanupForm.value.modules : null,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      cleanFileSystem: cleanupForm.value.cleanFileSystem,
      cleanDatabase: cleanupForm.value.cleanDatabase,
      confirmed: true
    });

    if (response.data.success) {
      executeResult.value = response.data.data;
      if (executeResult.value.success) {
        ElMessage.success('清理成功');
        // 清空预览结果
        previewResult.value = null;
        hasPreviewResult.value = false;
      } else {
        ElMessage.error('清理失败: ' + executeResult.value.message);
      }
    } else {
      ElMessage.error('清理失败: ' + response.data.message);
    }
  } catch (error) {
    console.error('执行清理失败', error);
    ElMessage.error('执行清理失败: ' + (error.response?.data?.message || error.message));
  } finally {
    executing.value = false;
  }
};
</script>

<style scoped>
.system-cleanup-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.warning-card {
  margin-bottom: 20px;
}

.config-card {
  margin-bottom: 20px;
}

.result-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: #606266;
}

:deep(.el-checkbox) {
  margin-right: 20px;
  margin-bottom: 10px;
}

:deep(.custom-date-picker) {
  width: 260px !important;
}

:deep(.custom-date-picker .el-range-editor) {
  width: 260px !important;
}

:deep(.custom-date-picker .el-range-input) {
  width: 80px !important;
}

:deep(.custom-date-picker .el-range-separator) {
  padding: 0 5px;
  width: auto;
}
</style>

