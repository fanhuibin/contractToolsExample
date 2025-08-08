<template>
  <el-container class="layout-container">
    <!-- AI组件 -->
    <AiChat v-model="showAiChat" />
    
    <!-- 侧边栏 -->
    <el-aside width="200px" class="aside" v-if="!route.meta?.fullscreen">
      <div class="logo">
        <h2>合同工具集</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/fulfillment">
          <el-icon><Document /></el-icon>
          <span>合同履约任务</span>
        </el-menu-item>
         <el-menu-item index="/contract-extract">
          <el-icon><Document /></el-icon>
          <span>合同抽取</span>
        </el-menu-item>
        <el-menu-item index="/onlyoffice">
          <el-icon><Monitor /></el-icon>
          <span>OnlyOffice预览</span>
        </el-menu-item>
        <el-menu-item index="/template-design">
          <el-icon><Monitor /></el-icon>
          <span>模板设计</span>
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
import { useRoute } from 'vue-router'
import { Document, Files, Folder, Monitor, ArrowDown, ChatDotRound } from '@element-plus/icons-vue'
import AiChat from '@/components/ai/AiChat.vue'

const route = useRoute()

// AI组件状态
const showAiChat = ref(false)

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 当前页面标题
const currentTitle = computed(() => {
  return route.meta?.title || '首页'
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  color: #bfcbd9;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  color: #fff;
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
</style> 