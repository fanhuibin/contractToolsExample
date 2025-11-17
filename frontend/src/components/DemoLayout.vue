<template>
  <div class="demo-layout">
    <!-- 顶部导航栏 -->
    <header class="demo-header">
      <div class="header-content">
        <div class="logo">
          <el-icon><Stamp /></el-icon>
          <span>肇新合同组件Demo</span>
        </div>
        <nav class="nav-menu">
          <router-link 
            to="/compare" 
            class="nav-item"
            :class="{ active: currentRoute === 'compare' }"
          >
            <el-icon><ScaleToOriginal /></el-icon>
            <span>智能文档比对</span>
          </router-link>
          <router-link 
            to="/extract-main" 
            class="nav-item"
            :class="{ active: currentRoute === 'extract' }"
          >
            <el-icon><Files /></el-icon>
            <span>智能文档抽取</span>
          </router-link>
          <router-link 
            to="/compose-main" 
            class="nav-item"
            :class="{ active: currentRoute === 'compose' }"
          >
            <el-icon><DocumentCopy /></el-icon>
            <span>智能合同合成</span>
          </router-link>
          <a 
            :href="ZHAOXIN_CONFIG.frontendUrl" 
            target="_blank"
            class="nav-item nav-external"
          >
            <el-icon><Monitor /></el-icon>
            <span>完整组件库</span>
            <el-icon class="external-icon"><TopRight /></el-icon>
          </a>
        </nav>
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="demo-main">
      <!-- 左侧演示文档区 -->
      <aside class="demo-sidebar">
        <div class="sidebar-header">
          <el-icon><FolderOpened /></el-icon>
          <span>演示文档</span>
        </div>
        
        <!-- 功能按钮区域 -->
        <div v-if="category === 'extract' || category === 'compose' || category === 'compare'" class="sidebar-action">
          <!-- 抽取和合成：模板管理 -->
          <el-button 
            v-if="category === 'extract' || category === 'compose'"
            type="primary" 
            @click="handleManageTemplate"
            style="width: 100%"
          >
            <el-icon><Setting /></el-icon>
            模板管理
          </el-button>
          <!-- 比对：新建任务 -->
          <el-button 
            v-if="category === 'compare'"
            type="primary" 
            @click="handleNewTask"
            style="width: 100%"
          >
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
        
        <div class="sidebar-content">
          <el-skeleton v-if="loading" :rows="5" animated />
          <div v-else>
            <div 
              v-for="doc in filteredDocuments" 
              :key="doc.id"
              class="doc-item"
              :class="{ active: selectedDocId === doc.id }"
              @click="handleDocClick(doc)"
            >
              <div class="doc-icon">
                <el-icon><DocumentRemove /></el-icon>
              </div>
              <div class="doc-info">
                <div class="doc-name">{{ doc.name }}</div>
                <div class="doc-desc">{{ doc.description }}</div>
                <div 
                  v-if="(doc.templateCode || doc.templateId) && category !== 'compose'" 
                  class="doc-template"
                >
                  <el-tag v-if="doc.templateCode" size="small" type="success" style="margin-right: 4px">
                    编号: {{ doc.templateCode }}
                  </el-tag>
                  <el-tag v-if="doc.templateId" size="small" type="info">
                    ID: {{ doc.templateId }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-empty 
              v-if="filteredDocuments.length === 0" 
              description="暂无演示文档"
              :image-size="80"
            />
          </div>
        </div>
      </aside>

      <!-- 右侧功能区 -->
      <main class="demo-content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { 
  Stamp, Files, ScaleToOriginal, DocumentCopy, 
  FolderOpened, Document, DocumentRemove, Plus,
  Monitor, TopRight, Setting
} from '@element-plus/icons-vue'
import { getDemoDocuments } from '../api/demo'
import { ElMessage } from 'element-plus'
import { ZHAOXIN_CONFIG } from '../config'

const props = defineProps({
  category: {
    type: String,
    required: true,
    validator: (value) => ['extract', 'compare', 'compose'].includes(value)
  }
})

const emit = defineEmits(['doc-select', 'manage-template', 'new-task'])

const route = useRoute()
const loading = ref(false)
const documents = ref([])
const selectedDocId = ref(null) // 当前选中的文档ID

// 当前路由
const currentRoute = computed(() => {
  if (route.path.includes('extract')) return 'extract'
  if (route.path.includes('compare')) return 'compare'
  if (route.path.includes('compose')) return 'compose'
  return ''
})

// 过滤当前分类的文档
const filteredDocuments = computed(() => {
  return documents.value.filter(doc => doc.category === props.category)
})

// 加载演示文档列表
const loadDocuments = async () => {
  try {
    loading.value = true
    const res = await getDemoDocuments()
    if (res.data.code === 200) {
      documents.value = res.data.data
      console.log('✅ 加载演示文档成功:', documents.value)
    } else {
      throw new Error(res.data.message || '加载失败')
    }
  } catch (error) {
    console.error('❌ 加载演示文档失败:', error)
    ElMessage.error('加载演示文档失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 点击文档
const handleDocClick = (doc) => {
  console.log('📄 选择演示文档:', doc)
  selectedDocId.value = doc.id // 更新选中状态
  emit('doc-select', doc)
}

// 模板管理
const handleManageTemplate = () => {
  console.log('⚙️ 点击模板管理')
  emit('manage-template')
}

// 新建任务
const handleNewTask = () => {
  console.log('📝 点击新建任务')
  selectedDocId.value = null // 清除选中状态
  emit('new-task')
}

onMounted(() => {
  loadDocuments()
  console.log('🔗 完整组件库链接地址:', ZHAOXIN_CONFIG.frontendUrl)
})
</script>

<style scoped lang="scss">
.demo-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

/* 顶部导航栏 */
.demo-header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;

  .header-content {
    width: 100%;
    max-width: 1920px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    color: #303133;
    font-size: 20px;
    font-weight: 600;

    .el-icon {
      font-size: 28px;
      color: #409EFF;
    }
  }

  .nav-menu {
    display: flex;
    gap: 8px;
  }

  .nav-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    color: #606266;
    text-decoration: none;
    border-radius: 6px;
    transition: all 0.3s;
    font-size: 15px;

    .el-icon {
      font-size: 18px;
    }

    &:hover {
      background: #f5f7fa;
      color: #409EFF;
    }

    &.active {
      background: #ecf5ff;
      color: #409EFF;
      font-weight: 500;
    }

    &.nav-external {
      margin-left: 8px;
      border: 1px solid #dcdfe6;
      color: #409EFF;
      
      .external-icon {
        font-size: 14px;
        margin-left: 4px;
      }

      &:hover {
        background: #ecf5ff;
        border-color: #409EFF;
      }
    }
  }
}

/* 主内容区 */
.demo-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  max-width: 1920px;
  width: 100%;
  margin: 0 auto;
}

/* 左侧演示文档区 */
.demo-sidebar {
  width: 320px;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    height: 56px;
    padding: 0 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    border-bottom: 1px solid #e4e7ed;
    background: #fafafa;
    font-size: 16px;
    font-weight: 600;
    color: #303133;

    .el-icon {
      font-size: 20px;
      color: #409EFF;
    }
  }

  .sidebar-action {
    padding: 16px;
    border-bottom: 1px solid #e4e7ed;
    background: #fff;

    .el-button {
      height: 40px;
      font-size: 15px;
      font-weight: 500;
    }
  }

  .sidebar-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 3px;
    }
  }

  .doc-item {
    display: flex;
    gap: 12px;
    padding: 14px;
    margin-bottom: 12px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    background: white;

    &:hover {
      border-color: #409EFF;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
      transform: translateY(-2px);
    }

    /* 选中状态样式 */
    &.active {
      border-color: #409EFF;
      border-width: 2px;
    }

    .doc-icon {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fef0f0;
      border-radius: 8px;
      color: #f56c6c;
      font-size: 20px;
      flex-shrink: 0;
    }

    .doc-info {
      flex: 1;
      min-width: 0;
    }

    .doc-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .doc-desc {
      font-size: 12px;
      color: #909399;
      line-height: 1.4;
      display: -webkit-box;
      line-clamp: 2;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .doc-template {
      margin-top: 6px;

      .el-tag {
        font-size: 11px;
      }
    }
  }
}

/* 右侧功能区 */
.demo-content {
  flex: 1;
  overflow-y: auto;
  background: #f5f7fa;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 4px;
  }
}

/* 响应式适配 */
@media (max-width: 1440px) {
  .demo-sidebar {
    width: 280px;
  }
}

@media (max-width: 1280px) {
  .demo-header {
    .logo {
      font-size: 18px;

      .el-icon {
        font-size: 24px;
      }
    }

    .nav-item {
      padding: 8px 16px;
      font-size: 14px;
    }
  }

  .demo-sidebar {
    width: 260px;
  }
}
</style>

