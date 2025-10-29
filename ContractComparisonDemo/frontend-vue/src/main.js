import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'

// 导入页面组件
import Compare from './views/Compare.vue'
import Result from './views/Result.vue'

// 配置路由
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/compare'
    },
    {
      path: '/compare',
      name: 'Compare',
      component: Compare
    },
    {
      path: '/result/:taskId',
      name: 'Result',
      component: Result
    }
  ]
})

const app = createApp(App)
app.use(router)
app.mount('#app')

