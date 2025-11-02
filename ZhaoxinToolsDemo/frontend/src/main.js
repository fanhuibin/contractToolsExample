import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import './style.css'

// 导入页面组件
import ExtractMain from './views/ExtractMain.vue'

// 配置路由
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/extract-main'
    },
    {
      path: '/extract-main',
      name: 'ExtractMain',
      component: ExtractMain
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
app.mount('#app')

