import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'

// 导入页面组件
import Extract from './views/Extract.vue'

// 配置路由
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/extract'
    },
    {
      path: '/extract',
      name: 'Extract',
      component: Extract
    }
    // 已移除的路由（改用弹窗模式）：
    // - /extract/result/:taskId (ExtractResult)
    // - /template-manage (TemplateManage)
    // - /template-design/:id (TemplateDesign)
  ]
})

const app = createApp(App)
app.use(router)
app.mount('#app')

