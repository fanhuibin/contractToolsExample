<template>
  <el-container class="layout-container">
    <!-- AI组件 -->
    <AiChat v-model="showAiChat" />
    
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
        <el-menu-item index="/auto-fulfillment">
          <el-icon><Calendar /></el-icon>
          <span>自动履约任务</span>
        </el-menu-item>
        <el-menu-item index="/contract-extract">
          <el-icon><Document /></el-icon>
          <span>合同抽取</span>
        </el-menu-item>
        <el-menu-item index="/info-extract">
          <el-icon><Search /></el-icon>
          <span>智能信息提取</span>
        </el-menu-item>
        <el-menu-item index="/rule-extract">
          <el-icon><Grid /></el-icon>
          <span>规则抽取</span>
        </el-menu-item>
        <el-menu-item index="/contract-review">
          <el-icon><DocumentChecked /></el-icon>
          <span>合同智能审核</span>
        </el-menu-item>
        <el-menu-item index="/onlyoffice">
          <el-icon><Monitor /></el-icon>
          <span>OnlyOffice预览</span>
        </el-menu-item>
        <!-- <el-menu-item index="/compare">
          <el-icon><Files /></el-icon>
          <span>PDF合同比对</span>
        </el-menu-item> -->
        <el-menu-item index="/gpu-ocr-compare">
          <el-icon><DataAnalysis /></el-icon>
          <span>合同比对</span>
        </el-menu-item>
        <el-menu-item index="/compose/start">
          <el-icon><Edit /></el-icon>
          <span>智能合同合成</span>
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
            class="ai-button" 
            @click="showAiChat = true"
          >
            <el-icon><ChatDotRound /></el-icon>
            AI助手
          </el-button>
          

          
          <el-dropdown>
            <span class="user-info">
              管理员 <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人信息</el-dropdown-item>
                <el-dropdown-item>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Document, 
  Files, 
  Monitor, 
  ArrowDown, 
  ChatDotRound, 
  House, 
  Calendar,
  Search,
  DocumentChecked,
  DataAnalysis,
  Edit,
  Grid
} from '@element-plus/icons-vue'
import AiChat from '@/components/ai/AiChat.vue'

const route = useRoute()
const router = useRouter()

// AI组件状态
const showAiChat = ref(false)

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
  gap: 10px;
}

.ai-button {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
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