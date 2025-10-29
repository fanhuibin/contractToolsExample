<template>
  <div class="compose-start">
    <PageHeader 
      title="智能合同合成" 
      description='请选择"新建模板"或"模板库"，完成设计后进入合同合成。'
      :icon="Document"
    />

    <div class="cards">
      <el-card class="card">
        <template #header>
          <div class="card-title">使用新的模板</div>
        </template>
        <div class="cover cover-new">上传 .docx → 填写信息 → 开始设计</div>
        <div class="desc">上传模板文件后填写模板名称与分类，系统将创建模板并进入模板设计页面。</div>
        <div class="actions">
          <el-button type="primary" size="large" @click="goNewTemplate">新建并开始设计</el-button>
        </div>
      </el-card>

      <el-card class="card">
        <template #header>
          <div class="card-title">使用已有模板</div>
        </template>
        <div class="cover cover-existing">从模板库选择 → 设计或直接合成</div>
        <div class="desc">在模板库选择已有模板，可进入“设计模板”编辑已插入元素，或直接发起合同合成。</div>
        <div class="actions">
          <el-button type="primary" size="large" @click="goTemplates">进入模板库</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Document } from '@element-plus/icons-vue'
import { PageHeader } from '@/components/common'

const router = useRouter()
function goNewTemplate() { router.push('/templates/new') }
function goTemplates() { router.push('/templates') }
</script>

<style scoped>
.compose-start { 
  padding: 16px; 
}

.mb12 {
  margin-bottom: 12px;
}

.cards { 
  display: grid; 
  grid-template-columns: 1fr 1fr; 
  gap: var(--zx-spacing-lg); 
}

.card { 
  border-radius: var(--zx-radius-xl); 
}

.card-title { 
  font-weight: var(--zx-font-semibold); 
}

.cover { 
  height: 120px; 
  border-radius: var(--zx-radius-lg); 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-weight: var(--zx-font-semibold); 
  margin-bottom: var(--zx-spacing-md); 
}

.cover-new { 
  background: linear-gradient(135deg, #e6f4ff, #f0f5ff); 
  color: #1677ff; 
}

.cover-existing { 
  background: linear-gradient(135deg, #fffbe6, #fff1f0); 
  color: #fa8c16; 
}

.desc { 
  color: var(--zx-text-regular); 
  margin-bottom: var(--zx-spacing-md); 
}

.actions { 
  display: flex; 
  gap: var(--zx-spacing-md); 
}

@media (max-width: 960px) { 
  .cards { 
    grid-template-columns: 1fr; 
  } 
}
</style>


