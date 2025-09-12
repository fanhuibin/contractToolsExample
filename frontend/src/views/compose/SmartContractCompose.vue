<template>
  <div class="smart-compose">
    <el-card class="page-header-card">
      <div class="page-header">
        <div class="header-content">
          <h2><i class="el-icon header-icon"></i>智能合同合成</h2>
          <p>按步骤完成：模板管理 → 模板设计 → 合同合成。</p>
        </div>
        <div class="header-decoration"></div>
      </div>
    </el-card>

    <div class="steps-wrapper">
      <a-steps :current="currentStep" size="small" :items="steps" @change="onStepChange" progress-dot />
    </div>

    <div class="steps-content">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <span>点击步骤卡点将跳转至对应页面</span>
        </template>
      </el-alert>
      <div class="step-actions">
        <el-button type="primary" @click="goCurrent">进入当前步骤</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 0: 模板管理 -> /templates
// 1: 模板设计 -> /template-design
// 2: 合同合成 -> /contract-compose
const currentStep = ref<number>(Number(route.query.step || 0))

const steps = computed(() => [
  { title: '模板管理', description: '创建/选择模板' },
  { title: '模板设计', description: '插入元素与占位符' },
  { title: '合同合成', description: '填写字段合成生成' }
])

function onStepChange(idx: number) {
  currentStep.value = idx
  goCurrent()
}

function goCurrent() {
  switch (currentStep.value) {
    case 0:
      router.push('/templates')
      break
    case 1:
      router.push('/template-design')
      break
    case 2:
      router.push('/contract-compose')
      break
  }
}
</script>

<style scoped>
.smart-compose { padding: 16px; }
.steps-wrapper { background:#fff; padding: 12px 16px; border-radius: 8px; margin-top: 12px; border: 1px solid #ebeef5; }
.steps-content { margin-top: 12px; }
.step-actions { margin-top: 12px; }

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

/* 使用 AntD 默认 progress-dot 风格，无需自定义点样式 */
</style>


