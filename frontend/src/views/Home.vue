<template>
  <div class="home-page">
    <div class="hero-section">
      <h1>肇新合同组件Demo</h1>
      <p class="subtitle">体验智能文档处理的强大功能</p>
    </div>

    <div class="features-grid">
      <el-card class="feature-card" @click="navigateTo('/compare')">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon" color="#67C23A"><DataAnalysis /></el-icon>
            <span>智能文档比对</span>
          </div>
        </template>
        <p>快速比对两个文档的差异，生成高亮标注的比对报告</p>
        <div class="feature-tags">
          <el-tag size="small">智能去水印</el-tag>
          <el-tag size="small" type="success">GPU加速</el-tag>
          <el-tag size="small" type="danger">差异分析</el-tag>
        </div>
      </el-card>

      <el-card class="feature-card" @click="navigateTo('/extract-main')">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon" color="#409EFF"><Grid /></el-icon>
            <span>智能文档抽取</span>
          </div>
        </template>
        <p>基于规则引擎从文档中自动提取结构化信息</p>
        <div class="feature-tags">
          <el-tag size="small">规则引擎</el-tag>
          <el-tag size="small" type="success">OCR识别</el-tag>
          <el-tag size="small" type="warning">AI辅助</el-tag>
        </div>
      </el-card>

      <el-card class="feature-card" @click="navigateTo('/compose-main')">
        <template #header>
          <div class="card-header">
            <el-icon class="feature-icon" color="#E6A23C"><Edit /></el-icon>
            <span>智能合同合成</span>
          </div>
        </template>
        <p>基于模板自动生成合同文档，支持富文本和动态数据填充</p>
        <div class="feature-tags">
          <el-tag size="small">可视化设计</el-tag>
          <el-tag size="small" type="success">富文本</el-tag>
          <el-tag size="small" type="warning">动态表格</el-tag>
        </div>
      </el-card>
    </div>

    <div class="info-section">
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        show-icon
      >
        点击卡片即可进入相应功能的演示页面。本 Demo 展示了如何通过 API 和 iframe 集成肇新智能工具集。
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Grid, DataAnalysis, Edit } from '@element-plus/icons-vue'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;

  .hero-section {
    text-align: center;
    color: white;
    margin-bottom: 60px;

    h1 {
      font-size: 48px;
      font-weight: 700;
      margin-bottom: 16px;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
    }

    .subtitle {
      font-size: 20px;
      opacity: 0.9;
    }
  }

  .features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 30px;
    max-width: 1200px;
    width: 100%;
    margin-bottom: 40px;

    .feature-card {
      cursor: pointer;
      transition: all 0.3s;
      border-radius: 12px;
      overflow: hidden;

      &:hover {
        transform: translateY(-8px);
        box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
      }

      .card-header {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 18px;
        font-weight: 600;
        color: #303133;

        .feature-icon {
          font-size: 28px;
        }
      }

      p {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 16px;
        min-height: 48px;
      }

      .feature-tags {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
    }
  }

  .info-section {
    max-width: 800px;
    width: 100%;

    .el-alert {
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.95);
    }
  }
}

@media (max-width: 768px) {
  .home-page {
    .hero-section {
      h1 {
        font-size: 32px;
      }

      .subtitle {
        font-size: 16px;
      }
    }

    .features-grid {
      grid-template-columns: 1fr;
      gap: 20px;
    }
  }
}
</style>

