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
    port: 3003,  // 使用3003端口，避免与肇新前端(3000)和比对Demo(3002)冲突
    proxy: {
      '/api': {
        target: 'http://localhost:8091',  // Demo后端地址
        changeOrigin: true,
      }
    }
  }
})

