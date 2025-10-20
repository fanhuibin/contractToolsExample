<template>
  <el-container class="layout-container">
    <!-- 侧边栏（使用 Element Plus 菜单） -->
    <el-aside width="220px" class="aside" v-if="!route.meta?.fullscreen && !route.meta?.hideAside">
      <div class="logo">
        <h2>合同工具集</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/rule-extract">
          <el-icon><Grid /></el-icon>
          <span>智能文档抽取</span>
        </el-menu-item>
        <el-menu-item index="/gpu-ocr-compare">
          <el-icon><DataAnalysis /></el-icon>
          <span>智能文档比对</span>
        </el-menu-item>
        <el-menu-item index="/compose/start">
          <el-icon><Edit /></el-icon>
          <span>智能合同合成</span>
        </el-menu-item>
        <el-menu-item index="/ocr-extract">
          <el-icon><Reading /></el-icon>
          <span>智能文档解析</span>
        </el-menu-item>
        <el-menu-item index="/onlyoffice">
          <el-icon><Monitor /></el-icon>
          <span>文档在线编辑</span>
        </el-menu-item>
        <el-menu-item index="/document-convert">
          <el-icon><Refresh /></el-icon>
          <span>文档格式转换</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container :class="{ 'no-padding': route.meta?.fullscreen }">
      <!-- 头部 -->
      <el-header class="header" v-if="!route.meta?.fullscreen">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button 
            type="primary" 
            link 
            @click="openUrl('http://zhaoxinms.com')"
          >
            <el-icon><Reading /></el-icon>
            文档中心
          </el-button>
        <el-button
          type="primary"
          link
          @click="router.push('/license')"
        >
          <el-icon><Key /></el-icon>
          授权信息
        </el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main" :class="{ fullscreen: route.meta?.fullscreen }">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Document, 
  Monitor, 
  House, 
  DataAnalysis,
  Edit,
  Grid,
  Reading,
  Refresh,
  Key
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 当前页面标题
const currentTitle = computed(() => {
  return route.meta?.title || '首页'
})

// 菜单选择处理
function handleMenuSelect(index: string) {
  router.push(index)
}

// 打开外部链接
function openUrl(url: string) {
  window.open(url, '_blank')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: var(--zx-bg-white);
  border-right: 1px solid var(--zx-border-lighter);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--zx-bg-white);
  color: var(--zx-text-primary);
  border-bottom: 1px solid var(--zx-border-lighter);
}

.logo h2 {
  margin: 0;
  font-size: var(--zx-font-xl);
  font-weight: var(--zx-font-semibold);
  color: var(--zx-primary);
}

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right :deep(.el-button) {
  font-size: 14px;
}

.header-right :deep(.el-button .el-icon) {
  margin-right: 4px;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}

.main.fullscreen {
  padding: 0;
}

.no-padding {
  padding: 0;
}

/* Element Plus 菜单样式增强 */
:deep(.el-menu-item) {
  transition: all var(--zx-transition-base);
}

:deep(.el-menu-item:hover) {
  background-color: var(--zx-primary-light-9);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--zx-primary-light-8);
  color: var(--zx-primary);
  font-weight: var(--zx-font-medium);
}
</style> 