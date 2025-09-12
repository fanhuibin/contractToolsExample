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
    title: 'GPU合同比对',
    description: '基于GPU加速的OCR识别和智能比对，提供更快的处理速度和更高的准确率。',
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
    title: '自动履约任务',
    description: '基于关键词与规则识别履约事项，分组管理与提醒。',
    image: '/images/auto-fulfillment.webp',
    button_text: '进入功能',
    link: '/auto-fulfillment'
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
  max-width: 1600px; /* 增加页面最大宽度，为合同组件区域提供更多空间 */
  margin: 0 auto;
  padding: 0 20px;
}

/* 英雄区域 */
.hero-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  padding: 80px 0;
  min-height: 500px;
  max-width: 1200px; /* 限制英雄区域最大宽度，保持原有尺寸 */
  margin: 0 auto; /* 英雄区域居中 */
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  margin: 0;
}

.hero-subtitle {
  font-size: 18px;
  color: #606266;
  line-height: 1.6;
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
  border-radius: 8px;
}

/* 统计数据区域已移除 */

/* 服务展示区域 */
.services-section {
  margin: 60px 0;
  width: 100%; /* 确保服务区域占满可用宽度 */
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  margin-bottom: 48px;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr); /* 桌面端固定5列 */
  gap: 32px; /* 增加卡片间距，让整体更宽 */
  max-width: 100%; /* 确保网格占满容器宽度 */
}

/* 特色功能区域已移除 */

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 40px 0;
    text-align: center;
  }
  
  .hero-title {
    font-size: 36px;
  }
  

  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
  
  .stat-item {
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }
  
  .services-grid {
    grid-template-columns: repeat(2, 1fr); /* 平板两列 */
  }
  
  .features-grid {
    grid-template-columns: 1fr;
    padding: 0 20px;
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 0 12px;
  }
  
  .hero-title {
    font-size: 28px;
  }
  
  .hero-subtitle {
    font-size: 16px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-section {
    padding: 24px;
  }
}
</style>
