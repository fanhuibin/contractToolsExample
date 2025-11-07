<template>
  <div class="home-page">
    <!-- 英雄区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">肇新合同组件库</h1>
        <p class="hero-subtitle">
          集成深度学习版面分析、自然语言处理与规则引擎等核心技术，
          提供文档抽取、智能比对、合同合成、结构化解析等全栈文档处理能力，
          助力企业实现文档自动化与智能化管理
        </p>
      </div>
             <div class="hero-image">
         <img src="/images/hero-image-new.png" alt="合同工具集" class="hero-img" />
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

    <!-- 页脚版权区域 -->
    <div class="footer-section">
      <div class="footer-content">
        <p class="copyright">© 2025 肇新科技. 保留所有权利.</p>
        <div class="footer-links">
          <a href="https://zhaoxinms.com" target="_blank" class="footer-link">官方网站</a>
          <span class="divider">|</span>
          <a @click="openDocCenter" class="footer-link">文档中心</a>
        </div>
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
    title: '智能文档抽取',
    description: '融合深度学习版面分析、语义理解与规则引擎，实现关键信息精准定位与结构化抽取，支持自定义抽取模板与多格式文档解析。',
    image: '/images/contract-extract.webp',
    button_text: '进入功能',
    link: '/rule-extract'
  },
  {
    title: '智能文档比对',
    description: '基于文本语义分析与差异检测算法，实现文档逐字逐句智能比对，支持可视化差异标注、版本追溯与比对报告导出。',
    image: '/images/ocr-compare.webp',
    button_text: '进入功能',
    link: '/gpu-ocr-compare'
  },
  {
    title: '智能合同合成',
    description: '采用模板引擎与变量映射技术，通过智能占位符自动填充实现合同快速生成，支持多模板管理与批量合成。',
    image: '/images/contract-compose.webp',
    button_text: '进入功能',
    link: '/templates'
  },
  {
    title: '智能文档解析',
    description: '运用AI智能识别技术，结合版面分析与文本处理，实现高精度文档内容提取，支持图文混排与结构化输出。',
    image: '/images/auto-fulfillment.webp',
    button_text: '进入功能',
    link: '/ocr-extract'
  },
  {
    title: '文档在线编辑',
    description: '提供在线文档协作编辑能力，支持Word、Excel、PPT等多种格式的实时编辑、版本管理与多人协同。',
    image: '/images/contract-compose.webp',
    button_text: '进入功能',
    link: '/onlyoffice'
  }
])

// 特色功能区域已移除

const handleServiceClick = (service: any) => {
  router.push(service.link)
}

const handleServiceAction = (service: any) => {
  router.push(service.link)
}

// 打开文档中心
const openDocCenter = () => {
  window.open('/doc-center', '_blank')
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
  grid-template-columns: 1fr 1.6fr;
  gap: var(--zx-spacing-4xl);
  align-items: center;
  padding: var(--zx-spacing-4xl) 0;
  min-height: auto;
  max-width: var(--zx-container-2xl);
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

.hero-image .hero-img {
  max-width: 100%;
  max-height: 450px;
  width: auto;
  height: auto;
  object-fit: contain;
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

/* 页脚版权区域 */
.footer-section {
  margin-top: var(--zx-spacing-5xl);
  padding: var(--zx-spacing-4xl) 0;
  border-top: 1px solid var(--zx-border-lighter);
}

.footer-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zx-spacing-lg);
}

.copyright {
  margin: 0;
  font-size: var(--zx-font-sm);
  color: var(--zx-text-secondary);
}

.footer-links {
  display: flex;
  align-items: center;
  gap: var(--zx-spacing-md);
}

.footer-link {
  font-size: var(--zx-font-sm);
  color: var(--zx-text-regular);
  text-decoration: none;
  cursor: pointer;
  transition: color var(--zx-transition-base);
}

.footer-link:hover {
  color: var(--zx-primary);
}

.divider {
  color: var(--zx-text-placeholder);
  font-size: var(--zx-font-sm);
}

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
  
  .hero-image .hero-img {
    max-width: 90%;
    max-height: 350px;
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
  
  .hero-image .hero-img {
    max-width: 95%;
    max-height: 300px;
  }
  
  .services-grid {
    grid-template-columns: 1fr;
  }
}
</style>
