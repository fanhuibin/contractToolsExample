<template>
  <div class="home-page">
    <!-- 英雄区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">肇新科技合同组件</h1>
                 <p class="hero-subtitle">智能合同处理引擎，助力企业数字化转型，提升合同管理效率</p>
                 
      </div>
             <div class="hero-image">
         <img src="/images/imgi_3_theme-index-blue.c38b733.png" alt="合同工具集" />
       </div>
    </div>

    

    <!-- 服务展示区域 -->
    <div class="services-section">
      <h2 class="section-title">合同组件</h2>
      <div class="services-grid">
        <ServiceCard 
          v-for="service in services" 
          :key="service.title"
          :service="service"
          @card-click="handleServiceClick"
          @button-click="handleServiceAction"
        />
      </div>
    </div>

    
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// 无需图标导入（特色功能区域已移除）
import ServiceCard from '@/components/home/ServiceCard.vue'

const router = useRouter()

// 删除统计数据区，无需对应数据

// 服务数据（与图片命名、功能路径一一对应，桌面端四列一行）
const services = ref([
  {
    title: '合同智能审核',
    description: 'AI 识别风险条款与整改建议，支持锚点定位与联动。',
    image: '/images/contract-review.webp',
    button_text: '进入功能',
    link: '/contract-review'
  },
  {
    title: '合同合成',
    description: '基于模板的SDT占位符自动填充，快速生成标准合同。',
    image: '/images/contract-compose.webp',
    button_text: '进入功能',
    link: '/contract-compose'
  },
  {
    title: '合同比对',
    description: '基于加速的识别和智能比对，提供更快的处理速度和更高的准确率。',
    image: '/images/ocr-compare.webp',
    button_text: '进入功能',
    link: '/gpu-ocr-compare'
  },
  {
    title: '合同信息提取',
    description: '提取合同关键信息字段并规范化输出，支持多模板。',
    image: '/images/contract-extract.webp',
    button_text: '进入功能',
    link: '/contract-extract'
  },
  {
    title: 'OCR文本提取',
    description: 'PDF文档OCR识别，支持页眉页脚过滤，图文对照显示。',
    image: '/images/ocr-extract.webp',
    button_text: '进入功能',
    link: '/ocr-extract'
  }
])

// 特色功能区域已移除

const handleServiceClick = (service: any) => {
  router.push(service.link)
}

const handleServiceAction = (service: any) => {
  router.push(service.link)
}


</script>

<style scoped>
.home-page {
  max-width: var(--zx-container-2xl);
  margin: 0 auto;
  padding: 0 var(--zx-spacing-xl);
}

/* 英雄区域 */
.hero-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--zx-spacing-5xl);
  align-items: center;
  padding: var(--zx-spacing-5xl) 0;
  min-height: 500px;
  max-width: var(--zx-container-xl);
  margin: 0 auto;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: var(--zx-spacing-2xl);
}

.hero-title {
  font-size: var(--zx-font-5xl);
  font-weight: var(--zx-font-bold);
  color: var(--zx-text-primary);
  line-height: var(--zx-leading-tight);
  margin: 0;
}

.hero-subtitle {
  font-size: var(--zx-font-xl);
  color: var(--zx-text-regular);
  line-height: var(--zx-leading-relaxed);
  margin: 0;
}



.hero-image {
  display: flex;
  justify-content: center;
  align-items: center;
}

.hero-image img {
  max-width: 100%;
  height: auto;
  border-radius: var(--zx-radius-lg);
}

/* 服务展示区域 */
.services-section {
  margin: var(--zx-spacing-5xl) 0;
  width: 100%;
}

.section-title {
  font-size: var(--zx-font-4xl);
  font-weight: var(--zx-font-semibold);
  color: var(--zx-text-primary);
  text-align: center;
  margin-bottom: var(--zx-spacing-4xl);
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--zx-spacing-3xl);
  max-width: 100%;
}

/* 特色功能区域已移除 */

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: var(--zx-spacing-3xl);
    padding: var(--zx-spacing-3xl) 0;
    text-align: center;
  }
  
  .hero-title {
    font-size: var(--zx-font-4xl);
  }
  
  .services-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 0 var(--zx-spacing-md);
  }
  
  .hero-title {
    font-size: var(--zx-font-3xl);
  }
  
  .hero-subtitle {
    font-size: var(--zx-font-lg);
  }
  
  .services-grid {
    grid-template-columns: 1fr;
  }
}
</style>
