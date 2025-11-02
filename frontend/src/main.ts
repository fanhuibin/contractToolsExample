import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 导入全局设计系统
import './styles/index.scss'

import App from './App.vue'
import router from './router'

// 导入全局通用组件
import { GlobalBackButton } from './components/common'

// 导入嵌入模式插件
import { setupEmbedModePlugin } from './utils/embed-mode-plugin'

const app = createApp(App)

// 注册全局通用组件
app.component('GlobalBackButton', GlobalBackButton)

// 安装嵌入模式插件（拦截 router.back()）
setupEmbedModePlugin(router)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

app.mount('#app') 