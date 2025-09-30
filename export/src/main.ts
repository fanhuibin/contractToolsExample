import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import GPUOCRCanvasCompareResult from './views/GPUOCRCanvasCompareResult.vue'

// 创建路由
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/gpu-ocr-canvas-compare-result'
    },
    {
      path: '/gpu-ocr-canvas-compare-result',
      name: 'GPUOCRCanvasCompareResult',
      component: GPUOCRCanvasCompareResult
    }
  ]
})

// 创建应用
const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)
app.mount('#app')
