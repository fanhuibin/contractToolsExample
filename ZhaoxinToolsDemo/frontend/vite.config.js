import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3004,  // 使用3004端口，避免与肇新前端(3000)和其他Demo冲突
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 肇新SDK后端地址
        changeOrigin: true,
      }
    }
  }
})

