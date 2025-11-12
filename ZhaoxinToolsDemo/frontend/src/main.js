import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import './style.css'
import { loadConfig } from './config'

// 导入页面组件
import ExtractMain from './views/ExtractMain.vue'
import Compare from './views/Compare.vue'
import CompareResult from './views/CompareResult.vue'
import ComposeMain from './views/ComposeMain.vue'

// 配置路由
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/compare'
    },
    {
      path: '/extract-main',
      name: 'ExtractMain',
      component: ExtractMain
    },
    {
      path: '/compare',
      name: 'Compare',
      component: Compare
    },
    {
      path: '/compare/result/:taskId',
      name: 'CompareResult',
      component: CompareResult
    },
    {
      path: '/compose-main',
      name: 'ComposeMain',
      component: ComposeMain
    }
    // 已移除的路由（改用弹窗模式）：
    // - /extract/result/:taskId (ExtractResult)
    // - /template-manage (TemplateManage)
    // - /template-design/:id (TemplateDesign)
    // - /extract (Extract.vue 已删除，使用 ExtractMain 替代)
  ]
})

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

// 加载配置后挂载应用
loadConfig().then(() => {
  app.mount('#app')
}).catch((error) => {
  console.error('配置加载失败，使用默认配置:', error)
  app.mount('#app')
})

