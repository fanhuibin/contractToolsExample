<template>
  <div class="compose-start">
    <el-card class="page-header-card">
      <div class="page-header">
        <div class="header-content">
          <h2><el-icon class="header-icon"><Document /></el-icon>智能合同合成</h2>
          <p>请选择“新建模板”或“模板库”，完成设计后进入合同合成。</p>
        </div>
        <div class="header-decoration"></div>
      </div>
    </el-card>
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
const router = useRouter()
function goNewTemplate() { router.push('/templates/new') }
function goTemplates() { router.push('/templates') }
</script>

<style scoped>
.compose-start { padding: 16px; }
.page-header-card { 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  overflow: hidden;
  transition: all 0.3s ease;
  margin-bottom: 12px;
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
.cards { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.card { border-radius: 10px; }
.card-title { font-weight: 600; }
.cover { height: 120px; border-radius: 8px; display:flex; align-items:center; justify-content:center; font-weight: 600; margin-bottom: 12px; }
.cover-new { background: linear-gradient(135deg,#e6f4ff,#f0f5ff); color:#1677ff; }
.cover-existing { background: linear-gradient(135deg,#fffbe6,#fff1f0); color:#fa8c16; }
.desc { color: #606266; margin-bottom: 12px; }
.actions { display:flex; gap: 12px; }
@media (max-width: 960px) { .cards { grid-template-columns: 1fr; } }
</style>


