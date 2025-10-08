<template>
  <div class="contract-extract-page">
    <PageHeader 
      title="合同信息提取"
      description="上传合同文件，利用AI自动提取关键信息。您可以自定义需要提取的字段。"
      :icon="Document"
    >
      <template #actions>
        <el-button type="primary" @click="goRuleSettings">提取规则设置</el-button>
      </template>
    </PageHeader>
    
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
import { InfoFilled, Document } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus'
import { PageHeader } from '@/components/common'

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
.contract-extract-page { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }

/* 页面头部：与审核页保持一致 */
.page-header-card { 
  margin-bottom: 20px; 
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
.actions { position: absolute; right: 20px; top: 16px; z-index: 3; }

/* 主内容区间距与说明折叠样式延用 */
.main-content { margin-bottom: 20px; }
.collapse-title { display: flex; align-items: center; font-size: 16px; color: #303133; }
.info-content { padding: 0 15px; }
.info-content h4 { margin: 15px 0 10px; font-size: 16px; color: #303133; }
.info-content p { margin: 5px 0; color: #606266; }
.info-content ul { margin: 5px 0; padding-left: 20px; color: #606266; }
.info-content li { margin: 5px 0; }
</style>
