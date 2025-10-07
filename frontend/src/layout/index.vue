<template>
  <el-container class="layout-container">
    <!-- AI组件 -->
    <AiChat v-model="showAiChat" />
    
    <!-- 侧边栏（改为 Ant Design Vue 菜单） -->
    <el-aside width="220px" class="aside" v-if="!route.meta?.fullscreen && !route.meta?.hideAside">
      <div class="logo">
        <h2>合同工具集</h2>
      </div>
      <a-menu
        mode="inline"
        :selectedKeys="[activeMenu]"
        :items="menuItems"
        @click="onMenuClick"
        style="height: calc(100vh - 60px); overflow: auto;"
      />
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
import { ref, computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Files, Folder, Monitor, ArrowDown, ChatDotRound, House } from '@element-plus/icons-vue'
import AiChat from '@/components/ai/AiChat.vue'
import { HomeOutlined, FileTextOutlined, FileSearchOutlined, ApartmentOutlined, ProfileOutlined, SnippetsOutlined } from '@ant-design/icons-vue'

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

// AntD 菜单数据
const menuItems = [
  { key: '/home', icon: () => h(HomeOutlined), label: '首页' },
  { key: '/auto-fulfillment', icon: () => h(FileSearchOutlined), label: '自动履约任务' },
  { key: '/contract-extract', icon: () => h(FileTextOutlined), label: '合同抽取' },
  { key: '/info-extract', icon: () => h(FileSearchOutlined), label: '智能信息提取' },
  { key: '/contract-review', icon: () => h(ProfileOutlined), label: '合同智能审核' },
  { key: '/onlyoffice', icon: () => h(ApartmentOutlined), label: 'OnlyOffice预览' },
  { key: '/compare', icon: () => h(SnippetsOutlined), label: 'PDF合同比对' },
  { key: '/gpu-ocr-compare', icon: () => h(FileSearchOutlined), label: 'GPU合同比对' },
  // 用单一入口"智能合同合成"替换三项：模板管理/模板设计/合同合成
  { key: '/compose/start', icon: () => h(SnippetsOutlined), label: '智能合同合成' }
]

function onMenuClick(info: any) {
  const key = info?.key
  if (typeof key === 'string') {
    router.push(key)
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #fff;
  color: rgba(0, 0, 0, 0.88);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  color: rgba(0, 0, 0, 0.88);
  border-bottom: 1px solid #f0f0f0;
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.menu {
  border: none;
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

/* AntD 菜单风格微调：去掉右侧分割线 */
:deep(.ant-menu-inline) {
  border-right: 0;
}
</style> 