<template>
  <div class="doc-center-wrapper">
    <!-- 独立标题栏 -->
    <div class="doc-header">
      <div class="header-left">
        <el-icon class="logo-icon"><Document /></el-icon>
        <h1>肇新合同工具集 - 文档中心</h1>
      </div>
      <div class="header-right">
        <el-button type="primary" link @click="goToHome">
          <el-icon><HomeFilled /></el-icon>
          返回系统首页
        </el-button>
      </div>
    </div>
    
    <!-- 文档内容区 -->
    <div class="doc-center">
      <div class="doc-nav">
      <el-menu
        :default-active="activeDoc"
        @select="handleDocSelect"
        class="doc-menu"
      >
        <el-sub-menu index="api">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>API 接口文档</span>
          </template>
          <el-menu-item index="api-readme">
            <el-icon><Notebook /></el-icon>
            <span>文档总览</span>
          </el-menu-item>
          <el-menu-item index="api-convert">
            <el-icon><RefreshRight /></el-icon>
            <span>文档格式转换</span>
          </el-menu-item>
          <el-menu-item index="api-compare">
            <el-icon><View /></el-icon>
            <span>智能文档比对</span>
          </el-menu-item>
          <el-menu-item index="api-extract">
            <el-icon><DocumentCopy /></el-icon>
            <span>智能文档抽取</span>
          </el-menu-item>
          <el-menu-item index="api-parse">
            <el-icon><Reading /></el-icon>
            <span>智能文档解析</span>
          </el-menu-item>
          <el-menu-item index="api-compose-api">
            <el-icon><Connection /></el-icon>
            <span>智能合同合成</span>
          </el-menu-item>
          <el-menu-item index="api-compose-features">
            <el-icon><List /></el-icon>
            <span>文档合成功能支持</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="design">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>设计规范</span>
          </template>
          <el-menu-item index="api-design">
            <el-icon><EditPen /></el-icon>
            <span>API设计规范</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="other">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>其他文档</span>
          </template>
          <el-menu-item index="postman">
            <el-icon><Download /></el-icon>
            <span>Postman 使用指南</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    
    <div ref="docContent" class="doc-content">
      <el-card v-loading="loading">
        <template #header>
          <div class="doc-header">
            <h2>{{ currentDocTitle }}</h2>
            <el-button 
              size="small" 
              :icon="CopyDocument"
              @click="copyMarkdown"
            >
              复制Markdown
            </el-button>
          </div>
        </template>
        
        <div 
          ref="markdownContainer"
          class="markdown-body" 
          v-html="renderedMarkdown"
          @click="handleLinkClick"
        ></div>
      </el-card>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Notebook,
  RefreshRight,
  View,
  DocumentCopy,
  Reading,
  PictureRounded,
  Connection,
  List,
  Files,
  Download,
  CopyDocument,
  EditPen,
  HomeFilled
} from '@element-plus/icons-vue'
import { marked } from 'marked'

// 文档映射表
const docMap: Record<string, { title: string; path: string }> = {
  'api-readme': { title: 'API文档总览', path: '/docs/api/README.md' },
  'api-convert': { title: '文档格式转换API', path: '/docs/api/文档格式转换-API文档.md' },
  'api-compare': { title: '智能文档比对API', path: '/docs/api/智能合同比对-API-Documentation.md' },
  'api-extract': { title: '智能文档抽取API', path: '/docs/api/智能文档抽取-API文档.md' },
  'api-parse': { title: '智能文档解析API', path: '/docs/api/智能文档解析-API文档.md' },
  'api-compose-api': { title: '智能合同合成API', path: '/docs/api/智能合同合成-API版本-API文档.md' },
  'api-compose-features': { title: '文档合成功能支持', path: '/docs/api/文档合成功能支持说明.md' },
  'api-design': { title: 'API设计规范', path: '/docs/api/API设计文档.md' },
  'postman': { title: 'Postman Collection 使用指南', path: '/docs/api/Postman-Collection使用指南.md' }
}

const route = useRoute()
const router = useRouter()

const activeDoc = ref<string>('api-readme')
const markdownContent = ref<string>('')
const loading = ref(false)
const markdownContainer = ref<HTMLElement>()
const docContent = ref<HTMLElement>()

const currentDocTitle = computed(() => {
  return docMap[activeDoc.value]?.title || '文档'
})

const renderedMarkdown = computed(() => {
  if (!markdownContent.value) return ''
  
  // 配置 marked 选项
  marked.setOptions({
    breaks: true,
    gfm: true
  })
  
  return marked(markdownContent.value)
})

// 加载文档
const loadDoc = async (docKey: string) => {
  const doc = docMap[docKey]
  if (!doc) return
  
  loading.value = true
  
  try {
    // 加载文档内容
    const response = await fetch(doc.path)
    markdownContent.value = await response.text()
  } catch (error) {
    console.error('加载文档失败:', error)
    markdownContent.value = `# 文档加载失败\n\n无法加载文档：${doc.path}\n\n请确保文档文件存在。`
    ElMessage.error('文档加载失败')
  } finally {
    loading.value = false
    // 滚动到顶部
    await nextTick()
    if (docContent.value) {
      docContent.value.scrollTop = 0
    }
  }
}

// 处理文档选择
const handleDocSelect = (key: string) => {
  activeDoc.value = key
  router.push({ path: '/doc-center', query: { doc: key } })
  loadDoc(key)
}

// 处理链接点击（支持文档内跳转和文档间跳转）
const handleLinkClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'A') {
    const href = target.getAttribute('href')
    if (!href) return
    
    // 处理锚点跳转
    if (href.startsWith('#')) {
      e.preventDefault()
      const id = href.substring(1)
      const element = document.getElementById(id)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      }
      return
    }
    
    // 处理查询参数格式的链接 (?doc=xxx)
    if (href.includes('?doc=')) {
      e.preventDefault()
      try {
        const urlObj = new URL(href, window.location.origin)
        const docKey = urlObj.searchParams.get('doc')
        if (docKey && docMap[docKey]) {
          handleDocSelect(docKey)
        } else {
          ElMessage.warning(`未找到文档`)
        }
      } catch (error) {
        console.error('解析URL失败:', error)
      }
      return
    }
    
    // 处理 Markdown 文件链接
    if (href.endsWith('.md') || href.includes('.md#')) {
      e.preventDefault()
      
      // 提取文件名（去掉 ./ 前缀和锚点）
      let fileName = href.replace('./', '').replace('../', '')
      const hashIndex = fileName.indexOf('#')
      if (hashIndex !== -1) {
        fileName = fileName.substring(0, hashIndex)
      }
      
      // URL 解码文件名（处理中文等特殊字符）
      try {
        fileName = decodeURIComponent(fileName)
      } catch (error) {
        console.error('文件名解码失败:', error)
      }
      
      // 根据文件名查找对应的 docKey
      const docKey = findDocKeyByFileName(fileName)
      if (docKey) {
        handleDocSelect(docKey)
        // 如果有锚点，延迟跳转到锚点位置
        if (hashIndex !== -1) {
          const anchorId = href.substring(hashIndex + 1)
          setTimeout(() => {
            const element = document.getElementById(anchorId)
            if (element) {
              element.scrollIntoView({ behavior: 'smooth' })
            }
          }, 300)
        }
      } else {
        ElMessage.warning(`未找到文档：${fileName}`)
      }
      return
    }
    
    // 外部链接在新标签页打开
    if (href.startsWith('http://') || href.startsWith('https://')) {
      e.preventDefault()
      window.open(href, '_blank')
      return
    }
  }
}

// 根据文件名查找 docKey
const findDocKeyByFileName = (fileName: string): string | null => {
  for (const [key, value] of Object.entries(docMap)) {
    if (value.path.endsWith(fileName)) {
      return key
    }
  }
  return null
}

// 复制 Markdown
const copyMarkdown = () => {
  navigator.clipboard.writeText(markdownContent.value).then(() => {
    ElMessage.success('Markdown内容已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 返回系统首页
const goToHome = () => {
  // 如果是在新窗口中打开的，关闭当前窗口
  if (window.opener) {
    window.close()
  } else {
    // 否则导航到首页
    router.push('/')
  }
}

// 监听路由变化
watch(() => route.query.doc, (newDoc) => {
  if (newDoc && typeof newDoc === 'string') {
    activeDoc.value = newDoc
    loadDoc(newDoc)
  }
}, { immediate: true })

onMounted(() => {
  // 初始加载
  const docKey = (route.query.doc as string) || 'api-readme'
  activeDoc.value = docKey
  loadDoc(docKey)
})
</script>

<style scoped lang="scss">
.doc-center-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.doc-header {
  height: 60px;
  background: #5dade2;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .logo-icon {
      font-size: 28px;
    }
    
    h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }
  
  .header-right {
    :deep(.el-button) {
      color: white;
      font-size: 14px;
      
      &:hover {
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }
}

.doc-center {
  display: flex;
  flex: 1;
  height: calc(100vh - 60px);
  background: #f5f7fa;
  overflow: hidden;
}

.doc-nav {
  width: 280px;
  background: white;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  
  .doc-menu {
    border-right: none;
  }
}

.doc-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  
  :deep(.el-card) {
    min-height: calc(100vh - 100px);
  }
  
  .doc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }
}

.markdown-body {
  padding: 20px 0;
  line-height: 1.6;
  color: #333;
  
  :deep(h1) {
    font-size: 32px;
    font-weight: 600;
    border-bottom: 2px solid #5dade2;
    padding-bottom: 10px;
    margin: 30px 0 20px;
  }
  
  :deep(h2) {
    font-size: 28px;
    font-weight: 600;
    margin: 25px 0 15px;
    color: #5dade2;
  }
  
  :deep(h3) {
    font-size: 24px;
    font-weight: 600;
    margin: 20px 0 12px;
  }
  
  :deep(h4) {
    font-size: 20px;
    font-weight: 600;
    margin: 15px 0 10px;
  }
  
  :deep(p) {
    margin: 12px 0;
    line-height: 1.8;
  }
  
  :deep(code) {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', Courier, monospace;
    font-size: 14px;
    color: #e83e8c;
  }
  
  :deep(pre) {
    background: #282c34;
    color: #abb2bf;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 16px 0;
    
    code {
      background: transparent;
      color: inherit;
      padding: 0;
    }
  }
  
  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    
    th, td {
      border: 1px solid #ddd;
      padding: 10px;
      text-align: left;
    }
    
    th {
      background: #5dade2;
      color: white;
      font-weight: 600;
    }
    
    tr:nth-child(even) {
      background: #f5f7fa;
    }
  }
  
  :deep(blockquote) {
    border-left: 4px solid #5dade2;
    padding-left: 16px;
    margin: 16px 0;
    color: #666;
    background: #f5f7fa;
    padding: 12px 16px;
    border-radius: 4px;
  }
  
  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin: 12px 0;
    
    li {
      margin: 6px 0;
      line-height: 1.8;
    }
  }
  
  :deep(a) {
    color: #5dade2;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
  
  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
    margin: 16px 0;
  }
  
  :deep(hr) {
    border: none;
    border-top: 2px solid #e4e7ed;
    margin: 24px 0;
  }
}

</style>

