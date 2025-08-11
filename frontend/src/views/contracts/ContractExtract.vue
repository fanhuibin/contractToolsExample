<template>
  <div class="contract-extract-page">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="page-header-card">
          <div class="page-header">
            <h2>合同信息提取</h2>
            <p>上传合同文件，利用AI自动提取关键信息。您可以自定义需要提取的字段。</p>
            <div class="actions">
              <el-button type="primary" @click="goRuleSettings">提取规则设置</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="main-content">
      <el-col :span="24">
        <ContractExtractor ref="extractorRef" />
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="24">
        <el-collapse v-model="activeCollapse">
          <el-collapse-item name="1">
            <template #title>
              <div class="collapse-title">
                <el-icon><InfoFilled /></el-icon>
                <span style="margin-left: 5px;">功能说明与使用提示</span>
              </div>
            </template>
            <div class="info-content">
              <h4>支持的文件格式</h4>
              <p>PDF、Word文档(.doc, .docx)、Excel表格(.xls, .xlsx)、图片(.jpg, .jpeg, .png)</p>
              
              <h4>文件大小限制</h4>
              <p>单个文件不超过30MB</p>
              
              <h4>使用提示</h4>
              <ul>
                <li>您可以通过下方的推荐标签或手动输入来添加、删除需要提取的字段。</li>
                <li>文件内容越清晰、扫描件质量越高，提取效果越好。</li>
                <li>处理时间与文件大小和复杂度相关，请耐心等待。</li>
                <li>提取结果可一键复制或导出为文本文件。</li>
              </ul>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import ContractExtractor from '@/components/ai/ContractExtractor.vue';
import { InfoFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus'

const activeCollapse = ref('');
const extractorRef = ref<any>(null)

function goRuleSettings() {
  const inst = extractorRef.value
  if (inst && typeof inst.openRuleSettingsForSelectedTemplate === 'function') {
    inst.openRuleSettingsForSelectedTemplate()
  } else {
    ElMessage.warning('请先在右侧选择一个模板，再进入规则设置')
  }
}
</script>

<style scoped>
.contract-extract-page {
  padding: 20px;
}

.page-header-card {
  margin-bottom: 20px;
  border-left: 5px solid var(--el-color-primary);
}

.page-header {
  padding: 5px 0;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 10px 0 0;
  color: #606266;
  font-size: 14px;
}

.main-content {
  margin-bottom: 20px;
}

.collapse-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  color: #303133;
}

.info-content {
  padding: 0 15px;
}

.info-content h4 {
  margin: 15px 0 10px;
  font-size: 16px;
  color: #303133;
}

.info-content p {
  margin: 5px 0;
  color: #606266;
}

.info-content ul {
  margin: 5px 0;
  padding-left: 20px;
  color: #606266;
}

.info-content li {
  margin: 5px 0;
}
</style>
